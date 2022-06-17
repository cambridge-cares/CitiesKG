package uk.ac.cam.cares.twa.cities.tasks.geo;

import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.agents.geo.ThematicSurfaceDiscoveryAgent;

import java.util.*;
import java.util.concurrent.*;

/**
 * Executes the main function of {@link ThematicSurfaceDiscoveryAgent}: to query a list of buildings for their
 * lodXMultiSurface geometries, determine components' themes from building geometry, and update the CKG database with a
 * revised hierarchy structured into thematic surfaces. This agent can only be used for buildings with no interior
 * geometry and strictly top-down topography.
 * @author <a href="mailto:jec226@cam.ac.uk">Jefferson Chua</a>
 * @version $Id$
 */
public class ThematicSurfaceDiscoveryTask implements Runnable {

  ExecutorService executor = Executors.newFixedThreadPool(5);

  private final List<String> buildingIris;
  private final ThematicSurfaceDiscoveryAgent.Params params;

  private final ConcurrentLinkedQueue<MultiSurfaceThematicisationTask> lxmsThematicisationTaskQueue = new ConcurrentLinkedQueue<>();
  private final List<MultiSurfaceThematicisationTask> lxmsThematicisationTaskList = new ArrayList<>();
  private final List<BuildingsMergeTask> buildingMergeTaskList = new ArrayList<>();

  public ThematicSurfaceDiscoveryTask(List<String> buildingIris, ThematicSurfaceDiscoveryAgent.Params params) {
    this.buildingIris = buildingIris;
    this.params = params;
  }

  @Override
  public void run() {
    try {
      if (params.mode == ThematicSurfaceDiscoveryAgent.Mode.MERGE){
        buildingMergeTaskList.add(new BuildingsMergeTask(buildingIris, params));
        executor.invokeAll(buildingMergeTaskList);
      }else {
//        // Parallelised collection of buildings' lodXMultiSurfaces and registration of tasks to process them.
        List<BuildingHullsRegistrationTask> buildingRegistrationTasks = new ArrayList<>();
        for (String buildingIri : buildingIris)
          buildingRegistrationTasks.add(new BuildingHullsRegistrationTask(buildingIri, params, lxmsThematicisationTaskQueue));
        executor.invokeAll(buildingRegistrationTasks);
        lxmsThematicisationTaskList.addAll(lxmsThematicisationTaskQueue);
        // Parallelised stage 1: tentative determination of themes and flip if able to determine appropriate
        executor.invokeAll(lxmsThematicisationTaskList);
        // Nonparallelised flip resolution for indeterminate case
        resolveIndeterminateFlips();
        // Parallelised stage 2: restructuring of hierarchy and push to database
        executor.invokeAll(lxmsThematicisationTaskList);
      }
    } catch (InterruptedException e) {
      throw new JPSRuntimeException(e);
    }
  }

  /**
   * Inspects the results of the discovery stage of thematicisation tasks and assesses whether more determined to flip
   * roofs and grounds than not; if so, flip the tasks which could not determine whether to flip during the first stage.
   */
  private void resolveIndeterminateFlips() {
    // Did more tasks flip than not flip?
    int flipBalance = 0;
    for (MultiSurfaceThematicisationTask task : lxmsThematicisationTaskList)
      if (task.flipped != null)
        flipBalance += task.flipped ? 1 : -1;
    // If so, flip indeterminate cases
    if (flipBalance > 0)
      for (MultiSurfaceThematicisationTask task : lxmsThematicisationTaskList)
        if (task.flipped == null)
          task.flip();
  }

}
