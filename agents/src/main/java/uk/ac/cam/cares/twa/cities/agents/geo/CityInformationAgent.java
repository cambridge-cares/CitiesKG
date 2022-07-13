package uk.ac.cam.cares.twa.cities.agents.geo;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.Set;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.twa.cities.models.ModelContext;
import uk.ac.cam.cares.twa.cities.models.geo.CityObject;


/**
 * CityInformationAgent is about something good.
 */
@WebServlet(urlPatterns = {CityInformationAgent.URI_CITY_OBJECT_INFORMATION})
public class CityInformationAgent extends JPSAgent {

  public static final String URI_CITY_OBJECT_INFORMATION = "/cityobjectinformation";
  public static final String KEY_REQ_METHOD = "method";
  public static final String KEY_IRIS = "iris";
  public static final String KEY_ATTRIBUTES = "attributes";
  public static final String SLASH = "/";

  private static String route;


  public CityInformationAgent() {
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
    JSONArray cityObjectInformation = new JSONArray();

    System.err.println(iris);

    for (String cityObjectIri : uris) {
      ModelContext context = new ModelContext(route, getNamespace(cityObjectIri) + "/");
      CityObject cityObject = context.loadAll(CityObject.class, cityObjectIri);

      ArrayList<CityObject> cityObjectList = new ArrayList<>();
      cityObjectList.add(cityObject);
      cityObjectInformation.put(cityObjectList);
    }
    requestParams.append(KEY_ATTRIBUTES, cityObjectInformation);
    return requestParams;
  }

  private String getNamespace(String uriString) {
    String[] splitUri = uriString.split("/");
    return String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length - 2));
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


  /**
   * reads variable values relevant for CityInformationAgent class from config.properties file.
   */
  private void readConfig() {
    ResourceBundle config = ResourceBundle.getBundle("config");
    route = config.getString("uri.route");
  }

}