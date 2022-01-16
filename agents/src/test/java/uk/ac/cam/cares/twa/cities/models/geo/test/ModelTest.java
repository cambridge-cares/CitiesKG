package uk.ac.cam.cares.twa.cities.models.geo.test;

import junit.framework.TestCase;
import org.json.JSONArray;
import org.mockito.Mockito;
import uk.ac.cam.cares.jps.base.interfaces.StoreClientInterface;
import uk.ac.cam.cares.jps.base.query.RemoteStoreClient;
import uk.ac.cam.cares.twa.cities.FieldInterface;
import uk.ac.cam.cares.twa.cities.FieldKey;
import uk.ac.cam.cares.twa.cities.Model;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertNotEquals;

public class ModelTest extends TestCase {

  private final StoreClientInterface kgClient = new RemoteStoreClient("http://localhost:9999/blazegraph/namespace/test/sparql", "http://localhost:9999/blazegraph/namespace/test/sparql");
  private final RemoteStoreClient dummyClient = Mockito.mock(RemoteStoreClient.class);

  private TestModel clearDatabaseAndPushModel() {
    kgClient.executeUpdate("CLEAR ALL");
    TestModel model = new TestModel(12345, 25, 3);
    model.dirtyAll();
    model.queuePushUpdate(true, true);
    Model.executeUpdates(kgClient, true);
    return model;
  }

  private int countTriples() {
    JSONArray response = new JSONArray(kgClient.execute("SELECT (COUNT(*) AS ?count) WHERE { ?a ?b ?c }"));
    return response.getJSONObject(0).getInt("count");
  }

  public void testModelInitialisation() {
    TestModel freshModel = new TestModel();
    // Check scalars initialise to null and vectors initialise to empty lists
    assertEquals(new ArrayList<>(), freshModel.getForwardVector());
    assertNull(freshModel.getIntProp());
    assertNull(freshModel.getModelProp());
    // Check that the "original field values" are not equal to the default values (dirty on ex nihilo initialisation).
    FieldInterface exampleScalarField = freshModel.getMetaModel().scalarFieldList.get(0).getValue();
    FieldInterface exampleVectorField = freshModel.getMetaModel().scalarFieldList.get(0).getValue();
    assertNotEquals(exampleScalarField.getMinimised(freshModel),
        freshModel.getOriginalFieldValues()[exampleScalarField.index]);
    assertNotEquals(exampleVectorField.getMinimised(freshModel),
        freshModel.getOriginalFieldValues()[exampleVectorField.index]);
  }

  public void testClearAll() {
    TestModel blankModel = new TestModel();
    TestModel nonBlankModel = new TestModel(123, 6, 0);
    assertNotEquals(blankModel, nonBlankModel);
    nonBlankModel.clearAll();
    assertEquals(blankModel, nonBlankModel);
  }

  public void testCleanAll() {
    // Baseline: new models are dirty.
    Model.clearUpdateQueue();
    Model testModel = new TestModel(2345, 4, 0);
    testModel.queuePushUpdate(true, true);
    assertNotEquals("", TestModel.getUpdateQueueString());
    Model.clearUpdateQueue();
    // A cleaned new model does not push updates.
    testModel = new TestModel(2345, 4, 0);
    testModel.cleanAll();
    testModel.queuePushUpdate(true, true);
    assertEquals("", TestModel.getUpdateQueueString());
    Model.clearUpdateQueue();
  }

  public void testDirtyAll() {
    TestModel testModel = new TestModel(2345, 4, 0);
    testModel.cleanAll();
    testModel.queuePushUpdate(true, true);
    assertEquals("", TestModel.getUpdateQueueString());
    testModel.dirtyAll();
    testModel.queuePushUpdate(true, true);
    String updateString = TestModel.getUpdateQueueString();
    assertNotEquals("", updateString);
    for (FieldKey key : testModel.getMetaModel().fieldMap.keySet()) {
      assertTrue(updateString.contains(key.predicate));
    }
  }

  public void testDirtyingBehaviour() {
    Model.clearUpdateQueue();
    TestModel testModel = new TestModel(23456, 5, 0);
    testModel.cleanAll();
    testModel.queuePushUpdate(true, true);
    assertEquals("", TestModel.getUpdateQueueString());
    // Test scalar dirtying
    testModel.setIntProp(3);
    assertFalse(TestModel.getUpdateQueueString().contains("intprop"));
    testModel.queuePushUpdate(true, true);
    assertTrue(TestModel.getUpdateQueueString().contains("intprop"));
    // Test pushing again does nothing
    String updateString = TestModel.getUpdateQueueString();
    testModel.queuePushUpdate(true, true);
    assertEquals(updateString, TestModel.getUpdateQueueString());
    // Test vector dirtying: add new element
    testModel.getForwardVector().add(4.0);
    assertFalse(TestModel.getUpdateQueueString().contains("forwardvector"));
    testModel.queuePushUpdate(true, true);
    assertTrue(TestModel.getUpdateQueueString().contains("forwardvector"));
    Model.executeUpdates(dummyClient, true);
    // Test vector dirtying: remove element
    testModel.getForwardVector().remove(0);
    testModel.queuePushUpdate(true, true);
    assertTrue(TestModel.getUpdateQueueString().contains("forwardvector"));
    Model.executeUpdates(dummyClient, true);
    // Test vector dirtying: order change should not cause dirtying
    testModel.getForwardVector().add(testModel.getForwardVector().remove(0));
    testModel.queuePushUpdate(true, true);
    assertEquals("", TestModel.getUpdateQueueString());
  }

  public void testSetIri() {
    Model model = new TestModel();
    model.setIri("testuuid-12345", "http://localhost:9999/testnamespace/");
    assertEquals(URI.create("http://localhost:9999/testnamespace/testmodels/testuuid-12345"), model.getIri());
  }

  public void testExecuteUpdateQueue() {
    Model.clearUpdateQueue();
    assertEquals("", TestModel.getUpdateQueueString());
    String updateContents =
        "INSERT DATA {\n" +
            "  <http://test/test> <http://test/test2> <http://test/test3> .\n" +
            "}\n";
    // Check correctly added
    TestModel.getUpdateQueue().add(updateContents);
    assertEquals(updateContents, TestModel.getUpdateQueueString());
    // Short update should not trigger flush without force
    Model.executeUpdates(dummyClient, false);
    assertEquals(updateContents, TestModel.getUpdateQueueString());
    // Check flush with force option works
    Model.executeUpdates(dummyClient, true);
    assertEquals("", TestModel.getUpdateQueueString());
    // Try long update (need >250000 characters)
    for (int i = 0; i < 50000; i++) {
      TestModel.getUpdateQueue().add("INSERT DATA {\n" +
          "  <http://test/test> <http://test/test" + i + "> <http://test/test3> .\n" +
          "}\n");
    }
    Model.executeUpdates(dummyClient, false);
    assertEquals("", TestModel.getUpdateQueueString());
  }

  public void testClearUpdateQueue() {
    String updateContents =
        "INSERT DATA {\n" +
            "  <http://test/test> <http://test/test2> <http://test/test3> .\n" +
            "}\n";
    // Check correctly added
    TestModel.getUpdateQueue().add(updateContents);
    assertNotEquals("", TestModel.getUpdateQueueString());
    Model.clearUpdateQueue();
    assertEquals("", TestModel.getUpdateQueueString());
  }

  public void testPushNewObject() {
    // Push all
    TestModel model = clearDatabaseAndPushModel();
    assertEquals(20 + 25 * 2, countTriples());
    // Push forward
    kgClient.executeUpdate("CLEAR ALL");
    model.dirtyAll();
    model.queuePushUpdate(true, false);
    Model.executeUpdates(kgClient, true);
    assertEquals(18 + 25, countTriples());
    // Push backward
    kgClient.executeUpdate("CLEAR ALL");
    model.dirtyAll();
    model.queuePushUpdate(false, true);
    Model.executeUpdates(kgClient, true);
    assertEquals(2 + 25, countTriples());
    // Push nothing, then forward, then backward, then all
    kgClient.executeUpdate("CLEAR ALL");
    model.dirtyAll();
    model.queuePushUpdate(false, false);
    Model.executeUpdates(kgClient, true);
    assertEquals(0, countTriples());
    model.dirtyAll();
    model.queuePushUpdate(true, false);
    Model.executeUpdates(kgClient, true);
    assertEquals(18 + 25, countTriples());
    model.dirtyAll();
    model.queuePushUpdate(false, true);
    Model.executeUpdates(kgClient, true);
    assertEquals(20 + 25 * 2, countTriples());
    model.dirtyAll();
    model.queuePushUpdate(true, true);
    Model.executeUpdates(kgClient, true);
    assertEquals(20 + 25 * 2, countTriples());
  }

  public void testDelete() {
    TestModel model1 = clearDatabaseAndPushModel();
    int firstModelCount = countTriples();
    TestModel model2 = new TestModel(3152, 8, 1);
    model2.queuePushUpdate(true, true);
    Model.executeUpdates(kgClient, true);
    int secondModelCount = countTriples();
    model1.queueDeletionUpdate();
    Model.executeUpdates(kgClient, true);
    assertEquals(secondModelCount - firstModelCount, countTriples());
    model2.queueDeletionUpdate();
    Model.executeUpdates(kgClient, true);
    assertEquals(0, countTriples());
    model1.queuePushUpdate(true, true);
    Model.executeUpdates(kgClient, true);
    assertEquals(firstModelCount, countTriples());
  }

  public void testPullAll() {
    TestModel model = clearDatabaseAndPushModel();
    model.queuePushUpdate(true, true);
    Model.executeUpdates(kgClient, true);
    TestModel pulledModel = new TestModel();
    pulledModel.setIri(model.getIri());
    pulledModel.pullAll(kgClient, 0);
    assertEquals(model, pulledModel);
  }

  public void testPullRecursive() {
    TestModel model = clearDatabaseAndPushModel();
    model.getModelProp().queuePushUpdate(true, true);
    model.getModelProp().getModelProp().queuePushUpdate(true, true);
    model.getModelProp().getModelProp().getModelProp().queuePushUpdate(true, true);
    Model.executeUpdates(kgClient, true);
    TestModel pulledModel = new TestModel();
    pulledModel.setIri(model.getIri());
    pulledModel.pullAll(kgClient, 2);
    assertEquals(model, pulledModel);
    assertEquals(model.getModelProp(), pulledModel.getModelProp());
    assertEquals(model.getModelProp().getModelProp(), pulledModel.getModelProp().getModelProp());
    assertNotEquals(model.getModelProp().getModelProp().getModelProp(), pulledModel.getModelProp().getModelProp().getModelProp());
    TestModel dummyThirdLevelModel = new TestModel();
    dummyThirdLevelModel.setIri(model.getModelProp().getModelProp().getModelProp().getIri());
    assertEquals(dummyThirdLevelModel, pulledModel.getModelProp().getModelProp().getModelProp());
  }

  public void testPullScalars() {
    TestModel model = clearDatabaseAndPushModel();
    TestModel pulledModel = new TestModel();
    pulledModel.setIri(model.getIri());
    pulledModel.pullScalars(kgClient);
    assertNotEquals(model, pulledModel);
    for (Map.Entry<FieldKey, FieldInterface> entry : model.getMetaModel().scalarFieldList) {
      assertTrue(entry.getValue().equals(model, pulledModel));
    }
  }

  public void testPullVector() {
    TestModel model = clearDatabaseAndPushModel();
    TestModel pulledModel = new TestModel();
    pulledModel.setIri(model.getIri());
    pulledModel.pullVector(Arrays.asList("forwardVector", "emptyForwardVector"), kgClient);
    assertNotEquals(model, pulledModel);
    for (Map.Entry<FieldKey, FieldInterface> entry : model.getMetaModel().vectorFieldList) {
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
    pulledModel.pullAll(kgClient, 0);
    pulledModel.dirtyAll();
    pulledModel.queuePushUpdate(true, true);
    Model.executeUpdates(kgClient, true);
    // Repulling should not have changed anything
    TestModel repulledModel = new TestModel();
    repulledModel.setIri(pulledModel.getIri());
    repulledModel.pullAll(kgClient, 0);
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
