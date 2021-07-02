package uk.ac.cam.cares.twa.cities.agents.geo;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Var;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import org.json.JSONObject;
import javax.ws.rs.BadRequestException;
import java.net.URI;

/** This comment describes the DistanceAgent class.
 *  Two unique IRIs are used as inputs for the DistanceAgent class.
 *  First, the agent checks if the distance already exists between two IRIs and if not, it uses the envelope string of each IRI to create two envelope objects and calculate the distance between them.
 *  Second, the DistanceAgent class inserts the resulted distance into the Blazegraph and also returns it.
 */
public class DistanceAgent extends JPSAgent {
    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {
        validateInput(requestParams);
        return requestParams;
    }

    @Override
    public boolean validateInput(JSONObject requestParams) throws BadRequestException {
        if (requestParams.isEmpty()) {
            throw new BadRequestException();
        }
        return true;
    }


    /** builds a SPARQL query for a specific URI to find a distance.
     */
    private Query getDistanceQuery(String uriString1, String uriString2){
        SelectBuilder sb = new SelectBuilder()
                .addPrefix( "ocgml",  ONTOLOGY_URI )
                .addVar( "?distance" )
                .addGraph(NodeFactory.createURI(DISTANCE_GRAPH_URI), "?firstUri", "ocgml:hasDistance", "?distanceUri")
                .addWhere("?secondUri", "ocgml:hasDistance", "?distanceUri")
                .addWhere("?distanceUri", "ocgml:hasValue", "?Distance");
        sb.setVar( Var.alloc( "firstUri" ), NodeFactory.createURI(uriString1));
        sb.setVar( Var.alloc( "secondUri" ), NodeFactory.createURI(uriString2));
        Query q = sb.build();

        return q;
    }


    /** Retrieves values from KG for distances between objects.
     */
    private float[] getDistance(URI[] objects){
        float[] distances = new float[objects.length * objects.length];
        return distances;

    }


    /** gets KG Client for created distance query.
     */
    private KnowledgeBaseClientInterface getKGClientForDistanceQuery() {
        String targetResourceIRIOrPath = ROUTE;
        KnowledgeBaseClientInterface kgClient = KGRouter.getKnowledgeBaseClient(targetResourceIRIOrPath,
                true,
                false);

        return kgClient;
    }


    /** Returns object envelope with its attributes.
     */
    private String[] getEnvelope(URI[] objects){
        String[] envelopeString = new String[objects.length];
        return envelopeString;

    }


    /** Computes distance between two centroids. Distance3D calculation works only with cartesian CRS
     */
    private double computeDistance(Envelope envelope1, Envelope envelope2){
        //if JAVA library is not available:
        //CommandHelper.executeCommands("", new ArrayList<String>("python script"));

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
            throw new JPSRuntimeException(e);
        }
        return point;
    }


    /** The setDistance method writes distance between objects into KG.
     */
   private void setDistance(String[] objects, double[] distances){}

}
