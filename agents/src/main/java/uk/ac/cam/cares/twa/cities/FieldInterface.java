package uk.ac.cam.cares.twa.cities;

import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;

import java.io.InvalidClassException;
import java.lang.reflect.*;
import java.net.URI;
import java.util.List;

@FunctionalInterface
interface Parser {
  Object parse(JSONObject row, KnowledgeBaseClientInterface kgClient, int recursiveInstantiationDepth) throws Exception;
}

@FunctionalInterface
interface Putter {
  void consume(Object object, Object value) throws Exception;
}

@FunctionalInterface
interface LiteralGetter {
  String get(Object object) throws Exception;
}

public class FieldInterface {
  public final boolean isList;
  public final boolean isModel;
  // Access for the field itself
  public final Field field;
  public final Method getter;
  public final Method setter;
  // Functions for consuming updates from the database
  private final Parser parser;
  private final Putter putter;
  // Function for generating string representations of literals to push to the database
  private final LiteralGetter literalGetter;
  // List constructor if isList
  private final Constructor<?> listConstructor;

  public FieldInterface(Field field) throws NoSuchMethodException, InvalidClassException {
    this.field = field;
    // Determine characteristics of field
    Class<?> parentType = field.getDeclaringClass();
    Class<?> outerType = field.getType();
    isList = List.class.isAssignableFrom(outerType);
    Class<?> innerType = isList ? field.getAnnotation(ModelField.class).innerType() : outerType;
    isModel = Model.class.isAssignableFrom(innerType);
    // Get the lombok accessors/modifiers --- we can't access private/protected fields. :(
    String fieldName = field.getName();
    fieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    getter = parentType.getMethod("get" + fieldName);
    setter = parentType.getMethod("set" + fieldName, outerType);
    // Generate parser
    if (isModel) {
      Constructor<?> constructor = innerType.getConstructor();
      parser = (JSONObject row, KnowledgeBaseClientInterface kgClient, int recursiveInstantiationDepth) -> {
        Model model = (Model) constructor.newInstance();
        if (recursiveInstantiationDepth > 0)
          model.pullAll(row.getString(Model.VALUE), kgClient, recursiveInstantiationDepth - 1);
        else
          model.setIri(URI.create(row.getString(Model.VALUE)));
        return model;
      };
      literalGetter = (Object value) -> String.format("<%s>", ((Model) value).getIri());
    } else if (innerType == Integer.class) {
      parser = (JSONObject row, KnowledgeBaseClientInterface kgc, int rid) -> row.getInt(Model.VALUE);
      literalGetter = (Object value) -> String.format("\"%s\"^^xsd:integer", String.valueOf((int) value));
    } else if (innerType == Double.class) {
      parser = (JSONObject row, KnowledgeBaseClientInterface kgc, int rid) -> row.getDouble(Model.VALUE);
      literalGetter = (Object value) -> String.format("\"%s\"^^xsd:double", String.valueOf((double) value));
    } else if (innerType == String.class) {
      parser = (JSONObject row, KnowledgeBaseClientInterface kgc, int rid) -> row.getString(Model.VALUE);
      literalGetter = (Object value) -> String.format("\"%s\"^^xsd:string", (String) value);
    } else if (innerType == URI.class) {
      parser = (JSONObject row, KnowledgeBaseClientInterface kgc, int rid) -> URI.create(row.getString(Model.VALUE));
      literalGetter = (Object value) -> String.format("<%s>", (URI) value);
    } else if (DatatypeModel.class.isAssignableFrom(innerType)) {
      Constructor<?> constructor = innerType.getConstructor(String.class, String.class);
      parser = (JSONObject row, KnowledgeBaseClientInterface kgc, int rid)
          -> constructor.newInstance(row.getString(Model.VALUE), row.getString(Model.DATATYPE));
      literalGetter = (Object value) -> ((DatatypeModel) value).getLiteralString();
    } else {
      throw new InvalidClassException(innerType.toString(), "Class not supported.");
    }
    // Generate pusher
    if (isList) {
      // Due to type erasure, ArrayLists accept Objects, not innerTypes.
      Method adder = outerType.getMethod("add", Object.class);
      putter = (Object object, Object value) -> adder.invoke(getter.invoke(object), value);
      // Override previously assigned default constructor, which was for innerType
      listConstructor = outerType.getConstructor();
    } else {
      putter = (Object object, Object value) -> setter.invoke(object, value);
      listConstructor = null;
    }
  }

  /**
   * Populates the associated field of the object with the value in the row.
   */
  public void pull(Object object, JSONObject row, KnowledgeBaseClientInterface kgClient, int recursiveInstantiationDepth) {
    try {
      putter.consume(object, parser.parse(row, kgClient, recursiveInstantiationDepth));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void copy(Object from, Object to) {
    try {
      setter.invoke(to, getter.invoke(from));
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new JPSRuntimeException(e);
    }
  }

  public boolean equals(Object first, Object second) {
    try {
      return getter.invoke(first) == getter.invoke(second);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new JPSRuntimeException(e);
    }
  }

  /**
   * Clears the associated field of the object with the value in the row.
   */
  public void clear(Object object) {
    try {
      setter.invoke(object, isList ? listConstructor.newInstance() : null);
    } catch (Exception e) {
      throw new JPSRuntimeException(e);
    }
  }

  /**
   * Gets the string representation of the current field value for pushing to the database.
   */
  public String getLiteral(Object object) {
    try {
      Object value = getter.invoke(object);
      return value == null ? "[]" : literalGetter.get(value);
    } catch (Exception e) {
      throw new JPSRuntimeException(e);
    }
  }

}