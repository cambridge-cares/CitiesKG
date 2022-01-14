package uk.ac.cam.cares.twa.cities;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.interfaces.StoreClientInterface;

import java.io.InvalidClassException;
import java.lang.reflect.*;
import java.net.URI;
import java.util.List;

@FunctionalInterface
interface Parser {
  Object parse(JSONObject row, StoreClientInterface kgClient, int recursiveInstantiationDepth) throws Exception;
}

@FunctionalInterface
interface Putter {
  void consume(Object object, Object value) throws Exception;
}

@FunctionalInterface
interface NodeGetter {
  Node get(Object object) throws Exception;
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
  // Function for generating literal nodes to push to the database
  private final NodeGetter nodeGetter;
  // List-only methods
  private final Constructor<?> listConstructor;

  public FieldInterface(Field field) throws NoSuchMethodException, InvalidClassException {
    this.field = field;
    // Determine characteristics of field
    Class<?> parentType = field.getDeclaringClass();
    Class<?> outerType = field.getType();
    isList = List.class.isAssignableFrom(outerType);
    Class<?> innerType = isList ? field.getAnnotation(FieldAnnotation.class).innerType() : outerType;
    isModel = Model.class.isAssignableFrom(innerType);
    // Get the lombok accessors/modifiers --- we can't access private/protected fields. :(
    String fieldName = field.getName();
    fieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    getter = parentType.getMethod("get" + fieldName);
    setter = parentType.getMethod("set" + fieldName, outerType);
    // Generate parser
    if (isModel) {
      Constructor<?> constructor = innerType.getConstructor();
      parser = (JSONObject row, StoreClientInterface kgClient, int recursiveInstantiationDepth) -> {
        Model model = (Model) constructor.newInstance();
        if (recursiveInstantiationDepth > 0)
          model.pullAll(row.getString(Model.VALUE), kgClient, recursiveInstantiationDepth - 1);
        else
          model.setIri(URI.create(row.getString(Model.VALUE)));
        return model;
      };
      nodeGetter = (Object value) -> NodeFactory.createURI(((Model) value).getIri().toString());
    } else if (innerType == Integer.class) {
      parser = (JSONObject row, StoreClientInterface kgc, int rid) -> row.getInt(Model.VALUE);
      nodeGetter = (Object value) -> NodeFactory.createLiteral(String.valueOf((int) value), XSDDatatype.XSDinteger);
    } else if (innerType == Double.class) {
      parser = (JSONObject row, StoreClientInterface kgc, int rid) -> row.getDouble(Model.VALUE);
      nodeGetter = (Object value) -> NodeFactory.createLiteral(String.valueOf((double) value), XSDDatatype.XSDdouble);
    } else if (innerType == String.class) {
      parser = (JSONObject row, StoreClientInterface kgc, int rid) -> row.getString(Model.VALUE);
      nodeGetter = (Object value) -> NodeFactory.createLiteral((String) value, XSDDatatype.XSDstring);
    } else if (innerType == URI.class) {
      parser = (JSONObject row, StoreClientInterface kgc, int rid) -> URI.create(row.getString(Model.VALUE));
      nodeGetter = (Object value) -> NodeFactory.createURI(((URI) value).toString());
    } else if (DatatypeModel.class.isAssignableFrom(innerType)) {
      Constructor<?> constructor = innerType.getConstructor(String.class, String.class);
      parser = (JSONObject row, StoreClientInterface kgc, int rid)
          -> constructor.newInstance(row.getString(Model.VALUE), row.getString(Model.DATATYPE));
      nodeGetter = (Object value) -> ((DatatypeModel) value).getNode();
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
      putter = setter::invoke;
      listConstructor = null;
    }
  }

  /**
   * Populates the associated field of the object with the value in the row.
   */
  public void pull(Object object, JSONObject row, StoreClientInterface kgClient, int recursiveInstantiationDepth) {
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
      Object firstValue = getter.invoke(first);
      Object secondValue = getter.invoke(second);
      return firstValue == secondValue || (first != null && first.equals(second));
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
   * Use with non-List fields.
   */
  public Node getNode(Object object) {
    try {
      Object value = getter.invoke(object);
      return value == null ? NodeFactory.createBlankNode() : nodeGetter.get(value);
    } catch (Exception e) {
      throw new JPSRuntimeException(e);
    }
  }

  /**
   * Gets the string representations of the current elements of a list field, for pushing to the database.
   */
  public Node[] getNodes(Object object) {
    try {
      List<?> list = (List<?>) getter.invoke(object);
      Node[] literals = new Node[list.size()];
      for (int i = 0; i < literals.length; i++)
        literals[i] = list.get(i) == null ? NodeFactory.createBlankNode() : nodeGetter.get(list.get(i));
      return literals;
    } catch (Exception e) {
      throw new JPSRuntimeException(e);
    }
  }

}