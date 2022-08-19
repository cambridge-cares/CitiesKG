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
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;

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
  public void testNewImporterTaskMethods() {
    PageRankTask task = new PageRankTask();
    assertEquals(11, task.getClass().getDeclaredMethods().length);
  }

  @Test
  public void testNewImporterTaskStopMethod() {
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
  public void testNewImporterTaskIsRunningMethod() {
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

}
