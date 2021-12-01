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
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;
import uk.ac.cam.cares.jps.base.query.KGRouter;
import uk.ac.cam.cares.twa.cities.model.geo.CityObject;


/**
 * CityInformationAgent is about something good.
 */
@WebServlet(urlPatterns = {CityInformationAgent.URI_CITY_OBJECT_INFORMATION})
public class CityInformationAgent extends JPSAgent {

  public static final String URI_CITY_OBJECT_INFORMATION = "/cityobjectinformation";
  public static final String KEY_REQ_METHOD = "method";
  public static final String KEY_IRIS = "iris";
  public static final String KEY_ATTRIBUTES = "attributes";

  private KnowledgeBaseClientInterface kgClient;
  private static String route = "http://kb/citieskg-berlin";
  private boolean lazyload = false;

  @Override
  public JSONObject processRequestParameters(JSONObject requestParams) {

    validateInput(requestParams);

    ArrayList<String> uris = new ArrayList<>();
    JSONArray iris = requestParams.getJSONArray(KEY_IRIS);
    for (Object iri : iris) {
      uris.add(iri.toString());
    }
    JSONArray cityObjectInformation = new JSONArray(); // ask Arek if JSON Array is the right entity

    setKGClient(true);

    for (int iri = 0; iri < uris.size(); iri++) {
      String cityObjectIri= uris.get(iri);
      try {
        CityObject cityObject = new CityObject();

        cityObject.fillScalars(cityObjectIri, kgClient);
        cityObject.fillGenericAttributes(cityObjectIri,kgClient, lazyload);
        cityObject.fillExternalReferences(cityObjectIri,kgClient, lazyload);

        ArrayList<CityObject> cityObjectList = new ArrayList<>();
        cityObjectList.add(cityObject);
        cityObjectInformation.put(cityObjectList);
        /*
        cityObjectInformation.put(cityObject.getId());
        cityObjectInformation.put(cityObject.getGenericAttributesIris());
        cityObjectInformation.put(cityObject.getGenericAttributes()); */
      }
      catch (NoSuchFieldException | IllegalAccessException e) {
        e.printStackTrace();
      }

    }
    requestParams.append(KEY_ATTRIBUTES, cityObjectInformation);
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
   * sets KG Client for specific endpoint.
   * @param isQuery boolean
   */
  private void setKGClient(boolean isQuery) {
    this.kgClient = KGRouter.getKnowledgeBaseClient(route, isQuery, !isQuery);
  }
}