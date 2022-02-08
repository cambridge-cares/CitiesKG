package uk.ac.cam.cares.twa.cities.tasks.geo.test;

import junit.framework.TestCase;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.SPARQLUtils;
import uk.ac.cam.cares.twa.cities.tasks.geo.BuildingHullsRegistrationTask;
import uk.ac.cam.cares.twa.cities.tasks.geo.MultiSurfaceThematicisationTask;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BuildingHullsRegistrationTaskTest extends TestCase {

  public void testInvocation() {
    ConcurrentLinkedQueue<MultiSurfaceThematicisationTask> queue = new ConcurrentLinkedQueue<>();
    BuildingHullsRegistrationTask task = new BuildingHullsRegistrationTask(
        "dummy_iri", "dummy_kgid", new boolean[]{true, false, false, true}, 2.0, queue);

    try(MockedStatic<AccessAgentCaller> accessAgentCallerMock = Mockito.mockStatic(AccessAgentCaller.class)) {
      String predicateLod1 = SPARQLUtils.expandQualifiedName(SchemaManagerAdapter.ONTO_LOD1_MULTI_SURFACE_ID);
      String predicateLod2 = SPARQLUtils.expandQualifiedName(SchemaManagerAdapter.ONTO_LOD2_MULTI_SURFACE_ID);
      accessAgentCallerMock.when(() -> AccessAgentCaller.query(Mockito.anyString(), Mockito.anyString()))
          .thenReturn(String.format(
              "{ \"result\": \"[" +
                  "{'predicate': '%s', 'value':'http://lod1', 'graph':'building'}," +
                  "{'predicate': '%s', 'value':'http://lod2', 'graph':'building'}" +
                  "]\" }",
              predicateLod1, predicateLod2));
      task.call();
    }
    // Only one task created: only lod1 is both existent and asked for.
    assertEquals(1, queue.size());
    // Check task created correctly
    MultiSurfaceThematicisationTask lxmsTask1 = queue.poll();
    assertNotNull(lxmsTask1);
    assertEquals(1, lxmsTask1.lod);
    assertEquals("http://lod1", lxmsTask1.root.getIri().toString());
    assertEquals(2.0, lxmsTask1.threshold);
    assertEquals("dummy_kgid", lxmsTask1.kgId);
  }

}
