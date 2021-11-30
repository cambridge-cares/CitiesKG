package uk.ac.cam.cares.twa.cities.agents.geo;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;

@WebServlet(
    urlPatterns = {
        ThematicSurfaceDiscoveryAgent.URI_LISTEN
    })
public class ThematicSurfaceDiscoveryAgent extends JPSAgent {

  public static final String URI_LISTEN = "/discovery/thematicsurface";

  @Override
  public JSONObject processRequestParameters(JSONObject requestParams) {

    return requestParams;
  }

  @Override
  public boolean validateInput(JSONObject requestParams) throws BadRequestException {

    return false;
  }
}
