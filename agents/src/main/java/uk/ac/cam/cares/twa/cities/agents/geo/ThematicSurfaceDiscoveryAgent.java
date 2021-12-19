package uk.ac.cam.cares.twa.cities.agents.geo;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;

import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;
import uk.ac.cam.cares.jps.base.query.RemoteKnowledgeBaseClient;
import uk.ac.cam.cares.twa.cities.Model;
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

  public static final String URI_LISTEN = "/discovery/thematicsurface";
  public static final String KEY_COBI = "cityObjectIRI";
  private static final String OCGML = ResourceBundle.getBundle("config").getString("uri.ontology.ontocitygml");
  private static final String SRS = "srs";
  private URI cityObjectURI;

  private KnowledgeBaseClientInterface kgClient; // AccessAgent should be used instead of this
  private static String route;

  public ThematicSurfaceDiscoveryAgent() {
    super();
    ResourceBundle config = ResourceBundle.getBundle("config");
    route = config.getString("uri.route");
    this.kgClient = new RemoteKnowledgeBaseClient(route, route);
  }

  @Override
  public JSONObject processRequestParameters(JSONObject requestParams) {
    validateInput(requestParams);
    requestParams.put("acceptHeaders", "application/json");
    // Check srs
    // Move this into an srs agent?
    Instant before = Instant.now();
    String responseString = kgClient.execute("SELECT ?" + SRS + " WHERE {?a <" + OCGML + "srsname> ?" + SRS + " }");
    JSONArray srsQueryResponse = new JSONArray(responseString);
    if (srsQueryResponse.length() == 0) {
      throw new BadRequestException("Namespace has no coordinate reference system specified.");
    } else if (srsQueryResponse.length() > 1) {
      throw new BadRequestException("Namespace has more than one coordinate reference system specified.");
    } else {
      GeometryType.setSourceCrsName(srsQueryResponse.getJSONObject(0).getString(SRS));
      GeometryType.setMetricCrsName("EPSG:25833");
    }
    Duration srsTime = Duration.between(before, Instant.now());
    before = Instant.now();
    // Check cityObject is building
    CityObject cityObject = new CityObject();
    cityObject.pullAll(cityObjectURI.toString(), kgClient, 1);
    if (cityObject.getObjectClassId() != 26)
      throw new BadRequestException("IRI is not a building.");
    // Load building
    String buildingIri = cityObject.getIri().toString().replace("cityobject", "building");
    Building building = new Building();
    building.pullAll(buildingIri, kgClient, 99);
    Duration queryTime = Duration.between(before, Instant.now());
    before = Instant.now();
    // Fetch relevant multisurface
    SurfaceGeometry root = building.getLod2MultiSurfaceId();
    if (root == null) root = building.getLod1MultiSurfaceId();
    if (root == null) throw new JPSRuntimeException("Building does not have an LoD1 or LoD2 MultiSurface.");
    // Sort into thematic surfaces
    List<List<SurfaceGeometry>> topLevelThematicGeometries = Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    List<List<SurfaceGeometry>> bottomLevelThematicGeometries = Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    List<SurfaceGeometry> mixedGeometries = new ArrayList<>();
    recursiveDiscover(root, topLevelThematicGeometries, bottomLevelThematicGeometries, mixedGeometries);
    // Calculate centroid of detected roofs' centroids and centroid of detected grounds' centroids
    double averageRoofZ = computeUnweightedCentroid(bottomLevelThematicGeometries.get(Theme.ROOF.index)).getZ();
    double averageGroundZ = computeUnweightedCentroid(bottomLevelThematicGeometries.get(Theme.GROUND.index)).getZ();
    // If the roofs are below the grounds, we probably have the winding convention inverted: flip.
    if (averageRoofZ < averageGroundZ) {
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
        tsCityObject.setIri(uuid, Model.getNamespace(buildingIri));
        // Construct ThematicSurface
        ThematicSurface thematicSurface = new ThematicSurface();
        thematicSurface.setLod2MultiSurfaceId(topLevelGeometry);
        thematicSurface.setObjectClassId(33 + i);
        thematicSurface.setBuildingId(building.getIri());
        thematicSurface.setIri(uuid, Model.getNamespace(buildingIri));
        // Reassign SurfaceGeometry hierarchial properties
        topLevelGeometry.setParentId(null);
        List<SurfaceGeometry> allDescendantGeometries = topLevelGeometry.getFlattenedSubtree(false);
        for (SurfaceGeometry geometry : allDescendantGeometries) {
          geometry.setRootId(topLevelGeometry.getIri());
          geometry.setCityObjectId(thematicSurface.getIri());
        }
        // Push updates
        thematicSurface.queuePushForwardScalars();
        tsCityObject.queuePushForwardScalars();
        for (SurfaceGeometry geometry : allDescendantGeometries) geometry.queuePushForwardScalars();
      }
    }
    for (SurfaceGeometry geometry : mixedGeometries) geometry.queueDeleteInstantiation();
    // Unset building lodMultiSurfaceId. Technically these triples should already have been deleted when destroying
    // the mixed geometries, but just to be safe.
    building.setLod1MultiSurfaceId(null);
    building.setLod2MultiSurfaceId(null);
    building.queuePushForwardScalars();
    Duration processingTime = Duration.between(before, Instant.now());
    before = Instant.now();
    Model.executeQueuedUpdates(kgClient);
    Duration updateTime = Duration.between(before, Instant.now());
    JSONObject times = new JSONObject();
    times.append("srsTime", queryTime.toMillis());
    times.append("queryTime", queryTime.toMillis());
    times.append("processingTime", processingTime.toMillis());
    times.append("updateTime", updateTime.toMillis());
    requestParams.append("times", times);
    return requestParams;
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
      System.err.println(geometry.getNormal().getX() + "," + geometry.getNormal().getY() + "," + geometry.getNormal().getZ() + ":" + geometry.getArea());
      double z = geometry.getNormal().getZ();
      aggregateTheme = Math.abs(z) < 0.01 ? Theme.WALL : (z > 0 ? Theme.ROOF : Theme.GROUND);
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

  @Override
  public boolean validateInput(JSONObject requestParams) throws BadRequestException {
    if (!requestParams.isEmpty()
        && requestParams.get(CityImportAgent.KEY_REQ_METHOD).equals(HttpMethod.GET)) {
      Set<String> keys = requestParams.keySet();
      if (keys.contains(KEY_COBI)) {
        try {
          System.out.println(requestParams.getString(KEY_COBI));
          cityObjectURI = new URI(requestParams.getString(KEY_COBI));
          return true;
        } catch (URISyntaxException e) {
          throw new BadRequestException();
        }
      }
    }
    throw new BadRequestException();
  }

}
