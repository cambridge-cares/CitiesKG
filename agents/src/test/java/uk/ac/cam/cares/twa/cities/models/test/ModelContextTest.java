package uk.ac.cam.cares.twa.cities.models.test;

import junit.framework.TestCase;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;
import org.mockito.Mockito;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.models.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertNotEquals;

/**
 * To run ModelContextTest, you need:
 * - A Blazegraph namespace at http://localhost:9999/blazegraph/namespace/test/sparql
 */
public class ModelContextTest extends TestCase {

  private static final String testResourceId = "HARDCODE:http://localhost:9999/blazegraph/namespace/test/sparql";
  private static final String testNamespace = "http://localhost:9999/blazegraph/namespace/test/sparql/";

  private static final MetaModel metaModel = MetaModel.get(TestModel.class);

  private int countTriples(ModelContext context) {
    JSONArray response = context.query("SELECT (COUNT(*) AS ?count) WHERE { ?a ?b ?c }");
    return response.getJSONObject(0).getInt("count");
  }

  public void testPushNewObject() {
    ModelContext context = Mockito.spy(new ModelContext(testResourceId, testNamespace));
    context.update("CLEAR ALL");
    TestModel.createRandom(context, 12345, 3, 3).pushChanges();
    Mockito.verify(context, Mockito.never()).query(Mockito.contains("DELETE"));
    assertEquals(metaModel.scalarFieldList.size() + (metaModel.vectorFieldList.size() - 1) * 3, countTriples(context));
  }

  public void testPushChanges() {
    ModelContext context = Mockito.spy(new ModelContext(testResourceId, testNamespace));
    context.update("CLEAR ALL");
    TestModel model = TestModel.createRandom(context, 12345, 3, 3);
    model.pushChanges();
    Mockito.verify(context, Mockito.never()).query(Mockito.contains("DELETE"));
    model.setDoubleProp(7.77);
    model.pushChanges();
    Mockito.verify(context, Mockito.times(1)).update(Mockito.contains("DELETE"));
    Mockito.verify(context, Mockito.times(2)).update(Mockito.contains("doubleprop"));
    Mockito.verify(context, Mockito.times(1)).update(Mockito.contains("stringprop"));
    assertEquals(metaModel.scalarFieldList.size() + (metaModel.vectorFieldList.size() - 1) * 3, countTriples(context));
  }

  public void testPushPartialChanges() {
    ModelContext pushContext = Mockito.spy(new ModelContext(testResourceId, testNamespace));
    pushContext.update("CLEAR ALL");
    TestModel pushModel = TestModel.createRandom(pushContext, 12345, 3, 3);
    pushContext.pushAllChanges();

    ModelContext pullContext = Mockito.spy(new ModelContext(testResourceId, testNamespace));
    TestModel pullModel = pullContext.createHollowModel(TestModel.class, pushModel.getIri());
    pullModel.pullScalars("doubleProp", "uriProp");
    pullModel.setDoubleProp(7.77);
    pullModel.setStringProp("modified");
    pullModel.pushChanges();
    // Only fields which were pulled should ever be pushed, even if unpulled fields have been changed.
    Mockito.verify(pullContext).update(Mockito.contains("doubleprop"));
    Mockito.verify(pullContext, Mockito.never()).update(Mockito.contains("stringprop"));
    Mockito.verify(pullContext, Mockito.never()).update(Mockito.contains("uriprop"));
    // If we pull the string field, we should afterwards be able to change and push it.
    pullModel.pullScalars("stringProp");
    pullModel.pushChanges();
    Mockito.verify(pullContext, Mockito.never()).update(Mockito.contains("stringProp"));
    pullModel.setStringProp("modified2");
    pullModel.pushChanges();
    Mockito.verify(pullContext, Mockito.times(1)).update(Mockito.contains("stringprop"));
    Mockito.verify(pullContext, Mockito.times(1)).update(Mockito.contains("doubleprop"));
    Mockito.verify(pullContext, Mockito.never()).update(Mockito.contains("uriprop"));
  }

  public void testDelete() {

    ModelContext context = new ModelContext(testResourceId, testNamespace);
    context.update("CLEAR ALL");

    TestModel model1 = TestModel.createRandom(context, 12345, 3, 0);
    model1.pushChanges();
    int firstModelCount = countTriples(context);
    context.pushAllChanges();

    TestModel model2 = TestModel.createRandom(context, 3152, 8, 0);
    context.pushAllChanges();
    int secondModelCount = countTriples(context);

    model1.delete();
    context.pushAllChanges();
    assertEquals(secondModelCount - firstModelCount, countTriples(context));

    model2.delete();
    context.pushAllChanges();
    assertEquals(0, countTriples(context));

    model1 = TestModel.createRandom(context, 12345, 3, 0);
    model1.pushChanges();
    assertEquals(firstModelCount, countTriples(context));

  }

  public void testPullAll() {
    ModelContext pushContext = new ModelContext(testResourceId, testNamespace);
    pushContext.update("CLEAR ALL");
    TestModel pushModel = TestModel.createRandom(pushContext, 12345, 3, 3);
    pushContext.pushAllChanges();

    ModelContext pullContext = new ModelContext(testResourceId, testNamespace);
    TestModel pullModel = pullContext.loadModel(TestModel.class, pushModel.getIri());
    assertEquals(pushModel, pullModel);
  }

  public void testPullRecursive() {
    ModelContext pushContext = new ModelContext(testResourceId, testNamespace);
    pushContext.update("CLEAR ALL");
    TestModel pushModel = TestModel.createRandom(pushContext, 12345, 3, 3);
    pushContext.pushAllChanges();

    ModelContext pullContext = new ModelContext(testResourceId, testNamespace);
    TestModel pullModel = pullContext.recursiveLoadModel(TestModel.class, pushModel.getIri(), 2);
    assertEquals(pushModel, pullModel);
    assertEquals(pushModel.getModelProp(), pullModel.getModelProp());
    assertEquals(pushModel.getModelProp().getModelProp(), pullModel.getModelProp().getModelProp());
    assertNotEquals(pushModel.getModelProp().getModelProp().getModelProp(), pullModel.getModelProp().getModelProp().getModelProp());

    ModelContext comparisonContext = new ModelContext(testResourceId, testNamespace);
    TestModel comparisonModel = comparisonContext.createHollowModel(TestModel.class, pushModel.getModelProp().getModelProp().getModelProp().getIri());
    assertEquals(comparisonModel, pullModel.getModelProp().getModelProp().getModelProp());
  }

  public void testPullScalars() {
    ModelContext pushContext = new ModelContext(testResourceId, testNamespace);
    pushContext.update("CLEAR ALL");
    TestModel pushModel = TestModel.createRandom(pushContext, 12345, 3, 3);
    pushContext.pushAllChanges();

    ModelContext pullContext = new ModelContext(testResourceId, testNamespace);
    TestModel pullModel = pullContext.createHollowModel(TestModel.class, pushModel.getIri());
    pullModel.pullScalars("doubleProp", "uriProp");
    assertEquals(pushModel.getDoubleProp(), pullModel.getDoubleProp());
    assertEquals(pushModel.getUriProp(), pullModel.getUriProp());
    assertNotEquals(pushModel.getStringProp(), pullModel.getStringProp());
    pullModel.pullScalars();
    for (Map.Entry<FieldKey, FieldInterface> entry : metaModel.scalarFieldList) {
      assertTrue(entry.getValue().equals(pushModel, pullModel));
    }
  }

  public void testPullVector() {
    ModelContext pushContext = new ModelContext(testResourceId, testNamespace);
    pushContext.update("CLEAR ALL");
    TestModel pushModel = TestModel.createRandom(pushContext, 12345, 3, 3);
    pushContext.pushAllChanges();

    ModelContext pullContext = new ModelContext(testResourceId, testNamespace);
    TestModel pullModel = pullContext.createHollowModel(TestModel.class, pushModel.getIri());
    pullModel.pullVectors("backwardVector", "emptyForwardVector");
    assertEquals(new HashSet<>(pushModel.getBackwardVector()), new HashSet<>(pullModel.getBackwardVector()));
    assertEquals(new HashSet<>(pushModel.getEmptyForwardVector()), new HashSet<>(pullModel.getEmptyForwardVector()));
    assertNotEquals(new HashSet<>(pushModel.getForwardVector()), new HashSet<>(pullModel.getForwardVector()));
    pullModel.pullVectors();
    for (Map.Entry<FieldKey, FieldInterface> entry : metaModel.vectorFieldList) {
      assertTrue(entry.getValue().equals(pushModel, pullModel));
    }
  }

  public void testEquals() {
    ModelContext context1 = new ModelContext(testResourceId, testNamespace);
    ModelContext context2 = new ModelContext(testResourceId, testNamespace);
    TestModel model1 = TestModel.createRandom(context1, 1234, 7, 2);
    TestModel model2 = TestModel.createRandom(context2, 1234, 7, 2);
    assertEquals(model1, model2);
    model2.getModelProp().setStringProp("modified string 1");
    assertEquals(model1, model2);
    model2.setModelProp(TestModel.createRandom(context2, 12, 0, 1));
    assertNotEquals(model1, model2);
    model1.setModelProp(TestModel.createRandom(context1, 12, 0, 1));
    assertEquals(model1, model2);
    model2.setStringProp("modified string 2");
    assertNotEquals(model1, model2);
  }

  public void testIRICollision() {
    ModelContext context = new ModelContext(testResourceId, testNamespace);
    context.createNewModel(TestModel.class, "test");
    try {
      context.createNewModel(TestModel.class, "test");
      fail();
    } catch (JPSRuntimeException ignored) {
    }
  }

  public void testQuadModelInTripleContext() {
    ModelContext context = new ModelContext(testResourceId);
    try {
      context.createNewModel(TestModel.class, "test");
      fail();
    } catch (JPSRuntimeException ignored) {
    }
  }

  public static class TripleTestModel extends Model {
    @Getter @Setter @FieldAnnotation("JPSLAND:stringprop") private String stringProp;
    @Getter @Setter @FieldAnnotation("dbpediao:intprop") private Integer intProp;
    @Getter @Setter @FieldAnnotation("dbpediao:doubleprop") private Double doubleProp;
    @Getter @Setter @FieldAnnotation(value = "dbpediao:forwardvector", innerType = Double.class) private ArrayList<Double> forwardVector;
    @Getter @Setter @FieldAnnotation(value = "dbpediao:emptyforwardvector", innerType = Double.class) private ArrayList<Double> emptyForwardVector;
    @Getter @Setter @FieldAnnotation(value = "dbpediao:backwardVector", backward = true, innerType = URI.class) private ArrayList<URI> backwardVector;
  }

  public void testTripleModelPushPullAll() {
    ModelContext pushContext = new ModelContext(testResourceId);
    pushContext.update("CLEAR ALL");
    TripleTestModel pushModel = pushContext.createNewModel(TripleTestModel.class, "http://testiri.com/test");
    pushModel.setStringProp("teststring");
    pushModel.getForwardVector().add(2.77);
    pushContext.pushAllChanges();

    ModelContext pullContext = new ModelContext(testResourceId);
    TripleTestModel pullModel = pullContext.loadModel(TripleTestModel.class, pushModel.getIri());
    assertEquals(pushModel, pullModel);
  }

  public void testTripleModelPullScalarsVectors() {
    ModelContext pushContext = new ModelContext(testResourceId);
    pushContext.update("CLEAR ALL");
    TripleTestModel pushModel = pushContext.createNewModel(TripleTestModel.class, "http://testiri.com/test");
    pushModel.setStringProp("teststring");
    pushModel.setDoubleProp(77.11);
    pushModel.getForwardVector().add(2.77);
    pushModel.getBackwardVector().add(URI.create("http://testiri.com/testuri"));
    pushContext.pushAllChanges();

    ModelContext pullContext = new ModelContext(testResourceId);
    TripleTestModel pullModel = pullContext.loadPartialModel(TripleTestModel.class, pushModel.getIri(), "stringProp", "forwardVector");
    assertNotEquals(pushModel, pullModel);
    assertEquals(pushModel.getStringProp(), pullModel.getStringProp());
    assertNotEquals(pushModel.getDoubleProp(), pullModel.getDoubleProp());
    assertEquals(pushModel.getForwardVector(), pullModel.getForwardVector());
    assertNotEquals(pushModel.getBackwardVector(), pullModel.getBackwardVector());
  }

}
