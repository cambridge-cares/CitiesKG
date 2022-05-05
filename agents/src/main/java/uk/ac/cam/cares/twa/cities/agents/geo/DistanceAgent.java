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
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.models.ModelContext;
import uk.ac.cam.cares.twa.cities.models.geo.CityObject;
import uk.ac.cam.cares.twa.cities.models.geo.EnvelopeType;
import uk.ac.cam.cares.twa.cities.models.geo.GeometryType;

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
  public static final String KEY_DISTANCES = "distances";

  private static final String RDF_SCHEMA = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
  private static final String XML_SCHEMA = "http://www.w3.org/2001/XMLSchema#";
  private static final String OWL_SCHEMA = "http://www.w3.org/2002/07/owl#";
  private static final String DISTANCE_GRAPH = "/distance/";
  public static final String DEFAULT_SRS = "EPSG:4326";
  public static final String DEFAULT_TARGET_SRS = "EPSG:24500";

  // Repeating variables in SPARQL queries
  private static final String DISTANCE_URI = "distanceUri";
  private static final String OM_PREFIX = "om";
  private static final String PHENOMENON_PREDICATE = "hasPhenomenon";
  private static final String DISTANCE_OBJECT = "distance";
  private static final String DISTANCE_VALUE_URI = "valueUri";
  private static final String OCGML_PREFIX = "ocgml";
  private static final String SRS_PREDICATE = "srsname";
  private static final String METRIC_SRS_PREDICATE = "metricSrsName";
  private static final String SRS_NAME_OBJECT = "srsName";
  private static final String GRAPH_NAME = "graph";
  private static final String RDF_PREFIX = "rdf";
  private static final String RDF_PREDICATE = "type";
  private static final String OWL_PREFIX = "owl";
  private static final String OWL_PREDICATE = "NamedIndividual";
  private static final String NUMERIC_PREDICATE = "hasNumericValue";
  private static final String VALUE_PREDICATE = "hasValue";
  private static final String QST_MARK = "?";
  private static final String COLON = ":";

  // Variables fetched from config.properties file.
  private static String ocgmlUri;
  private static String unitOntology;
  private static String route;
  private static ModelContext context;

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

    if(uris.size() > 0) this.context = new ModelContext(route, getNamespace(uris.get(0)) + "/");

    ArrayList<Double> distances = new ArrayList<>();

    for (int firstURI = 0; firstURI < uris.size(); firstURI++) {
      String firstObjectUri = uris.get(firstURI);

      for (int secondURI = firstURI + 1; secondURI < uris.size(); secondURI++) {
        String secondObjectUri = uris.get(secondURI);

        double distance = getDistance(firstObjectUri, secondObjectUri);

        if (distance < 0) {
          String firstSrs = getObjectSrs(firstObjectUri, true);
          String secondSrs = getObjectSrs(secondObjectUri, true);
          distance =
              computeDistance(getEnvelope(firstObjectUri, firstSrs), getEnvelope(secondObjectUri, secondSrs));
          setDistance(firstObjectUri, secondObjectUri, distance);
        }
        distances.add(distance);
      }
    }
    requestParams.append(KEY_DISTANCES, distances);
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

  /** reads variable values relevant for DistanceAgent class from config.properties file. */
  private void readConfig() {
    ResourceBundle config = ResourceBundle.getBundle("config");
    route = config.getString("uri.route");
    ocgmlUri = config.getString("uri.ontology.ontocitygml");
    unitOntology = config.getString("uri.ontology.om");
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
            .addPrefix(OM_PREFIX, unitOntology)
            .addWhere(QST_MARK + DISTANCE_URI, OM_PREFIX + COLON + PHENOMENON_PREDICATE, NodeFactory.createURI(firstUriString))
            .addWhere(QST_MARK + DISTANCE_URI, OM_PREFIX + COLON + PHENOMENON_PREDICATE, NodeFactory.createURI(secondUriString))
            .addWhere(QST_MARK + DISTANCE_URI, OM_PREFIX + COLON + VALUE_PREDICATE, QST_MARK + DISTANCE_VALUE_URI)
            .addWhere(QST_MARK + DISTANCE_VALUE_URI, OM_PREFIX + COLON + NUMERIC_PREDICATE, QST_MARK + DISTANCE_OBJECT);
    SelectBuilder sb = new SelectBuilder()
            .addVar(QST_MARK + DISTANCE_OBJECT)
            .addGraph(NodeFactory.createURI(getDistanceGraphUri(firstUriString)), wb);
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

    Query q = getDistanceQuery(firstUriString, secondUriString);
    JSONArray queryResult = this.context.query(q.toString());

    if (!queryResult.isEmpty()) {
      distance = Double.parseDouble(queryResult.getJSONObject(0).get(DISTANCE_OBJECT).toString());
    }
    return distance;
  }

  /**
   * builds a SPARQL query to retrieve Object's namespace srs.
   *
   * @param uriString city object Uri as string.
   * @return sparql query.
   */
  private Query getObjectSRSQuery(String uriString, boolean source) {
    String predicate;
    if (source) { predicate = OCGML_PREFIX + COLON + SRS_PREDICATE; }
    else { predicate = OCGML_PREFIX + COLON + METRIC_SRS_PREDICATE; }

    SelectBuilder sb =
        new SelectBuilder()
            .addPrefix(OCGML_PREFIX, ocgmlUri)
            .addVar(QST_MARK + SRS_NAME_OBJECT)
            .addWhere(QST_MARK + "s" , predicate, QST_MARK + SRS_NAME_OBJECT);
    sb.setVar(Var.alloc("s"), NodeFactory.createURI(getNamespace(uriString) + "/"));

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
    if (source) { srs = DEFAULT_SRS; }
    else { srs = DEFAULT_TARGET_SRS; }

    Query q = getObjectSRSQuery(uriString, source);
    JSONArray queryResult = this.context.query(q.toString());

    if (!queryResult.isEmpty()) {
      srs = queryResult.getJSONObject(0).get(SRS_NAME_OBJECT).toString();
    }
    return srs;
  }

  /**
   * returns object's envelope with its attributes.
   *
   * @param uriString city object id
   * @return envelope
   */
  public EnvelopeType getEnvelope(String uriString, String coordinateSystem) {
    GeometryType.setSourceCrsName(coordinateSystem);
    CityObject cityObject = context.loadAll(CityObject.class, uriString);
    return cityObject.getEnvelopeType();
  }

  /**
   * computes distance between two centroids. Distance3D calculation works only with cartesian CRS.
   *
   * @param envelope1 city object 1 envelope
   * @param envelope2 city object 2 envelope
   * @return distance
   */
  public double computeDistance(EnvelopeType envelope1, EnvelopeType envelope2) {
    Coordinate centroid1 = envelope1.getCentroid();
    Coordinate centroid2 = envelope2.getCentroid();
    CoordinateReferenceSystem crs1 = envelope1.getSourceCrs();
    CoordinateReferenceSystem crs2 = envelope2.getSourceCrs();
    CoordinateReferenceSystem targetCrs = envelope1.getMetricCrs();
    try {
      Coordinate metricCentroid1 = JTS.transform(centroid1, null, CRS.findMathTransform(crs1, targetCrs, true));
      Coordinate metricCentroid2 = JTS.transform(centroid2, null, CRS.findMathTransform(crs2, targetCrs, true));
      return metricCentroid1.distance(metricCentroid2);
    } catch (FactoryException | TransformException e) {
      throw new JPSRuntimeException(e);
    }
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
      String distanceGraphUri = getDistanceGraphUri(firstUri);
    String distanceUri = distanceGraphUri + "DIST_" + UUID.randomUUID() + "/";
    String valueUri = distanceGraphUri + "VAL_" + UUID.randomUUID() + "/";

    UpdateBuilder ib =
        new UpdateBuilder()
            .addPrefix(OM_PREFIX, unitOntology)
            .addPrefix(RDF_PREFIX, RDF_SCHEMA)
            .addPrefix("xsd", XML_SCHEMA)
            .addPrefix(OWL_PREFIX, OWL_SCHEMA)
            .addInsert(QST_MARK + GRAPH_NAME, NodeFactory.createURI(distanceUri), RDF_PREFIX + COLON + RDF_PREDICATE, OM_PREFIX + COLON + "Total3DStartEndDistance")
            .addInsert(QST_MARK + GRAPH_NAME, NodeFactory.createURI(distanceUri), RDF_PREFIX + COLON + RDF_PREDICATE, OWL_PREFIX + COLON + OWL_PREDICATE)
            .addInsert(QST_MARK + GRAPH_NAME, NodeFactory.createURI(distanceUri), OM_PREFIX + COLON + PHENOMENON_PREDICATE, NodeFactory.createURI(firstUri))
            .addInsert(QST_MARK + GRAPH_NAME, NodeFactory.createURI(distanceUri), OM_PREFIX + COLON + PHENOMENON_PREDICATE, NodeFactory.createURI(secondUri))
            .addInsert(QST_MARK + GRAPH_NAME, NodeFactory.createURI(distanceUri), OM_PREFIX + COLON + "hasDimension", OM_PREFIX + COLON + "lengthDimension")
            .addInsert(QST_MARK + GRAPH_NAME, NodeFactory.createURI(distanceUri), OM_PREFIX + COLON + VALUE_PREDICATE, NodeFactory.createURI(valueUri))
            .addInsert(QST_MARK + GRAPH_NAME, NodeFactory.createURI(valueUri), RDF_PREFIX + COLON + RDF_PREDICATE, OWL_PREFIX + COLON + OWL_PREDICATE)
            .addInsert(QST_MARK + GRAPH_NAME, NodeFactory.createURI(valueUri), RDF_PREFIX + COLON + RDF_PREDICATE, OM_PREFIX + COLON + "Measure")
            .addInsert(QST_MARK + GRAPH_NAME, NodeFactory.createURI(valueUri), OM_PREFIX + COLON + NUMERIC_PREDICATE, distance)
            .addInsert(QST_MARK + GRAPH_NAME, NodeFactory.createURI(valueUri), OM_PREFIX + COLON + "hasUnit", OM_PREFIX + COLON + "metre");
    ib.setVar(Var.alloc(GRAPH_NAME), NodeFactory.createURI(distanceGraphUri));

    return ib.buildRequest();
  }

  /**
   * inserts distance between two URIs into KG.
   *
   * @param firstUri city object 1
   * @param secondUri city object 2
   * @param distance distance between two city objects
   */
  private void setDistance(String firstUri, String secondUri, double distance) {
    UpdateRequest ur = getSetDistanceQuery(firstUri, secondUri, distance);
    this.context.update(ur.toString());
  }
}
