package uk.ac.cam.cares.twa.cities.tasks.geo;

import uk.ac.cam.cares.twa.cities.models.geo.Building;
import uk.ac.cam.cares.twa.cities.models.geo.SurfaceGeometry;

import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Queries a building for its lodXMultiSurfaces and creates and queues a {@link MultiSurfaceThematicisationTask} for
 * each multi-surface of level of detail requested in the <code>lods</code> argument.
 * @author <a href="mailto:jec226@cam.ac.uk">Jefferson Chua</a>
 * @version $Id$
 */
public class BuildingHullsRegistrationTask implements Callable<Void>{

  private final String buildingIri;
  private final String kgId;
  private final boolean[] lods;
  private final double threshold;
  private final ConcurrentLinkedQueue<MultiSurfaceThematicisationTask> outputQueue;

  public BuildingHullsRegistrationTask(String buildingIri, String kgId, boolean[] lods, double threshold, ConcurrentLinkedQueue<MultiSurfaceThematicisationTask> outputQueue) {
    this.buildingIri = buildingIri;
    this.kgId = kgId;
    this.lods = lods;
    this.threshold = threshold;
    this.outputQueue = outputQueue;
  }

  public Void call() {
      Building building = new Building();
      building.setIri(URI.create(buildingIri));
      building.pullAll(kgId, 0);
      for (int lod = 1; lod <= 4; lod++) {
        if (!lods[lod - 1]) continue;
        SurfaceGeometry multiSurface =
            lod == 1 ? building.getLod1MultiSurfaceId() :
                lod == 2 ? building.getLod2MultiSurfaceId() :
                    lod == 3 ? building.getLod3MultiSurfaceId() :
                        building.getLod4MultiSurfaceId();
        if (multiSurface != null)
          outputQueue.add(
              new MultiSurfaceThematicisationTask(multiSurface, lod, threshold, kgId));
      }
      return null;
    }

}
