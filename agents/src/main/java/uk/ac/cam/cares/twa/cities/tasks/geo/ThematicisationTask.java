package uk.ac.cam.cares.twa.cities.tasks.geo;

import org.locationtech.jts.geom.Coordinate;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.interfaces.StoreClientInterface;
import uk.ac.cam.cares.twa.cities.Model;
import uk.ac.cam.cares.twa.cities.models.geo.*;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ThematicisationTask implements Runnable {

  private static final String UPDATING_PERSON = "ThematicSurfaceDiscoveryAgent";
  private static final String MALFORMED_SURFACE_GEOMETRY_EXCEPTION_TEXT =
      "Malformed building: SurfaceGeometry contains both sub-geometries and explicit GeometryType.";

  private final List<String> buildingIris;

  private final StoreClientInterface kgClient;
  private final boolean[] thematiciseLod;
  private final double threshold;
  private final Queue<Consumer<Boolean>> deferredPostprocessTasks;

  public ThematicisationTask(List<String> buildingIris, boolean[] thematiciseLod, double threshold, StoreClientInterface kgClient) {
    this.kgClient = kgClient;
    this.buildingIris = buildingIris;
    this.thematiciseLod = thematiciseLod;
    this.threshold = threshold;
    deferredPostprocessTasks = new LinkedList<>();
  }

  @Override
  public void run() {
    // Do first round of building assessments, collecting data on how many flipped
    int flipBalance = 0;
    for (String buildingIri : buildingIris)
      flipBalance += thematiciseBuilding(buildingIri);
    // Any deferred tasks are executed, using an average flip verdict from the non-deferred executions.
    for (Consumer<Boolean> task : deferredPostprocessTasks)
      task.accept(flipBalance > 0);
  }

  /**
   * Pulls a Building by iri, sorts through its lodXMultiSurface(s), and attempts to transform geometry tree(s) into
   * ThematicSurface-based hierarch(ies). See thematiciseLodMultiSurface for more details on the process.
   * @param buildingIri the iri of the building to be processed
   * @return the number of flipped lodXMultiSurfaces minus the number of non-flipped ones.
   */
  private int thematiciseBuilding(String buildingIri) {
    // Load building
    Building building = new Building();
    building.pullIndiscriminate(buildingIri, kgClient, 0);
    int subFlipBalance = 0;
    for (int i = 0; i < 4; i++) {
      if (thematiciseLod[i])
        subFlipBalance += thematiciseLodMultiSurface(building, i + 1);
    }
    return subFlipBalance;
  }

  /**
   * Attempts to intelligently transform the geometry tree of the lodXMultiSurfaace of a provided building into a
   * ThematicSurface-based hierarchy.
   * <p>
   * The determination of surface theme is based on polygon normals. The axis of the normal can be determined given only
   * the GeometryType. This is sufficient to discover walls, defined as polygons whose normal axes deviate less than a
   * defined threshold angle from the horizontal plane. However, distinguishing roof and ground polygons requires also
   * the direction of the normal, which requires information on the winding order of the polygon points.
   * <p>
   * A counter-clockwise winding order is initially assumed. After initial assignment, if the ground surfaces are on
   * average above the roof surfaces, they are flipped. If they have the same z, or if there are no ground surfaces, or
   * if there are no roof surfaces, the result is indeterminate and the remaining postprocessing work is deferred.
   * <p>
   * Deferred postprocessing tasks are added to deferredPostprocessTasks as Consumer<boolean> functions which accept
   * whether to flip as the argument.
   * @param building the Building Model object to be processed
   * @return whether the roof and ground surfaces were flipped after geometric meta-analysis, where 1 indicates yes, -1
   * indicates no, and 0 is a null result: there was no multiSurface to process, or postprocessing was deferred.
   */
  private int thematiciseLodMultiSurface(Building building, int lod) {
    SurfaceGeometry root =
        lod == 1 ? building.getLod1MultiSurfaceId() :
            lod == 2 ? building.getLod2MultiSurfaceId() :
                lod == 3 ? building.getLod3MultiSurfaceId() :
                    lod == 4 ? building.getLod4MultiSurfaceId() :
                        null;
    if (root == null) return 0;
    Model.cumulativeUpdateExecutionNanoseconds = 0;
    Instant start = Instant.now();
    root.pullIndiscriminate(root.getIri().toString(), kgClient, 99);
    // Sort into thematic surfaces
    List<List<SurfaceGeometry>> topLevelThematicGeometries = Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    List<List<SurfaceGeometry>> bottomLevelThematicGeometries = Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    List<SurfaceGeometry> mixedGeometries = new ArrayList<>();
    recursiveDiscover(root, topLevelThematicGeometries, bottomLevelThematicGeometries, mixedGeometries);
    // Calculate centroid of detected roofs' centroids and centroid of detected grounds' centroids
    double averageRoofZ = computeUnweightedCentroid(bottomLevelThematicGeometries.get(Theme.ROOF.index)).getZ();
    double averageGroundZ = computeUnweightedCentroid(bottomLevelThematicGeometries.get(Theme.GROUND.index)).getZ();
    // If the data is nice --- there are instances of each surface type, and they aren't at the same height --- we can
    // immediately decide whether to flip and then push our changes. Otherwise, we queue this case to be resolved later.
    if ((bottomLevelThematicGeometries.get(Theme.ROOF.index).size() != 0 &&
        bottomLevelThematicGeometries.get(Theme.GROUND.index).size() != 0 &&
        averageRoofZ != averageGroundZ)) {
      boolean flip = averageRoofZ < averageGroundZ;
      postprocessLodMultiSurface(building, lod, topLevelThematicGeometries, mixedGeometries, flip);
      double totalSeconds = 1.0e-9 * Duration.between(start, Instant.now()).toNanos();
      double updateSeconds = 1.0e-9 * Model.cumulativeUpdateExecutionNanoseconds;
      System.err.println(String.format("Building took: %.2fs (total), %.2fs (executing). %.2f%% of time spent executing.%n", totalSeconds, updateSeconds, updateSeconds/totalSeconds*100));
      return flip ? 1 : -1;
    } else {
      // Non-nice data; queue.
      deferredPostprocessTasks.add((Boolean flip) -> postprocessLodMultiSurface(building, lod, topLevelThematicGeometries, mixedGeometries, flip));
      return 0;
    }
  }

  /**
   * The second half of the building processing task begun in processBuilding(). The parameters passed in are the
   * working Model variables pulled and built in processBuilding(). This method should not be directly called except
   * from processBuilding() or through the lambda constructed in and provided by processBuilding().
   * @param building                   the building being processed
   * @param topLevelThematicGeometries the top level thematic geometries collected in processBuilding
   * @param mixedGeometries            the mixed geometries collected in processBuilding
   * @param flip                       whether to reverse the default tentative assignment of roofs vs grounds.
   */
  private void postprocessLodMultiSurface(Building building,
                                          int lod,
                                          List<List<SurfaceGeometry>> topLevelThematicGeometries,
                                          List<SurfaceGeometry> mixedGeometries,
                                          boolean flip) {
    if (flip) {
      List<SurfaceGeometry> temp = topLevelThematicGeometries.get(Theme.ROOF.index);
      topLevelThematicGeometries.set(Theme.ROOF.index, topLevelThematicGeometries.get(Theme.GROUND.index));
      topLevelThematicGeometries.set(Theme.GROUND.index, temp);
    }
    for (int i = 0; i < topLevelThematicGeometries.size(); i++) {
      for (SurfaceGeometry topLevelGeometry : topLevelThematicGeometries.get(i)) {
        String uuid = UUID.randomUUID().toString();
        // Construct CityObject for the thematic surface
        CityObject tsCityObject = new CityObject();
        tsCityObject.setEnvelopeType(new EnvelopeType(topLevelGeometry));
        tsCityObject.setCreationDate(OffsetDateTime.now().toString());
        tsCityObject.setLastModificationDate(OffsetDateTime.now().toString());
        tsCityObject.setUpdatingPerson(UPDATING_PERSON);
        tsCityObject.setGmlId(uuid);
        tsCityObject.setIri(uuid, building.getNamespace());
        // Construct ThematicSurface
        ThematicSurface thematicSurface = new ThematicSurface();
        if(lod <= 2) thematicSurface.setLod2MultiSurfaceId(topLevelGeometry);
        else if(lod == 3) thematicSurface.setLod3MultiSurfaceId(topLevelGeometry);
        else if(lod == 4) thematicSurface.setLod4MultiSurfaceId(topLevelGeometry);
        thematicSurface.setObjectClassId(33 + i);
        thematicSurface.setBuildingId(building.getIri());
        thematicSurface.setIri(uuid, building.getNamespace());
        // Reassign SurfaceGeometry hierarchical properties
        topLevelGeometry.setParentId(null);
        List<SurfaceGeometry> allDescendantGeometries = topLevelGeometry.getFlattenedSubtree(false);
        for (SurfaceGeometry geometry : allDescendantGeometries) {
          geometry.setRootId(topLevelGeometry.getIri());
          geometry.setCityObjectId(thematicSurface.getIri());
        }
        // Push updates
        thematicSurface.queueAndExecutePushForwardUpdate(kgClient);
        tsCityObject.queueAndExecutePushForwardUpdate(kgClient);
        for (SurfaceGeometry geometry : allDescendantGeometries) geometry.queueAndExecutePushForwardUpdate(kgClient);
      }
    }
    for (SurfaceGeometry geometry : mixedGeometries) geometry.queueAndExecuteDeletionUpdate(kgClient);
    Model.executeUpdates(kgClient, true);
  }

  enum Theme {
    UNSET(-1),
    ROOF(0),
    WALL(1),
    GROUND(2),
    MIXED(3);
    public final int index;

    Theme(int index) {
      this.index = index;
    }
  }

  /**
   * Discovers which SurfaceGeometries should be assigned as lod1MultiSurface of a new ThematicSurface.
   * The algorithm so goes:
   * -- A SurfaceGeometry is UNSET if it has no children SurfaceGeometries and has no GeometryType.
   * -- A SurfaceGeometry is ROOF, WALL or GROUND if either
   * ---- It has no children and has a GeometryType polygon which faces up, horizontal or down respectively; or
   * ---- Its children are all (ROOF|UNSET), all (WALL|UNSET), or all (GROUND|UNSET) respectively.
   * -- A SurfaceGeometry is MIXED if and only if its children's themes include two or more of ROOF, WALL and GROUND.
   * It is assumed that no SurfaceGeometry both has children and a GeometryType; behaviour is undefined in such a case.
   * If a SurfaceGeometry is determined to be a top-level thematic geometry, i.e.
   * -- it is ROOF, WALL or GROUND; and
   * -- its parent is MIXED, or it has no parent,
   * then the SurfaceGeometry is added to the relevant topLevelThematicGeometries collection. Non-top-level geometries
   * are not sorted. After the geometry tree is traversed, topLevelThematicGeometries contains a set of
   * SurfaceGeometries which satisfy the following criteria:
   * -- None of them are descendants of each other.
   * -- Each is the root of a subtree which has pseudo-homogeneous theme (only UNSET and one of ROOF, WALL and GROUND).
   * -- Collectively, their descendants contain all SurfaceGeometries with non-blank GeometryTypes.
   * @param surface                       the SurfaceGeometry currently being processed.
   * @param topLevelThematicGeometries    the lists into which discovered top-level geometries are stored, where the
   *                                      appropriate list for each surface theme is identified by the enum index.
   * @param bottomLevelThematicGeometries the lists into which discovered bottom-level geometries are stored, where the
   *                                      appropriate list for each surface theme is identified by the enum index.
   */
  private Theme recursiveDiscover(SurfaceGeometry surface,
                                  List<List<SurfaceGeometry>> topLevelThematicGeometries,
                                  List<List<SurfaceGeometry>> bottomLevelThematicGeometries,
                                  List<SurfaceGeometry> mixedGeometries) {
    List<SurfaceGeometry> children = surface.getSurfaceGeometries();
    GeometryType geometry = surface.getGeometryType();
    Theme aggregateTheme = Theme.UNSET;
    if (geometry != null && children.size() != 0) {
      throw new JPSRuntimeException(MALFORMED_SURFACE_GEOMETRY_EXCEPTION_TEXT);
    } else if (geometry != null) {
      // "Leaf" SurfaceGeometry with actual polygon: determine based on normal.
      // Note that this considers 0 area polygons into GROUND.
      double z = geometry.getNormal().getZ();
      aggregateTheme = Math.abs(z) < Math.sin(Math.toRadians(threshold)) ? Theme.WALL : (z > 0 ? Theme.ROOF : Theme.GROUND);
      bottomLevelThematicGeometries.get(aggregateTheme.index).add(surface);
    } else {
      // Structural SurfaceGeometry with no actual polygon: determine based on children.
      Theme[] themes = new Theme[children.size()];
      for (int i = 0; i < children.size(); i++) {
        themes[i] = recursiveDiscover(children.get(i), topLevelThematicGeometries, bottomLevelThematicGeometries, mixedGeometries);
        // Do not short-circuit this loop! We need to exhaustively traverse the tree to fill
        // bottomLevelThematicGeometries and mixedGeometries.
        if (aggregateTheme != themes[i])
          aggregateTheme = aggregateTheme == Theme.UNSET ? themes[i] : Theme.MIXED;
      }
      // If I am MIXED, my children should go into the top-level lists. (Except those that are themselves MIXED, which
      // have already done this themselves.) UNSET top-level children don't go into a list; such SurfaceGeometries are
      // discarded since there is no good place to put them.
      if (aggregateTheme == Theme.MIXED) {
        mixedGeometries.add(surface);
        for (int i = 0; i < children.size(); i++)
          if (themes[i].index <= 2 && themes[i].index >= 0)
            topLevelThematicGeometries.get(themes[i].index).add(children.get(i));
      }
    }
    return aggregateTheme;
  }

  public static Coordinate computeUnweightedCentroid(List<SurfaceGeometry> surfaceGeometries) {
    Stream<Coordinate> subCentroids = surfaceGeometries.stream().map(
        (SurfaceGeometry geometry) -> geometry.getGeometryType().getCentroid());
    return GeometryType.computeCentroid(subCentroids.toArray(Coordinate[]::new), false);
  }

}
