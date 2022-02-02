package uk.ac.cam.cares.twa.cities.tasks.geo.test;

import junit.framework.TestCase;
import org.mockito.Mockito;
import uk.ac.cam.cares.twa.cities.agents.geo.ThematicSurfaceDiscoveryAgent;
import uk.ac.cam.cares.twa.cities.models.geo.SurfaceGeometry;
import uk.ac.cam.cares.twa.cities.tasks.geo.MultiSurfaceThematicisationTask;
import uk.ac.cam.cares.twa.cities.tasks.geo.ThematicSurfaceDiscoveryTask;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ThematicSurfaceDiscoveryTaskTest extends TestCase {

  public void testResolveIndeterminateFlips() throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

    ThematicSurfaceDiscoveryTask task = new ThematicSurfaceDiscoveryTask(
        Arrays.asList("http://first"), new boolean[]{true, false, false, true}, 0, ThematicSurfaceDiscoveryAgent.Mode.RESTRUCTURE, "");

    // Collect private fields and methods
    Field lxsmTasksField = ThematicSurfaceDiscoveryTask.class.getDeclaredField("lxmsThematicisationTaskList");
    lxsmTasksField.setAccessible(true);
    Method resolveIndeterminateFlipsMethod = ThematicSurfaceDiscoveryTask.class.getDeclaredMethod("resolveIndeterminateFlips");
    resolveIndeterminateFlipsMethod.setAccessible(true);

    List<MultiSurfaceThematicisationTask> lxsmTasks;

    // Test no-indeterminate case
    lxsmTasks = createLxmsTaskList(3, 3, 0);
    lxsmTasksField.set(task, lxsmTasks);
    resolveIndeterminateFlipsMethod.invoke(task);
    verifyOnlyFirstNWereFlipped(lxsmTasks, 0);

    // Test some-indeterminate, equal flipped/unflipped case
    lxsmTasks = createLxmsTaskList(3, 3, 2);
    lxsmTasksField.set(task, lxsmTasks);
    resolveIndeterminateFlipsMethod.invoke(task);
    verifyOnlyFirstNWereFlipped(lxsmTasks, 0);

    // Test some-indeterminate, flipped > unflipped case
    lxsmTasks = createLxmsTaskList(5, 3, 2);
    lxsmTasksField.set(task, lxsmTasks);
    resolveIndeterminateFlipsMethod.invoke(task);
    verifyOnlyFirstNWereFlipped(lxsmTasks, 2);

    // Test some-indeterminate, flipped < unflipped case
    lxsmTasks = createLxmsTaskList(3, 5, 2);
    lxsmTasksField.set(task, lxsmTasks);
    resolveIndeterminateFlipsMethod.invoke(task);
    verifyOnlyFirstNWereFlipped(lxsmTasks, 0);

    // Test many-indeterminate case
    lxsmTasks = createLxmsTaskList(5, 3, 16);
    lxsmTasksField.set(task, lxsmTasks);
    resolveIndeterminateFlipsMethod.invoke(task);
    verifyOnlyFirstNWereFlipped(lxsmTasks, 16);

  }

  private List<MultiSurfaceThematicisationTask> createLxmsTaskList(int flipped, int unflipped, int indeterminate) {
    List<MultiSurfaceThematicisationTask> lxmsTasks = new ArrayList<>();
    for(int i = 0; i < indeterminate; i++) {
      MultiSurfaceThematicisationTask lxmsTask = Mockito.mock(MultiSurfaceThematicisationTask.class);
      lxmsTask.flipped = null;
      lxmsTasks.add(lxmsTask);
    }
    for(int i = 0; i < unflipped; i++) {
      MultiSurfaceThematicisationTask lxmsTask = Mockito.mock(MultiSurfaceThematicisationTask.class);
      lxmsTask.flipped = false;
      lxmsTasks.add(lxmsTask);
    }
    for(int i = 0; i < flipped; i++) {
      MultiSurfaceThematicisationTask lxmsTask = Mockito.mock(MultiSurfaceThematicisationTask.class);
      lxmsTask.flipped = true;
      lxmsTasks.add(lxmsTask);
    }
    return lxmsTasks;
  }

  private void verifyOnlyFirstNWereFlipped(List<MultiSurfaceThematicisationTask> lxmsTasks, int n) {
    for(int i = 0; i < lxmsTasks.size(); i++) {
      if(i < n) {
        Mockito.verify(lxmsTasks.get(i)).flip();
      } else {
        Mockito.verify(lxmsTasks.get(i), Mockito.never()).flip();
      }
    }
  }

}
