package uk.ac.cam.cares.twa.cities.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

@RunWith(MockitoJUnitRunner.class)
public class CardinalityRestrictionCheckingTaskTest {

  @Test
  public void testNewCardinalityRestrictionCheckingTask() {
    CardinalityRestrictionCheckingTask task;

    try {
      task = new CardinalityRestrictionCheckingTask();
      assertNotNull(task);
    } catch (Exception e) {
      fail();
    }

  }

  @Test
  public void testNewCardinalityRestrictionCheckingTaskFields() {
    CardinalityRestrictionCheckingTask task = new CardinalityRestrictionCheckingTask();
    assertEquals(1, task.getClass().getDeclaredFields().length);

    Field taskIri;

    try {
      taskIri = task.getClass().getDeclaredField("taskIri");
      taskIri.setAccessible(true);
      assertEquals(taskIri.get(task), IRI.create("http://www.theworldavatar.com/ontologies/OntoInfer.owl#CardinalityRestrictionCheckingTask"));
    }  catch (NoSuchFieldException | IllegalAccessException e) {
      fail();
    }

  }

  @Test
  public void testNewCardinalityRestrictionCheckingTaskMethods() {
    CardinalityRestrictionCheckingTask task = new CardinalityRestrictionCheckingTask();
    assertEquals(2, task.getClass().getDeclaredMethods().length);
  }

  @Test
  public void testNewCardinalityRestrictionCheckingRunMethod() {
    CardinalityRestrictionCheckingTask task = new CardinalityRestrictionCheckingTask();

    try {
      Method run = task.getClass().getDeclaredMethod("run");
      run.setAccessible(true);
      Field stop = task.getClass().getSuperclass().getDeclaredField("stop");
      stop.setAccessible(true);
      Method isRunning = task.getClass().getSuperclass().getDeclaredMethod("isRunning");
      isRunning.setAccessible(true);
      Method setTargetGraph = task.getClass().getSuperclass().getDeclaredMethod("setTargetGraph", String.class);
      setTargetGraph.setAccessible(true);
      setTargetGraph.invoke(task, "http:/www.test.com/");
      new LinkedBlockingDeque<>();
      Method setStringMapQueue = task.getClass().getSuperclass().getDeclaredMethod("setStringMapQueue", BlockingQueue.class);
      setStringMapQueue.setAccessible(true);
      setStringMapQueue.invoke(task, new LinkedBlockingDeque<>());
      Field dataQueue = task.getClass().getSuperclass().getDeclaredField("dataQueue");
      dataQueue.setAccessible(true);
      Method setResultQueue = task.getClass().getSuperclass().getDeclaredMethod("setResultQueue", BlockingQueue.class);
      setResultQueue.setAccessible(true);
      setResultQueue.invoke(task, new LinkedBlockingDeque<>());
      Field resultQueue = task.getClass().getSuperclass().getDeclaredField("resultQueue");
      resultQueue.setAccessible(true);
      String taskIRI = "http://www.theworldavatar.com/ontologies/OntoInfer.owl#CardinalityRestrictionCheckingTask";
      JSONArray testOnto =  new JSONArray(
          "[{\"s\": \"http://www.test.com/c1\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
              + "{\"s\": \"http://www.test.com/c2\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
              + "{\"s\": \"http://www.test.com/c3\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
              + "{\"s\": \"http://www.test.com/h1\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#ObjectProperty\"},"
              + "{\"s\": \"http://www.test.com/h2\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#ObjectProperty\"},"
              + "{\"s\": \"http://www.test.com/h3\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#ObjectProperty\"},"
              + "{\"s\": \"http://www.test.com/r1\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Restriction\"},"
              + "{\"s\": \"http://www.test.com/r2\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Restriction\"},"
              + "{\"s\": \"http://www.test.com/r3\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Restriction\"},"
              + "{\"s\": \"http://www.test.com/r1\", \"p\": \"http://www.w3.org/2002/07/owl#onProperty\", \"o\": \"http://www.test.com/h1\"},"
              + "{\"s\": \"http://www.test.com/r2\", \"p\": \"http://www.w3.org/2002/07/owl#onProperty\", \"o\": \"http://www.test.com/h2\"},"
              + "{\"s\": \"http://www.test.com/r3\", \"p\": \"http://www.w3.org/2002/07/owl#onProperty\", \"o\": \"http://www.test.com/h3\"},"
              + "{\"s\": \"http://www.test.com/r1\", \"p\": \"http://www.w3.org/2002/07/owl#qualifiedCardinality\", \"o\": '\"3\"^^http://www.w3.org/2001/XMLSchema#nonNegativeInteger'},"
              + "{\"s\": \"http://www.test.com/r2\", \"p\": \"http://www.w3.org/2002/07/owl#minQualifiedCardinality\", \"o\": '\"1\"^^http://www.w3.org/2001/XMLSchema#nonNegativeInteger'},"
              + "{\"s\": \"http://www.test.com/r3\", \"p\": \"http://www.w3.org/2002/07/owl#maxQualifiedCardinality\", \"o\": '\"2\"^^http://www.w3.org/2001/XMLSchema#nonNegativeInteger'},"
              + "{\"s\": \"http://www.test.com/r1\", \"p\": \"http://www.w3.org/2002/07/owl#onClass\", \"o\": \"http://www.test.com/c2\"},"
              + "{\"s\": \"http://www.test.com/r2\", \"p\": \"http://www.w3.org/2002/07/owl#onClass\", \"o\": \"http://www.test.com/c1\"},"
              + "{\"s\": \"http://www.test.com/r3\", \"p\": \"http://www.w3.org/2002/07/owl#onClass\", \"o\": \"http://www.test.com/c2\"},"
              + "{\"s\": \"http://www.test.com/c1\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/r1\"},"
              + "{\"s\": \"http://www.test.com/c2\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/r2\"},"
              + "{\"s\": \"http://www.test.com/c2\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/r3\"}]");

      String ontoIri = "http://www.test.com/onto";
      String srcIri = "http://www.test.com/c1";
      String dstIri = "http://www.test.com/c2";
      String propIri  = "http://www.test.com/h1";

      Map<String, JSONArray> map = new HashMap<>();
      map.put(taskIRI, testOnto);
      map.put("ontologyIRI", new JSONArray().put(ontoIri));
      map.put("sourceIRI", new JSONArray().put(srcIri));
      map.put("destinationIRI", new JSONArray().put(dstIri));
      map.put("propertyIRI", new JSONArray().put(propIri));

      ((LinkedBlockingDeque) dataQueue.get(task)).put(map);

      run.invoke(task);
      assertTrue(((Map) ((BlockingQueue) resultQueue.get(task)).take()).get(taskIRI).toString()
          .equals(new JSONArray().put(new JSONObject().put(ontoIri, new JSONObject().put("ObjectExactCardinality", 3))).toString()));

      assertFalse((Boolean) isRunning.invoke(task));
      stop.set(task, false);

      map = new HashMap<>();
      map.put(taskIRI, testOnto);
      map.put("ontologyIRI", new JSONArray().put(ontoIri));
      map.put("sourceIRI", new JSONArray().put(dstIri));
      map.put("destinationIRI", new JSONArray().put(srcIri));
      map.put("propertyIRI", new JSONArray().put("http://www.test.com/h2"));

      ((LinkedBlockingDeque) dataQueue.get(task)).put(map);

      run.invoke(task);
      assertTrue(((Map) ((BlockingQueue) resultQueue.get(task)).take()).get(taskIRI).toString()
          .equals(new JSONArray().put(new JSONObject().put(ontoIri, new JSONObject().put("ObjectMinCardinality", 1))).toString()));
      stop.set(task, false);

      map = new HashMap<>();
      map.put(taskIRI, testOnto);
      map.put("ontologyIRI", new JSONArray().put(ontoIri));
      map.put("sourceIRI", new JSONArray().put(dstIri));
      map.put("destinationIRI", new JSONArray().put(dstIri));
      map.put("propertyIRI", new JSONArray().put("http://www.test.com/h3"));

      ((LinkedBlockingDeque) dataQueue.get(task)).put(map);

      run.invoke(task);
      assertTrue(((Map) ((BlockingQueue) resultQueue.get(task)).take()).get(taskIRI).toString()
          .equals(new JSONArray().put(new JSONObject().put(ontoIri, new JSONObject().put("ObjectMaxCardinality", 2))).toString()));
      stop.set(task, false);

      map = new HashMap<>();
      map.put(taskIRI, testOnto);
      map.put("ontologyIRI", new JSONArray().put(ontoIri));
      map.put("sourceIRI", new JSONArray().put(dstIri));
      map.put("destinationIRI", new JSONArray().put(srcIri));
      map.put("propertyIRI", new JSONArray().put(propIri));

      ((LinkedBlockingDeque) dataQueue.get(task)).put(map);

      run.invoke(task);
      assertTrue(((Map) ((BlockingQueue) resultQueue.get(task)).take()).get(taskIRI).toString()
          .equals(new JSONArray().put(new JSONObject().put(ontoIri, false)).toString()));
      stop.set(task, false);

      map = new HashMap<>();
      map.put(taskIRI, testOnto);
      map.put("ontologyIRI", new JSONArray().put(ontoIri));
      map.put("sourceIRI", new JSONArray().put("http:/invalidIRI"));
      map.put("destinationIRI", new JSONArray().put(dstIri));
      map.put("propertyIRI", new JSONArray().put(propIri));

      ((LinkedBlockingDeque) dataQueue.get(task)).put(map);

      run.invoke(task);

      assertTrue(((Map) ((BlockingQueue) resultQueue.get(task)).take()).get(taskIRI).toString()
          .equals(new JSONArray().put(new JSONObject().put(ontoIri, false)).toString()));
      stop.set(task, false);

      map = new HashMap<>();
      map.put(taskIRI, testOnto);
      map.put("ontologyIRI", new JSONArray().put(ontoIri));
      map.put("sourceIRI", new JSONArray().put(srcIri));
      map.put("destinationIRI", new JSONArray().put("http:/invalidIRI2"));
      map.put("propertyIRI", new JSONArray().put(propIri));

      ((LinkedBlockingDeque) dataQueue.get(task)).put(map);

      run.invoke(task);

      assertTrue(((Map) ((BlockingQueue) resultQueue.get(task)).take()).get(taskIRI).toString()
          .equals(new JSONArray().put(new JSONObject().put(ontoIri, false)).toString()));

      stop.set(task, false);

      map = new HashMap<>();
      map.put(taskIRI, testOnto);
      map.put("ontologyIRI", new JSONArray().put(ontoIri));
      map.put("sourceIRI", new JSONArray().put("http:/invalidIRI"));
      map.put("destinationIRI", new JSONArray().put("http:/invalidIRI2"));
      map.put("propertyIRI", new JSONArray().put(propIri));

      ((LinkedBlockingDeque) dataQueue.get(task)).put(map);

      run.invoke(task);

      assertTrue(((Map) ((BlockingQueue) resultQueue.get(task)).take()).get(taskIRI).toString()
          .equals(new JSONArray().put(new JSONObject().put(ontoIri, false)).toString()));

      stop.set(task, false);

      map = new HashMap<>();
      map.put(taskIRI, testOnto);
      map.put("ontologyIRI", new JSONArray().put(ontoIri));
      map.put("sourceIRI", new JSONArray().put("http://www.w3.org/2002/07/owl#Thing"));
      map.put("destinationIRI", new JSONArray().put("http://www.test.com/c1"));
      map.put("propertyIRI", new JSONArray().put("http://www.test.com/h2"));

      ((LinkedBlockingDeque) dataQueue.get(task)).put(map);

      run.invoke(task);

      assertTrue(((Map) ((BlockingQueue) resultQueue.get(task)).take()).get(taskIRI).toString()
          .equals(new JSONArray().put(new JSONObject().put(ontoIri, false)).toString()));

      stop.set(task, false);

      map = new HashMap<>();
      map.put(taskIRI, testOnto);
      map.put("ontologyIRI", new JSONArray().put(ontoIri));
      map.put("sourceIRI", new JSONArray().put("http://www.test.com/c2"));
      map.put("destinationIRI", new JSONArray().put("http://www.w3.org/2002/07/owl#Thing"));
      map.put("propertyIRI", new JSONArray().put("http://www.test.com/h3"));

      ((LinkedBlockingDeque) dataQueue.get(task)).put(map);

      run.invoke(task);

      assertTrue(((Map) ((BlockingQueue) resultQueue.get(task)).take()).get(taskIRI).toString()
          .equals(new JSONArray().put(new JSONObject().put(ontoIri, false)).toString()));

      stop.set(task, false);

      map = new HashMap<>();
      map.put(taskIRI, testOnto);
      map.put("ontologyIRI", new JSONArray().put(ontoIri));
      map.put("sourceIRI", new JSONArray().put("http://www.w3.org/2002/07/owl#Thing"));
      map.put("destinationIRI", new JSONArray().put("http://www.w3.org/2002/07/owl#Thing"));
      map.put("propertyIRI", new JSONArray().put("http://www.test.com/h3"));

      ((LinkedBlockingDeque) dataQueue.get(task)).put(map);

      run.invoke(task);

      assertTrue(((Map) ((BlockingQueue) resultQueue.get(task)).take()).get(taskIRI).toString()
          .equals(new JSONArray().put(new JSONObject().put(ontoIri, false)).toString()));

      assertFalse((Boolean) isRunning.invoke(task));

    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | NoSuchFieldException | InterruptedException e) {
      fail();
    }

  }

  @Test
  public void testNewCardinalityRestrictionCheckingTaskGetReasonerOutputMethod() {
    CardinalityRestrictionCheckingTask task = new CardinalityRestrictionCheckingTask();

    try {
      Method getReasonerOutput = task.getClass().getDeclaredMethod("getReasonerOutput",
          Reasoner.class, String.class, IRI.class, IRI.class, IRI.class);
      getReasonerOutput.setAccessible(true);
      Method createModel = task.getClass().getSuperclass().getDeclaredMethod("createModel", JSONArray.class);
      createModel.setAccessible(true);
      String ontoIri = "http://www.test.com/onto";
      IRI srcIri = IRI.create("http://www.test.com/c1");
      IRI dstIri = IRI.create("http://www.test.com/c2");
      IRI propIri = IRI.create("http://www.test.com/h1");

      JSONArray output;

      JSONArray input = new JSONArray(
          "[{\"s\": \"http://www.test.com/c1\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
              + "{\"s\": \"http://www.test.com/c2\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
              + "{\"s\": \"http://www.test.com/c3\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
              + "{\"s\": \"http://www.test.com/h1\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#ObjectProperty\"},"
              + "{\"s\": \"http://www.test.com/h2\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#ObjectProperty\"},"
              + "{\"s\": \"http://www.test.com/h3\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#ObjectProperty\"},"
              + "{\"s\": \"http://www.test.com/r1\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Restriction\"},"
              + "{\"s\": \"http://www.test.com/r2\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Restriction\"},"
              + "{\"s\": \"http://www.test.com/r3\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Restriction\"},"
              + "{\"s\": \"http://www.test.com/r1\", \"p\": \"http://www.w3.org/2002/07/owl#onProperty\", \"o\": \"http://www.test.com/h1\"},"
              + "{\"s\": \"http://www.test.com/r2\", \"p\": \"http://www.w3.org/2002/07/owl#onProperty\", \"o\": \"http://www.test.com/h2\"},"
              + "{\"s\": \"http://www.test.com/r3\", \"p\": \"http://www.w3.org/2002/07/owl#onProperty\", \"o\": \"http://www.test.com/h3\"},"
              + "{\"s\": \"http://www.test.com/r1\", \"p\": \"http://www.w3.org/2002/07/owl#qualifiedCardinality\", \"o\": '\"3\"^^http://www.w3.org/2001/XMLSchema#nonNegativeInteger'},"
              + "{\"s\": \"http://www.test.com/r2\", \"p\": \"http://www.w3.org/2002/07/owl#minQualifiedCardinality\", \"o\": '\"1\"^^http://www.w3.org/2001/XMLSchema#nonNegativeInteger'},"
              + "{\"s\": \"http://www.test.com/r3\", \"p\": \"http://www.w3.org/2002/07/owl#maxQualifiedCardinality\", \"o\": '\"2\"^^http://www.w3.org/2001/XMLSchema#nonNegativeInteger'},"
              + "{\"s\": \"http://www.test.com/r1\", \"p\": \"http://www.w3.org/2002/07/owl#onClass\", \"o\": \"http://www.test.com/c2\"},"
              + "{\"s\": \"http://www.test.com/r2\", \"p\": \"http://www.w3.org/2002/07/owl#onClass\", \"o\": \"http://www.test.com/c1\"},"
              + "{\"s\": \"http://www.test.com/r3\", \"p\": \"http://www.w3.org/2002/07/owl#onClass\", \"o\": \"http://www.test.com/c2\"},"
              + "{\"s\": \"http://www.test.com/c1\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/r1\"},"
              + "{\"s\": \"http://www.test.com/c2\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/r2\"},"
              + "{\"s\": \"http://www.test.com/c2\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/r3\"}]");

      OWLOntology ontology = (OWLOntology) createModel.invoke(task, input);

      Reasoner reasoner = new Reasoner(new Configuration(), ontology);

      //IRI & IRI case
      output = (JSONArray) getReasonerOutput.invoke(task, reasoner, ontoIri, srcIri, dstIri, propIri);
      assertTrue(output.toString().equals(new JSONArray().put(new JSONObject().put(ontoIri, new JSONObject().put("ObjectExactCardinality", 3))).toString()));
      output = (JSONArray) getReasonerOutput.invoke(task, reasoner, ontoIri, dstIri, srcIri, propIri);
      assertTrue(output.toString().equals(new JSONArray().put(new JSONObject().put(ontoIri, false)).toString()));

    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail();
    }

  }

}
