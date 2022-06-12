package uk.ac.cam.cares.twa.cities.agents.geo;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Iterator;
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
import org.citydb.database.adapter.blazegraph.SchemaManagerAdapter;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.config.JPSConstants;
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
  public static final String KEY_CITY_OBJECT_INFORMATION = "cityobjectinformation";
  public static final String KEY_TOTAL_GFA = "TotalGFA";
  public static final String KEY_USE_PREDICATE = "allowsUse";
  public static final String KEY_PROGRAMME_PREDICATE = "allowsProgramme";
  private static final String QM  =  "?";
  private static final String ZONE = "zone";
  private static final String USE =  "use";
  private static final String ONTOZONING_PREFIX = "zo";
  private static final String CITY_OBJECT_ID = "cityObjectId";


  @Getter private String route;
  private boolean lazyload;
  @Getter private String onto_zoning;


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

    if (requestParams.keySet().contains(KEY_CONTEXT)) {
      Set<String> agentURLs = requestParams.getJSONObject(KEY_CONTEXT).keySet();
      for (String agentURL : agentURLs) {
        JSONObject requestBody =  new JSONObject();
        requestBody.put(KEY_IRIS, requestParams.getJSONArray(KEY_IRIS));
        JSONObject agentKeyValuePairs = requestParams.getJSONObject(KEY_CONTEXT).getJSONObject(agentURL);
        JSONObject response = new JSONObject();

        // if CIA is directly connecting to access agent but original request misses "targetresourceiri" key.
        if ((agentURL.contains(JPSConstants.ACCESS_AGENT_PATH)) && !(agentKeyValuePairs.keySet().contains(JPSConstants.TARGETIRI))){
          JSONObject filters = requestParams.getJSONObject(KEY_CONTEXT).getJSONObject(agentURL);
          if (filters.keySet().size() > 1) {
            String predicate = "";
            if (filters.keySet().contains(KEY_USE_PREDICATE)) {
              predicate = KEY_USE_PREDICATE;
            } else {
              predicate = KEY_PROGRAMME_PREDICATE;
            }
            ArrayList<String> onto_elements = new ArrayList<>(filters.getJSONObject(predicate).keySet());
            response.put("filtered", getFilteredObjects(predicate, onto_elements));
            requestParams.put(agentURL, response);
          }
        } else {
            for (String key : agentKeyValuePairs.keySet()) {
              requestBody.put(key, agentKeyValuePairs.get(key));
              String[] params = new String[0];
              HttpPost request =  Http.post(agentURL, requestBody, "application/json","application/json", params);
              response = new JSONObject(Http.execute(request));
              requestParams.append(agentURL, response);
            }
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
                JSONObject agentKeyValuePairs = requestParams.getJSONObject(KEY_CONTEXT).getJSONObject(agentURL);
                if ((agentURL.contains(JPSConstants.ACCESS_AGENT_PATH)) &&
                    !(agentKeyValuePairs.keySet().contains(JPSConstants.TARGETIRI))) {
                  if (!agentKeyValuePairs.keySet().contains(KEY_TOTAL_GFA)) {
                    throw new BadRequestException();
                  } else {
                    if (agentKeyValuePairs.keySet().contains(KEY_USE_PREDICATE) && agentKeyValuePairs.keySet()
                        .contains(KEY_PROGRAMME_PREDICATE)) {
                      throw new BadRequestException();
                    } else if (!(agentKeyValuePairs.keySet().contains(KEY_USE_PREDICATE) || agentKeyValuePairs.keySet()
                        .contains(KEY_PROGRAMME_PREDICATE))) {
                      if (agentKeyValuePairs.keySet().size() > 1) {
                        throw new BadRequestException();
                      }
                    }
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
    onto_zoning = config.getString("uri.ontology.ontozoning");
  }

  private String getNamespace(String uriString) {
    String[] splitUri = uriString.split("/");
    return String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length - 2));
  }

  private Query getFilterQuery(String predicate, ArrayList<String> onto_class) {

    WhereBuilder wb =
        new WhereBuilder()
            .addPrefix(ONTOZONING_PREFIX, onto_zoning);
    if (predicate.equals(KEY_USE_PREDICATE)) {
      for (String use_class: onto_class) {
        wb.addWhere(QM +ZONE, ONTOZONING_PREFIX+":" + KEY_USE_PREDICATE, ONTOZONING_PREFIX + ":" + use_class);
      }
    } else if (predicate.equals(KEY_PROGRAMME_PREDICATE)) {
      for (String programme_class : onto_class) {
        wb.addWhere(QM +ZONE, ONTOZONING_PREFIX + ":" + KEY_USE_PREDICATE, QM + USE);
        wb.addWhere(QM + USE,  ONTOZONING_PREFIX + ":" + KEY_PROGRAMME_PREDICATE, ONTOZONING_PREFIX + ":" + programme_class);
      }
    }
    wb.addWhere(QM + CITY_OBJECT_ID, ONTOZONING_PREFIX + ":hasZone", QM +ZONE);
    SelectBuilder sb = new SelectBuilder()
        .addVar(QM + CITY_OBJECT_ID)
        .addGraph(NodeFactory.createURI("http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/ontozone/"), wb);
    return sb.build();
  }

  private JSONArray getFilteredObjects (String predicate, ArrayList<String> onto_elements) {
    Query query = getFilterQuery(predicate, onto_elements);
    JSONArray cityobjects = new JSONArray();
    JSONArray query_result = AccessAgentCaller.queryStore(route, query.toString());
    for (int i = 0; i < query_result.length(); i++) {
      JSONObject row = (JSONObject) query_result.get(i);
      cityobjects.put(row.get(CITY_OBJECT_ID));
    }
    return cityobjects;
  }

  private JSONArray filterGFA () {
    JSONArray cityobjects = new JSONArray();
    return cityobjects;
  }
}