package uk.ac.cam.cares.twa.cities.models;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.interfaces.StoreClientInterface;

import java.io.InvalidClassException;
import java.lang.reflect.*;
import java.net.URI;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A collection of methods and functions for interacting with data from a {@link FieldAnnotation}-annotated field
 * of a {@link Model} subclass. This should only be used by Model base class methods via {@link MetaModel} for
 * the purposes of change checking, parsing SPARQL query responses and writing SPARQL updates.
 * @author <a href="mailto:jec226@cam.ac.uk">Jefferson Chua</a>
 * @version $Id$
 */
public class FieldInterface {

  /**
   * Puts a value object into a field of a {@link Model} object. If the field is a List, this should add the value
   * as a new element; else, it should set (overwrite) the value of the field.
   */
  @FunctionalInterface
  interface Putter {
    void consume(Object object, Object value) throws Exception;
  }

  /**
   * Converts string representation into Java object
   */
  @FunctionalInterface
  interface Parser {
    Object parse(String value, String datatype, String kgId, int recursiveInstantiationDepth) throws Exception;
  }

  /**
   * Converts Java object into Jena {@link Node}
   */
  @FunctionalInterface
  interface NodeGetter {
    Node get(Object object) throws Exception;
  }

  // Convenience metadata
  public final boolean isList;
  public final int index;

  // Direct access to the field itself
  public final Field field;
  public final Method getter;
  public final Method setter;

  // Only for vector fields (lists)
  private final Constructor<?> listConstructor;

  // Only depends on outerType (isList)
  private final Putter putter;

  // The below only depend on innerType; they below operate on the element level. See FunctionalInterface descriptions.
  private final Parser parser;
  private final Function<Object, Object> minimiser;
  private final NodeGetter nodeGetter;

  public FieldInterface(Field field, int index) throws NoSuchMethodException, InvalidClassException {
    this.field = field;
    this.index = index;
    // Determine characteristics of field
    Class<?> parentType = field.getDeclaringClass();
    Class<?> outerType = field.getType();
    isList = List.class.isAssignableFrom(outerType);
    Class<?> innerType = isList ? field.getAnnotation(FieldAnnotation.class).innerType() : outerType;
    // Get the lombok accessors/modifiers --- we can't access private/protected fields. :(
    String fieldName = field.getName();
    fieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    getter = parentType.getMethod("get" + fieldName);
    setter = parentType.getMethod("set" + fieldName, outerType);
    // Generate parser
    if (Model.class.isAssignableFrom(innerType)) {
      Constructor<?> constructor = innerType.getConstructor();
      parser = (String value, String datatype, String kgId, int recursiveInstantiationDepth) -> {
        Model model = (Model) constructor.newInstance();
        if (recursiveInstantiationDepth > 0) {
          model.setIri(URI.create(value));
          model.pullAll(kgId, recursiveInstantiationDepth - 1);
        } else {
          model.setIri(URI.create(value));
        }
        return model;
      };
      nodeGetter = (Object value) -> {
        URI iri = ((Model) value).getIri();
        return iri == null ? NodeFactory.createBlankNode() : NodeFactory.createURI(iri.toString());
      };
      minimiser = (Object value) -> (((Model) value).getIri() != null) ? ((Model) value).getIri().toString() : null;
    } else if (innerType == URI.class) {
      parser = (String value, String datatype, String kgId, int rid) -> URI.create(value);
      nodeGetter = (Object value) -> NodeFactory.createURI(value.toString());
      minimiser = Object::toString;
    } else if (DatatypeModel.class.isAssignableFrom(innerType)) {
      Constructor<?> constructor = innerType.getConstructor(String.class, String.class);
      parser = (String value, String datatype, String kgId, int rid) -> constructor.newInstance(value, datatype);
      nodeGetter = (Object value) -> ((DatatypeModel) value).getNode();
      minimiser = (Object value) -> ((DatatypeModel) value).getNode().toString();
    } else {
      minimiser = (Object value) -> value;
      if (innerType == Integer.class) {
        parser = (String value, String datatype, String kgId, int rid) -> Integer.valueOf(value);
        nodeGetter = (Object value) -> NodeFactory.createLiteral(String.valueOf((int) value), XSDDatatype.XSDinteger);
      } else if (innerType == Double.class) {
        parser = (String value, String datatype, String kgId, int rid) -> Double.valueOf(value);
        nodeGetter = (Object value) -> NodeFactory.createLiteral(String.valueOf((double) value), XSDDatatype.XSDdouble);
      } else if (innerType == String.class) {
        parser = (String value, String datatype, String kgId, int rid) -> value;
        nodeGetter = (Object value) -> NodeFactory.createLiteral((String) value, XSDDatatype.XSDstring);
      } else {
        throw new InvalidClassException(innerType.toString());
      }
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
   * Parses and puts a datum represented by a value string and a datatype string into this field of a {@link Model}.
   * For a scalar field, this sets the value. For a vector field (list), this adds the parsed value to the array.
   * @param object                      the object to put the value into.
   * @param valueString                 the string representation of the value to be put.
   * @param datatypeString              the string representation of the RDF datatype of the value to be put.
   * @param kgId                        resource ID of graph to query additional data from; only used for {@link Model}
   *                                    when <code>recursiveInstantiationDepth</code> > 0.
   * @param recursiveInstantiationDepth number of further nested levels of {@link Model} fields to recursively
   *                                    query and instantiate.
   */
  public void put(Object object, String valueString, String datatypeString, String kgId, int recursiveInstantiationDepth) {
    try {
      putter.consume(object, parser.parse(valueString, datatypeString, kgId, recursiveInstantiationDepth));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Overwrites the existing value of a field with a default value. For a scalar field, this is <code>null</code>. For
   * a vector field, this is an empty {@link ArrayList}.
   * @param object the object for which to clear this field.
   */
  public void clear(Object object) {
    try {
      setter.invoke(object, isList ? listConstructor.newInstance() : null);
    } catch (Exception e) {
      throw new JPSRuntimeException(e);
    }
  }

  /**
   * Gets a minimised representation of the field value that can be used with <code>equals</code> to check equality on a
   * database representation level (i.e. they return the same on <code>getNode</code>), which is value equality but with
   * caveats: only IRI is considered for models, and order is ignored for vector fields.
   * @param object the object from which to read and minimise the field value.
   * @return the minimised representation.
   */
  public Object getMinimised(Object object) {
    try {
      Object value = getter.invoke(object);
      if (isList) {
        // Use a list instead of an array since the List<?>.equals does element-by-element comparison.
        return ((List<?>) value).stream().map(
            (obj) -> obj == null ? null : minimiser.apply(obj)
        ).collect(Collectors.toSet());
      } else {
        return value == null ? null : minimiser.apply(value);
      }
    } catch (IllegalStateException | InvocationTargetException | IllegalAccessException e) {
      throw new JPSRuntimeException(e);
    }
  }

  /**
   * Gets a Jena {@link Node} representing the current field value, for use in UpdateBuilder. Scalar fields only.
   * @param object the object from which to read the value.
   * @return a {@link Node} representing the value.
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
   * Gets a {@link Node}<code>[]</code> representing the current field value, for use in UpdateBuilder. Vector fields only.
   * @param object the object from which to read the values.
   * @return an array of {@link Node}s representing the values.
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

  /**
   * Determines if two objects are equal in this field on a database representation (i.e. {@link Node}) level. This
   * is value equality with the caveats: only IRI is considered for models, and order is ignored for vector fields.
   * Internally uses <code>getMinimised</code>.
   * @param o1 the first object to compare.
   * @param o2 the second object to compare.
   * @return whether they are equal on a database representation level.
   */
  public boolean equals(Object o1, Object o2) {
    return Objects.equals(getMinimised(o1), getMinimised(o2));
  }

}