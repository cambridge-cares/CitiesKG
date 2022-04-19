package uk.ac.cam.cares.twa.cities.tasks.geo;

import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.SPARQLUtils;
import uk.ac.cam.cares.twa.cities.agents.geo.ThematicSurfaceDiscoveryAgent;
import uk.ac.cam.cares.twa.cities.models.Model;
import uk.ac.cam.cares.twa.cities.models.ModelContext;
import uk.ac.cam.cares.twa.cities.models.geo.*;

import java.math.BigInteger;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
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
 * @author <a href="mailto:jec226@cam.ac.uk">Jefferson Chua</a>
 * @version $Id$
 */
public class MultiSurfaceThematicisationTask implements Callable<Void> {
  ExecutorService executor = Executors.newFixedThreadPool(5);

  public enum Theme {
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

  private static final String SLASH = "/";
  private static final String COMMENT_PREDICATE = "<http://www.w3.org/2000/01/rdf-schema#comment>";
  private static final String GROUND_COMMENT = "ground";
  private static final String WALL_COMMENT = "wall";
  private static final String ROOF_COMMENT = "roof";
  private static final String UPDATING_PERSON = "ThematicSurfaceDiscoveryAgent";
  private static final String MALFORMED_SURFACE_GEOMETRY_EXCEPTION_TEXT =
      "Malformed building: SurfaceGeometry contains both sub-geometries and explicit GeometryType.";

  public final SurfaceGeometry[] roots;
  public final ModelContext context;
  public final int lod;
  public final ThematicSurfaceDiscoveryAgent.Params params;

  public boolean stage = false;
  public Boolean flipped = null;

  private final List<List<SurfaceGeometry>> topLevelThematicGeometries = Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
  private final List<List<SurfaceGeometry>> bottomLevelThematicGeometries = Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
  private final List<SurfaceGeometry> mixedGeometries = new ArrayList<>();

  private final List<Set<SurfaceGeometry>> buildings = new ArrayList<>();

  public MultiSurfaceThematicisationTask(int lod, ThematicSurfaceDiscoveryAgent.Params params, String ... rootIris) {
    this.context = params.makeContext();
    this.roots = Arrays.stream(rootIris).map(
        (rootIri) -> context.createHollowModel(SurfaceGeometry.class, rootIri)
    ).toArray(SurfaceGeometry[]::new);
    this.lod = lod;
    this.params = params;
  }

  public Void call() {
    if (!stage) {
      if(params.mode == ThematicSurfaceDiscoveryAgent.Mode.SPLIT) {
        geoTouches();
      }else {
        tryClassifyGeometries();
      }
      stage = true;
    } else {
      if(params.mode == ThematicSurfaceDiscoveryAgent.Mode.RESTRUCTURE)
        restructureAndPush();
      else if(params.mode == ThematicSurfaceDiscoveryAgent.Mode.FOOTPRINT)
        addFootprintAndPush();
      else if(params.mode == ThematicSurfaceDiscoveryAgent.Mode.VALIDATE)
        commentAndPush();
      else
        touchesAndPush();
    }
    return null;
  }

  /**
   * Reclassifies all identified roof surfaces as ground surfaces, and vice versa. This operates on the
   * <code>topLevelThematicGeometries</code> and <code>bottomLevelThematicGeometries</code> collections.
   */
  public void flip() {
    List<SurfaceGeometry> temp = topLevelThematicGeometries.get(Theme.ROOF.index);
    topLevelThematicGeometries.set(Theme.ROOF.index, topLevelThematicGeometries.get(Theme.GROUND.index));
    topLevelThematicGeometries.set(Theme.GROUND.index, temp);
    temp = bottomLevelThematicGeometries.get(Theme.ROOF.index);
    bottomLevelThematicGeometries.set(Theme.ROOF.index, bottomLevelThematicGeometries.get(Theme.GROUND.index));
    bottomLevelThematicGeometries.set(Theme.GROUND.index, temp);
  }

  /**
   * Recursively traverse the geometry tree and populate the collections <code>topLevelThematicGeometries</code>,
   * <code>bottomLevelThematicGeometries</code>, and <code>mixedGeometries</code>. These classifications are further
   * described in the Javadoc for <code>recursiveDiscover</code>. Also performs the initial flip assessment, checking
   * whether the roof surfaces are on average below the ground surfaces, switching their theme assignments if so. The
   * outcome of this check is recorded in the <code>flipped</code> variable, which is null in the indeterminate case.
   */
  private void tryClassifyGeometries() {
    for(SurfaceGeometry root: roots) {
      context.pullAllWhere(
          SurfaceGeometry.class,
          new WhereBuilder().addWhere(
              ModelContext.getModelVar(),
              NodeFactory.createURI(SPARQLUtils.expandQualifiedName(SchemaManagerAdapter.ONTO_ROOT_ID)),
              NodeFactory.createURI(root.getIri())
          )
      );
      // Sort into thematic surfaces
      recursiveDiscover(root);
    }
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
    List<SurfaceGeometry> children = surface.getChildGeometries();
    GeometryType geometry = surface.getGeometryType();
    Theme aggregateTheme = Theme.UNSET;
    if (geometry != null && children.size() != 0) {
      throw new JPSRuntimeException(MALFORMED_SURFACE_GEOMETRY_EXCEPTION_TEXT);
    } else if (geometry != null) {
      // "Leaf" SurfaceGeometry with actual polygon: determine based on normal.
      // Note that this considers 0 area polygons into GROUND.
      double z = geometry.getNormal().getZ();
      aggregateTheme = Math.abs(z) < Math.sin(Math.toRadians(params.threshold)) ? Theme.WALL : (z > 0 ? Theme.ROOF : Theme.GROUND);
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

  /**
   * Perform the modifications to transform the geometry tree to a <code>ThematicSurface</code>-based hierarchy, and
   * then push the changes to the database. This does sever the link between the <code>Building</code> and the root
   * <code>SurfaceGeometry</code>, and links the new <code>ThematicSurface</code>s and the <code>Building</code>.
   */
  private void restructureAndPush() {
    for (int i = 0; i < topLevelThematicGeometries.size(); i++) {
      for (SurfaceGeometry topLevelGeometry : topLevelThematicGeometries.get(i)) {
        String uuid = UUID.randomUUID().toString();
        // Construct CityObject for the thematic surface
        String tsCityObjectIri = params.namespace + SchemaManagerAdapter.CITY_OBJECT_GRAPH + SLASH + uuid;
        CityObject tsCityObject = context.createNewModel(CityObject.class, tsCityObjectIri);
        tsCityObject.setObjectClassId(BigInteger.valueOf(33 + i));
        tsCityObject.setEnvelopeType(new EnvelopeType(topLevelGeometry));
        tsCityObject.setCreationDate(OffsetDateTime.now().toString());
        tsCityObject.setLastModificationDate(OffsetDateTime.now().toString());
        tsCityObject.setUpdatingPerson(UPDATING_PERSON);
        tsCityObject.setGmlId(uuid);
        // Construct ThematicSurface
        String thematicSurfaceIri = params.namespace + SchemaManagerAdapter.THEMATIC_SURFACE_GRAPH + SLASH + uuid;
        ThematicSurface thematicSurface = context.createNewModel(ThematicSurface.class, thematicSurfaceIri);
        if (lod <= 1) thematicSurface.setLod2MultiSurfaceId(topLevelGeometry);
        else if (lod == 2) thematicSurface.setLod3MultiSurfaceId(topLevelGeometry);
        else if (lod == 3) thematicSurface.setLod4MultiSurfaceId(topLevelGeometry);
        thematicSurface.setObjectClassId(BigInteger.valueOf(33 + i));
        thematicSurface.setBuildingId(context.getModel(Building.class, topLevelGeometry.getCityObjectId().toString()));
        // Reassign SurfaceGeometry hierarchical properties
        topLevelGeometry.setParentId(null);
        List<SurfaceGeometry> allDescendantGeometries = topLevelGeometry.getFlattenedSubtree(false);
        for (SurfaceGeometry geometry : allDescendantGeometries) {
          geometry.setRootId(topLevelGeometry.getId());
          geometry.setCityObjectId(thematicSurface.getId());
        }
      }
    }
    for (SurfaceGeometry geometry : mixedGeometries) {
      geometry.delete(true);
    }
    context.pushAllChanges();
  }

  /**
   * Adds rdfs:comment properties to bottom-level thematic geometries. This is done instead of restructureAndPush() if
   * the mode chosen was Mode.VALIDATE.
   */
  private void commentAndPush() {
    WhereBuilder whereBuilder = new WhereBuilder();
    for (SurfaceGeometry geometry : bottomLevelThematicGeometries.get(Theme.GROUND.index)) {
      whereBuilder.addWhere(NodeFactory.createURI(geometry.getIri()), COMMENT_PREDICATE, NodeFactory.createLiteral(GROUND_COMMENT));
    }
    for (SurfaceGeometry geometry : bottomLevelThematicGeometries.get(Theme.WALL.index)) {
      whereBuilder.addWhere(NodeFactory.createURI(geometry.getIri()), COMMENT_PREDICATE, NodeFactory.createLiteral(WALL_COMMENT));
    }
    for (SurfaceGeometry geometry : bottomLevelThematicGeometries.get(Theme.ROOF.index)) {
      whereBuilder.addWhere(NodeFactory.createURI(geometry.getIri()), COMMENT_PREDICATE, NodeFactory.createLiteral(ROOF_COMMENT));
    }
    Node graph = NodeFactory.createURI(params.namespace + SchemaManagerAdapter.SURFACE_GEOMETRY_GRAPH + SLASH);
    context.update(new UpdateBuilder().addInsert(graph, whereBuilder).build().toString());
  }

  /**
   * Creates one MultiSurface with copies of all bottom-level thematic geometries identified as ground as direct children
   * and assigns this surface as lod0FootprintId to the parent building. This is done instead of restructureAndPush() if
   * the mode chosen was Mode.FOOTPRINT.
   */
  private void addFootprintAndPush() {

    if (bottomLevelThematicGeometries.get(Theme.GROUND.index).size() > 0) {
      // Get Building and create Ground MultiSurface
      Building bldg;
      String cityObjectId = bottomLevelThematicGeometries.get(0).get(0).getCityObjectId().toString();
      if (cityObjectId.contains(params.namespace + SchemaManagerAdapter.THEMATIC_SURFACE_GRAPH)) {
        // Get parent building in case thematic surfaces already exist ...
        ThematicSurface thematicSurface = context.getModel(ThematicSurface.class, cityObjectId);
        context.pullPartial(thematicSurface, "buildingId");
        bldg = context.getModel(Building.class, thematicSurface.getBuildingId().getIri());
      } else {
        // ... otherwise fetch building as CityObjectId of surfaces directly
        bldg = context.getModel(Building.class, bottomLevelThematicGeometries.get(0).get(0).getCityObjectId().toString());
      }
      bldg.setDirty("lod0FootprintId");
      String uuid = "UUID_" + UUID.randomUUID().toString();
      String groundSurfaceIri = params.namespace + SchemaManagerAdapter.SURFACE_GEOMETRY_GRAPH + SLASH + uuid;
      SurfaceGeometry ground = context.createNewModel(SurfaceGeometry.class, groundSurfaceIri);
      ground.setCityObjectId(URI.create(bldg.getIri()));
      ground.setRootId(URI.create(ground.getIri()));
      ground.setGmlId(uuid);
      ground.setIsComposite(BigInteger.valueOf(1));
      bldg.setLod0FootprintId(ground);

      // Create LOD0 ground surfaces and append to Ground MultiSurface
      SurfaceGeometry lod0;
      for (SurfaceGeometry geometry : bottomLevelThematicGeometries.get(Theme.GROUND.index)) {
        // Construct LOD0 copy of geometry
        uuid = "UUID_" + UUID.randomUUID().toString();
        String lod0SurfaceIri = params.namespace + SchemaManagerAdapter.SURFACE_GEOMETRY_GRAPH + SLASH + uuid;
        lod0 = context.createNewModel(SurfaceGeometry.class, lod0SurfaceIri);
        lod0.setCityObjectId(URI.create(bldg.getIri()));
        lod0.setParentId(ground);
        lod0.setRootId(URI.create(ground.getIri()));
        lod0.setGmlId(uuid);
        lod0.setGeometryType(geometry.getGeometryType());
        lod0.setIsComposite(BigInteger.valueOf(0));
      }

      context.pushAllChanges();
    }
  }

  /**
   * calculate touches of surface
   */
  private void geoTouches() {
    try{
      for(SurfaceGeometry root: roots) {
        context.pullAllWhere(
                SurfaceGeometry.class,
                new WhereBuilder().addWhere(
                        ModelContext.getModelVar(),
                        NodeFactory.createURI(SPARQLUtils.expandQualifiedName(SchemaManagerAdapter.ONTO_ROOT_ID)),
                        NodeFactory.createURI(root.getIri())
                )
        );

        List<SurfaceGeometry> children = root.getChildGeometries();
        recursiveTouches (children);
      }
      if (buildings.size() > 1) {
        flipped = false;
      }
    }catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void recursiveTouches (List<SurfaceGeometry> children) {
    List<SurfaceGeometry> singleList = new ArrayList<>();

    for(int i = 0; i < children.size() - 1; i++) {
      Set<SurfaceGeometry> building1 = new HashSet<>();
      int key = -1;
      SurfaceGeometry currentSurf = children.get(i);
      GeometryType currentGeom = currentSurf.getGeometryType();
      Polygon currentPolygon = currentGeom.getPolygon();
      if (currentGeom == null ) continue;
      key = isContainSurface(currentSurf);
      if (key == -1) {
        building1.add(currentSurf);
      } else {
        building1 = buildings.get(key);
      }

      for (int j = i+1; j < children.size(); j++) {
        SurfaceGeometry compareSurf = children.get(j);
        if (building1.contains(compareSurf)) continue;
        GeometryType compareGeom = compareSurf.getGeometryType();
        if (compareGeom == null) continue;
        Polygon comparePolygon = compareGeom.getPolygon();
        if (!currentPolygon.disjoint(comparePolygon) || currentPolygon.touches(comparePolygon) || currentPolygon.distance(comparePolygon) == 0) {
          int tempK = isContainSurface(compareSurf);
          if(tempK == -1) {
            building1.add(compareSurf);
          } else if (tempK != key){
            building1.addAll(buildings.get(tempK));
            buildings.remove(tempK);
          }
        }
      }

      if (building1.size() == 1){
        singleList.add(currentSurf);
      } else {
        if (key == -1) {
          buildings.add(building1);
        }
      }
    }


    if(singleList.size() > 0) {
      int location = 0;
      for (SurfaceGeometry single : singleList) {
        location = findClosedSurface(single);
        if (location != -1) {
          buildings.get(location).add(single);
        }
      }
    }
  }

  private int isContainSurface (SurfaceGeometry checkSurface) {
    int key = -1;
    for (int i = 0; i < buildings.size(); i++) {
      Set<SurfaceGeometry> sortSurfaces = buildings.get(i);
      if (sortSurfaces != null) {
        if (sortSurfaces.contains(checkSurface)){
          key = i;
          break;
        }
      }
    }
    return key;
  }

  private int findClosedSurface(SurfaceGeometry currentSurface) {
    int location = -1;
    double distance = 9999.99;
    for (int key1 = 0; key1 < buildings.size(); key1 ++) {
      Set<SurfaceGeometry> building = buildings.get(key1);
      for (SurfaceGeometry compareSurf : building) {
        GeometryType singleGeom = currentSurface.getGeometryType();
        Polygon singlePolygon = singleGeom.getPolygon();
        GeometryType compareGeom = compareSurf.getGeometryType();
        Polygon comparePolygon = compareGeom.getPolygon();
        if (singlePolygon.distance(comparePolygon) < distance) {
          distance = singlePolygon.distance(comparePolygon);
          location = key1;
        }
      }
    }
    return location;
  }

  /**
   * 1. insert new building, cityobject, genericattribute, root surfacegeometry
   * 2. update surfacegeometry
   */
  private void touchesAndPush() {
      int size = this.buildings.size();

      for(SurfaceGeometry root: roots) {
        String rootURI = root.getIri();
        String buildingtId = root.getCityObjectId().toString();
        String cityobjectId = buildingtId.replace("building", "cityobject");

        SurfaceGeometry rootSurface = context.getModel(SurfaceGeometry.class, rootURI);
        Building buildingModel = context.loadAll(Building.class, buildingtId);
        CityObject cityObjectModel = context.loadAll(CityObject.class, cityobjectId);
        ArrayList<GenericAttribute> genAttribModel = cityObjectModel.getGenericAttributes();
        for (int i = 0; i< genAttribModel.size(); i++){
          GenericAttribute genericAttriM = genAttribModel.get(i);
          context.pullAllWhere(
                  GenericAttribute.class,
                  new WhereBuilder().addWhere(
                          ModelContext.getModelVar(),
                          NodeFactory.createURI(SPARQLUtils.expandQualifiedName(SchemaManagerAdapter.ONTO_ID)),
                          NodeFactory.createURI(genericAttriM.getIri())
                  )
          );
          genAttribModel.set(i, context.getModel(GenericAttribute.class, genericAttriM.getIri()));
        }
        if (size > 1) {
          for (Set<SurfaceGeometry> building: buildings) {
            ArrayList<SurfaceGeometry> buildingList = new ArrayList<>(building);

            String genericAttri_uuid = "UUID_" + UUID.randomUUID().toString();
            String genericAttriIri = params.namespace + SchemaManagerAdapter.GENERIC_ATTRIB_GARPH + SLASH + genericAttri_uuid;
            GenericAttribute newGenericAttri = context.createNewModel(GenericAttribute.class, genericAttriIri);
            String rootSurface_uuid = "UUID_" + UUID.randomUUID().toString();
            String rootSurfaceIri = params.namespace + SchemaManagerAdapter.SURFACE_GEOMETRY_GRAPH + SLASH + rootSurface_uuid;
            SurfaceGeometry newRootSurface = context.createNewModel(SurfaceGeometry.class, rootSurfaceIri);
            String building_uuid = "UUID_" + UUID.randomUUID().toString();
            String buildingIri = params.namespace + SchemaManagerAdapter.BUILDING_GRAPH + SLASH + building_uuid + SLASH;
            Building newBuilding = context.createNewModel(Building.class, buildingIri);
            String cityObject_uuid = building_uuid;
            String cityObjectIri = params.namespace + SchemaManagerAdapter.CITY_OBJECT_GRAPH + SLASH + cityObject_uuid;
            CityObject newCityObject = context.createNewModel(CityObject.class, cityObjectIri);

            newGenericAttri.setAttrName(genAttribModel.get(0).getAttrName());
            newGenericAttri.setDataType(genAttribModel.get(0).getDataType());
            newGenericAttri.setStrVal(genAttribModel.get(0).getStrVal());
            ArrayList<GenericAttribute> newGenAttriList = new ArrayList<>();
            newGenAttriList.add(newGenericAttri);
            newGenericAttri.setCityObjectId(newCityObject.getId());

            newCityObject.setGmlId(cityObject_uuid);
            newCityObject.setObjectClassId(cityObjectModel.getObjectClassId());
            newCityObject.setEnvelopeType(new EnvelopeType(buildingList));
            newCityObject.setCreationDate(OffsetDateTime.now().toString());
            newCityObject.setLastModificationDate(OffsetDateTime.now().toString());
            newCityObject.setUpdatingPerson(UPDATING_PERSON);
            newCityObject.setGenericAttributes(newGenAttriList);

            newRootSurface.setGmlId(rootSurface_uuid);
            newRootSurface.setRootId(URI.create(rootSurfaceIri));
            newRootSurface.setCityObjectId(URI.create(buildingIri));
            newRootSurface.setChildGeometries(buildingList);
            newRootSurface.setIsComposite(rootSurface.getIsComposite());
            newRootSurface.setIsReverse(rootSurface.getIsReverse());
            newRootSurface.setIsSolid(rootSurface.getIsSolid());
            newRootSurface.setIsTriangulated(rootSurface.getIsTriangulated());
            newRootSurface.setIsXlink(rootSurface.getIsXlink());

            newBuilding.setLod3MultiSurfaceId(newRootSurface);
            newBuilding.setObjectClassId(buildingModel.getObjectClassId());
            newBuilding.setBuildingRootId(URI.create(buildingIri));

            context.pushChanges(newBuilding);
            context.pushChanges(newCityObject);
            context.pushChanges(newRootSurface);
            for (GenericAttribute newGenericModel : newGenAttriList) {
              context.pushChanges(newGenericModel);
              context.retire(newGenericModel);
            }
            for(SurfaceGeometry newSurfaceGeometry : buildingList){
              //update
              newSurfaceGeometry.setRootId(URI.create(rootSurfaceIri));
              newSurfaceGeometry.setParentId(newRootSurface);
              newSurfaceGeometry.setCityObjectId(URI.create(buildingIri));
              context.pushChanges(newSurfaceGeometry);
              context.retire(newSurfaceGeometry);
            }
            context.retire(newBuilding);
            context.retire(newCityObject);
            context.retire(newRootSurface);
          }

          for (GenericAttribute oldGenericModel : genAttribModel) {
            context.delete(oldGenericModel,true);
          }
          context.delete(rootSurface,true);
//        context.pushChanges(rootSurface);
          context.delete(buildingModel,true);
//        context.pushChanges(buildingModel);
          context.delete(cityObjectModel,true);
//        context.pushChanges(cityObjectModel);
          context.pushAllChanges();;

          }else{
          context.members.clear();
//          for (GenericAttribute oldGenericModel : genAttribModel) {
//            context.retire(oldGenericModel);
//          }
//          context.retire(rootSurface);
//          context.retire(buildingModel);
//          context.retire(cityObjectModel);
        }
      }
 }
  /**
   * Compute the average coordinate of the centroids of a number of SurfaaceGeometry objects, i.e. their collcetive
   * unweighted centroid.
   * @param surfaceGeometries the objects whose centroids are to be averaged.
   * @return the collective unweighted centroid.
   */
  public static Coordinate computeUnweightedCentroid(List<SurfaceGeometry> surfaceGeometries) {
    Stream<Coordinate> subCentroids = surfaceGeometries.stream().map(
        (SurfaceGeometry geometry) -> geometry.getGeometryType().getCentroid());
    return GeometryType.computeCentroid(subCentroids.toArray(Coordinate[]::new), false);
  }


}
