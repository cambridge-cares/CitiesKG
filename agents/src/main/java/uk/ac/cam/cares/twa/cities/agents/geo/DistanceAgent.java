package uk.ac.cam.cares.twa.cities.agents.geo;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.update.UpdateRequest;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.operation.distance3d.Distance3DOp;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;
import uk.ac.cam.cares.jps.base.query.KGRouter;
import uk.ac.cam.cares.twa.cities.model.geo.Envelope;

/**
 * DistanceAgent class retrieves existing distance between the centroids of two objects envelopes
 * from the KG. If such distance does not exists, DistanceAgent computes distance, inserts it into
 * the KG.
 */
@WebServlet(urlPatterns = {DistanceAgent.URI_DISTANCE})
public class DistanceAgent extends JPSAgent {

  public static final String URI_DISTANCE = "/distance";
  public static final String KEY_REQ_METHOD = "method";
  public static final String KEY_IRIS = "iris";

  private static final String UNIT_ONTOLOGY =
      "http://www.ontology-of-units-of-measure.org/resource/om-2/";
  private static final String RDF_PREFIX = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
  private static final String XML_SCHEMA = "http://www.w3.org/2001/XMLSchema#";
  private static final String OWL_SCHEMA = "http://www.w3.org/2002/07/owl#";
  private static final String DISTANCE_GRAPH = "/distance/";
  public static final String DEFAULT_SRS = "EPSG:4236";
  public static final String DEFAULT_TARGET_SRS = "EPSG:4236";

  private String ocgmlUri;
  private KnowledgeBaseClientInterface kgClient;
  private static String route;
  private String distanceGraphUri = "http://localhost" + DISTANCE_GRAPH;

  public DistanceAgent() {
    super();
    readConfig();
  }

  @Override
  public JSONObject processRequestParameters(JSONObject requestParams) {

    validateInput(requestParams);

    ArrayList<String> uris = new ArrayList<>();
    JSONArray iris = requestParams.getJSONArray(KEY_IRIS);
    for (Object iri : iris) {
      uris.add(iri.toString());
    }

    ArrayList<Double> distances = new ArrayList<>();

    for (int firstURI = 0; firstURI < uris.size(); firstURI++) {
      String firstObjectUri = uris.get(firstURI);

      for (int secondURI = firstURI + 1; secondURI < uris.size(); secondURI++) {
        String secondObjectUri = uris.get(secondURI);

        distanceGraphUri = getDistanceGraphUri(firstObjectUri);
        double distance = getDistance(firstObjectUri, secondObjectUri);

        if (distance < 0) {
          String firstSrs = getObjectSrs(firstObjectUri, true);
          String secondSrs = getObjectSrs(secondObjectUri, true);
          String targetSrs = getObjectSrs(firstObjectUri, false);
          distance =
              computeDistance(
                  getEnvelope(firstObjectUri, firstSrs), getEnvelope(secondObjectUri, secondSrs),
                  targetSrs);
          setDistance(firstObjectUri, secondObjectUri, distance);
        }
        distances.add(distance);
      }
    }
    requestParams.append("distances", distances);
    return requestParams;
  }

  @Override
  public boolean validateInput(JSONObject requestParams) throws BadRequestException {
    if (!requestParams.isEmpty()) {
      Set<String> keys = requestParams.keySet();
      if (keys.contains(KEY_REQ_METHOD) && keys.contains(KEY_IRIS)) {
        if (requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.POST)) {
          try {
            JSONArray iris = requestParams.getJSONArray(KEY_IRIS);
            for (Object iri : iris) {
              new URL((String) iri);
            }
            return true;

          } catch (Exception e) {
            throw new BadRequestException();
          }
        }
      }
    }
    throw new BadRequestException();
  }

  /**
   * reads variable values relevant for DistanceAgent class from config.properties file.
   */
  private void readConfig() {
    ResourceBundle config = ResourceBundle.getBundle("config");
    route = config.getString("uri.route");
    ocgmlUri = config.getString("uri.ontology.ontocitygml");
  }

  /**
   * builds a SPARQL query for a specific pair of URIs to retrieve a distance.
   *
   * @param firstUriString city object id 1
   * @param secondUriString city object id 2
   * @return returns a query string
   */
  private Query getDistanceQuery(String firstUriString, String secondUriString) {
    WhereBuilder wb =
        new WhereBuilder()
            .addPrefix("om", UNIT_ONTOLOGY)
            .addWhere("?distanceUri", "om:hasPhenomenon", NodeFactory.createURI(firstUriString))
            .addWhere("?distanceUri", "om:hasPhenomenon", NodeFactory.createURI(secondUriString))
            .addWhere("?distanceUri", "om:hasValue", "?valueUri")
            .addWhere("?valueUri", "om:hasNumericValue", "?distance");
    SelectBuilder sb =
        new SelectBuilder()
            .addVar("?distance")
            .addGraph(NodeFactory.createURI(distanceGraphUri), wb);
    return sb.build();
  }

  /**
   * creates distance graph uri from the cityobject uri and distance name tag.
   *
   * @param uriString city object id
   * @return uri of the distance graph in the object's namespace as string.
   */
  private String getDistanceGraphUri(String uriString) {
    String namespace = getNamespace(uriString);
    return namespace + DISTANCE_GRAPH;
  }

  /**
   * retrieves namespace from city object Uri.
   *
   * @param uriString city object id
   * @return Uri of the object's namespace as string.
   */
  private String getNamespace(String uriString) {
    String[] splitUri = uriString.split("/");
    return String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length - 2));
  }

  /**
   * executes query on SPARQL endpoint and retrieves distance between two specific URIs.
   *
   * @param firstUriString city object id 1
   * @param secondUriString city object id 2
   * @return distance as double
   */
  private double getDistance(String firstUriString, String secondUriString) {

    double distance = -1.0;
    setKGClient(true);

    Query q = getDistanceQuery(firstUriString, secondUriString);
    String queryResultString = kgClient.execute(q.toString());
    JSONArray queryResult = new JSONArray(queryResultString);

    if (!queryResult.isEmpty()) {
      distance = Double.parseDouble(queryResult.getJSONObject(0).get("distance").toString());
    }
    return distance;
  }

  /**
   * sets KG Client for specific endpoint.
   *
   * @param isQuery boolean
   */
  private void setKGClient(boolean isQuery) {

    this.kgClient = KGRouter.getKnowledgeBaseClient(route, isQuery, !isQuery);
  }

  /**
   * builds a SPARQL query to retrieve Object's namespace srs.
   *
   * @param uriString city object Uri as string.
   * @return sparql query.
   */
  private Query getObjectSRSQuery(String uriString, boolean source) {
    String predicate;
    if(source){
      predicate = "ocgml:srsname";
    }
    else{
      predicate = "ocgml:metricSrsName";
    }
    SelectBuilder sb =
        new SelectBuilder()
            .addPrefix("ocgml", ocgmlUri)
            .addVar("?srsName")
            .addWhere("?s", predicate, "?srsName");
    sb.setVar(Var.alloc("s"), NodeFactory.createURI(getNamespace(uriString) + "/sparql"));
    return sb.build();
  }

  /**
   * retrieves object's namespace srs.
   *
   * @param uriString object's Uri as string.
   * @return object's namespace srs.
   */
  private String getObjectSrs(String uriString, boolean source) {
    String srs;
    if (source){
      srs = DEFAULT_SRS;
    }
    else{
      srs = DEFAULT_TARGET_SRS;
    }
    setKGClient(true);

    Query q = getObjectSRSQuery(uriString, source);
    String queryResultString = kgClient.execute(q.toString());
    JSONArray queryResult = new JSONArray(queryResultString);

    if (!queryResult.isEmpty()) {
      srs = queryResult.getJSONObject(0).get("srsName").toString();
    }
    return srs;
  }

  /**
   * returns object's envelope with its attributes.
   *
   * @param uriString city object id
   * @return envelope
   */
  public Envelope getEnvelope(String uriString, String coordinateSystem) {
    Envelope envelope = new Envelope(coordinateSystem);
    String envelopeString = envelope.getEnvelopeString(uriString);
    envelope.extractEnvelopePoints(envelopeString);

    return envelope;
  }

  /**
   * computes distance between two centroids. Distance3D calculation works only with cartesian CRS.
   *
   * @param envelope1 city object 1 envelope
   * @param envelope2 city object 2 envelope
   * @return distance
   */
  public double computeDistance(Envelope envelope1, Envelope envelope2, String targetCrs) {

    Point centroid1 = envelope1.getCentroid();
    Point centroid2 = envelope2.getCentroid();
    String crs1 = envelope1.getCRS();
    String crs2 = envelope2.getCRS();
    centroid1 = setUniformCRS(centroid1, crs1, targetCrs);
    centroid2 = setUniformCRS(centroid2, crs2, targetCrs);

    return Distance3DOp.distance(centroid1, centroid2);
  }

  /**
   * sets point CRS to a fixed coordinate system.
   *
   * @param point original points
   * @param sourceCRSstring source CRS
   * @return points
   */
  private Point setUniformCRS(Point point, String sourceCRSstring, String targetCRSstring) {

    try {
      CoordinateReferenceSystem sourceCRS = CRS.decode(sourceCRSstring);
      CoordinateReferenceSystem targetCRS = CRS.decode(targetCRSstring);
      MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
      point = (Point) JTS.transform(point, transform);
    } catch (FactoryException | TransformException | JPSRuntimeException e) {
      throw new JPSRuntimeException(e);
    }
    return point;
  }

  /**
   * generates a update request to insert distance in the KG.
   *
   * @param firstUri city object 1
   * @param secondUri city object 2
   * @param distance distance between two city objects
   * @return update query
   */
  private UpdateRequest getSetDistanceQuery(String firstUri, String secondUri, double distance) {
    String distanceUri = distanceGraphUri + "DIST_" + UUID.randomUUID() + "/";
    String valueUri = distanceGraphUri + "VAL_" + UUID.randomUUID() + "/";

    UpdateBuilder ib =
        new UpdateBuilder()
            .addPrefix("om", UNIT_ONTOLOGY)
            .addPrefix("rdf", RDF_PREFIX)
            .addPrefix("xsd", XML_SCHEMA)
            .addPrefix("owl", OWL_SCHEMA)
            .addInsert(
                "?graph",
                NodeFactory.createURI(distanceUri),
                "rdf:type",
                "om:Total3DStartEndDistance")
            .addInsert(
                "?graph", NodeFactory.createURI(distanceUri), "rdf:type", "owl:NamedIndividual")
            .addInsert(
                "?graph",
                NodeFactory.createURI(distanceUri),
                "om:hasPhenomenon",
                NodeFactory.createURI(firstUri))
            .addInsert(
                "?graph",
                NodeFactory.createURI(distanceUri),
                "om:hasPhenomenon",
                NodeFactory.createURI(secondUri))
            .addInsert(
                "?graph",
                NodeFactory.createURI(distanceUri),
                "om:hasDimension",
                "om:lengthDimension")
            .addInsert(
                "?graph",
                NodeFactory.createURI(distanceUri),
                "om:hasValue",
                NodeFactory.createURI(valueUri))
            .addInsert("?graph", NodeFactory.createURI(valueUri), "rdf:type", "owl:NamedIndividual")
            .addInsert("?graph", NodeFactory.createURI(valueUri), "rdf:type", "om:Measure")
            .addInsert("?graph", NodeFactory.createURI(valueUri), "om:hasNumericValue", distance)
            .addInsert("?graph", NodeFactory.createURI(valueUri), "om:hasUnit", "om:metre");
    ib.setVar(Var.alloc("graph"), NodeFactory.createURI(distanceGraphUri));

    return ib.buildRequest();
  }

  /**
   * inserts distance between two URIs into KG.
   *
   * @param firstUri city object 1
   * @param secondUri city object 2
   * @param distance distance between two city objects
   * @return confirmation
   */
  private int setDistance(String firstUri, String secondUri, double distance) {

    UpdateRequest ur = getSetDistanceQuery(firstUri, secondUri, distance);
    setKGClient(false);

    return kgClient.executeUpdate(ur);
  }
}
