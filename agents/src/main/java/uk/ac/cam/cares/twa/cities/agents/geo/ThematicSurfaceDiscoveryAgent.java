package uk.ac.cam.cares.twa.cities.agents.geo;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;

import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;
import uk.ac.cam.cares.jps.base.query.RemoteKnowledgeBaseClient;
import uk.ac.cam.cares.twa.cities.Model;
import uk.ac.cam.cares.twa.cities.PrefixUtils;
import uk.ac.cam.cares.twa.cities.models.geo.*;

/**
 * A JPSAgent framework based ThematicSurfaceDiscoveryAgent class used to discover thematic surfaces
 * of city obejcts in the Semantic 3D City Database {@link <a
 * href="https://www.cares.cam.ac.uk/research/cities/">}. The agent: - listens to the GET requests
 * containing IRIs of city objects - checks the type of the city object - attempts to load thematic
 * surfaces of the obejct and - if present, returns them - if not present, determines thematic
 * surfaces based on the city obejct type and surface geometries - saves any newly discovered
 * thematic surfaces back to the CKG
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 * @version $Id$
 */
@WebServlet(urlPatterns = {ThematicSurfaceDiscoveryAgent.URI_LISTEN})
public class ThematicSurfaceDiscoveryAgent extends JPSAgent {

  public ExecutorService executor = Executors.newSingleThreadExecutor();

  public static final String URI_LISTEN = "/discovery/thematicsurface";
  public static final String KEY_NAMESPACE = "namespace";
  public static final String KEY_COBI = "cityObjectIRI";
  public static final String KEY_THRESHOLD = "threshold_angle";

  private static final String route = ResourceBundle.getBundle("config").getString("uri.route");
  private KnowledgeBaseClientInterface kgClient = new RemoteKnowledgeBaseClient(route, route); // AccessAgent should be used instead of this

  private URI buildingIri;
  private URI namespaceIri;
  private double threshold;

  @Override
  public boolean validateInput(JSONObject requestParams) throws BadRequestException {
    if (!requestParams.isEmpty()
        && requestParams.get(CityImportAgent.KEY_REQ_METHOD).equals(HttpMethod.GET)) {
      Set<String> keys = requestParams.keySet();
      try {
        if (keys.contains(KEY_THRESHOLD)) {
          threshold = requestParams.getDouble(KEY_THRESHOLD);
        } else {
          threshold = 15;
        }
        if (keys.contains(KEY_COBI)) {
          System.out.println(requestParams.getString(KEY_COBI));
          buildingIri = new URI(requestParams.getString(KEY_COBI));
          namespaceIri = new URI(Model.getNamespace(buildingIri.toString()));
          return true;
        } else if (keys.contains(KEY_NAMESPACE)) {
          System.out.println(requestParams.getString(KEY_NAMESPACE));
          buildingIri = null;
          namespaceIri = new URI(requestParams.getString(KEY_NAMESPACE));
          return true;
        }
      } catch (URISyntaxException e) {
        throw new BadRequestException(e);
      }
    }
    throw new BadRequestException();
  }

  @Override
  public JSONObject processRequestParameters(JSONObject requestParams) {
    validateInput(requestParams);
    requestParams.put("acceptHeaders", "application/json");
    // Check srs
    // Move this into an srs agent?
    JSONArray srsResponse = new JSONArray(kgClient.execute(PrefixUtils.insertPrefixStatements(
        String.format("SELECT ?srs WHERE {<%s> ocgml:srsname ?srs }", namespaceIri.toString()))));
    if (srsResponse.length() == 0) {
      throw new BadRequestException("Namespace has no coordinate reference system specified.");
    } else if (srsResponse.length() > 1) {
      throw new BadRequestException("Namespace has more than one coordinate reference system specified.");
    } else {
      GeometryType.setSourceCrsName(srsResponse.getJSONObject(0).getString("srs"));
      GeometryType.setMetricCrsName("EPSG:25833");
    }
    executor.execute(() -> {
      if (buildingIri != null) {
        // Don't have a delayed postprocess set up for a single building.
        processBuilding(buildingIri.toString(), null);
      } else {
        Instant startTime = Instant.now();
        // Find all buildings
        JSONArray buildingsResponse = new JSONArray(kgClient.execute(
            PrefixUtils.insertPrefixStatements(String.format(
                "SELECT ?bldg WHERE {?bldg %s \"26\"^^xsd:integer; %s ?val. }",
                SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID,
                SchemaManagerAdapter.ONTO_BUILDING_PARENT_ID))));
        // Process buildings, tracking whether or not they were flipped for roof vs ground.
        Queue<Consumer<Boolean>> delayedPostprocessTasks = new LinkedList<>();
        int flipBalance = 0;
        int upCount = 0;
        int downCount = 0;
        int delayedCount = 0;
        int nullCount = 0;
        for (int i = 0; i < buildingsResponse.length(); i++) {
          int del = processBuilding(buildingsResponse.getJSONObject(i).getString("bldg"), delayedPostprocessTasks);
          if(del == 1) upCount++;
          else if(del==-1) downCount++;
          else nullCount++;
          flipBalance += del;
        }
        // Process delayed flip-indeterminate cases, using the results from other buildings to hint flip determination.
        for (Consumer<Boolean> task : delayedPostprocessTasks) {
          task.accept(flipBalance > 0);
          nullCount--;
          delayedCount++;
        }
        Duration timeTaken = Duration.between(startTime, Instant.now());
        System.err.println(String.format("Processed %d buildings (%d+, %d-, %d~, %dx) in %ds.",
            buildingsResponse.length(), upCount, downCount, delayedCount, nullCount, timeTaken.getSeconds()));
      }
    });
    return requestParams;
  }

  /**
   * Pulls a Building by iri, sorts through its lodXMultiSurface, and attempts to transform the geometry tree into a
   * ThematicSurface-based hierarchy. If correct roof-vs-ground determinations cannot be made based on the Building's
   * data alone, the second half of the task is paused and queued. The caller then has the responsibility of triggering
   * the rest of the task at any later point by providing a "flip" hint on the roof-vs-ground issue.
   * <p>
   * The determination of surface theme is based on polygon normals, but unless winding order is known, only the axis
   * of the normal can be determined for a single GeometryType. Therefore, roof and ground polygons can be separated
   * but not each sorted category assigned an explicit label. If one category is on average above the other, then it
   * is assigned "roof" and the other "ground", but if they are on the same level or one is empty, it is impossible
   * to determine, and the issue is queued for later resolution.
   * @param buildingIri             the iri of the building to be processed
   * @param delayedPostprocessTasks the queue into which paused tasks will be submitted
   * @return which way the roofs-vs-grounds were assigned after geometric meta-analysis, which should be collated by the
   * caller to determine what to provide as "flip" hint to the queued task. 1 if flipped, -1 if not flipped, 0 if queued
   * without determination or if the building has no lodXMultiSurface to process.
   */
  private int processBuilding(String buildingIri, Queue<Consumer<Boolean>> delayedPostprocessTasks) {
    // Load building
    Building building = new Building();
    building.pullAll(buildingIri, kgClient, 0);
    // Fetch relevant multisurface
    SurfaceGeometry root = building.getLod2MultiSurfaceId();
    if (root == null) root = building.getLod3MultiSurfaceId();
    if (root == null) root = building.getLod1MultiSurfaceId();
    if (root == null) return 0;
    root.pullAll(root.getIri().toString(), kgClient, 99);
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
        averageRoofZ != averageGroundZ) || delayedPostprocessTasks == null) {
      boolean flip = averageRoofZ < averageGroundZ;
      postprocessBuilding(building, topLevelThematicGeometries, mixedGeometries, flip);
      return flip ? 1 : -1;
    } else {
      // Non-nice data; queue.
      delayedPostprocessTasks.add((Boolean flip) ->
          postprocessBuilding(building, topLevelThematicGeometries, mixedGeometries, flip));
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
  private void postprocessBuilding(Building building,
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
        // Construct CityObject (this is the same entity as the thematic surface, but a different model)
        CityObject tsCityObject = new CityObject();
        tsCityObject.setEnvelopeType(new EnvelopeType(topLevelGeometry));
        tsCityObject.setCreationDate(OffsetDateTime.now().toString());
        tsCityObject.setLastModificationDate(OffsetDateTime.now().toString());
        tsCityObject.setUpdatingPerson("ThematicSurfaceDiscoveryAgent");
        tsCityObject.setGmlId(uuid);
        tsCityObject.setIri(uuid, Model.getNamespace(building.getIri().toString()));
        // Construct ThematicSurface
        ThematicSurface thematicSurface = new ThematicSurface();
        thematicSurface.setLod2MultiSurfaceId(topLevelGeometry);
        thematicSurface.setObjectClassId(33 + i);
        thematicSurface.setBuildingId(building.getIri());
        thematicSurface.setIri(uuid, Model.getNamespace(building.getIri().toString()));
        // Reassign SurfaceGeometry hierarchical properties
        topLevelGeometry.setParentId(null);
        List<SurfaceGeometry> allDescendantGeometries = topLevelGeometry.getFlattenedSubtree(false);
        for (SurfaceGeometry geometry : allDescendantGeometries) {
          geometry.setRootId(topLevelGeometry.getIri());
          geometry.setCityObjectId(thematicSurface.getIri());
        }
        // Push updates
        thematicSurface.queuePushForward(kgClient);
        tsCityObject.queuePushForward(kgClient);
        for (SurfaceGeometry geometry : allDescendantGeometries) geometry.queuePushForward(kgClient);
      }
    }
    for (SurfaceGeometry geometry : mixedGeometries) geometry.queueDeleteInstantiation(kgClient);
    // Unset building lodMultiSurfaceId. Technically these triples should already have been deleted when destroying
    // the mixed geometries, but just to be safe.
    building.setLod1MultiSurfaceId(null);
    building.setLod2MultiSurfaceId(null);
    building.setLod3MultiSurfaceId(null);
    building.queuePushForward(kgClient);
    Model.executeQueuedUpdates(kgClient);
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
      throw new BadRequestException("Malformed building: SurfaceGeometry " + surface.getIri() +
          " contains both sub-geometries and explicit GeometryType.");
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
    System.err.println(surface.getIri() + " identified as " + aggregateTheme + ".");
    return aggregateTheme;
  }

  public static Coordinate computeUnweightedCentroid(List<SurfaceGeometry> surfaceGeometries) {
    Stream<Coordinate> subCentroids = surfaceGeometries.stream().map(
        (SurfaceGeometry geometry) -> geometry.getGeometryType().getCentroid());
    return GeometryType.computeCentroid(subCentroids.toArray(Coordinate[]::new), false);
  }

}
