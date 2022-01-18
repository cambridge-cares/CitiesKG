package uk.ac.cam.cares.twa.cities.tasks.geo;

import org.locationtech.jts.geom.Coordinate;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.models.Model;
import uk.ac.cam.cares.twa.cities.models.geo.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Stream;

/**
 * Attempts to intelligently transform the geometry tree of the lodXMultiSurface of a provided building into a
 * {@link ThematicSurface}-based hierarchy.
 * <p>
 * The determination of surface theme is based on polygon normals. The axis of the normal can be determined given only
 * the GeometryType. This is sufficient to discover walls, defined as polygons whose normal axes deviate less than a
 * defined threshold angle from the horizontal plane. However, distinguishing roof and ground polygons requires also
 * the direction of the normal, which requires information on the winding order of the polygon points.
 * <p>
 * A counter-clockwise winding order is initially assumed. After initial assignment, if the ground surfaces are on
 * average above the roof surfaces, they are flipped. If they have the same z, or if there are no ground surfaces, or
 * if there are no roof surfaces, the result is indeterminate and this is registered as a <code>null</code> value in the
 * <code>flipped</code> public field.
 * <p>
 * After the discovery stage is complete, the task is blocked and the blockage is registered in the {@link CountDownLatch}
 * passed during construction. It is the responsibility of the task creator to <code>notify()</code> task continuation
 * after inspecting <code>flipped</code> results and calling <code>flip()</code> on tasks with <code>null</code> results
 * if necessary.
 */
public class MultiSurfaceThematicisationTask implements Callable<Void> {

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

  private static final String UPDATING_PERSON = "ThematicSurfaceDiscoveryAgent";
  private static final String MALFORMED_SURFACE_GEOMETRY_EXCEPTION_TEXT =
      "Malformed building: SurfaceGeometry contains both sub-geometries and explicit GeometryType.";

  private final SurfaceGeometry root;
  private final String kgId;
  private final int lod;
  private final double threshold;

  public boolean stage = false;
  public Boolean flipped = null;

  private final List<List<SurfaceGeometry>> topLevelThematicGeometries = Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
  private final List<List<SurfaceGeometry>> bottomLevelThematicGeometries = Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
  private final List<SurfaceGeometry> mixedGeometries = new ArrayList<>();

  public MultiSurfaceThematicisationTask(SurfaceGeometry root, int lod, double threshold, String kgId) {
    this.root = root;
    this.kgId = kgId;
    this.lod = lod;
    this.threshold = threshold;
  }

  public Void call() {
    if(!stage) {
      System.err.println("MultiSurface map start.");
      stage1();
      System.err.println("MultiSurface mapped.");
      stage = true;
    } else {
      System.err.println("MultiSurface implement start.");
      stage2();
      System.err.println("MultiSurface implemented.");
    }
    return null;
  }

  public void flip() {
    List<SurfaceGeometry> temp = topLevelThematicGeometries.get(Theme.ROOF.index);
    topLevelThematicGeometries.set(Theme.ROOF.index, topLevelThematicGeometries.get(Theme.GROUND.index));
    topLevelThematicGeometries.set(Theme.GROUND.index, temp);
  }

  private void stage1() {
    root.pullAll(kgId, 99);
    // Sort into thematic surfaces
    recursiveDiscover(root);
    // Calculate centroid of detected roofs' centroids and centroid of detected grounds' centroids
    double averageRoofZ = computeUnweightedCentroid(bottomLevelThematicGeometries.get(Theme.ROOF.index)).getZ();
    double averageGroundZ = computeUnweightedCentroid(bottomLevelThematicGeometries.get(Theme.GROUND.index)).getZ();
    // If the data is nice --- there are instances of each surface type, and they aren't at the same height --- we can
    // immediately decide whether to flip and then push our changes. Otherwise, we queue this case to be resolved later.
    if ((bottomLevelThematicGeometries.get(Theme.ROOF.index).size() != 0 &&
        bottomLevelThematicGeometries.get(Theme.GROUND.index).size() != 0 &&
        averageRoofZ != averageGroundZ)) {
      flipped = averageRoofZ < averageGroundZ;
      if (flipped) flip();
    } else {
      flipped = null;
    }
  }

  /**
   * Discovers the theme of each {@link SurfaceGeometry} in the subtree of a root node with the following criteria:
   * <ul>
   *   <li>A SurfaceGeometry is UNSET if it has no children SurfaceGeometries and has no GeometryType.</li>
   *   <li>A SurfaceGeometry is ROOF, WALL or GROUND if either</li>
   *   <ul>
   *     <li>It has no children and has a GeometryType polygon which faces up, horizontal or down respectively; or</li>
   *     <li>Its children are all (ROOF|UNSET), all (WALL|UNSET), or all (GROUND|UNSET) respectively.</li>
   *   </ul>
   *   <li>A SurfaceGeometry is MIXED if and only if its children's themes include two or more of ROOF, WALL and GROUND.
   *   It is assumed that no SurfaceGeometry both has children and a GeometryType; behaviour is undefined in such a case.
   * </ul>
   * Each top-level thematic geometry, defined as any {@link SurfaceGeometry} which satisfies:
   * <ul>
   *   <li>it is ROOF, WALL or GROUND; and</li>
   *   <li>its parent is MIXED, or it has no parent,</li>
   * </ul>
   * is added to the <code>topLevelThematicGeometries</code> collection of corresponding theme. Bottom-level thematic
   * geometries, defined as all geometries with non-null GeometryType, are added to the corresponding
   * <code>bottomLevelThematicGeometries</code> collection. After the geometry tree is traversed, the
   * <code>topLevelThematicGeometries</code> collections collectively satisfy:
   * <ul>
   *   <li>No elements are descendants of each other.</li>
   *   <li>Each element's subtree is of pseudo-homogeneous theme (only children which are UNSET and one of ROOF, WALL and GROUND).</li>
   *   <li>Collectively, their descendants contain all bottom-level surface geometries.</li>
   * </ul>
   * @param surface the SurfaceGeometry currently being processed.
   */
  private Theme recursiveDiscover(SurfaceGeometry surface) {
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
        themes[i] = recursiveDiscover(children.get(i));
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

  private void stage2() {
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
        tsCityObject.setIri(uuid, root.getNamespace());
        // Construct ThematicSurface
        ThematicSurface thematicSurface = new ThematicSurface();
        if(lod <= 2) thematicSurface.setLod2MultiSurfaceId(topLevelGeometry);
        else if(lod == 3) thematicSurface.setLod3MultiSurfaceId(topLevelGeometry);
        else if(lod == 4) thematicSurface.setLod4MultiSurfaceId(topLevelGeometry);
        thematicSurface.setObjectClassId(33 + i);
        thematicSurface.setBuildingId(root.getCityObjectId());
        thematicSurface.setIri(uuid, root.getNamespace());
        // Reassign SurfaceGeometry hierarchical properties
        topLevelGeometry.setParentId(null);
        List<SurfaceGeometry> allDescendantGeometries = topLevelGeometry.getFlattenedSubtree(false);
        for (SurfaceGeometry geometry : allDescendantGeometries) {
          geometry.setRootId(topLevelGeometry.getIri());
          geometry.setCityObjectId(thematicSurface.getIri());
        }
        // Push updates
        thematicSurface.queuePushUpdate(true, false);
        tsCityObject.queuePushUpdate(true, false);
        Model.executeUpdates(kgId, false);
        for (SurfaceGeometry geometry : allDescendantGeometries) {
          geometry.queuePushUpdate(true, false);
          Model.executeUpdates(kgId, false);
        }
      }
    }
    for (SurfaceGeometry geometry : mixedGeometries) {
      geometry.queueDeletionUpdate();
      Model.executeUpdates(kgId, false);
    }
    Model.executeUpdates(kgId, true);
  }

  public static Coordinate computeUnweightedCentroid(List<SurfaceGeometry> surfaceGeometries) {
    Stream<Coordinate> subCentroids = surfaceGeometries.stream().map(
        (SurfaceGeometry geometry) -> geometry.getGeometryType().getCentroid());
    return GeometryType.computeCentroid(subCentroids.toArray(Coordinate[]::new), false);
  }


}
