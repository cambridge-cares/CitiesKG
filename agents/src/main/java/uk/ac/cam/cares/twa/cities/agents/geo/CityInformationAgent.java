package uk.ac.cam.cares.twa.cities.agents.geo;

import lombok.Getter;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.http.Http;
import uk.ac.cam.cares.ogm.models.ModelContext;
import uk.ac.cam.cares.twa.cities.AccessAgentMapping;
import uk.ac.cam.cares.twa.cities.model.geo.CityObject;
import uk.ac.cam.cares.twa.cities.model.ontochemplant.Building;
import uk.ac.cam.cares.twa.cities.model.ontochemplant.PlantItem;
import uk.ac.cam.cares.twa.cities.model.ontochemplant.ocgml;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Set;


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
      String route = AccessAgentMapping.getTargetResourceID(cityObjectIri);
      if (route != null) {
        this.route = route;
      }

      if (this.route == "jriEPSG24500") {

        // Temporary model for cityobject to check if the object is building or cityfurniture
        ModelContext city_object_context = new ModelContext(this.route, AccessAgentMapping.getNamespaceEndpoint(cityObjectIri));
        CityObject cityObject = city_object_context.createHollowModel(CityObject.class, cityObjectIri);
        city_object_context.pullPartial(cityObject, "objectClassId");

        // New model context for jibusinessunits namespace
        ModelContext context = new ModelContext("jibusinessunits");

        // City furniture object ID = 21, Building object ID = 26
        if (cityObject.getObjectClassId().intValue() == 21) {
          String cityfurnitureIri = cityObjectIri.replace("cityobject", "cityfurniture");
          ocgml x = context.createHollowModel(ocgml.class, cityfurnitureIri);
          context.pullAll(x);

          PlantItem plantitem = context.createHollowModel(PlantItem.class, x.getOntoCityGMLRepresentationOf().toString());
          context.recursivePullAll(plantitem, 1);

          ArrayList<PlantItem> PlantItemList = new ArrayList<>();
          PlantItemList.add(plantitem);
          cityObjectInformation.put(PlantItemList);

        } else {
          String buildingIri = cityObjectIri.replace("cityobject", "building");
          ocgml x = context.createHollowModel(ocgml.class, buildingIri);
          context.pullAll(x);

          Building building = context.createHollowModel(Building.class, x.getOntoCityGMLRepresentationOf().toString());
          context.recursivePullAll(building, 3);

          ArrayList<Building> BuildingList = new ArrayList<>();
          BuildingList.add(building);
          cityObjectInformation.put(BuildingList);
        }


      } else {
        ModelContext context = new ModelContext(this.route, AccessAgentMapping.getNamespaceEndpoint(cityObjectIri));
        CityObject cityObject = context.createHollowModel(CityObject.class, cityObjectIri);
        if (lazyload) {
          context.pullAll(cityObject);
        } else {
          context.recursivePullAll(cityObject, 1);
        }

        cityObject.setEnvelopeType(null);
        ArrayList<CityObject> cityObjectList = new ArrayList<>();
        cityObjectList.add(cityObject);
        cityObjectInformation.put(cityObjectList);
      }
    }

    requestParams.append(KEY_CITY_OBJECT_INFORMATION, cityObjectInformation);


    /**
     * {"iris": ["http://www.theworldavatar.com:83/citieskg/namespace/berlin/sparql/cityobject/BLDG_0003000000a50c90/"],
     * "context": {"http://www.theworldavatar.com:83/citieskg/otheragentIRI": {"key1":"value1", "key2": value2"},
     * "http://www.theworldavatar.com:83/citieskg/anotheragentIRI": {"key3":"value3", "key4": value4"},}
     * }
     **/

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
        try {
          JSONObject response = new JSONObject(Http.execute(request));
          // specific agent response added to the city information response.
          requestParams.append(agentURL, response);
        } catch (Exception e) {
          // ignore if no response from context endpoint
        }

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
}