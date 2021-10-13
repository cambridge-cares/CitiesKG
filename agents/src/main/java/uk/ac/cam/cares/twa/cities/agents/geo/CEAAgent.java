package uk.ac.cam.cares.twa.cities.agents.geo;

import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import org.json.JSONObject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.util.Set;
import javax.servlet.annotation.WebServlet;

@WebServlet(
        urlPatterns = {
                CEAAgent.URI_ACTION
        })
public class CEAAgent extends JPSAgent {
    public static final String KEY_REQ_METHOD = "method";
    public static final String URI_ACTION = "/cea";
    public static final String KEY_IRI = "iri";

    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {

        if (validateInput(requestParams)) {
        }

        return requestParams;
    }

    @Override
    public boolean validateInput(JSONObject requestParams) throws BadRequestException {
        boolean error = true;

        if (!requestParams.isEmpty()) {
            Set<String> keys = requestParams.keySet();
            if (keys.contains(KEY_REQ_METHOD) && keys.contains(KEY_IRI)) {
                if (requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.POST)) {
                    try {
                        if (!requestParams.getString(KEY_IRI).isEmpty()){
                            error = false;
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

}
