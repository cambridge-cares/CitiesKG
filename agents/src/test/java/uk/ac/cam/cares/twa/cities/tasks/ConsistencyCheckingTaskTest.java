package uk.ac.cam.cares.twa.cities.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

@RunWith(MockitoJUnitRunner.class)
public class ConsistencyCheckingTaskTest {

  @Test
  public void testNewConsistencyCheckingTask() {
    ConsistencyCheckingTask task;

    try {
      task = new ConsistencyCheckingTask();
      assertNotNull(task);
    } catch (Exception e) {
      fail();
    }

  }

  @Test
  public void testNewConsistencyCheckingTaskFields() {
    ConsistencyCheckingTask task = new ConsistencyCheckingTask();
    assertEquals(1, task.getClass().getDeclaredFields().length);

    Field taskIri;

    try {
      taskIri = task.getClass().getDeclaredField("taskIri");
      taskIri.setAccessible(true);
      assertEquals(taskIri.get(task), IRI.create("http://www.theworldavatar.com/ontologies/OntoInfer.owl#ConsistencyCheckingTask"));
    }  catch (NoSuchFieldException | IllegalAccessException e) {
      fail();
    }

  }

  @Test
  public void testNewConsistencyCheckingTaskMethods() {
    ConsistencyCheckingTask task = new ConsistencyCheckingTask();
    assertEquals(1, task.getClass().getDeclaredMethods().length);
  }

  @Test
  public void testNewConsistencyCheckingRunMethod() {
    ConsistencyCheckingTask task = new ConsistencyCheckingTask();

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

      Map<String, JSONArray> map = new HashMap<>();
      //consistent ontology check
      map.put("http://www.theworldavatar.com/ontologies/OntoInfer.owl#ConsistencyCheckingTask",
          new JSONArray("[{\"s\": \"http://www.test.com/1\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
              + "{\"s\": \"http://www.test.com/2\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
              + "{\"s\": \"http://www.test.com/3\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
              + "{\"s\": \"http://www.test.com/1\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/2\"},"
              + "{\"s\": \"http://www.test.com/3\", \"p\": \"http://www.w3.org/2002/07/owl#disjointWith\", \"o\": \"http://www.test.com/2\"},"
              + "{\"s\": \"http://www.test.com/1\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/3\"}]"));
      map.put("ontologyIRI", new JSONArray("[\"http://www.test.com/onto\"]"));
      ((LinkedBlockingDeque) dataQueue.get(task)).put(map);

      run.invoke(task);
      assertEquals(((Map) ((BlockingQueue) resultQueue.get(task)).take())
          .get("http://www.theworldavatar.com/ontologies/OntoInfer.owl#ConsistencyCheckingTask").toString(),
          "[\"http://www.test.com/onto : true\"]");

      assertFalse((Boolean) isRunning.invoke(task));
      stop.set(task, false);

      map = new HashMap<>();
      //inconsistent ontology check
      map.put("http://www.theworldavatar.com/ontologies/OntoInfer.owl#ConsistencyCheckingTask",
          new JSONArray("[{\"s\": \"http://www.test.com/1\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
              + "{\"s\": \"http://www.test.com/2\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
              + "{\"s\": \"http://www.test.com/3\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#Class\"},"
              + "{\"s\": \"http://www.test.com/a\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.w3.org/2002/07/owl#NamedIndividual\"},"
              + "{\"s\": \"http://www.test.com/a\", \"p\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"o\": \"http://www.test.com/1\"},"
              + "{\"s\": \"http://www.test.com/1\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/2\"},"
              + "{\"s\": \"http://www.test.com/3\", \"p\": \"http://www.w3.org/2002/07/owl#disjointWith\", \"o\": \"http://www.test.com/2\"},"
              + "{\"s\": \"http://www.test.com/1\", \"p\": \"http://www.w3.org/2000/01/rdf-schema#subClassOf\", \"o\": \"http://www.test.com/3\"}]"));
      map.put("ontologyIRI", new JSONArray("[\"http://www.test.com/onto\"]"));
      ((LinkedBlockingDeque) dataQueue.get(task)).put(map);

      run.invoke(task);
      assertEquals(((Map) ((BlockingQueue) resultQueue.get(task)).take())
              .get("http://www.theworldavatar.com/ontologies/OntoInfer.owl#ConsistencyCheckingTask").toString(),
          "[\"http://www.test.com/onto : false\"]");

      assertFalse((Boolean) isRunning.invoke(task));

    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | NoSuchFieldException | InterruptedException e) {
      fail();
    }

  }

}
