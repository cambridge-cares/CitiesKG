package uk.ac.cam.cares.twa.cities;

import org.json.JSONObject;
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
interface Pusher {
  void push(Object object, Object value) throws Exception;
}

@FunctionalInterface
interface DefaultConstructor {
  Object get() throws Exception;
}

public class FieldPopulator {
  public final Field field;
  public final Method getter;
  public final Method setter;
  private Parser parser;
  private Pusher pusher;
  private DefaultConstructor defaultConstructor;

  public FieldPopulator(Field field) throws NoSuchMethodException, InvalidClassException {
    this.field = field;
    // Determine characteristics of field
    Class<?> parentType = field.getDeclaringClass();
    Class<?> outerType = field.getType();
    boolean isList = List.class.isAssignableFrom(outerType);
    Class<?> innerType = isList ? field.getAnnotation(ModelField.class).innerType() : outerType;
    boolean isModel = Model.class.isAssignableFrom(innerType);
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
          model.populateAll(row.getString(Model.VALUE), kgClient, recursiveInstantiationDepth - 1);
        else
          model.setIri(URI.create(row.getString(Model.VALUE)));
        return model;
      };
      defaultConstructor = () -> null;
    } else if (innerType == int.class) {
      parser = (JSONObject row, KnowledgeBaseClientInterface kgc, int rid) -> row.getInt(Model.VALUE);
      defaultConstructor = () -> 0;
    } else if (innerType == double.class) {
      parser = (JSONObject row, KnowledgeBaseClientInterface kgc, int rid) -> row.getDouble(Model.VALUE);
      defaultConstructor = () -> 0;
    } else if (innerType == String.class) {
      parser = (JSONObject row, KnowledgeBaseClientInterface kgc, int rid) -> row.getString(Model.VALUE);
      defaultConstructor = () -> null;
    } else if (innerType == URI.class) {
      parser = (JSONObject row, KnowledgeBaseClientInterface kgc, int rid) -> URI.create(row.getString(Model.VALUE));
      defaultConstructor = () -> null;
    } else {
      throw new InvalidClassException(innerType.toString(), "Class not supported.");
    }
    // Generate pusher
    if (isList) {
      // Due to type erasure, ArrayLists accept Objects, not innerTypes.
      Method adder = outerType.getMethod("add", Object.class);
      pusher = (Object object, Object value) -> adder.invoke(getter.invoke(object), value);
      // Override previously assigned default constructor, which was for innerType
      defaultConstructor = outerType.getConstructor()::newInstance;
    } else {
      pusher = (Object object, Object value) -> setter.invoke(object, value);
    }
    System.err.println("innerType: " + innerType + ", outerType: " + outerType + ", isList: " + isList + ", isModel: " + isModel);
  }

  /**
   * Populates the associated field of the object with the value in the row.
   */
  public void populate(Object object, JSONObject row, KnowledgeBaseClientInterface kgClient, int recursiveInstantiationDepth) {
    try {
      pusher.push(object, parser.parse(row, kgClient, recursiveInstantiationDepth));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Clears the associated field of the object with the value in the row.
   */
  public void clear(Object object) {
    try {
      setter.invoke(object, defaultConstructor.get());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}