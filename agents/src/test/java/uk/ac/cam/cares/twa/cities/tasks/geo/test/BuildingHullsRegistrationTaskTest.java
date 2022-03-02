package uk.ac.cam.cares.twa.cities.tasks.geo.test;

import junit.framework.TestCase;
import org.mockito.Mockito;
import uk.ac.cam.cares.twa.cities.agents.geo.ThematicSurfaceDiscoveryAgent;
import uk.ac.cam.cares.twa.cities.models.ModelContext;
import uk.ac.cam.cares.twa.cities.models.geo.Building;
import uk.ac.cam.cares.twa.cities.models.geo.SurfaceGeometry;
import uk.ac.cam.cares.twa.cities.tasks.geo.BuildingHullsRegistrationTask;
import uk.ac.cam.cares.twa.cities.tasks.geo.MultiSurfaceThematicisationTask;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BuildingHullsRegistrationTaskTest extends TestCase {

  public void testInvocation() throws NoSuchFieldException, IllegalAccessException {

    ConcurrentLinkedQueue<MultiSurfaceThematicisationTask> queue = new ConcurrentLinkedQueue<>();
    BuildingHullsRegistrationTask task = new BuildingHullsRegistrationTask("",
        new ThematicSurfaceDiscoveryAgent.Params(
            "",
            "",
            new boolean[]{true, false, false, true},
            2.0,
            ThematicSurfaceDiscoveryAgent.Mode.RESTRUCTURE
        ), queue);

    ModelContext context = Mockito.spy(new ModelContext("", ""));
    Building building = context.createNewModel(Building.class, "a");
    building.setLod1MultiSurfaceId(context.createHollowModel(SurfaceGeometry.class, "b"));
    building.setLod2MultiSurfaceId(context.createHollowModel(SurfaceGeometry.class, "c"));
    Mockito.doReturn(building).when(context).loadAll(Mockito.any(), Mockito.anyString());
    Field contextField = BuildingHullsRegistrationTask.class.getDeclaredField("context");
    contextField.setAccessible(true);
    contextField.set(task, context);
    task.call();

    // Only one task created: only lod1 is both existent and asked for.
    assertEquals(1, queue.size());
    // Check task created correctly
    MultiSurfaceThematicisationTask lxmsTask = queue.poll();
    assertNotNull(lxmsTask);
    assertEquals(1, lxmsTask.lod);
    assertEquals(2.0, lxmsTask.params.threshold);
    assertEquals("b", lxmsTask.roots[0].getIri());
  }

}
