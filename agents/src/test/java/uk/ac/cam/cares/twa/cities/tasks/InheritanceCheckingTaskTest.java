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
public class InheritanceCheckingTaskTest {

  @Test
  public void testNewInheritanceCheckingTask() {
    InheritanceCheckingTask task;

    try {
      task = new InheritanceCheckingTask();
      assertNotNull(task);
    } catch (Exception e) {
      fail();
    }

  }

  @Test
  public void testNewInheritanceCheckingTaskFields() {
    InheritanceCheckingTask task = new InheritanceCheckingTask();
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
      assertEquals(taskIri.get(task), IRI.create("http://www.theworldavatar.com/ontologies/OntoInfer.owl#InheritanceCheckingTask"));
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
  public void testNewInheritanceCheckingTaskMethods() {
    InheritanceCheckingTask task = new InheritanceCheckingTask();
    assertEquals(9, task.getClass().getDeclaredMethods().length);
  }

  @Test
  public void testNewInheritanceCheckingTaskGetTaskIriMethod() {
    InheritanceCheckingTask task = new InheritanceCheckingTask();
    try {
      Method getTaskIri = task.getClass().getDeclaredMethod("getTaskIri");
      getTaskIri.setAccessible(true);
      assertEquals(((IRI) getTaskIri.invoke(task)).toString(), "http://www.theworldavatar.com/ontologies/OntoInfer.owl#InheritanceCheckingTask");
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }
  }

  @Test
  public void testNewInheritanceCheckingTaskSetStringMapQueueMethod() {
    InheritanceCheckingTask task = new InheritanceCheckingTask();
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
  public void testNewInheritanceCheckingTaskSetResultMapQueueMethod() {
    InheritanceCheckingTask task = new InheritanceCheckingTask();
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
  public void testNewInheritanceCheckingTaskSetTargetGraphMethod() {
    InheritanceCheckingTask task = new InheritanceCheckingTask();
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
  public void testNewInheritanceCheckingTaskIsRunningMethod() {
    InheritanceCheckingTask task = new InheritanceCheckingTask();
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
  public void testNewInheritanceCheckingTaskStopMethod() {
    InheritanceCheckingTask task = new InheritanceCheckingTask();

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
  public void testNewInheritanceCheckingRunMethod() {
    InheritanceCheckingTask task = new InheritanceCheckingTask();

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
      String taskIRI = "http://www.theworldavatar.com/ontologies/OntoInfer.owl#InheritanceCheckingTask";
      //@todo: implement

      assertFalse((Boolean) isRunning.invoke(task));

    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | NoSuchFieldException | InterruptedException e) {
      fail();
    }

  }

  @Test
  public void testNewInheritanceCheckingTaskGetReasonerOutputMethod() {
    InheritanceCheckingTask task = new InheritanceCheckingTask();

    try {
      Method getReasonerOutput = task.getClass().getDeclaredMethod("getReasonerOutput",
          Reasoner.class, String.class, IRI.class, IRI.class, IRI.class);
      getReasonerOutput.setAccessible(true);
      Method createModel = task.getClass().getDeclaredMethod("createModel", JSONArray.class);
      createModel.setAccessible(true);
      String ontoIri = "http://www.test.com/onto";
      IRI srcIri = IRI.create("http://www.test.com/a");
      IRI dstIri = IRI.create("http://www.test.com/brown");
      IRI propIri = IRI.create("http://www.test.com/hasHairColour");

      JSONArray output;
      JSONArray input = new JSONArray(
          "[{\"s\": \"http://www.test.com/BrownHairedPerson\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
          + "{\"s\": \"http://www.test.com/BrownHaired\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
          + "{\"s\": \"http://www.test.com/WithHair\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
          + "{\"s\": \"http://www.test.com/Person\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
          + "{\"s\": \"http://www.test.com/hasHairColour\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Cowl:ObjectProperty\"},"
          + "{\"s\": \"http://www.test.com/hasHairColour\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#domain\", \"o\": \"http://www.test.com/Person\"},"
          + "{\"s\": \"http://www.test.com/hasHairColour\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#range\", \"o\": \"http://www.test.com/WithHair\"},"
          + "{\"s\": \"http://www.test.com/a\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#NamedIndividual\"},"
          + "{\"s\": \"http://www.test.com/a\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.test.com/BrownHairedPerson\"},"
          + "{\"s\": \"http://www.test.com/brown\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#NamedIndividual\"},"
          + "{\"s\": \"http://www.test.com/brown\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.test.com/BrownHaired\"},"
          + "{\"s\": \"http://www.test.com/BrownHairedPerson\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/BrownHaired\"},"
          + "{\"s\": \"http://www.test.com/BrownHairedPerson\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/Person\"},"
          + "{\"s\": \"http://www.test.com/BrownHaired\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/WithHair\"}]");
      OWLOntology ontology = (OWLOntology) createModel.invoke(task, input);
      Reasoner reasoner = new Reasoner(ontology);

      //IRI & IRI case
      output = (JSONArray) getReasonerOutput.invoke(task, reasoner, ontoIri, srcIri, dstIri, propIri);
      assertTrue(output.toString().equals(new JSONArray().put(new JSONObject().put(ontoIri, true)).toString()));
      output = (JSONArray) getReasonerOutput.invoke(task, reasoner, ontoIri, dstIri, srcIri, propIri);
      assertTrue(output.toString().equals(new JSONArray().put(new JSONObject().put(ontoIri, false)).toString()));


    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail();
    }

  }

  @Test
  public void testNewInheritanceCheckingTaskCreateModelMethod() {
    InheritanceCheckingTask task = new InheritanceCheckingTask();

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
