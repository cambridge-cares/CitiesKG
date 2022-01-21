package uk.ac.cam.cares.twa.cities.models.test;

import junit.framework.TestCase;
import org.apache.jena.update.UpdateRequest;
import org.json.JSONArray;
import org.junit.jupiter.api.Disabled;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.SPARQLUtils;
import uk.ac.cam.cares.twa.cities.models.FieldInterface;
import uk.ac.cam.cares.twa.cities.models.FieldKey;
import uk.ac.cam.cares.twa.cities.models.MetaModel;
import uk.ac.cam.cares.twa.cities.models.Model;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertNotEquals;

/**
 * To run ModelTest, you need:
 * - AccessAgent running on http://localhost:48080
 * - A Blazegraph namespace at http://localhost:9999/blazegraph/namespace/test/sparql
 */
public class ModelTest extends TestCase {

  private static final String testResourceId = "http://localhost:48080/local-test";

  private final Field metaModel;

  public ModelTest() throws NoSuchFieldException {
    metaModel = Model.class.getDeclaredField("metaModel");
    metaModel.setAccessible(true);
  }

  private TestModel clearDatabaseAndPushModel() {
    AccessAgentCaller.update(testResourceId, "CLEAR ALL");
    TestModel model = new TestModel(12345, 25, 3);
    model.dirtyAll();
    Model.clearUpdateQueue();
    model.queuePushUpdate(true, true);
    Model.executeUpdates(testResourceId, true);
    return model;
  }

  private int countTriples() {
    String responseString = AccessAgentCaller.query(testResourceId, "SELECT (COUNT(*) AS ?count) WHERE { ?a ?b ?c }");
    JSONArray response = SPARQLUtils.unpackQueryResponse(responseString);
    return response.getJSONObject(0).getInt("count");
  }

  public void testModelInitialisation() throws IllegalAccessException, NoSuchFieldException {
    TestModel freshModel = new TestModel();
    // Check scalars initialise to null and vectors initialise to empty lists
    assertEquals(new ArrayList<>(), freshModel.getForwardVector());
    assertNull(freshModel.getIntProp());
    assertNull(freshModel.getModelProp());
    // Check that the "original field values" are not equal to the default values (dirty on ex nihilo initialisation).
    FieldInterface exampleScalarField = ((MetaModel)metaModel.get(freshModel)).scalarFieldList.get(0).getValue();
    FieldInterface exampleVectorField = ((MetaModel)metaModel.get(freshModel)).scalarFieldList.get(0).getValue();
    Field originalFieldValues = Model.class.getDeclaredField("originalFieldValues");
    originalFieldValues.setAccessible(true);
    assertNotEquals(exampleScalarField.getMinimised(freshModel),
        ((Object[])originalFieldValues.get(freshModel))[exampleScalarField.index]);
    assertNotEquals(exampleVectorField.getMinimised(freshModel),
        ((Object[])originalFieldValues.get(freshModel))[exampleVectorField.index]);
  }

  public void testClearAll() {
    TestModel blankModel = new TestModel();
    TestModel nonBlankModel = new TestModel(123, 6, 0);
    assertNotEquals(blankModel, nonBlankModel);
    nonBlankModel.clearAll();
    assertEquals(blankModel, nonBlankModel);
  }

  public void testCleanAll() throws IllegalAccessException {
    // Baseline: new models are dirty.
    Model.clearUpdateQueue();
    Model testModel = new TestModel(2345, 4, 0);
    testModel.queuePushUpdate(true, true);
    assertNotEquals("", Model.peekUpdateQueue());
    Model.clearUpdateQueue();
    // A cleaned new model does not push updates.
    testModel = new TestModel(2345, 4, 0);
    testModel.cleanAll();
    testModel.queuePushUpdate(true, true);
    assertEquals("", Model.peekUpdateQueue());
    Model.clearUpdateQueue();
  }

  public void testDirtyAll() throws IllegalAccessException {
    TestModel testModel = new TestModel(2345, 4, 0);
    testModel.cleanAll();
    testModel.queuePushUpdate(true, true);
    assertEquals("", Model.peekUpdateQueue());
    testModel.dirtyAll();
    testModel.queuePushUpdate(true, true);
    String updateString = Model.peekUpdateQueue();
    assertNotEquals("", updateString);
    for (FieldKey key : ((MetaModel)metaModel.get(testModel)).fieldMap.keySet()) {
      assertTrue(updateString.contains(key.predicate));
    }
  }

  public void testDirtyingBehaviour() throws IllegalAccessException {
    Model.clearUpdateQueue();
    TestModel testModel = new TestModel(23456, 5, 0);
    testModel.cleanAll();
    testModel.queuePushUpdate(true, true);
    assertEquals("", Model.peekUpdateQueue());
    // Test scalar dirtying
    testModel.setIntProp(3);
    assertFalse(Model.peekUpdateQueue().contains("intprop"));
    testModel.queuePushUpdate(true, true);
    assertTrue(Model.peekUpdateQueue().contains("intprop"));
    // Test pushing again does nothing
    String updateString = Model.peekUpdateQueue();
    testModel.queuePushUpdate(true, true);
    assertEquals(updateString, Model.peekUpdateQueue());
    // Test vector dirtying: add new element
    testModel.getForwardVector().add(4.0);
    assertFalse(Model.peekUpdateQueue().contains("forwardvector"));
    testModel.queuePushUpdate(true, true);
    assertTrue(Model.peekUpdateQueue().contains("forwardvector"));
    Model.clearUpdateQueue();
    // Test vector dirtying: remove element
    testModel.getForwardVector().remove(0);
    testModel.queuePushUpdate(true, true);
    assertTrue(Model.peekUpdateQueue().contains("forwardvector"));
    Model.clearUpdateQueue();
    // Test vector dirtying: order change should not cause dirtying
    testModel.getForwardVector().add(testModel.getForwardVector().remove(0));
    testModel.queuePushUpdate(true, true);
    Model.clearUpdateQueue();
  }

  public void testSetIri() {
    Model model = new TestModel();
    model.setIri("testuuid-12345", "http://localhost:9999/testnamespace/");
    assertEquals(URI.create("http://localhost:9999/testnamespace/testmodels/testuuid-12345"), model.getIri());
  }

  public void testExecuteUpdateQueue() {
    try(MockedStatic<AccessAgentCaller> mock = Mockito.mockStatic(AccessAgentCaller.class)) { // disables executions
      Model.clearUpdateQueue();
      assertEquals("", Model.peekUpdateQueue());
      String updateContents =
          "INSERT DATA {\n" +
              "  <http://test/test> <http://test/test2> <http://test/test3> .\n" +
              "}\n";
      // Check correctly added
      Model.queueUpdate(updateContents);
      assertEquals(updateContents, Model.peekUpdateQueue());
      // Short update should not trigger flush without force
      Model.executeUpdates(testResourceId, false);
      assertEquals(updateContents, Model.peekUpdateQueue());
      // Check flush with force option works
      Model.executeUpdates(testResourceId, true);
      assertEquals("", Model.peekUpdateQueue());
      // Try long update (need >250000 characters)
      for (int i = 0; i < 10000; i++) {
        Model.queueUpdate("INSERT DATA {\n" +
            "  <http://test/test/test/test/test> <http://test/test/test/test/test" + i + "> <http://test/test3> .\n" +
            "}\n");
      }
      Model.executeUpdates(testResourceId, false);
      assertEquals("", Model.peekUpdateQueue());
    }
  }

  public void testClearUpdateQueue() throws IllegalAccessException {
    String updateContents =
        "INSERT DATA {\n" +
            "  <http://test/test> <http://test/test2> <http://test/test3> .\n" +
            "}\n";
    // Check correctly added
    Model.queueUpdate(updateContents);
    assertNotEquals("", Model.peekUpdateQueue());
    Model.clearUpdateQueue();
    assertEquals("", Model.peekUpdateQueue());
  }

  public void testPushNewObject() {
    // Push all
    TestModel model = clearDatabaseAndPushModel();
    assertEquals(20 + 25 * 2, countTriples());
    // Push forward
    AccessAgentCaller.update(testResourceId, "CLEAR ALL");
    model.dirtyAll();
    model.queuePushUpdate(true, false);
    Model.executeUpdates(testResourceId, true);
    assertEquals(18 + 25, countTriples());
    // Push backward
    AccessAgentCaller.update(testResourceId, "CLEAR ALL");
    model.dirtyAll();
    model.queuePushUpdate(false, true);
    Model.executeUpdates(testResourceId, true);
    assertEquals(2 + 25, countTriples());
    // Push nothing, then forward, then backward, then all
    AccessAgentCaller.update(testResourceId, "CLEAR ALL");
    model.dirtyAll();
    model.queuePushUpdate(false, false);
    Model.executeUpdates(testResourceId, true);
    assertEquals(0, countTriples());
    model.dirtyAll();
    model.queuePushUpdate(true, false);
    Model.executeUpdates(testResourceId, true);
    assertEquals(18 + 25, countTriples());
    model.dirtyAll();
    model.queuePushUpdate(false, true);
    Model.executeUpdates(testResourceId, true);
    assertEquals(20 + 25 * 2, countTriples());
    model.dirtyAll();
    model.queuePushUpdate(true, true);
    Model.executeUpdates(testResourceId, true);
    assertEquals(20 + 25 * 2, countTriples());
  }

  public void testDelete() {

    TestModel model1 = clearDatabaseAndPushModel();
    int firstModelCount = countTriples();

    TestModel model2 = new TestModel(3152, 8, 1);
    model2.queuePushUpdate(true, true);
    Model.executeUpdates(testResourceId, true);
    int secondModelCount = countTriples();

    model1.queueDeletionUpdate();
    Model.executeUpdates(testResourceId, true);
    assertEquals(secondModelCount - firstModelCount, countTriples());

    model2.queueDeletionUpdate();
    Model.executeUpdates(testResourceId, true);
    assertEquals(0, countTriples());

    model1.queuePushUpdate(true, true);
    Model.executeUpdates(testResourceId, true);
    assertEquals(firstModelCount, countTriples());

  }

  public void testPullAll() {
    TestModel model = clearDatabaseAndPushModel();
    model.queuePushUpdate(true, true);
    Model.executeUpdates(testResourceId, true);
    TestModel pulledModel = new TestModel();
    pulledModel.setIri(model.getIri());
    pulledModel.pullAll(testResourceId, 0);
    assertEquals(model, pulledModel);
  }

  public void testPullRecursive() {
    TestModel model = clearDatabaseAndPushModel();
    model.getModelProp().queuePushUpdate(true, true);
    model.getModelProp().getModelProp().queuePushUpdate(true, true);
    model.getModelProp().getModelProp().getModelProp().queuePushUpdate(true, true);
    Model.executeUpdates(testResourceId, true);
    TestModel pulledModel = new TestModel();
    pulledModel.setIri(model.getIri());
    pulledModel.pullAll(testResourceId, 2);
    assertEquals(model, pulledModel);
    assertEquals(model.getModelProp(), pulledModel.getModelProp());
    assertEquals(model.getModelProp().getModelProp(), pulledModel.getModelProp().getModelProp());
    assertNotEquals(model.getModelProp().getModelProp().getModelProp(), pulledModel.getModelProp().getModelProp().getModelProp());
    TestModel dummyThirdLevelModel = new TestModel();
    dummyThirdLevelModel.setIri(model.getModelProp().getModelProp().getModelProp().getIri());
    assertEquals(dummyThirdLevelModel, pulledModel.getModelProp().getModelProp().getModelProp());
  }

  // Currently, this FAILS because the query is too long for AccessAgent.
  public void testPullScalars() throws IllegalAccessException {
    TestModel model = clearDatabaseAndPushModel();
    TestModel pulledModel = new TestModel();
    pulledModel.setIri(model.getIri());
    pulledModel.pullScalars(testResourceId);
    assertNotEquals(model, pulledModel);
    for (Map.Entry<FieldKey, FieldInterface> entry : ((MetaModel)metaModel.get(model)).scalarFieldList) {
      assertTrue(entry.getValue().equals(model, pulledModel));
    }
  }

  public void testPullVector() throws IllegalAccessException {
    TestModel model = clearDatabaseAndPushModel();
    TestModel pulledModel = new TestModel();
    pulledModel.setIri(model.getIri());
    pulledModel.pullVector(Arrays.asList("forwardVector", "emptyForwardVector"), testResourceId);
    assertNotEquals(model, pulledModel);
    for (Map.Entry<FieldKey, FieldInterface> entry : ((MetaModel)metaModel.get(model)).vectorFieldList) {
      switch (entry.getValue().field.getName()) {
        case "forwardVector":
        case "emptyForwardVector":
          assertTrue(entry.getValue().equals(model, pulledModel));
          break;
        default:
          assertFalse(entry.getValue().equals(model, pulledModel));
      }
    }
  }

  public void testPushPulledObject() {
    TestModel model = clearDatabaseAndPushModel();
    TestModel pulledModel = new TestModel();
    pulledModel.setIri(model.getIri());
    pulledModel.pullAll(testResourceId, 0);
    pulledModel.dirtyAll();
    pulledModel.queuePushUpdate(true, true);
    Model.executeUpdates(testResourceId, true);
    // Repulling should not have changed anything
    TestModel repulledModel = new TestModel();
    repulledModel.setIri(pulledModel.getIri());
    repulledModel.pullAll(testResourceId, 0);
    assertEquals(pulledModel, repulledModel);
  }

  public void testEquals() {
    TestModel model1 = new TestModel(1234, 7, 2);
    TestModel model2 = new TestModel(1234, 7, 2);
    assertEquals(model1, model2);
    model2.getModelProp().setStringProp("modified string 1");
    assertEquals(model1, model2);
    model2.setIri(URI.create("http://eg.com/changediri"));
    assertNotEquals(model1, model2);
    model2.setIri(model1.getIri());
    assertEquals(model1, model2);
    model2.setStringProp("modified string 2");
    assertNotEquals(model1, model2);
  }

  public void testGetNamespace() {
    // static version
    assertEquals("http://example.com/namespace/namespacename/",
        Model.getNamespace("http://example.com/namespace/namespacename/graphname/uuid"));
    // instance version
    Model model = new TestModel();
    model.setIri(URI.create("http://example.com/namespace/namespacename/graphname/uuid"));
    assertEquals("http://example.com/namespace/namespacename/", model.getNamespace());
  }

  public void testBuildGraphIri() {
    // static version
    assertEquals("http://example.com/namespace/namespacename/graphname/",
        Model.buildGraphIri("http://example.com/namespace/namespacename/", "graphname"));
    // instance version
    Model model = new TestModel();
    model.setIri(URI.create("http://example.com/namespace/namespacename/graphname/uuid"));
    assertEquals("http://example.com/namespace/namespacename/differentgraph/",
        model.buildGraphIri("differentgraph"));
  }

}
