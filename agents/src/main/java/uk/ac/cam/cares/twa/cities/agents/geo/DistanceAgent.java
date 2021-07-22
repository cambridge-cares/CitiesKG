package uk.ac.cam.cares.twa.cities.agents.geo;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
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

/** This comment describes the DistanceAgent class.
 *  Two unique IRIs are used as inputs for the DistanceAgent class.
 *  First, the agent checks if the distance already exists between two IRIs and if not, it uses the envelope string of each IRI to create two envelope objects and calculate the distance between them.
 *  Second, the DistanceAgent class inserts the resulted distance into the Blazegraph and also returns it.
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


    public static void main(String[] args) throws ParseException {
        DistanceAgent distanceAgent =  new DistanceAgent();
        String uri1 = "http://localhost/berlin/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3/";
        String uri2 = "http://localhost/berlin/cityobject/UUID_6cbfb096-5116-4962-9162-48b736768cd4/";

        double distance;
        distance = distanceAgent.computeDistance(distanceAgent.getEnvelope(uri1), distanceAgent.getEnvelope(uri2));
        System.out.println(distanceAgent.setDistance(uri1, uri2, distance));
    }


    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {

        validateInput(requestParams);

        ArrayList<String> uris = new ArrayList<>();
        JSONArray iris = requestParams.getJSONArray(KEY_IRIS);
        for (Object iri: iris) {
            uris.add(iri.toString());
        }

        ArrayList distances = new ArrayList();

        for (int firstURI = 0; firstURI < uris.size(); firstURI++) {
            String objectUri1 = uris.get(firstURI);

            for ( int secondURI = firstURI+1; secondURI< uris.size(); secondURI++){
                String objectUri2 = uris.get(secondURI);

                double distance = 0;

                try {
                    distance = getDistance(objectUri1, objectUri2);
                }
                catch (Exception e) {
                    distance = computeDistance(getEnvelope(objectUri1), getEnvelope(objectUri2));
                    setDistance(objectUri1, objectUri1, distance);
                }
                distances.add(distance);
            }
        }
        requestParams.append("distances", distances);
        return requestParams;
    }

    @Override
    public boolean validateInput(JSONObject requestParams) throws BadRequestException {
        boolean error = true;
        if (!requestParams.isEmpty()) {
            Set<String> keys = requestParams.keySet();
            if (keys.contains(KEY_REQ_METHOD) && keys.contains(KEY_IRIS)) {
                if (requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.POST)) {
                    try {
                      JSONArray iris = requestParams.getJSONArray(KEY_IRIS);
                      for (Object iri: iris) {
                          URL distUrl = new URL((String) iri);
                      }
                    } catch (Exception e) {
                        throw new BadRequestException();
                    }
                }
            }
        }
        if (error) {
            throw new BadRequestException();
        }
        return true;
    }


    /** builds a SPARQL query for a specific URI to find a distance. */
    private Query getDistanceQuery(String uriString1, String uriString2) throws ParseException {

        WhereBuilder wb = new WhereBuilder()
                .addPrefix( "om",  UNIT_ONTOLOGY )
                .addWhere("?distanceUri", "om:hasPhenomenon", NodeFactory.createURI(uriString1))
                .addWhere("?distanceUri", "om:hasPhenomenon", NodeFactory.createURI(uriString2))
                .addWhere("?distanceUri", "om:hasValue", "?valueUri")
                .addWhere("?valueUri", "om:hasNumericValue", "?distance");
        SelectBuilder sb = new SelectBuilder()
                .addVar( "?distance" )
                .addGraph(NodeFactory.createURI(DISTANCE_GRAPH_URI), wb);
        Query q = sb.build();

        return q;
    }

    /** get KGClient via KGrouter and execute query to get distances between two objects. */
    private double getDistance(String uriString1, String uriString2) throws ParseException {

       double distance;
       setKGClient(ROUTE, true);

        Query q = getDistanceQuery(uriString1, uriString2);
        String queryResultString = kgClient.execute(q.toString());
       JSONArray queryResult = new JSONArray(queryResultString);
       distance = Double.parseDouble(queryResult.getJSONObject(0).get("distance").toString());

       return distance;
    }

    /** sets KG Client for created query. */
    private void setKGClient(String route, boolean isQuery){
        // if isQuery true, a query client is created for querying the KG, if false: and update client is created.
        this.kgClient = KGRouter.getKnowledgeBaseClient(route,
                isQuery,
                !isQuery);
    }

    /** Returns object envelope with its attributes. */
    public Envelope getEnvelope(String uriString) {

        String coordinateSystem = "EPSG:4326";
        Envelope envelope = new Envelope(coordinateSystem);
        String envelopeString = envelope.getEnvelopeString(uriString);
        envelope.extractEnvelopePoints(envelopeString);

        return envelope;
    }

    /** Computes distance between two centroids. Distance3D calculation works only with cartesian CRS. */
    public double computeDistance(Envelope envelope1, Envelope envelope2){

        Point centroid1 = envelope1.getCentroid();
        Point centroid2 = envelope2.getCentroid();
        String crs1 = envelope1.getCRS();
        String crs2 = envelope2.getCRS();
        centroid1 = setUniformCRS(centroid1, crs1, "EPSG:24500");
        centroid2 = setUniformCRS(centroid2, crs2, "EPSG:24500");

        return Distance3DOp.distance(centroid1, centroid2);
    }

    /** Sets point CRS to the same coordinate system.*/
    private Point setUniformCRS(Point point, String sourceCRSstring, String targetCRSstring) {
        try {
            CoordinateReferenceSystem sourceCRS = CRS.decode(sourceCRSstring);
            CoordinateReferenceSystem targetCRS = CRS.decode(targetCRSstring);
            MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
            point = (Point) JTS.transform(point, transform);
        } catch (FactoryException | TransformException e) {
            throw new JPSRuntimeException(e); }
        return point;
    }

    /** This method generates a update request to insert distance in the KG. */
    private UpdateRequest getSetDistanceQuery(String firstUri, String secondUri, double distance){

        String distanceUri = DISTANCE_GRAPH_URI + "DIST_" + uniqueURIGenerator() + "/";
        String valueUri = DISTANCE_GRAPH_URI + "VAL_" + uniqueURIGenerator() + "/";

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
        UpdateRequest ur = ib.buildRequest();

        return ur;
    }

    /** This method generates a unique ID for distance subject.*/
    private String uniqueURIGenerator(){
        String uuid = UUID.randomUUID().toString();

        return uuid;
    }

    /** The setDistance method writes distance between objects into KG. */
   private int setDistance(String firstUri, String secondUri, double distance){
       UpdateRequest ur = getSetDistanceQuery(firstUri, secondUri, distance);
       setKGClient(ROUTE, false);

       return kgClient.executeUpdate(ur);
   }
}
