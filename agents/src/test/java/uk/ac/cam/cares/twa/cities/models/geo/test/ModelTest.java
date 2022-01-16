package uk.ac.cam.cares.twa.cities.models.geo.test;

import junit.framework.TestCase;
import org.apache.jena.update.UpdateRequest;
import org.json.JSONArray;
import org.mockito.Mockito;
import uk.ac.cam.cares.jps.base.interfaces.StoreClientInterface;
import uk.ac.cam.cares.jps.base.query.RemoteStoreClient;
import uk.ac.cam.cares.twa.cities.FieldInterface;
import uk.ac.cam.cares.twa.cities.FieldKey;
import uk.ac.cam.cares.twa.cities.MetaModel;
import uk.ac.cam.cares.twa.cities.Model;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertNotEquals;

public class ModelTest extends TestCase {

  private final StoreClientInterface kgClient = new RemoteStoreClient("http://localhost:9999/blazegraph/namespace/test/sparql", "http://localhost:9999/blazegraph/namespace/test/sparql");
  private final RemoteStoreClient dummyClient = Mockito.mock(RemoteStoreClient.class);

  private final Field metaModel;
  private final Field updateQueue;

  public ModelTest() throws NoSuchFieldException {
    metaModel = Model.class.getDeclaredField("metaModel");
    metaModel.setAccessible(true);
    updateQueue = Model.class.getDeclaredField("updateQueue");
    updateQueue.setAccessible(true);
  }

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
    assertNotEquals("", updateQueue.get(null).toString());
    Model.clearUpdateQueue();
    // A cleaned new model does not push updates.
    testModel = new TestModel(2345, 4, 0);
    testModel.cleanAll();
    testModel.queuePushUpdate(true, true);
    assertEquals("", updateQueue.get(null).toString());
    Model.clearUpdateQueue();
  }

  public void testDirtyAll() throws IllegalAccessException {
    TestModel testModel = new TestModel(2345, 4, 0);
    testModel.cleanAll();
    testModel.queuePushUpdate(true, true);
    assertEquals("", updateQueue.get(null).toString());
    testModel.dirtyAll();
    testModel.queuePushUpdate(true, true);
    String updateString = updateQueue.get(null).toString();
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
    assertEquals("", updateQueue.get(null).toString());
    // Test scalar dirtying
    testModel.setIntProp(3);
    assertFalse(updateQueue.get(null).toString().contains("intprop"));
    testModel.queuePushUpdate(true, true);
    assertTrue(updateQueue.get(null).toString().contains("intprop"));
    // Test pushing again does nothing
    String updateString = updateQueue.get(null).toString();
    testModel.queuePushUpdate(true, true);
    assertEquals(updateString, updateQueue.get(null).toString());
    // Test vector dirtying: add new element
    testModel.getForwardVector().add(4.0);
    assertFalse(updateQueue.get(null).toString().contains("forwardvector"));
    testModel.queuePushUpdate(true, true);
    assertTrue(updateQueue.get(null).toString().contains("forwardvector"));
    Model.executeUpdates(dummyClient, true);
    // Test vector dirtying: remove element
    testModel.getForwardVector().remove(0);
    testModel.queuePushUpdate(true, true);
    assertTrue(updateQueue.get(null).toString().contains("forwardvector"));
    Model.executeUpdates(dummyClient, true);
    // Test vector dirtying: order change should not cause dirtying
    testModel.getForwardVector().add(testModel.getForwardVector().remove(0));
    testModel.queuePushUpdate(true, true);
    assertEquals("", updateQueue.get(null).toString());
  }

  public void testSetIri() {
    Model model = new TestModel();
    model.setIri("testuuid-12345", "http://localhost:9999/testnamespace/");
    assertEquals(URI.create("http://localhost:9999/testnamespace/testmodels/testuuid-12345"), model.getIri());
  }

  public void testExecuteUpdateQueue() throws IllegalAccessException {
    Model.clearUpdateQueue();
    assertEquals("", updateQueue.get(null).toString());
    String updateContents =
        "INSERT DATA {\n" +
            "  <http://test/test> <http://test/test2> <http://test/test3> .\n" +
            "}\n";
    // Check correctly added
    ((UpdateRequest)updateQueue.get(null)).add(updateContents);
    assertEquals(updateContents, updateQueue.get(null).toString());
    // Short update should not trigger flush without force
    Model.executeUpdates(dummyClient, false);
    assertEquals(updateContents, updateQueue.get(null).toString());
    // Check flush with force option works
    Model.executeUpdates(dummyClient, true);
    assertEquals("", updateQueue.get(null).toString());
    // Try long update (need >250000 characters)
    for (int i = 0; i < 50000; i++) {
      ((UpdateRequest)updateQueue.get(null)).add("INSERT DATA {\n" +
          "  <http://test/test> <http://test/test" + i + "> <http://test/test3> .\n" +
          "}\n");
    }
    Model.executeUpdates(dummyClient, false);
    assertEquals("", updateQueue.get(null).toString());
  }

  public void testClearUpdateQueue() throws IllegalAccessException {
    String updateContents =
        "INSERT DATA {\n" +
            "  <http://test/test> <http://test/test2> <http://test/test3> .\n" +
            "}\n";
    // Check correctly added
    ((UpdateRequest)updateQueue.get(null)).add(updateContents);
    assertNotEquals("", updateQueue.get(null).toString());
    Model.clearUpdateQueue();
    assertEquals("", updateQueue.get(null).toString());
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

  public void testPullScalars() throws IllegalAccessException {
    TestModel model = clearDatabaseAndPushModel();
    TestModel pulledModel = new TestModel();
    pulledModel.setIri(model.getIri());
    pulledModel.pullScalars(kgClient);
    assertNotEquals(model, pulledModel);
    for (Map.Entry<FieldKey, FieldInterface> entry : ((MetaModel)metaModel.get(model)).scalarFieldList) {
      assertTrue(entry.getValue().equals(model, pulledModel));
    }
  }

  public void testPullVector() throws IllegalAccessException {
    TestModel model = clearDatabaseAndPushModel();
    TestModel pulledModel = new TestModel();
    pulledModel.setIri(model.getIri());
    pulledModel.pullVector(Arrays.asList("forwardVector", "emptyForwardVector"), kgClient);
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
