package uk.ac.cam.cares.twa.cities.agents.geo;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.Set;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import lombok.Getter;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.twa.cities.AccessAgentMapping;
import uk.ac.cam.cares.twa.cities.models.ModelContext;
import uk.ac.cam.cares.twa.cities.models.geo.CityObject;
import uk.ac.cam.cares.jps.base.http.Http;


/**
 * CityInformationAgent is about something good.
 */
@WebServlet(urlPatterns = {CityInformationAgent.URI_CITY_OBJECT_INFORMATION})
public class CityInformationAgent extends JPSAgent {

  public static final String URI_CITY_OBJECT_INFORMATION = "/cityobjectinformation";
  public static final String KEY_REQ_METHOD = "method";
  public static final String KEY_IRIS = "iris";
  public static String KEY_CONTEXT = "context";
  public static final String KEY_CITY_OBJECT_INFORMATION = "cityobjectinformation";

  @Getter private String route;
  private boolean lazyload;


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
    for (String cityObjectIri : uris) {
      String graphNamespace = getNamespace(cityObjectIri)+ "/";

      // get route from graph namespace endpoint
      // if endpoint is not found in mapping, use value read from config.properties
      String route = AccessAgentMapping.getTargetResourceID(graphNamespace);
      if (route != null) {
        this.route = route;
      }

      ModelContext context = new ModelContext(this.route, graphNamespace);
      CityObject cityObject = context.createHollowModel(CityObject.class, cityObjectIri);
      if (lazyload) {
        context.pullAll(cityObject);
      }
      else {
        context.recursivePullAll(cityObject, 1);
      }
      ArrayList<CityObject> cityObjectList = new ArrayList<>();
      cityObjectList.add(cityObject);
      cityObjectInformation.put(cityObjectList);
    }

    requestParams.append(KEY_CITY_OBJECT_INFORMATION, cityObjectInformation);

    //{"iris": ["http://www.theworldavatar.com:83/citieskg/namespace/berlin/sparql/cityobject/BLDG_0003000000a50c90/"],
    //  "context": {"http://www.theworldavatar.com:83/citieskg/otheragentIRI": {"key1":"value1", "key2": value2"},
    //  "http://www.theworldavatar.com:83/citieskg/anotheragentIRI": {"key3":"value3", "key4": value4"},}
    //  }

    // passing information from original request to other agents mentioned in the context.
    if (requestParams.keySet().contains(KEY_CONTEXT)) {
      Set<String> agentURLs = requestParams.getJSONObject(KEY_CONTEXT).keySet();
      for (String agentURL : agentURLs) {
        JSONObject requestBody =  new JSONObject();
        requestBody.put(KEY_IRIS, requestParams.getJSONArray(KEY_IRIS));
        JSONObject agentKeyValuePairs = requestParams.getJSONObject(KEY_CONTEXT).getJSONObject(agentURL);
        for (String key : agentKeyValuePairs.keySet()) {
          requestBody.put(key,agentKeyValuePairs.get(key));
        }
        String[] params = new String[0];
        HttpPost request =  Http.post(agentURL, requestBody, "application/json","application/json", params);
        JSONObject response = new JSONObject(Http.execute(request));

        // specific agent response added to the city information response.
        requestParams.append(agentURL, response);
      }
    }


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

            //to check if agent iris are valid URL.
            if (keys.contains(KEY_CONTEXT)){
              Set<String> agentURLs = requestParams.getJSONObject(KEY_CONTEXT).keySet();
              for (String agentURL : agentURLs) {
                new URL(agentURL);
              }
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
    lazyload = Boolean.getBoolean(config.getString("loading.status"));
    route = config.getString("uri.route");
  }

  private String getNamespace(String uriString) {
    String[] splitUri = uriString.split("/");
    return String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length - 2));
  }
}