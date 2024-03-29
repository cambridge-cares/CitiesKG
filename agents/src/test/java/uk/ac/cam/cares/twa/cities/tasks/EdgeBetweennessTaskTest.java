package uk.ac.cam.cares.twa.cities.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.semanticweb.owlapi.model.IRI;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.agents.GraphInferenceAgent;

@RunWith(MockitoJUnitRunner.class)
public class EdgeBetweennessTaskTest {

  @Test
  public void testNewEdgeBetweennessTask() {
    EdgeBetweennessTask task;

    try {
      task = new EdgeBetweennessTask();
      assertNotNull(task);
    } catch (Exception e) {
      fail();
    }

  }

  @Test
  public void testNewEdgeBetweennessTaskFields() {
    EdgeBetweennessTask task = new EdgeBetweennessTask();
    assertEquals(5, task.getClass().getDeclaredFields().length);

    Field taskIri;
    Field stop;
    Field dataQueue;
    Field targetGraph;
    Field urlToNum;

    try {
      taskIri = task.getClass().getDeclaredField("taskIri");
      taskIri.setAccessible(true);
      assertEquals(taskIri.get(task), IRI.create("http://www.theworldavatar.com/ontologies/OntoInfer.owl#EdgeBetweennessTask"));
      stop = task.getClass().getDeclaredField("stop");
      stop.setAccessible(true);
      assertFalse((Boolean) stop.get(task));
      urlToNum = task.getClass().getDeclaredField("urlToNum");
      urlToNum.setAccessible(true);
      assertEquals(((Map) urlToNum.get(task)).size(), 0);
      dataQueue = task.getClass().getDeclaredField("dataQueue");
      dataQueue.setAccessible(true);
      assertNull(dataQueue.get(task));
      targetGraph = task.getClass().getDeclaredField("targetGraph");
      targetGraph.setAccessible(true);
      assertNull(targetGraph.get(task));
    }  catch (NoSuchFieldException | IllegalAccessException e) {
      fail();
    }

  }

  @Test
  public void testNewEdgeBetweennessTaskMethods() {
    EdgeBetweennessTask task = new EdgeBetweennessTask();
    assertEquals(12, task.getClass().getDeclaredMethods().length);
  }

  @Test
  public void testNewEdgeBetweennessTaskGetTaskIriMethod() {
    EdgeBetweennessTask task = new EdgeBetweennessTask();
    try {
      Method getTaskIri = task.getClass().getDeclaredMethod("getTaskIri");
      getTaskIri.setAccessible(true);
      assertEquals(((IRI) getTaskIri.invoke(task)).toString(), "http://www.theworldavatar.com/ontologies/OntoInfer.owl#EdgeBetweennessTask");
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }
  }

  @Test
  public void testNewEdgeBetweennessTaskSetStringMapQueueMethod() {
    EdgeBetweennessTask task = new EdgeBetweennessTask();
    try {
      Method setStringMapQueue = task.getClass().getDeclaredMethod("setStringMapQueue", BlockingQueue.class);
      setStringMapQueue.setAccessible(true);
      Field dataQueue = task.getClass().getDeclaredField("dataQueue");
      dataQueue.setAccessible(true);
      setStringMapQueue.invoke(task, new LinkedBlockingDeque<>());
      assertTrue(((BlockingDeque) dataQueue.get(task)).isEmpty());
   } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | NoSuchFieldException e) {
      fail();
    }
  }

  @Test
  public void testNewEdgeBetweennessTaskSetTargetGraphMethod() {
    EdgeBetweennessTask task = new EdgeBetweennessTask();
    try {
      Method setTargetGraph = task.getClass().getDeclaredMethod("setTargetGraph", String.class);
      setTargetGraph.setAccessible(true);
      Field targetGraph = task.getClass().getDeclaredField("targetGraph");
      targetGraph.setAccessible(true);
      setTargetGraph.invoke(task, "http:/www.test.com/");
      assertEquals(((Node) targetGraph.get(task)).getURI(), "http:/www.test.com/OntoInfer/");
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | NoSuchFieldException e) {
      fail();
    }
  }

  @Test
  public void testNewEdgeBetweennessTaskIsRunningMethod() {
    EdgeBetweennessTask task = new EdgeBetweennessTask();
    try {
      Method isRunning = task.getClass().getDeclaredMethod("isRunning");
      isRunning.setAccessible(true);
      Field stopF = task.getClass().getDeclaredField("stop");
      stopF.setAccessible(true);
      assertTrue((Boolean) isRunning.invoke(task));
      Method stopM = task.getClass().getDeclaredMethod("stop");
      stopM.setAccessible(true);
      stopM.invoke(task);
      assertFalse((Boolean) isRunning.invoke(task));
    } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }
  }

  @Test
  public void testNewEdgeBetweennessTaskStopMethod() {
    EdgeBetweennessTask task = new EdgeBetweennessTask();

    try {
      Field stopF = task.getClass().getDeclaredField("stop");
      stopF.setAccessible(true);
      assertFalse((Boolean) stopF.get(task));
      Method stopM = task.getClass().getDeclaredMethod("stop");
      stopM.setAccessible(true);
      stopM.invoke(task);
      assertTrue((Boolean) stopF.get(task));
    } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }

  }

  @Test
  public void testNewEdgeBetweennessRunMethod() {
    EdgeBetweennessTask task = new EdgeBetweennessTask();

    try {
      Method run = task.getClass().getDeclaredMethod("run");
      run.setAccessible(true);
      Method isRunning = task.getClass().getDeclaredMethod("isRunning");
      isRunning.setAccessible(true);
      Method setTargetGraph = task.getClass().getDeclaredMethod("setTargetGraph", String.class);
      setTargetGraph.setAccessible(true);
      setTargetGraph.invoke(task, "http:/www.test.com/");
      new LinkedBlockingDeque<>();
      Method setStringMapQueue = task.getClass().getDeclaredMethod("setStringMapQueue", BlockingQueue.class);
      setStringMapQueue.setAccessible(true);
      setStringMapQueue.invoke(task, new LinkedBlockingDeque<>());
      Field dataQueue = task.getClass().getDeclaredField("dataQueue");
      dataQueue.setAccessible(true);

      try (MockedStatic<AccessAgentCaller> aacMock = mockStatic(AccessAgentCaller.class)) {
        //Do not execute any code with AccessAgentCaller - it is tested separately
        aacMock.when(() -> AccessAgentCaller.updateStore(anyString(), anyString()))
            .thenAnswer((Answer<Void>) invocation -> null);
        Map<String, JSONArray> map = Collections.singletonMap("http://www.theworldavatar.com/ontologies/OntoInfer.owl#EdgeBetweennessTask",
            new JSONArray("[{\"s\": \"http://www.test.com/1\", \"p\": \"http://www.test.com/2#p\", \"o\": \"http://www.test.com/3\"},"
                + "{\"s\": \"http://www.test.com/4\", \"p\": \"http://www.test.com/5#p\", \"o\": \"http://www.test.com/6\"},"
                + "{\"s\": \"http://www.test.com/7\", \"p\": \"http://www.test.com/8#p\", \"o\": \"http://www.test.com/9\"}]"));
        ((LinkedBlockingDeque) dataQueue.get(task)).put(map);
        //Call method that uses AccessAgentCaller inside
        //Other methods from this class are tested separately
        run.invoke(task);
        assertFalse((Boolean) isRunning.invoke(task));
      }

    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | NoSuchFieldException | InterruptedException e) {
      fail();
    }

  }

  @Test
  public void testNewEdgeBetweennessTaskCreateGraphMethod() {
    EdgeBetweennessTask task = new EdgeBetweennessTask();

    try {
      Method createGraph = task.getClass().getDeclaredMethod("createGraph", JSONArray.class);
      createGraph.setAccessible(true);
      JSONArray a = new JSONArray("[{\"s\": \"http://www.test.com/1\", \"p\": \"http://www.test.com/2#p\", \"o\": \"http://www.test.com/3\"}]");
      UndirectedSparseGraph g = (UndirectedSparseGraph) createGraph.invoke(task, a);
      assertEquals(g.getEdgeCount(), 1);
      assertEquals(g.getVertexCount(), 2);
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }

  }

  @Test
  public void testNewEdgeBetweennessTaskRemapSetMethod() {
    EdgeBetweennessTask task = new EdgeBetweennessTask();

    try {
      Method remapSet = task.getClass().getDeclaredMethod("remapSet", Set.class);
      remapSet.setAccessible(true);
      Field urlToNum = task.getClass().getDeclaredField("urlToNum");
      urlToNum.setAccessible(true);
      ((Map) urlToNum.get(task)).put("http://www.test.com/1", 1);
      ((Map) urlToNum.get(task)).put("http://www.test.com/2", 2);
      ((Map) urlToNum.get(task)).put("http://www.test.com/3", 3);
      Set clusters = new HashSet<>();
      Set cluserA = new HashSet<>();
      Set cluserB = new HashSet<>();
      cluserA.add(2);
      cluserB.add(1);
      cluserB.add(3);
      clusters.add(cluserA);
      clusters.add(cluserB);
      Set remapped = (Set) remapSet.invoke(task, clusters);
      assertEquals(remapped.size(), 2);
      Set remappedA = (Set) remapped.toArray()[0];
      assertEquals(remappedA.size(), 1);
      Set remappedB = (Set) remapped.toArray()[1];
      assertEquals(remappedB.size(), 2);
      assertTrue(remappedA.contains("http://www.test.com/2"));
      assertTrue(remappedB.contains("http://www.test.com/1"));
      assertTrue(remappedB.contains("http://www.test.com/3"));
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | NoSuchFieldException e) {
      fail();
    }

  }

  @Test
  public void testNewEdgeBetweennessTaskStoreResultsMethod() {
    EdgeBetweennessTask task = new EdgeBetweennessTask();

    try {
      Method storeResults = task.getClass().getDeclaredMethod("storeResults", HashSet.class);
      storeResults.setAccessible(true);
      Method createGraph = task.getClass().getDeclaredMethod("createGraph", JSONArray.class);
      createGraph.setAccessible(true);
      Method setTargetGraph = task.getClass().getDeclaredMethod("setTargetGraph", String.class);
      setTargetGraph.setAccessible(true);
      setTargetGraph.invoke(task, "http:/www.test.com/");
      HashSet<HashSet<String>> clusters = new HashSet<>();
      HashSet<String> cluserA = new HashSet<>();
      HashSet<String> cluserB = new HashSet<>();
      cluserA.add("http://www.test.com/2");
      cluserB.add("http://www.test.com/1");
      cluserB.add("http://www.test.com/3");
      clusters.add(cluserA);
      clusters.add(cluserB);
      try (MockedStatic<AccessAgentCaller> aacMock = mockStatic(AccessAgentCaller.class)) {
        //Do not execute any code with AccessAgentCaller - it is tested separately
        aacMock.when(() -> AccessAgentCaller.updateStore(anyString(), anyString()))
            .thenAnswer((Answer<Void>) invocation -> null);
        //Call method that uses AccessAgentCaller inside
        //Other methods from this class are tested separately
        storeResults.invoke(task, clusters);
      }

    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }

  }
  
  @Test
  public void testNewEdgeBetweennessTaskPrepareUpdateMethod() {
    EdgeBetweennessTask task = new EdgeBetweennessTask();

    try {
      Method prepareUpdate = task.getClass().getDeclaredMethod("prepareUpdate", int.class, UpdateBuilder.class, String.class);
      prepareUpdate.setAccessible(true);
      Method setTargetGraph = task.getClass().getDeclaredMethod("setTargetGraph", String.class);
      setTargetGraph.setAccessible(true);
      setTargetGraph.invoke(task, "http:/www.test.com/");
      UpdateBuilder ub = new UpdateBuilder();
      ub.addPrefix(GraphInferenceAgent.ONINF_PREFIX, GraphInferenceAgent.ONINF_SCHEMA);
      int clusterNum = 2;
      String vert = "http://www.test.com/1";
      prepareUpdate.invoke(task, clusterNum, ub, vert);
      String upds =  ub.build().toString();
      assertTrue(upds.contains("INSERT DATA {\n"));
      assertTrue(upds.contains("  GRAPH <http:/www.test.com/OntoInfer/> {\n"));
      assertTrue(upds.contains("    <http:/www.test.com/OntoInfer/"));
      assertTrue(upds.contains(" <http://www.theworldavatar.com/ontologies/OntoInfer.owl#hasInferenceObject> <http://www.test.com/1> .\n"));
      assertTrue(upds.contains(" <http://www.theworldavatar.com/ontologies/OntoInfer.owl#hasInferenceAlgorithm> <http://www.theworldavatar.com/ontologies/OntoInfer.owl#EdgeBetweennessAlgorithm> .\n"));
      assertTrue(upds.contains(" <http://www.theworldavatar.com/ontologies/OntoInfer.owl#hasInferredValue> 2 .\n"));
      assertTrue(upds.contains("  }\n"));
      assertTrue(upds.contains("}"));
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }

  }

  @Test
  public void testNewEdgeBetweennessTaskPrepareUpdateBuilderMethod() {
    EdgeBetweennessTask task = new EdgeBetweennessTask();

    try {
      Method prepareUpdateBuilder = task.getClass().getDeclaredMethod("prepareUpdateBuilder");
      prepareUpdateBuilder.setAccessible(true);
      UpdateBuilder ub = (UpdateBuilder) prepareUpdateBuilder.invoke(task);
      ub.addInsert("http://www.test.com/test/", NodeFactory.createURI("http://www.test.com/1"), "oninf:test",
          NodeFactory.createURI("http://www.test.com/2"));
      assertEquals(ub.build().toString(), "INSERT DATA {\n"
          + "  GRAPH \"http://www.test.com/test/\" {\n"
          + "    <http://www.test.com/1> <http://www.theworldavatar.com/ontologies/OntoInfer.owl#test> <http://www.test.com/2> .\n"
          + "  }\n"
          + "}\n");

    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }

  }

  @Test
  public void testNewEdgeBetweennessTaskPersistUpdateMethod() {
    EdgeBetweennessTask task = new EdgeBetweennessTask();

    try {
      Method persistUpdate = task.getClass().getDeclaredMethod("persistUpdate", UpdateBuilder.class);
      persistUpdate.setAccessible(true);
      UpdateBuilder ub = new UpdateBuilder();
      ub.addInsert("http://www.test.com/test/", NodeFactory.createURI("http://www.test.com/1"), "oninf:test",
          NodeFactory.createURI("http://www.test.com/2"));


      try (MockedStatic<AccessAgentCaller> aacMock = mockStatic(AccessAgentCaller.class)) {
        //Do not execute any code with AccessAgentCaller - it is tested separately
        aacMock.when(() -> AccessAgentCaller.updateStore(anyString(), anyString())).thenAnswer((Answer<Void>) invocation -> null);
        //Call method that uses AccessAgentCaller inside
        ub = (UpdateBuilder) persistUpdate.invoke(task, ub);

        ub.addInsert("http://www.test.com/test/", NodeFactory.createURI("http://www.test.com/1"), "oninf:test",
            NodeFactory.createURI("http://www.test.com/2"));
        assertEquals(ub.build().toString(), "INSERT DATA {\n"
            + "  GRAPH \"http://www.test.com/test/\" {\n"
            + "    <http://www.test.com/1> <http://www.theworldavatar.com/ontologies/OntoInfer.owl#test> <http://www.test.com/2> .\n"
            + "  }\n"
            + "}\n");
      }

    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }

  }

}
