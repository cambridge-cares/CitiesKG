package uk.ac.cam.cares.twa.cities.agents.geo;

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

    /** The getDistance method retrieves values from KG for distances between objects.
     *  It returns distance if such already exists, and if not - computes, sets in the KG and returns it.
     */
    private float[] getDistance(URI[] objects){
        float[] distances = new float[objects.length * objects.length];
        return distances;

    }

    /** The getEnvelope method retrieves values from the KG for objects envelopes.
     *  It returns object's envelope string.
     */
    private String[] getEnvelope(URI[] objects){
        String[] envelopeString = new String[objects.length];
        return envelopeString;

    }

    /** The computeDistance method computes distance between envelope centroids.
     *  It returns array of distances between objects.
     */
    private float[] computeDistance(float[] centroids){
        float[] computedDistances = new float [centroids.length * centroids.length];
        return computedDistances;

    }

    /** The setUniformCRS method sets the CRS to the same coordinate system.
     */
    private void setUniformCRS(String[] CRString){

    }

    /** The setDistance method writes distance between objects into KG.
     */
    private void setDistance(URI[] objects, float[] distances){

    }


}
