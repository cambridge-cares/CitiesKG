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

/** This comment describes the DistanceAgent class.
 *  Two unique IRIs are used as inputs for the DistanceAgent class.
 *  First, the agent checks if the distance already exists between two IRIs and if not, it uses the envelope string of each IRI to create two envelope objects and calculate the distance between them.
 *  Second, the DistanceAgent class inserts the resulted distance into the Blazegraph and also returns it.
 */
@WebServlet(
        urlPatterns = {
            DistanceAgent.URI_DISTANCE
        })
public class DistanceAgent extends JPSAgent {
    public static final String URI_DISTANCE = "/distance";
    public static final String KEY_REQ_METHOD = "method";
    public static final String KEY_IRIS = "iris";

    private static final String ONTOLOGY_URI = "http://locahost/ontocitygml/";
    private static final String KNOWLEDGE_GRAPH_URI = "http://localhost:9999/blazegraph/namespaces/SLA/sparql/";
    private static final String DISTANCE_GRAPH_URI = KNOWLEDGE_GRAPH_URI + "distance/";
    private static final String ROUTE = "http://kb/citieskg-singapore";

    private KnowledgeBaseClientInterface kgClient;

    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {
        validateInput(requestParams);
        String uri1 = "http://www.obja.com/a";
        String uri2 = "http://www.obja.com/b";

        ArrayList<String> uris = new ArrayList<>();
        uris.add(uri1);
        uris.add(uri2);

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
                        throw new BadRequestException(); }
            }
        }
    }
        if (error) {
        throw new BadRequestException(); }
        return true; }


    /** builds a SPARQL query for a specific URI to find a distance.
     */
    private Query getDistanceQuery(String uriString1, String uriString2){
        WhereBuilder wb = new WhereBuilder()
                .addPrefix( "ocgml",  ONTOLOGY_URI )
                .addWhere("?firstUri", "ocgml:hasDistance", "?distanceUri")
                .addWhere("?secondUri", "ocgml:hasDistance", "?distanceUri")
                .addWhere("?distanceUri", "ocgml:hasValue", "?distance");

        SelectBuilder sb = new SelectBuilder()
                .addVar( "?distance" )
                .addGraph(NodeFactory.createURI(DISTANCE_GRAPH_URI), wb);
        sb.setVar( Var.alloc( "firstUri" ), NodeFactory.createURI(uriString1));
        sb.setVar( Var.alloc( "secondUri" ), NodeFactory.createURI(uriString2));
        Query q = sb.build();

        return q;
    }


    /** get KGClient via KGrouter and execute query to get distances between two objects.
     */
    private double getDistance(String uriString1, String uriString2) {

       double distance;
       Query q = getDistanceQuery(uriString1, uriString2);
       setKGClient(ROUTE);

       String queryResultString = kgClient.execute(q.toString());

       JSONArray queryResult = new JSONArray(queryResultString);
       distance = (Double) queryResult.getJSONObject(0).get("Distance");

       return distance;
    }


    /** sets KG Client for created envelope query.
     */
    private void setKGClient(String route){
        this.kgClient = KGRouter.getKnowledgeBaseClient(route,
                true,
                false);
    }


    /** Returns object envelope with its attributes.
     */
    public Envelope getEnvelope(String uriString) {

        String coordinateSystem = "EPSG:4326";
        Envelope envelope = new Envelope(coordinateSystem);
        String envelopeString = envelope.getEnvelopeString(uriString);
        envelope.extractEnvelopePoints(envelopeString);

        return envelope;
    }


    /** Computes distance between two centroids. Distance3D calculation works only with cartesian CRS
     */
    public double computeDistance(Envelope envelope1, Envelope envelope2){

        Point centroid1 = envelope1.getCentroid();
        Point centroid2 = envelope2.getCentroid();
        String crs1 = envelope1.getCRS();
        String crs2 = envelope2.getCRS();
        centroid1 = setUniformCRS(centroid1, crs1, "EPSG:24500");
        centroid2 = setUniformCRS(centroid2, crs2, "EPSG:24500");

        return Distance3DOp.distance(centroid1, centroid2);
    }


    /** Sets point CRS to the same coordinate system.
     */
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


    /** This method generates a unique ID for distance subject.
     */
    private UpdateRequest getSetDistanceQuery(String firstUri, String secondUri, double distance){
        String distanceUri = uniqueURIGenerator();

        UpdateBuilder insertBuilder = new UpdateBuilder()
                .addPrefix( "ocgml",  ONTOLOGY_URI )
                .addInsert( "?graph", NodeFactory.createURI(firstUri), "ocgml:hasDistance", "?distanceUri")
                .addInsert( "?graph", NodeFactory.createURI(secondUri), "ocgml:hasDistance", "?distanceUri")
                .addInsert( "?graph", "?distanceUri", "ocgml:hasValue", distance);
        insertBuilder.setVar( Var.alloc( "graph" ), NodeFactory.createURI(DISTANCE_GRAPH_URI));
        insertBuilder.setVar( Var.alloc( "distanceUri" ), NodeFactory.createURI(distanceUri));
        UpdateRequest ur = insertBuilder.buildRequest();

        return ur;
    }


    /** This method generates a unique ID for distance subject.
     */
    private String uniqueURIGenerator(){
        String uuid = UUID.randomUUID().toString();
        String uniqueName = "DIST_" + uuid;
        String distanceUri =  DISTANCE_GRAPH_URI + uniqueName;
        return distanceUri;
    }


    /** The setDistance method writes distance between objects into KG.
     */
   private void setDistance(String firstUri, String secondUri,  double distance){

       UpdateRequest ur = getSetDistanceQuery(firstUri, secondUri, distance);
       setKGClient(ROUTE);
       kgClient.execute(ur.toString());
   }
}
