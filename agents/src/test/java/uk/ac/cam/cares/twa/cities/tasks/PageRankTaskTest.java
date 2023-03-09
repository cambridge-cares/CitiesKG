package uk.ac.cam.cares.twa.cities.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.graph.Graph;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import net.rootdev.jenajung.JenaJungGraph;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.graph.NodeFactory;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.semanticweb.owlapi.model.IRI;
import org.apache.jena.graph.Node;
import org.json.JSONArray;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.agents.GraphInferenceAgent;

@RunWith(MockitoJUnitRunner.class)
public class PageRankTaskTest {

  @Test
  public void testNewPageRankTask() {
    PageRankTask task;

    try {
      task = new PageRankTask();
      assertNotNull(task);
    } catch (Exception e) {
      fail();
    }

  }

  @Test
  public void testNewPageRankTaskFields() {
    PageRankTask task = new PageRankTask();
    assertEquals(4, task.getClass().getDeclaredFields().length);

    Field taskIri;
    Field stop;
    Field dataQueue;
    Field targetGraph;

    try {
      taskIri = task.getClass().getDeclaredField("taskIri");
      taskIri.setAccessible(true);
      assertEquals(taskIri.get(task), IRI.create("http://www.theworldavatar.com/ontologies/OntoInfer.owl#PageRankTask"));
      stop = task.getClass().getDeclaredField("stop");
      stop.setAccessible(true);
      assertFalse((Boolean) stop.get(task));
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
  public void testNewPageRankTaskMethods() {
    PageRankTask task = new PageRankTask();
    assertEquals(11, task.getClass().getDeclaredMethods().length);
  }

  @Test
  public void testNewPageRankTaskGetTaskIriMethod() {
    PageRankTask task = new PageRankTask();
    try {
      Method getTaskIri = task.getClass().getDeclaredMethod("getTaskIri");
      getTaskIri.setAccessible(true);
      assertEquals(((IRI) getTaskIri.invoke(task)).toString(), "http://www.theworldavatar.com/ontologies/OntoInfer.owl#PageRankTask");
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }
  }

  @Test
  public void testNewPageRankTaskSetStringMapQueueMethod() {
    PageRankTask task = new PageRankTask();
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
  public void testNewPageRankTaskSetTargetGraphMethod() {
    PageRankTask task = new PageRankTask();
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
  public void testNewPageRankTaskIsRunningMethod() {
    PageRankTask task = new PageRankTask();
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
  public void testNewPageRankTaskStopMethod() {
    PageRankTask task = new PageRankTask();

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
  public void testNewPageRankRunMethod() {
    PageRankTask task = new PageRankTask();

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
        Map<String, JSONArray> map = Collections.singletonMap("http://www.theworldavatar.com/ontologies/OntoInfer.owl#PageRankTask",
            new JSONArray("[{\"s\": \"http://www.test.com/1\", \"p\": \"http://www.test.com/2#p\", \"o\": \"http://www.test.com/3\"}]"));
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
  public void testNewPageRankTaskCreateGraphMethod() {
    PageRankTask task = new PageRankTask();

    try {
      Method createGraph = task.getClass().getDeclaredMethod("createGraph", JSONArray.class);
      createGraph.setAccessible(true);
      JSONArray a = new JSONArray("[{\"s\": \"http://www.test.com/1\", \"p\": \"http://www.test.com/2#p\", \"o\": \"http://www.test.com/3\"}]");
      JenaJungGraph g = (JenaJungGraph) createGraph.invoke(task, a);
      assertEquals(g.getEdgeCount(), 1);
      assertEquals(g.getVertexCount(), 2);
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }

  }

  @Test
  public void testNewPageRankTaskStoreResultsMethod() {
    PageRankTask task = new PageRankTask();

    try {
      Method storeResults = task.getClass().getDeclaredMethod("storeResults", Graph.class, PageRank.class);
      storeResults.setAccessible(true);
      Method createGraph = task.getClass().getDeclaredMethod("createGraph", JSONArray.class);
      createGraph.setAccessible(true);
      Method setTargetGraph = task.getClass().getDeclaredMethod("setTargetGraph", String.class);
      setTargetGraph.setAccessible(true);
      setTargetGraph.invoke(task, "http:/www.test.com/");
      JSONArray a = new JSONArray("[{\"s\": \"http://www.test.com/1\", \"p\": \"http://www.test.com/2#p\", \"o\": \"http://www.test.com/3\"}]");
      JenaJungGraph g = (JenaJungGraph) createGraph.invoke(task, a);
      PageRank<RDFNode, Statement> ranker = new PageRank<>(g, 0.3);
      ranker.evaluate();
      try (MockedStatic<AccessAgentCaller> aacMock = mockStatic(AccessAgentCaller.class)) {
        //Do not execute any code with AccessAgentCaller - it is tested separately
        aacMock.when(() -> AccessAgentCaller.updateStore(anyString(), anyString()))
            .thenAnswer((Answer<Void>) invocation -> null);
        //Call method that uses AccessAgentCaller inside
        //Other methods from this class are tested separately
        storeResults.invoke(task, g, ranker);
      }

    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }
  }
  
  @Test
  public void testNewPageRankTaskPrepareUpdateMethod() {
    PageRankTask task = new PageRankTask();

    try {
      Method prepareUpdate = task.getClass().getDeclaredMethod("prepareUpdate", PageRank.class, UpdateBuilder.class, Object.class);
      prepareUpdate.setAccessible(true);
      Method createGraph = task.getClass().getDeclaredMethod("createGraph", JSONArray.class);
      createGraph.setAccessible(true);
      Method setTargetGraph = task.getClass().getDeclaredMethod("setTargetGraph", String.class);
      setTargetGraph.setAccessible(true);
      setTargetGraph.invoke(task, "http:/www.test.com/");
      JSONArray a = new JSONArray("[{\"s\": \"http://www.test.com/1\", \"p\": \"http://www.test.com/2#p\", \"o\": \"http://www.test.com/3\"}]");
      Graph<RDFNode, Statement> g = (JenaJungGraph) createGraph.invoke(task, a);
      PageRank<RDFNode, Statement> ranker = new PageRank<>(g, 0.3);
      ranker.evaluate();
      UpdateBuilder ub = new UpdateBuilder();
      ub.addPrefix(GraphInferenceAgent.ONINF_PREFIX, GraphInferenceAgent.ONINF_SCHEMA);
      Object vert = g.getVertices().toArray()[0];
      prepareUpdate.invoke(task, ranker, ub, vert);
      String upds =  ub.build().toString();
      assertTrue(upds.contains("INSERT DATA {\n"));
      assertTrue(upds.contains("  GRAPH <http:/www.test.com/OntoInfer/> {\n"));
      assertTrue(upds.contains("    <http:/www.test.com/OntoInfer/"));
      assertTrue(upds.contains(" <http://www.theworldavatar.com/ontologies/OntoInfer.owl#hasInferenceObject> <http://www.test.com/1> .\n"));
      assertTrue(upds.contains(" <http://www.theworldavatar.com/ontologies/OntoInfer.owl#hasInferenceAlgorithm> <http://www.theworldavatar.com/ontologies/OntoInfer.owl#PageRankAlgorithm> .\n"));
      assertTrue(upds.contains(" <http://www.theworldavatar.com/ontologies/OntoInfer.owl#hasInferredValue> \"0.37037037037037035000\"^^<http://www.w3.org/2001/XMLSchema#double> .\n"));
      assertTrue(upds.contains("  }\n"));
      assertTrue(upds.contains("}"));
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }

  }

  @Test
  public void testNewPageRankTaskPrepareUpdateBuilderMethod() {
    PageRankTask task = new PageRankTask();

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
  public void testNewPageRankTaskPersistUpdateMethod() {
    PageRankTask task = new PageRankTask();

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
