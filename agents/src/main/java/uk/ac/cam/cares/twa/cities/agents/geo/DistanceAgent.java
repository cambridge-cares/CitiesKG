package uk.ac.cam.cares.twa.cities.agents.geo;

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

    /**
     *  The method retrieves values from KG for distances between objects. If it does not exists - it computes it.
     */
    private float[] getDistance(URI[] objects){
        float[] distances = new float[objects.length * objects.length];
        return distances;

    }

    /**
     *  The getEnvelope method retrieves values from the KG for objects envelopes as strings.
     */
    private String[] getEnvelope(URI[] objects){
        String[] envelopeString = new String[objects.length];
        return envelopeString;

    }

    /** The computeDistance method computes distance between two centroids.
     */
    private double computeDistance(Envelope envelope1, Envelope envelope2){
        //if JAVA library is not available:
        //CommandHelper.executeCommands("", new ArrayList<String>("python script"));

        Point centroid1 = envelope1.getCentroid();
        Point centroid2 = envelope2.getCentroid();
        String crs1 = envelope1.getCRS();
        String crs2 = envelope2.getCRS();
        if (!crs1.equals(crs2)){
            centroid2 = setUniformCRS(centroid2, crs2, crs1);
        }
        return centroid1.distance(centroid2);
    }

    /** The setUniformCRS method sets the CRS to the same coordinate system.
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
   // private void setDistance(URI[] objects, float[] distances){}

}
