package uk.ac.cam.cares.twa.cities.tasks.geo;

import uk.ac.cam.cares.twa.cities.agents.geo.ThematicSurfaceDiscoveryAgent;
import uk.ac.cam.cares.twa.cities.models.ModelContext;
import uk.ac.cam.cares.twa.cities.models.geo.Building;
import uk.ac.cam.cares.twa.cities.models.geo.SurfaceGeometry;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Queries a building for its lodXMultiSurfaces and creates and queues a {@link MultiSurfaceThematicisationTask} for
 * each multi-surface of level of detail requested in the <code>lods</code> argument.
 * @author <a href="mailto:jec226@cam.ac.uk">Jefferson Chua</a>
 * @version $Id$
 */
public class BuildingHullsRegistrationTask implements Callable<Void> {

  private final String buildingIri;
  private final ModelContext context;
  private final ThematicSurfaceDiscoveryAgent.Params params;
  private final ConcurrentLinkedQueue<MultiSurfaceThematicisationTask> outputQueue;

  public BuildingHullsRegistrationTask(String buildingIri, ThematicSurfaceDiscoveryAgent.Params params, ConcurrentLinkedQueue<MultiSurfaceThematicisationTask> outputQueue) {
    this.buildingIri = buildingIri;
    this.context = params.makeContext();
    this.params = params;
    this.outputQueue = outputQueue;
  }

  public Void call() {
    Building building = context.loadAll(Building.class, buildingIri);
    for (int lod = 1; lod <= 4; lod++) {
      if (!params.lods[lod - 1]) continue;
      SurfaceGeometry multiSurface =
          lod == 1 ? building.getLod1MultiSurfaceId() :
              lod == 2 ? building.getLod2MultiSurfaceId() :
                  lod == 3 ? building.getLod3MultiSurfaceId() :
                      building.getLod4MultiSurfaceId();
      if (multiSurface != null)
        outputQueue.add(new MultiSurfaceThematicisationTask(multiSurface.getIri(), lod, params));
    }
    return null;
  }

}
