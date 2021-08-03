package uk.ac.cam.cares.twa.cities.agents.geo;

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
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import org.json.JSONObject;
import javax.ws.rs.BadRequestException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;
import uk.ac.cam.cares.jps.base.query.KGRouter;
import uk.ac.cam.cares.twa.cities.model.geo.Envelope;
import org.locationtech.jts.operation.distance3d.Distance3DOp;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.HttpMethod;

/**
 *  DistanceAgent class retrieves existing distance between the centroids of two objects envelopes from the KG.
 *  If such distance does not exists, DistanceAgent computes distance, inserts it into the KG.
 */

@WebServlet(
        urlPatterns = { DistanceAgent.URI_DISTANCE })

public class DistanceAgent extends JPSAgent {

    public static final String URI_DISTANCE = "/distance";
    public static final String KEY_REQ_METHOD = "method";
    public static final String KEY_IRIS = "iris";
    private static final String UNIT_ONTOLOGY = "http://www.ontology-of-units-of-measure.org/resource/om-2/";
    private static final String RDF_PREFIX = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String XML_SCHEMA = "http://www.w3.org/2001/XMLSchema#";
    private static final String OWL_SCHEMA = "http://www.w3.org/2002/07/owl#";
    private static final String KNOWLEDGE_GRAPH_URI = "http://localhost/blazegraph/namespace/SLA/";
    private static final String DISTANCE_GRAPH_URI = KNOWLEDGE_GRAPH_URI + "distance/";
    private static final String ROUTE = "http://kb/singapore-local";
    private KnowledgeBaseClientInterface kgClient;
    private static final String targetCRSstring = "EPSG:24500";

    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {

        validateInput(requestParams);

        ArrayList<String> uris = new ArrayList<>();
        JSONArray iris = requestParams.getJSONArray(KEY_IRIS);
        for (Object iri: iris) {
            uris.add(iri.toString());
        }

        ArrayList<Double> distances = new ArrayList<>();

        for (int firstURI = 0; firstURI < uris.size(); firstURI++) {
            String firstObjectUri = uris.get(firstURI);

            for ( int secondURI = firstURI+1; secondURI< uris.size(); secondURI++){
                String secondObjectUri = uris.get(secondURI);

                double distance = getDistance(firstObjectUri,secondObjectUri);

                if (distance < 0){
                    distance = computeDistance(getEnvelope(firstObjectUri), getEnvelope(secondObjectUri));
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
                      for (Object iri: iris) {
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
     * builds a SPARQL query for a specific pair of URIs to retrieve a distance.
     * @param firstUriString city object id 1
     * @param secondUriString city object id 2
     * @return returns a query string
     */
    private Query getDistanceQuery(String firstUriString, String secondUriString) {

        WhereBuilder wb = new WhereBuilder()
                .addPrefix( "om",  UNIT_ONTOLOGY )
                .addWhere("?distanceUri", "om:hasPhenomenon", NodeFactory.createURI(firstUriString))
                .addWhere("?distanceUri", "om:hasPhenomenon", NodeFactory.createURI(secondUriString))
                .addWhere("?distanceUri", "om:hasValue", "?valueUri")
                .addWhere("?valueUri", "om:hasNumericValue", "?distance");
        SelectBuilder sb = new SelectBuilder()
                .addVar( "?distance" )
                .addGraph(NodeFactory.createURI(DISTANCE_GRAPH_URI), wb);

        return sb.build();
    }

    /**
     * executes query on SPARQL endpoint and retrieves distance between two specific URIs.
     * @param firstUriString city object id 1
     * @param secondUriString city object id 2
     * @return distance as double
     */
    private double getDistance(String firstUriString, String secondUriString){

        double distance =  -1.0;
        setKGClient(true);

        Query q = getDistanceQuery(firstUriString, secondUriString);
        String queryResultString = kgClient.execute(q.toString());
        JSONArray queryResult = new JSONArray(queryResultString);

        if(!queryResult.isEmpty()){
            distance = Double.parseDouble(queryResult.getJSONObject(0).get("distance").toString());
        }
        return distance;
    }


    /**
     * sets KG Client for specific endpoint.
     * @param isQuery boolean
     */
    private void setKGClient(boolean isQuery){

        this.kgClient = KGRouter.getKnowledgeBaseClient(ROUTE,
                isQuery,
                !isQuery);
    }

    /**
     * returns object's envelope with its attributes.
     * @param uriString  city object id
     * @return envelope
     */
    public Envelope getEnvelope(String uriString) {

        String coordinateSystem = "EPSG:4326";
        Envelope envelope = new Envelope(coordinateSystem);
        String envelopeString = envelope.getEnvelopeString(uriString);
        envelope.extractEnvelopePoints(envelopeString);

        return envelope;
    }

    /**
     * computes distance between two centroids. Distance3D calculation works only with cartesian CRS.
     * @param envelope1 city object 1 envelope
     * @param envelope2 city object 2 envelope
     * @return distance
     */
    public double computeDistance(Envelope envelope1, Envelope envelope2){

        Point centroid1 = envelope1.getCentroid();
        Point centroid2 = envelope2.getCentroid();
        String crs1 = envelope1.getCRS();
        String crs2 = envelope2.getCRS();
        centroid1 = setUniformCRS(centroid1, crs1);
        centroid2 = setUniformCRS(centroid2, crs2);

        return Distance3DOp.distance(centroid1, centroid2);
    }

    /**
     * sets point CRS to a fixed coordinate system.
     * @param point original points
     * @param sourceCRSstring source CRS
     * @return points
     */
    private Point setUniformCRS(Point point, String sourceCRSstring) {

        try {
            CoordinateReferenceSystem sourceCRS = CRS.decode(sourceCRSstring);
            CoordinateReferenceSystem targetCRS = CRS.decode(targetCRSstring);
            MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
            point = (Point) JTS.transform(point, transform);
        }
        catch (FactoryException | TransformException | JPSRuntimeException e){
            throw new JPSRuntimeException(e);
        }
        return point;
    }

    /**
     * generates a update request to insert distance in the KG.
     * @param firstUri city object 1
     * @param secondUri city object 2
     * @param distance distance between two city objects
     * @return update query
     */
    private UpdateRequest getSetDistanceQuery(String firstUri, String secondUri, double distance){

        String distanceUri = DISTANCE_GRAPH_URI + "DIST_" + UUID.randomUUID() + "/";
        String valueUri = DISTANCE_GRAPH_URI + "VAL_" + UUID.randomUUID() + "/";

        UpdateBuilder ib= new UpdateBuilder()
                .addPrefix( "om",  UNIT_ONTOLOGY )
                .addPrefix( "rdf",  RDF_PREFIX )
                .addPrefix( "xsd",  XML_SCHEMA )
                .addPrefix( "owl",  OWL_SCHEMA )
                .addInsert( "?graph", NodeFactory.createURI(distanceUri), "rdf:type", "om:Total3DStartEndDistance")
                .addInsert( "?graph", NodeFactory.createURI(distanceUri), "rdf:type", "owl:NamedIndividual")
                .addInsert( "?graph", NodeFactory.createURI(distanceUri), "om:hasPhenomenon", NodeFactory.createURI(firstUri))
                .addInsert( "?graph", NodeFactory.createURI(distanceUri), "om:hasPhenomenon", NodeFactory.createURI(secondUri))
                .addInsert( "?graph", NodeFactory.createURI(distanceUri), "om:hasDimension", "om:lengthDimension")
                .addInsert( "?graph", NodeFactory.createURI(distanceUri), "om:hasValue", NodeFactory.createURI(valueUri))
                .addInsert( "?graph",  NodeFactory.createURI(valueUri), "rdf:type", "owl:NamedIndividual")
                .addInsert( "?graph",  NodeFactory.createURI(valueUri), "rdf:type", "om:Measure")
                .addInsert( "?graph",  NodeFactory.createURI(valueUri), "om:hasNumericValue", distance)
                .addInsert( "?graph",  NodeFactory.createURI(valueUri), "om:hasUnit", "om:metre");
        ib.setVar( Var.alloc( "graph" ), NodeFactory.createURI(DISTANCE_GRAPH_URI));

        return ib.buildRequest();
    }

    /**
     * inserts distance between two URIs into KG.
     * @param firstUri city object 1
     * @param secondUri city object 2
     * @param distance distance between two city objects
     * @return confirmation
     */
   private int setDistance(String firstUri, String secondUri, double distance){

       UpdateRequest ur = getSetDistanceQuery(firstUri, secondUri, distance);
       setKGClient(false);

       return kgClient.executeUpdate(ur);
   }
}
