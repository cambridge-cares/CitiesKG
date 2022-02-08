package uk.ac.cam.cares.twa.cities.agents.geo;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Set;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import lombok.Getter;
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.twa.cities.models.geo.CityObject;
import uk.ac.cam.cares.twa.cities.models.geo.GenericAttribute;


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

    // for city object iris
    ArrayList<String> uris = new ArrayList<>();
    JSONArray iris = requestParams.getJSONArray(KEY_IRIS);
    for (Object iri : iris) { uris.add(iri.toString());
    }
    // for use case context
    //ArrayList<String> agents = new ArrayList<>();
    //JSONArray contexts = requestParams.getJSONArray((KEY_CONTEXT));
    //for (Object context : contexts) { agents.add(context.toString());
    //}

    JSONArray cityObjectInformation = new JSONArray();

    for (String cityObjectIri : uris) {
      CityObject cityObject = new CityObject();
      cityObject.setIri(URI.create(cityObjectIri));

      if (lazyload) {
        cityObject.pullAll(route, 0);
      }
      else {
        cityObject.pullAll(route, 1);
      }

      ArrayList<CityObject> cityObjectList = new ArrayList<>();
      cityObjectList.add(cityObject);
      cityObjectInformation.put(cityObjectList);

      //pass the information further to the other agent

    }
    requestParams.append(KEY_CITY_OBJECT_INFORMATION, cityObjectInformation);
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


  /**
   * reads variable values relevant for CityInformationAgent class from config.properties file.
   */
  private void readConfig() {
    ResourceBundle config = ResourceBundle.getBundle("config");
    lazyload = Boolean.getBoolean(config.getString("loading.status"));
    route = config.getString("uri.route");
  }
}