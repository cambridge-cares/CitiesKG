package uk.ac.cam.cares.twa.cities;

import junit.framework.TestCase;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.json.JSONObject;
import org.mockito.Mock;
import uk.ac.cam.cares.jps.base.interfaces.StoreClientInterface;
import uk.ac.cam.cares.twa.cities.models.geo.GeometryType;
import uk.ac.cam.cares.twa.cities.models.geo.test.TestModel;

import java.io.InvalidClassException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.verify;

public class FieldInterfaceTest extends TestCase {

  private static final String VALUE = "value";
  private static final String DATATYPE = "dtype";

  TestModel model1 = new TestModel();
  TestModel model2 = new TestModel();

  @Mock
  StoreClientInterface kgClient;

  public FieldInterfaceTest() throws InvocationTargetException, InstantiationException, IllegalAccessException {
    model1.resetCleanCopy();
    model2.resetCleanCopy();
  }

  public void testIntegerInterface() throws InvalidClassException, NoSuchFieldException, NoSuchMethodException {
    // Data to write
    this.testScalarInterface("intProp", TestModel::setIntProp, TestModel::getIntProp,
        "1", "http://www.w3.org/2001/XMLSchema#integer", 1, 2, NodeFactory.createLiteral(String.valueOf(2), XSDDatatype.XSDinteger));
  }

  public void testDoubleInterface() throws InvalidClassException, NoSuchFieldException, NoSuchMethodException {
    // Data to write
    this.testScalarInterface("doubleProp", TestModel::setDoubleProp, TestModel::getDoubleProp,
        "3.14", "http://www.w3.org/2001/XMLSchema#double", 3.14, 5.1167, NodeFactory.createLiteral("5.1167", XSDDatatype.XSDdouble));
  }

  public void testStringInterface() throws InvalidClassException, NoSuchFieldException, NoSuchMethodException {
    // Data to write
    this.testScalarInterface("stringProp", TestModel::setStringProp, TestModel::getStringProp,
        "teststring", "http://www.w3.org/2001/XMLSchema#string", "teststring", "test2", NodeFactory.createLiteral("test2", XSDDatatype.XSDstring));
  }

  public void testUriInterface() throws InvalidClassException, NoSuchFieldException, NoSuchMethodException {
    // Data to write
    this.testScalarInterface("uriProp", TestModel::setUriProp, TestModel::getUriProp,
        "https://example.com/testuri", "",
        URI.create("https://example.com/testuri"), URI.create("http://example.com/uri2"),
        NodeFactory.createURI("http://example.com/uri2"));
  }

  public void testModelInterface() throws InvalidClassException, NoSuchFieldException, NoSuchMethodException {
    // Data to write
    TestModel dataModel = new TestModel();
    dataModel.setIri(URI.create("http://example.com/testmodel"));
    TestModel secondModel = new TestModel();
    secondModel.setIri(URI.create("http://example.com/model2"));
    this.testScalarInterface("modelProp", TestModel::setModelProp, TestModel::getModelProp,
        "http://example.com/testmodel", "", dataModel, secondModel, NodeFactory.createURI("http://example.com/model2"));
  }

  public void testDatatypeModelInterface() throws InvalidClassException, NoSuchFieldException, NoSuchMethodException {
    // Data to write
    JSONObject row = new JSONObject();
    GeometryType rowGeometryType = new GeometryType(
        "1.0#1.0#3.0#1.0#2.0#3.0#2.0#2.0#3.0#2.0#1.0#3.0#1.0#1.0#1.0",
        "http://localhost/blazegraph/literals/POLYGON-3-15");
    GeometryType secondGeometryType = new GeometryType(
        "1.0#1.0#3.0#1.0#2.0#3.0#2.0#2.0#3.0#2.0#1.0#3.0#1.0#1.0#1.0#1.0#1.0#3.0",
        "http://localhost/blazegraph/literals/POLYGON-3-18");
    this.testScalarInterface("geometryProp", TestModel::setGeometryProp, TestModel::getGeometryProp,
        "1.0#1.0#3.0#1.0#2.0#3.0#2.0#2.0#3.0#2.0#1.0#3.0#1.0#1.0#1.0",
        "http://localhost/blazegraph/literals/POLYGON-3-15",
        rowGeometryType, secondGeometryType, secondGeometryType.getNode());
  }

  private <T> void testScalarInterface(
      String fieldName, BiConsumer<TestModel, T> directSetter, Function<TestModel, T> directGetter,
      String valueString, String datatypeString, T dataValue, T secondValue, Node secondValueNode)
      throws NoSuchFieldException, InvalidClassException, NoSuchMethodException {
    FieldInterface field = new FieldInterface(TestModel.class.getDeclaredField(fieldName));
    // Test writing JSON data to model field; this does not dirty the field
    field.put(model1, valueString, datatypeString, kgClient, 0);
    assertEquals(dataValue, directGetter.apply(model1));
    // Test equality checks
    assertFalse(field.equals(model1, model2)); // a != null
    assertFalse(field.equals(model2, model1)); // null != a
    directSetter.accept(model2, dataValue);
    assertTrue(field.equals(model1, model2)); // a == a
    directSetter.accept(model2, secondValue);
    assertFalse(field.equals(model1, model2)); // a != b
    directSetter.accept(model1, null);
    directSetter.accept(model2, null);
    assertTrue(field.equals(model1, model2)); // null == null
    // Test clear
    field.clear(model2);
    assertNull(directGetter.apply(model2));
    // Test getNode
    directSetter.accept(model2, secondValue);
    assertEquals(secondValueNode, field.getNode(model2));
    assertTrue(field.getNode(model1).isBlank());
  }

  public void testVectorInterface() throws NoSuchFieldException, InvalidClassException, NoSuchMethodException {
    FieldInterface field = new FieldInterface(TestModel.class.getDeclaredField("forwardVector"));
    // Test data insertion from null
    assertEquals(0, model1.getForwardVector().size());
    assertTrue(field.equals(model1, model2));
    field.put(model1, "1.32", "http://www.w3.org/2001/XMLSchema#double", kgClient, 0);
    assertEquals(1, model1.getForwardVector().size());
    assertEquals(1.32, model1.getForwardVector().get(0));
    assertFalse(field.equals(model1, model2));
    // Test modification dirties field
    model1.getForwardVector().add(5.32);
    // Test getting nodes
    assertArrayEquals(new Node[]{
        NodeFactory.createLiteral("1.32", XSDDatatype.XSDdouble),
        NodeFactory.createLiteral("5.32", XSDDatatype.XSDdouble)
    }, field.getNodes(model1));
    assertArrayEquals(new Node[0], field.getNodes(model2));
    // Test clear
    field.clear(model1);
    assertArrayEquals(new Node[0], field.getNodes(model1));
  }

}
