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
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
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
  public static final String KEY_CONTEXT = "context";
  public static final String KEY_FILTERS = "filters";
  public static final String KEY_TOTAL_GFA = "TotalGFA";
  public static final String KEY_USE_PREDICATE = "allowsUse";
  public static final String KEY_PROGRAMME_PREDICATE = "allowsProgramme";
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
      ModelContext context = new ModelContext(route, getNamespace(cityObjectIri)+ "/");
      CityObject cityObject = context.createHollowModel(CityObject.class, cityObjectIri);
      if (lazyload) {
        context.pullAll(cityObject);
      } else {
        context.recursivePullAll(cityObject, 1);
      }
      ArrayList<CityObject> cityObjectList = new ArrayList<>();
      cityObjectList.add(cityObject);
      cityObjectInformation.put(cityObjectList);
    }
    requestParams.append(KEY_CITY_OBJECT_INFORMATION, cityObjectInformation);

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
        requestParams.append(agentURL, response);
      }
    }
    // retrieve city objects based on provided filters.
    if (requestParams.keySet().contains(KEY_FILTERS)) {
      JSONObject filters = requestParams.getJSONObject(KEY_FILTERS);
      if (filters.keySet().size() > 1) {
        String predicate = "";
        if (filters.keySet().contains(KEY_USE_PREDICATE)) {
          predicate = KEY_USE_PREDICATE;
        } else {
          predicate = KEY_PROGRAMME_PREDICATE;
        }
        ArrayList<String> onto_elements = new ArrayList<>(filters.getJSONObject(predicate).keySet());
        Query query = getFilterQuery(predicate, onto_elements);
        JSONArray cityobjects = new JSONArray();
        JSONArray query_result = AccessAgentCaller.queryStore(route, query.toString());
        for (int i = 0; i < query_result.length(); i++) {
          JSONObject row = (JSONObject) query_result.get(i);
          cityobjects.put(row.get("plot_id"));
        }
        requestParams.put("filtered_objects", cityobjects);
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

            // to check that there is at least one filter parameter sent.
            if (keys.contains(KEY_FILTERS)){
              JSONObject filters = requestParams.getJSONObject(KEY_FILTERS);
              if (!filters.keySet().contains(KEY_TOTAL_GFA)) {
                throw new BadRequestException();
              } else {
                if (filters.keySet().contains(KEY_USE_PREDICATE) && filters.keySet()
                    .contains(KEY_PROGRAMME_PREDICATE)) {
                  throw new BadRequestException();
                } else if (!(filters.keySet().contains(KEY_USE_PREDICATE) || filters.keySet()
                    .contains(KEY_PROGRAMME_PREDICATE))) {
                  if (filters.keySet().size() > 1) {
                    throw new BadRequestException();
                  }
                }
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

  private Query getFilterQuery(String predicate, ArrayList<String> onto_elements) {

    WhereBuilder wb =
        new WhereBuilder()
            .addPrefix("zo", "http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#");
    if (predicate.equals(KEY_USE_PREDICATE)) {
      for (String element : onto_elements) {
        wb.addWhere("?zone", "zo:" + predicate, "zo:" + element);
      }
    } else if (predicate.equals(KEY_PROGRAMME_PREDICATE)) {
      for (String element : onto_elements) {
        wb.addWhere("?zone", "zo:allowsUse", "?use");
        wb.addWhere("?use", "zo:" + predicate, "zo:" + element);
      }
    }
    wb.addWhere("?plot_id", "zo:hasZone", "?zone");
    SelectBuilder sb = new SelectBuilder()
        .addVar("?plot_id")
        .addGraph(NodeFactory.createURI("http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/ontozone/"), wb);
    return sb.build();
  }
}