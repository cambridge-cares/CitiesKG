package uk.ac.cam.cares.twa.cities.tasks.geo;

import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.models.geo.*;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * TODO: WRITE
 */
public class ThematicSurfaceDiscoveryTask implements Runnable {

  ExecutorService executor = Executors.newFixedThreadPool(5);

  private final List<String> buildingIris;
  private final boolean[] lods;
  private final double threshold;
  private final String kgId;

  private final BlockingQueue<MultiSurfaceThematicisationTask> lxmsThematicisationTasks = new LinkedBlockingQueue<>();
  private final List<MultiSurfaceThematicisationTask> lxmsTaskList = new ArrayList<>();

  public ThematicSurfaceDiscoveryTask(List<String> buildingIris, boolean[] lods, double threshold, String kgId) {
    this.buildingIris = buildingIris;
    this.lods = lods;
    this.threshold = threshold;
    this.kgId = kgId;
  }

  @Override
  public void run() {
    try {
      // Parallelised collection of buildings' lodXMultiSurfaces and registration of tasks to process them.
      Instant start = Instant.now();
      executor.invokeAll(
          buildingIris.stream().map(this::registerThematicisationTasksForBuilding).collect(Collectors.toList())
      );
      lxmsThematicisationTasks.drainTo(lxmsTaskList);
      System.err.println("Registry complete. (" + Duration.between(start, Instant.now()).getSeconds() + "s).");
      // Parallelised stage 1: tentative determination of themes and flip if able to determine appropriate
      start = Instant.now();
      System.err.println("Starting stage 1:");
      executor.invokeAll(lxmsTaskList);
      System.err.println("Stage 1 complete. (" + Duration.between(start, Instant.now()).getSeconds() + "s).");
      // Nonparallelised flip resolution for indeterminate case
      start = Instant.now();
      System.err.println("Starting resolution:");
      resolveIndeterminateFlips();
      System.err.println("Resolution complete. (" + Duration.between(start, Instant.now()).toMillis() + "ms).");
      // Parallelised stage 2: restructuring of hierarchy and push to database
      start = Instant.now();
      System.err.println("Starting stage 2:");
      executor.invokeAll(lxmsTaskList);
      System.err.println("Stage 2 end.");
      System.err.println("Stage 2 complete. (" + Duration.between(start, Instant.now()).getSeconds() + "s).");
    } catch (InterruptedException e) {
      throw new JPSRuntimeException(e);
    }
    System.out.println(String.format("Thematicised %d lodXMultiSurfaces over %d buildings.",
        lxmsTaskList.size(), buildingIris.size()));
  }

  private void resolveIndeterminateFlips() {
    // Did more tasks flip than not flip?
    int flipBalance = 0;
    for (MultiSurfaceThematicisationTask task : lxmsTaskList)
      if (task.flipped != null)
        flipBalance += task.flipped ? 1 : -1;
    // If so, flip indeterminate cases
    if (flipBalance > 0)
      for (MultiSurfaceThematicisationTask task : lxmsTaskList)
        if (task.flipped == null)
          task.flip();
  }

  /**
   * Technically, this method <i>creates a task to</i> "register thematicisation tasks for building". In that, the
   * building is queried for its lodXMultiSurfaces, and the multi-surfaces of levels of detail which were requested in
   * the agent invocation parameters have {@link MultiSurfaceThematicisationTask}s constructed and added to this
   * {@link ThematicSurfaceDiscoveryTask}'s thematicisation task list.
   * @param buildingIri the IRI of the building to register thematicisation tasks for.
   * @return a Runnable which, when executed, registers thematicisation tasks for the building's lodXMultiSurfaces.
   */
  private Callable<Void> registerThematicisationTasksForBuilding(String buildingIri) {
    return () -> {
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
          lxmsThematicisationTasks.add(
              new MultiSurfaceThematicisationTask(multiSurface, lod, threshold, kgId));
      }
      System.err.println("Building registered (" + lxmsThematicisationTasks.size() + ").");
      return null;
    };
  }

}
