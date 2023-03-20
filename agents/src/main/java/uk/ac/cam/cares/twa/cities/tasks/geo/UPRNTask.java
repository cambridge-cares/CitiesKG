package uk.ac.cam.cares.twa.cities.tasks.geo;

import lombok.Getter;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.ogm.models.SPARQLUtils;
import uk.ac.cam.cares.ogm.models.ModelContext;
import uk.ac.cam.cares.twa.cities.model.geo.*;
import uk.ac.cam.cares.twa.cities.models.osid.UPRN;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

public class UPRNTask implements Runnable {

  @Getter private String buildingIri;
  @Getter private String namespaceIri;
  private ModelContext context;
  private HashMap<String, ArrayList<SurfaceGeometry>> footprints;

  private static final String QM = "?";
  private static final String ROOT = "root";
  private static final String BUILDING = "building";
  private static final String THEMSURF = "themsurf";
  private static final ArrayList<CityObject> failed_bldgs = new ArrayList<>();

  // for Features API
  private static final String FEATURES_API_ENDPOINT = "https://api.os.uk/features/v1/wfs";
  private final String osApiKey = ResourceBundle.getBundle("config").getString("os.api.key");

  private static final GeometryFactory geometryFactory = new GeometryFactory();
  private static final CoordinateReferenceSystem epsg27700;

  private static final Logger LOGGER = LogManager.getLogger(UPRNTask.class);
  static {
    try {
      epsg27700 = CRS.decode("EPSG:27700");
    } catch (FactoryException e) {
      throw new JPSRuntimeException(e);
    }
  }

  public UPRNTask(ModelContext context, String buildingIri, String namespaceIri) {
    this.context = context;
    this.buildingIri = buildingIri;
    this.namespaceIri = namespaceIri;
  }

  @Override
  public void run (){

    // Clear list of failed buildings (to avoid accumulation for consecutive requests with individual buildings)
    failed_bldgs.clear();
    // Query building cityobjects, which have envelopes
    WhereBuilder condition = new WhereBuilder();
    SPARQLUtils.addPrefix("ocgml", condition);
    condition.addWhere(ModelContext.getModelVar(), SchemaManagerAdapter.ONTO_OBJECT_CLASS_ID, Building.OBJECT_CLASS_ID);
    if (buildingIri != null)
      condition.addWhere(ModelContext.getModelVar(), SchemaManagerAdapter.ONTO_GML_ID, getTail(buildingIri));
    List<CityObject> buildings = new ArrayList<>(context.pullPartialWhere(CityObject.class, condition, "envelopeType", "gmlId"));

    footprints = new HashMap<>();
    getFootprintsFromThematicSurfaces();
    getFootprintsFromLod0FootprintId();

    int k=0;
    // Obtain UPRNs for each building by envelope, then geometrically test for only intersects to actually store.
    for (CityObject building : buildings) {
      k+=1;
      Coordinate lower = building.getEnvelopeType().getLowerBound();
      Coordinate upper = building.getEnvelopeType().getUpperBound();
      UPRN[] uprns = queryUprns(lower.x, lower.y, upper.x, upper.y, GeometryType.getSourceCrsName(), building);
      for (UPRN uprn : uprns)
        for (SurfaceGeometry geometry : footprints.get(building.getGmlId()))
          if (uprnIntersectsGeometry(uprn, geometry.getGeometryType()))
            if(!uprn.getIntersects().contains(building))
              uprn.getIntersects().add(building);
      if(k%2000==0)
        context.pushAllChanges();
    }

    LOGGER.info("Failed buildings:");
    for(CityObject cityObject : failed_bldgs){
      LOGGER.info(cityObject.getIri());
    }
    context.pushAllChanges();

  }

  private void getFootprintsFromThematicSurfaces() {
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

    // Sort geometries by pertinent building via thematic surface
    for (SurfaceGeometry geometry : geometries) {
      ThematicSurface parentSurface = context.optGetModel(ThematicSurface.class, geometry.getCityObjectId().toString());
      if (parentSurface == null) throw new JPSRuntimeException(geometry.getCityObjectId().toString());
      String gmlId = getTail(parentSurface.getBuildingId().getIri());
      if (!footprints.containsKey(gmlId)) footprints.put(gmlId, new ArrayList<>());
      footprints.get(gmlId).add(geometry);
    }
  }

  private void getFootprintsFromLod0FootprintId() {
    // Query leaf geometries
    WhereBuilder condition = new WhereBuilder();
    SPARQLUtils.addPrefix("ocgml", condition);
    Object buildingNode = buildingIri == null ? (QM + BUILDING) : NodeFactory.createURI(buildingIri);
    condition.addWhere(ModelContext.getModelVar(), SchemaManagerAdapter.ONTO_IS_COMPOSITE, BigInteger.valueOf(0))
        .addWhere(ModelContext.getModelVar(), SchemaManagerAdapter.ONTO_ROOT_ID, QM + ROOT)
        .addWhere(buildingNode, SchemaManagerAdapter.ONTO_FOOTPRINT_ID, QM + ROOT);
    List<SurfaceGeometry> geometries = context.pullPartialWhere(SurfaceGeometry.class, condition, "cityObjectId", "geometryType");
    // Sort geometries by pertinent building
    for (SurfaceGeometry geometry : geometries) {
      String gmlId = getTail(geometry.getCityObjectId().toString());
      if (!footprints.containsKey(gmlId)) footprints.put(gmlId, new ArrayList<>());
      footprints.get(gmlId).add(geometry);
    }
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

  private static String getTail(String str) {
    String[] parts = str.split("/");
    return parts[parts.length - 1];
  }


  public UPRN[] queryUprns(double startX, double startY, double endX, double endY, String srsName, CityObject bldg) {
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

      UPRN[] uprns = new UPRN[0];
      try {
        JSONArray uprnJsons = new JSONObject(responseString).getJSONArray("features");
        uprns = new UPRN[uprnJsons.length()];
        for (int i = 0; i < uprnJsons.length(); i++) {
          uprns[i] = UPRN.loadFromJson(context, uprnJsons.getJSONObject(i));
        }
      } catch (Exception e) {
        failed_bldgs.add(bldg);
      }

      return uprns;
    } catch (IOException e) {
      throw new JPSRuntimeException(e);
    }
  }

}
