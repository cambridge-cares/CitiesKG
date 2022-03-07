package uk.ac.cam.cares.twa.cities.agents.geo;

import lombok.Getter;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.SPARQLUtils;
import uk.ac.cam.cares.twa.cities.models.ModelContext;
import uk.ac.cam.cares.twa.cities.models.geo.*;
import uk.ac.cam.cares.twa.cities.models.osid.UPRN;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;

@WebServlet(urlPatterns = {UPRNAgent.URI_LISTEN})
public class UPRNAgent extends JPSAgent {

  public static final String URI_LISTEN = "/uprn";
  public static final String KEY_REQ_METHOD = "method";
  public static final String KEY_NAMESPACE = "namespace";
  public static final String KEY_BUILDING = "cityObjectIRI";

  // for SRS query
  private static final String NO_CRS_EXCEPTION_TEXT = "Namespace has no CRS specified.";
  private static final String MULTIPLE_CRS_EXCEPTION_TEXT = "Namespace has more than one CRS specified.";
  private static final String QM = "?";
  private static final String SRS = "srs";
  private static final String THEMSURF = "themsurf";

  // for Features API
  private static final String FEATURES_API_ENDPOINT = "https://api.os.uk/features/v1/wfs";
  private final String osApiKey = ResourceBundle.getBundle("config").getString("os.api.key");

  private ModelContext context;
  @Getter private String buildingIri;
  @Getter private String namespaceIri;

  private final String targetResourceId = ResourceBundle.getBundle("config").getString("uri.route");

  private static final GeometryFactory geometryFactory = new GeometryFactory();
  private static final CoordinateReferenceSystem epsg27700;

  static {
    try {
      epsg27700 = CRS.decode("EPSG:27700");
    } catch (FactoryException e) {
      throw new JPSRuntimeException(e);
    }
  }

  @Override
  public JSONObject processRequestParameters(JSONObject requestParams) {

    validateInput(requestParams);

    importSrs(context);

    Executors.newSingleThreadExecutor().execute(() -> {

      // Query ground surfaces
      WhereBuilder condition = new WhereBuilder();
      SPARQLUtils.addPrefix("ocgml", condition);
      condition.addWhere(ModelContext.getModelVar(), SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID, ThematicSurface.GROUND_SURFACE);
      if (buildingIri != null)
        condition.addWhere(ModelContext.getModelVar(), SchemaManagerAdapter.ONTO_BUILDING_ID, NodeFactory.createURI(buildingIri));
      context.pullPartialWhere(ThematicSurface.class, condition, "buildingId");

      // Query leaf geometries
      condition = new WhereBuilder();
      SPARQLUtils.addPrefix("ocgml", condition);
      condition.addWhere(ModelContext.getModelVar(), SchemaManagerAdapter.ONTO_CITY_OBJECT_ID, QM + THEMSURF)
          .addWhere(ModelContext.getModelVar(), SchemaManagerAdapter.ONTO_IS_COMPOSITE, BigInteger.valueOf(0))
          .addWhere(QM + THEMSURF, SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID, ThematicSurface.GROUND_SURFACE);
      if (buildingIri != null)
        condition.addWhere(QM + THEMSURF, SchemaManagerAdapter.ONTO_BUILDING_ID, NodeFactory.createURI(buildingIri));
      List<SurfaceGeometry> geometries = context.pullPartialWhere(SurfaceGeometry.class, condition, "cityObjectId", "geometryType");

      // Query building cityobjects, which have envelopes
      condition = new WhereBuilder();
      SPARQLUtils.addPrefix("ocgml", condition);
      condition.addWhere(ModelContext.getModelVar(), SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID, Building.OBJECT_CLASS_ID);
      if (buildingIri != null)
        condition.addWhere(ModelContext.getModelVar(), SchemaManagerAdapter.ONTO_GML_ID, getTail(buildingIri));
      List<CityObject> buildings = new ArrayList<>(context.pullPartialWhere(CityObject.class, condition, "envelopeType", "gmlId"));

      // Sort geometries surfaces by pertinent building via thematic surface
      HashMap<String, ArrayList<SurfaceGeometry>> footprints = new HashMap<>();
      for (SurfaceGeometry geometry : geometries) {
        ThematicSurface parentSurface = context.optGetModel(ThematicSurface.class, geometry.getCityObjectId().toString());
        if (parentSurface == null) throw new JPSRuntimeException(geometry.getCityObjectId().toString());
        String gmlId = getTail(parentSurface.getBuildingId().getIri());
        if (!footprints.containsKey(gmlId)) footprints.put(gmlId, new ArrayList<>());
        footprints.get(gmlId).add(geometry);
      }

      // Obtain UPRNs for each building by envelope, then geometrically test for only intersects to actually store.
      for (CityObject building : buildings) {
        Coordinate lower = building.getEnvelopeType().getLowerBound();
        Coordinate upper = building.getEnvelopeType().getUpperBound();
        UPRN[] uprns = queryUprns(lower.x, lower.y, upper.x, upper.y, GeometryType.getSourceCrsName());
        for (UPRN uprn : uprns)
          for (SurfaceGeometry geometry : footprints.get(building.getGmlId()))
            if (uprnIntersectsGeometry(uprn, geometry.getGeometryType()))
              uprn.getIntersects().add(building);
      }

      context.pushAllChanges();

    });

    return requestParams;
  }

  public static boolean uprnIntersectsGeometry(UPRN uprn, GeometryType geometryType) {
    try {
      MathTransform epsg27700ToPolygonMetric = CRS.findMathTransform(epsg27700, geometryType.getMetricCrs());
      Coordinate polygonMetricCrsCoord = new Coordinate();
      JTS.transform(uprn.getEastingNorthingCoordinate().coordinate, polygonMetricCrsCoord, epsg27700ToPolygonMetric);
      if(geometryType.getMetricPolygon().intersects(geometryFactory.createPoint(polygonMetricCrsCoord)))
        return true;
    } catch (FactoryException | TransformException e) {
      throw new JPSRuntimeException(e);
    }
    return false;
  }

  private String getTail(String str) {
    String[] parts = str.split("/");
    return parts[parts.length - 1];
  }

  @Override
  public boolean validateInput(JSONObject requestParams) throws BadRequestException {
    if (!requestParams.isEmpty()
        && requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.PUT)) {
      try {
        Set<String> keys = requestParams.keySet();
        if (keys.contains(KEY_NAMESPACE)) {
          namespaceIri = new URI(requestParams.getString(KEY_NAMESPACE)).toString();
          buildingIri = keys.contains(KEY_BUILDING) ? new URI(requestParams.getString(KEY_BUILDING)).toString() : null;
          context = new ModelContext(targetResourceId, namespaceIri);
          return true;
        }
      } catch (URISyntaxException | JSONException e) {
        throw new BadRequestException(e);
      }
    }
    throw new BadRequestException();
  }


  /**
   * Queries the database for the coordinate reference system to use and sets it as the {@link GeometryType} source crs.
   */
  private void importSrs(ModelContext context) throws JPSRuntimeException {
    // TODO: convert ocgml:srsname to a SchemaManagerAdapter constant when it exists.
    SelectBuilder srsQuery = new SelectBuilder();
    SPARQLUtils.addPrefix("ocgml:srsname", srsQuery);
    srsQuery.addVar(QM + SRS).addWhere(NodeFactory.createURI(namespaceIri), "ocgml:srsname", QM + SRS);
    JSONArray srsResponse = context.query(srsQuery.buildString());
    if (srsResponse.length() == 0) {
      throw new JPSRuntimeException(NO_CRS_EXCEPTION_TEXT);
    } else if (srsResponse.length() > 1) {
      throw new JPSRuntimeException(MULTIPLE_CRS_EXCEPTION_TEXT);
    } else {
      GeometryType.setSourceCrsName(srsResponse.getJSONObject(0).getString(SRS));
    }
  }

  public UPRN[] queryUprns(double startX, double startY, double endX, double endY, String srsName) {
    try {
      // Build request
      String url = FEATURES_API_ENDPOINT + "?key=" + osApiKey;
      url += "&service=wfs&version=2.0.0&outputFormat=GEOJSON&request=GetFeature&typeNames=OpenUPRN_Address&srsName=EPSG:27700";
      url += "&bbox=" + startX + "," + startY + "," + endX + "," + endY + "," + srsName;
      // Make request
      HttpGet get = new HttpGet(url);
      get.addHeader("Content-Type", "application/json");
      HttpResponse response = HttpClients.createDefault().execute(get);
      String responseString = EntityUtils.toString(response.getEntity());
      JSONArray uprnJsons = new JSONObject(responseString).getJSONArray("features");
      UPRN[] uprns = new UPRN[uprnJsons.length()];
      for (int i = 0; i < uprnJsons.length(); i++) {
        uprns[i] = UPRN.loadFromJson(context, uprnJsons.getJSONObject(i));
      }
      return uprns;
    } catch (IOException e) {
      throw new JPSRuntimeException(e);
    }
  }

}
