package uk.ac.cam.cares.twa.cities.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import org.apache.jena.graph.Node;
import org.coode.owlapi.rdfxml.parser.AnonymousNodeChecker;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

@RunWith(MockitoJUnitRunner.class)
public class ClassSpecialisationCheckingTaskTest {

  @Test
  public void testNewClassSpecialisationCheckingTask() {
    ClassSpecialisationCheckingTask task;

    try {
      task = new ClassSpecialisationCheckingTask();
      assertNotNull(task);
    } catch (Exception e) {
      fail();
    }

  }

  @Test
  public void testNewClassSpecialisationCheckingTaskFields() {
    ClassSpecialisationCheckingTask task = new ClassSpecialisationCheckingTask();
    assertEquals(6, task.getClass().getDeclaredFields().length);

    Field taskIri;
    Field stop;
    Field dataQueue;
    Field resultQueue;
    Field targetGraph;
    Field anonymousNodeChecker;

    try {
      taskIri = task.getClass().getDeclaredField("taskIri");
      taskIri.setAccessible(true);
      assertEquals(taskIri.get(task), IRI.create("http://www.theworldavatar.com/ontologies/OntoInfer.owl#ClassSpecialisationCheckingTask"));
      stop = task.getClass().getDeclaredField("stop");
      stop.setAccessible(true);
      assertFalse((Boolean) stop.get(task));
      dataQueue = task.getClass().getDeclaredField("dataQueue");
      dataQueue.setAccessible(true);
      assertNull(dataQueue.get(task));
      resultQueue = task.getClass().getDeclaredField("resultQueue");
      resultQueue.setAccessible(true);
      assertNull(resultQueue.get(task));
      targetGraph = task.getClass().getDeclaredField("targetGraph");
      targetGraph.setAccessible(true);
      assertNull(targetGraph.get(task));
      anonymousNodeChecker = task.getClass().getDeclaredField("anonymousNodeChecker");
      anonymousNodeChecker.setAccessible(true);
      assertFalse(((AnonymousNodeChecker) anonymousNodeChecker.get(task)).isAnonymousNode(""));
    }  catch (NoSuchFieldException | IllegalAccessException e) {
      fail();
    }

  }

  @Test
  public void testNewClassSpecialisationCheckingTaskMethods() {
    ClassSpecialisationCheckingTask task = new ClassSpecialisationCheckingTask();
    assertEquals(9, task.getClass().getDeclaredMethods().length);
  }

  @Test
  public void testNewClassSpecialisationCheckingTaskGetTaskIriMethod() {
    ClassSpecialisationCheckingTask task = new ClassSpecialisationCheckingTask();
    try {
      Method getTaskIri = task.getClass().getDeclaredMethod("getTaskIri");
      getTaskIri.setAccessible(true);
      assertEquals(((IRI) getTaskIri.invoke(task)).toString(), "http://www.theworldavatar.com/ontologies/OntoInfer.owl#ClassSpecialisationCheckingTask");
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }
  }

  @Test
  public void testNewClassSpecialisationCheckingTaskSetStringMapQueueMethod() {
    ClassSpecialisationCheckingTask task = new ClassSpecialisationCheckingTask();
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
  public void testNewClassSpecialisationCheckingTaskSetResultMapQueueMethod() {
    ClassSpecialisationCheckingTask task = new ClassSpecialisationCheckingTask();
    try {
      Method setStringMapQueue = task.getClass().getDeclaredMethod("setResultQueue", BlockingQueue.class);
      setStringMapQueue.setAccessible(true);
      Field resultQueue = task.getClass().getDeclaredField("resultQueue");
      resultQueue.setAccessible(true);
      setStringMapQueue.invoke(task, new LinkedBlockingDeque<>());
      assertTrue(((BlockingDeque) resultQueue.get(task)).isEmpty());
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | NoSuchFieldException e) {
      fail();
    }
  }

  @Test
  public void testNewClassSpecialisationCheckingTaskSetTargetGraphMethod() {
    ClassSpecialisationCheckingTask task = new ClassSpecialisationCheckingTask();
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
  public void testNewClassSpecialisationCheckingTaskIsRunningMethod() {
    ClassSpecialisationCheckingTask task = new ClassSpecialisationCheckingTask();
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
  public void testNewClassSpecialisationCheckingTaskStopMethod() {
    ClassSpecialisationCheckingTask task = new ClassSpecialisationCheckingTask();

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
  public void testNewClassSpecialisationCheckingRunMethod() {
    ClassSpecialisationCheckingTask task = new ClassSpecialisationCheckingTask();

    try {
      Method run = task.getClass().getDeclaredMethod("run");
      run.setAccessible(true);
      Field stop = task.getClass().getDeclaredField("stop");
      stop.setAccessible(true);
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
      Method setResultQueue = task.getClass().getDeclaredMethod("setResultQueue", BlockingQueue.class);
      setResultQueue.setAccessible(true);
      setResultQueue.invoke(task, new LinkedBlockingDeque<>());
      Field resultQueue = task.getClass().getDeclaredField("resultQueue");
      resultQueue.setAccessible(true);
      String taskIRI = "http://www.theworldavatar.com/ontologies/OntoInfer.owl#ClassSpecialisationCheckingTask";
      JSONArray testOnto = new JSONArray("[{\"s\": \"http://www.test.com/1\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
          + "{\"s\": \"http://www.test.com/2\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
          + "{\"s\": \"http://www.test.com/3\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
          + "{\"s\": \"http://www.test.com/4\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
          + "{\"s\": \"http://www.test.com/1\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/2\"},"
          + "{\"s\": \"http://www.test.com/2\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/4\"},"
          + "{\"s\": \"http://www.test.com/3\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/4\"},"
          + "{\"s\": \"http://www.test.com/1\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/3\"}]");
      String ontoIri = "http://www.test.com/onto";
      String srcIri = "http://www.test.com/1";
      String dstIri = "http://www.test.com/4";

      Map<String, JSONArray> map = new HashMap<>();
      map.put(taskIRI, testOnto);
      map.put("ontologyIRI", new JSONArray().put(ontoIri));
      map.put("sourceIRI", new JSONArray().put(srcIri));
      map.put("destinationIRI", new JSONArray().put(dstIri));

      ((LinkedBlockingDeque) dataQueue.get(task)).put(map);

      run.invoke(task);
      assertTrue(((Map) ((BlockingQueue) resultQueue.get(task)).take()).get(taskIRI).toString()
          .equals(new JSONArray().put(new JSONObject().put(ontoIri, true)).toString()));

      assertFalse((Boolean) isRunning.invoke(task));
      stop.set(task, false);

      map = new HashMap<>();
      map.put(taskIRI, testOnto);
      map.put("ontologyIRI", new JSONArray().put(ontoIri));
      map.put("sourceIRI", new JSONArray().put(dstIri));
      map.put("destinationIRI", new JSONArray().put(srcIri));

      ((LinkedBlockingDeque) dataQueue.get(task)).put(map);

      run.invoke(task);
      assertTrue(((Map) ((BlockingQueue) resultQueue.get(task)).take()).get(taskIRI).toString()
          .equals(new JSONArray().put(new JSONObject().put(ontoIri, false)).toString()));

      assertFalse((Boolean) isRunning.invoke(task));
      stop.set(task, false);

      map = new HashMap<>();
      map.put(taskIRI, testOnto);
      map.put("ontologyIRI", new JSONArray().put(ontoIri));
      map.put("sourceIRI", new JSONArray().put("*"));
      map.put("destinationIRI", new JSONArray().put(dstIri));

      ((LinkedBlockingDeque) dataQueue.get(task)).put(map);

      run.invoke(task);
      assertTrue(((Map) ((BlockingQueue) resultQueue.get(task)).take()).get(taskIRI).toString()
          .equals(new JSONArray().put("http://www.test.com/1").put("http://www.test.com/2")
              .put("http://www.test.com/3").put("http://www.w3.org/2002/07/owl#Nothing").toString()));

      assertFalse((Boolean) isRunning.invoke(task));
      stop.set(task, false);

      map = new HashMap<>();
      map.put(taskIRI, testOnto);
      map.put("ontologyIRI", new JSONArray().put(ontoIri));
      map.put("sourceIRI", new JSONArray().put(srcIri));
      map.put("destinationIRI", new JSONArray().put("*"));

      ((LinkedBlockingDeque) dataQueue.get(task)).put(map);

      run.invoke(task);

      assertTrue(((Map) ((BlockingQueue) resultQueue.get(task)).take()).get(taskIRI).toString()
          .equals(new JSONArray().put("http://www.test.com/2").put("http://www.w3.org/2002/07/owl#Thing")
              .put("http://www.test.com/3").put("http://www.test.com/4").toString()));

      assertFalse((Boolean) isRunning.invoke(task));
      stop.set(task, false);

      map = new HashMap<>();
      map.put(taskIRI, testOnto);
      map.put("ontologyIRI", new JSONArray().put(ontoIri));
      map.put("sourceIRI", new JSONArray().put("*"));
      map.put("destinationIRI", new JSONArray().put("*"));

      ((LinkedBlockingDeque) dataQueue.get(task)).put(map);

      run.invoke(task);

      assertTrue(((Map) ((BlockingQueue) resultQueue.get(task)).take()).get(taskIRI).toString()
          .equals(new JSONArray().put("http://www.w3.org/2002/07/owl#Nothing").toString()));

      assertFalse((Boolean) isRunning.invoke(task));

    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | NoSuchFieldException | InterruptedException e) {
      fail();
    }

  }

  @Test
  public void testNewClassSpecialisationCheckingTaskGetReasonerOutputMethod() {
    ClassSpecialisationCheckingTask task = new ClassSpecialisationCheckingTask();

    try {
      Method getReasonerOutput = task.getClass().getDeclaredMethod("getReasonerOutput",
          Reasoner.class, String.class, String.class, String.class);
      getReasonerOutput.setAccessible(true);
      Method createModel = task.getClass().getDeclaredMethod("createModel", JSONArray.class);
      createModel.setAccessible(true);
      String ontoIri = "http://www.test.com/onto";
      String srcIri = "http://www.test.com/1";
      String dstIri = "http://www.test.com/4";
      JSONArray output;
      JSONArray input = new JSONArray(
          "[{\"s\": \"http://www.test.com/1\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
          + "{\"s\": \"http://www.test.com/2\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
          + "{\"s\": \"http://www.test.com/3\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
          + "{\"s\": \"http://www.test.com/4\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
          + "{\"s\": \"http://www.test.com/1\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/2\"},"
          + "{\"s\": \"http://www.test.com/2\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/4\"},"
          + "{\"s\": \"http://www.test.com/3\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/4\"},"
          + "{\"s\": \"http://www.test.com/1\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/3\"}]");
      OWLOntology ontology = (OWLOntology) createModel.invoke(task, input);
      Reasoner reasoner = new Reasoner(ontology);

      //IRI & IRI case
      output = (JSONArray) getReasonerOutput.invoke(task, reasoner, ontoIri, srcIri, dstIri);
      assertTrue(output.toString().equals(new JSONArray().put(new JSONObject().put(ontoIri, true)).toString()));
      output = (JSONArray) getReasonerOutput.invoke(task, reasoner, ontoIri, dstIri, srcIri);
      assertTrue(output.toString().equals(new JSONArray().put(new JSONObject().put(ontoIri, false)).toString()));

      //* & IRI case
      output = (JSONArray) getReasonerOutput.invoke(task, reasoner, ontoIri, "*", dstIri);
      assertTrue(output.toString().equals(new JSONArray().put("http://www.test.com/1")
          .put("http://www.test.com/2").put("http://www.test.com/3").put("http://www.w3.org/2002/07/owl#Nothing").toString()));

      //IRI & * case
      output = (JSONArray) getReasonerOutput.invoke(task, reasoner, ontoIri, srcIri, "*");
      assertTrue(output.toString().equals(new JSONArray().put("http://www.test.com/2")
          .put("http://www.w3.org/2002/07/owl#Thing").put("http://www.test.com/3").put("http://www.test.com/4").toString()));

      //* & * case
      output = (JSONArray) getReasonerOutput.invoke(task, reasoner, ontoIri, "*", "*");
      assertTrue(output.toString().equals(new JSONArray().put("http://www.w3.org/2002/07/owl#Nothing").toString()));

    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail();
    }

  }

  @Test
  public void testNewClassSpecialisationCheckingTaskCreateModelMethod() {
    ClassSpecialisationCheckingTask task = new ClassSpecialisationCheckingTask();

    try {
      Method createModel = task.getClass().getDeclaredMethod("createModel", JSONArray.class);
      createModel.setAccessible(true);
      JSONArray a = new JSONArray("[{\"s\": \"http://www.test.com/1\", \"p\": \"http://www.test.com/2#p\", \"o\": \"http://www.test.com/3\"}]");
      OWLOntology o = (OWLOntology) createModel.invoke(task, a);
      assertEquals(o.getAxiomCount(), 1);
      assertEquals(o.getAxioms().toArray()[0].toString(),
          "AnnotationAssertion(<http://www.test.com/2#p> <http://www.test.com/1> <http://www.test.com/3>)");

    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }

  }


}
