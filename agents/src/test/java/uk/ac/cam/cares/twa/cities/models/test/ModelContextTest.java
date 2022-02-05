package uk.ac.cam.cares.twa.cities.models.test;

import junit.framework.TestCase;
import org.json.JSONArray;
import org.mockito.Mockito;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.SPARQLUtils;
import uk.ac.cam.cares.twa.cities.models.*;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotEquals;

/**
 * To run ModelContextTest, you need:
 * - A Blazegraph namespace at http://localhost:9999/blazegraph/namespace/test/sparql
 */
public class ModelContextTest extends TestCase {

  private static final String testResourceId = "HARDCODE:http://localhost:9999/blazegraph/namespace/test/sparql";
  private static final String testNamespace = "http://localhost:9999/blazegraph/namespace/test/sparql/";

  private final Field metaModel;

  public ModelContextTest() throws NoSuchFieldException {
    metaModel = Model.class.getDeclaredField("metaModel");
    metaModel.setAccessible(true);
  }

  private int countTriples(ModelContext context) {
    JSONArray response = context.query("SELECT (COUNT(*) AS ?count) WHERE { ?a ?b ?c }");
    return response.getJSONObject(0).getInt("count");
  }

  public void testPushNewObject() {
    ModelContext context = Mockito.spy(new ModelContext(testResourceId, testNamespace));
    context.update("CLEAR ALL");
    TestModel.createRandom(context, 12345, 3, 3).pushChanges();
    Mockito.verify(context, Mockito.never()).query(Mockito.contains("DELETE"));
    assertEquals(19 + 3 * 2, countTriples(context));
  }

  public void testPushChanges() {
    ModelContext context = Mockito.spy(new ModelContext(testResourceId, testNamespace));
    context.update("CLEAR ALL");
    TestModel model = TestModel.createRandom(context, 12345, 3, 3);
    model.pushChanges();
    Mockito.verify(context, Mockito.never()).query(Mockito.contains("DELETE"));
    assertEquals(19 + 3 * 2, countTriples(context));
    model.setDoubleProp(7.77);
    model.pushChanges();
    Mockito.verify(context, Mockito.times(1)).update(Mockito.contains("DELETE"));
    Mockito.verify(context, Mockito.times(2)).update(Mockito.contains("doubleprop"));
    Mockito.verify(context, Mockito.times(1)).update(Mockito.contains("stringprop"));
    assertEquals(19 + 3 * 2, countTriples(context));
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

  public void testPullScalars() throws IllegalAccessException {
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
    for (Map.Entry<FieldKey, FieldInterface> entry : ((MetaModel)metaModel.get(pullModel)).scalarFieldList) {
      assertTrue(entry.getValue().equals(pushModel, pullModel));
    }
  }

  public void testPullVector() throws IllegalAccessException {
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
    for (Map.Entry<FieldKey, FieldInterface> entry : ((MetaModel)metaModel.get(pullModel)).vectorFieldList) {
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
    System.out.println(model1.getModelProp().getIri() + " vs. " + model2.getModelProp().getIri());
    assertNotEquals(model1, model2);
    model1.setModelProp(TestModel.createRandom(context1, 12, 0, 1));
    assertEquals(model1, model2);
    model2.setStringProp("modified string 2");
    assertNotEquals(model1, model2);
  }

  public void testIRICollision() {
    ModelContext context = new ModelContext(testResourceId, testNamespace);
    context.createPrototypeModel(TestModel.class, "test");
    try {
      context.createPrototypeModel(TestModel.class, "test");
      fail();
    } catch (JPSRuntimeException ignored) {
    }
  }

}
