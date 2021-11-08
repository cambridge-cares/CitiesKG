package uk.ac.cam.cares.twa.cities.agents.geo;

import java.net.URL;
import java.util.ArrayList;
import java.util.Set;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;


/**
 * CityInformationAgent is about something good.
 */


@WebServlet(urlPatterns = {CityInformationAgent.URI_CITY_INFORMATION})
public class CityInformationAgent extends JPSAgent {

  public static final String URI_CITY_INFORMATION = "/cityinformation";
  public static final String KEY_REQ_METHOD = "method";
  public static final String KEY_IRIS = "iris";
  public static final String KEY_ATTRIBUTES = "attributes";


  @Override
  public JSONObject processRequestParameters(JSONObject requestParams) {

    validateInput(requestParams);

    ArrayList<String> uris = new ArrayList<>();
    JSONArray iris = requestParams.getJSONArray(KEY_IRIS);
    for (Object iri : iris) {
      uris.add(iri.toString());
    }
    JSONArray attributes = new JSONArray();

    // agent specific code to be added.

    requestParams.append(KEY_ATTRIBUTES, attributes);
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

  // add method signatures

}