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
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathFactory;
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
  public static final String ALLOWS_USE = "allowsUse";
  public static final String MAY_ALLOW_USE = "mayAllowUse";
  public static final String ALLOWS_PROGRAMME = "allowsProgramme";
  public static final String MAY_ALLOW_PROGRAMME = "mayAllowProgramme";
  public static final String HAS_ZONE_PREDICATE = "hasZone";
  private static final String QM  =  "?";
  private static final String ZONE = "zone";
  private static final String USE =  "use";
  private static final String ONTOZONING_PREFIX = "zo";
  private static final String CITY_OBJECT_ID = "cityObjectId";

  private static final String ONTO_PLANCON_PREFIX = "pco";
  private static final String OM_PREFIX = "om";


  @Getter private String route;
  private boolean lazyload;
  @Getter private String onto_zoning;
  @Getter private String om;

  @Getter private String onto_planning_concept;


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
            if (filters.keySet().contains(ALLOWS_USE)) {
              predicate = ALLOWS_USE;
            } else {
              predicate = ALLOWS_PROGRAMME;
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
                    if (agentKeyValuePairs.keySet().contains(ALLOWS_USE) && agentKeyValuePairs.keySet()
                        .contains(ALLOWS_PROGRAMME)) {
                      throw new BadRequestException();
                    } else if (!(agentKeyValuePairs.keySet().contains(ALLOWS_USE) || agentKeyValuePairs.keySet()
                        .contains(ALLOWS_PROGRAMME))) {
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
    om = config.getString("uri.ontology.om");
    onto_planning_concept =config.getString("uri.ontology.ontoplanningconcept");

  }

  private String getNamespace(String uriString) {
    String[] splitUri = uriString.split("/");
    return String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length - 2));
  }

  private Query getFilterQuery(String predicate, ArrayList<String> onto_class) {

    WhereBuilder wb = new WhereBuilder()
        .addPrefix(ONTOZONING_PREFIX, onto_zoning);
    if (predicate.equals(ALLOWS_USE)) {
      for (String use_class: onto_class) {
        Path allow = PathFactory.pathLink(NodeFactory.createURI(onto_zoning + ALLOWS_USE));
        Path mayAllow = PathFactory.pathLink(NodeFactory.createURI(onto_zoning + MAY_ALLOW_USE));
        Path fullPath = PathFactory.pathAlt(allow, mayAllow);
        wb.addWhere(QM +ZONE, fullPath.toString(), ONTOZONING_PREFIX + ":" + use_class);
      }
    } else if (predicate.equals(ALLOWS_PROGRAMME)) {
      wb.addWhere(QM +ZONE, ONTOZONING_PREFIX + ":" + ALLOWS_USE, QM + USE);
      for (String programme_class : onto_class) {
        Path allow = PathFactory.pathLink(NodeFactory.createURI(onto_zoning + ALLOWS_PROGRAMME));
        Path mayAllow = PathFactory.pathLink(NodeFactory.createURI(onto_zoning + MAY_ALLOW_PROGRAMME));
        Path fullPath = PathFactory.pathAlt(allow, mayAllow);
        wb.addWhere(QM + USE, fullPath.toString(), ONTOZONING_PREFIX + ":" + programme_class);
      }
    }
    wb.addWhere(QM + CITY_OBJECT_ID, ONTOZONING_PREFIX + ":" + HAS_ZONE_PREDICATE, QM +ZONE);

    WhereBuilder w2b = new WhereBuilder()
        .addPrefix(ONTO_PLANCON_PREFIX, onto_planning_concept)
        .addPrefix(OM_PREFIX, om);

    Path buildableSpace = PathFactory.pathLink(NodeFactory.createURI(onto_planning_concept + "hasBuildableSpace"));
    Path allowedGFA = PathFactory.pathLink(NodeFactory.createURI(onto_planning_concept + "hasAllowedGFA"));
    Path measure = PathFactory.pathLink(NodeFactory.createURI(om + "hasValue"));
    Path numeric_value = PathFactory.pathLink(NodeFactory.createURI(om + "hasNumericValue"));
    Path zoning_case = PathFactory.pathLink(NodeFactory.createURI(onto_planning_concept + "forZoningCase"));
    Path fullPath_gfa = PathFactory.pathSeq(buildableSpace, allowedGFA);
    Path fullPath_value = PathFactory.pathSeq(measure, numeric_value);
    Path fullPath_optional = PathFactory.pathSeq(buildableSpace, zoning_case);

    w2b.addWhere(QM+ CITY_OBJECT_ID, fullPath_gfa.toString(), QM + "allowed_GFA_Id");
    w2b.addWhere( QM + "allowed_GFA_Id", fullPath_value.toString(), QM + "allowedGFA_value");

    SelectBuilder sb = new SelectBuilder()
        .addVar(QM + CITY_OBJECT_ID)
        .addVar(QM + "allowedGFA_value")
        .addVar(QM + "ZoningCase")
        .addGraph(NodeFactory.createURI("http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/ontozone/"), wb)
        .addGraph(NodeFactory.createURI("http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/buildablespace/"), w2b)
        .addOptional(QM + CITY_OBJECT_ID, fullPath_optional.toString(), QM + "ZoningCase");
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
    return new JSONArray();
  }

  private Query getGFAQuery(JSONArray citybjects) {
    return new Query();
  }
}