package uk.ac.cam.cares.twa.cities.agents.geo;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
import uk.ac.cam.cares.ogm.models.ModelContext;
import uk.ac.cam.cares.ogm.models.geo.CityObject;
import uk.ac.cam.cares.jps.base.http.Http;
import uk.ac.cam.cares.twa.cities.AccessAgentMapping;


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
  private static final String GFA_VALUE = "GFAvalue";
  private static final String ALLOWED_GFA = "allowedGFA";
  private static final String ZONING_CASE =  "ZoningCase";
  private static final String BUILDABLE_SPACE =  "BuildableSpace";
  private static final String DEFAULT_ZONING_CASE = "default";
  private static final String HAS_VALUE = "hasValue";
  private static final String HAS_NUMERIC_VALUE = "hasNumericValue";
  private static final String HAS_BUILDABLE_SPACE = "hasBuildableSpace";
  private static final String HAS_ALLOWED_GFA = "hasAllowedGFA";
  private static final String FOR_ZONING_CASE = "forZoningCase";
  private static final String TOTAL_GFA =  "TotalGFA";

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
      String route = AccessAgentMapping.getTargetResourceID(cityObjectIri);
      if (route != null) {
        this.route =  route;
      }

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

          String predicate = "";
          if (filters.keySet().contains(ALLOWS_USE)) {
            predicate = ALLOWS_USE;
          } else if (filters.keySet().contains(ALLOWS_PROGRAMME)){
            predicate = ALLOWS_PROGRAMME;
          }

          HashMap<String, Double> gfas = new HashMap<>();
          if (!predicate.equals("")) {
            ArrayList<String> onto_elements = new ArrayList<>(filters.getJSONObject(predicate).keySet());
            for (String onto_element: onto_elements) {
              try {
                gfas.put(onto_element,
                    Double.parseDouble(filters.getJSONObject(predicate).getString(onto_element)));
              }
              catch (NumberFormatException exception) {
                gfas.put(onto_element, 0.);
              }
            }
          }
          double total_gfa = 0.;
          if (filters.keySet().contains(TOTAL_GFA)) {
            try {
              total_gfa = Double.parseDouble(filters.getString(TOTAL_GFA));
            }
            catch (NumberFormatException ignored) {
            }
          }
          response.put("filtered", getFilteredObjects(predicate, gfas, total_gfa));
          requestParams.put(agentURL, response);

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

  private Query getFilterQuery(String predicate, ArrayList<String> onto_class, boolean gfa_case) {

    SelectBuilder sb = new SelectBuilder();
    String ontoZoneGraph = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/ontozone/";
    String buildableSpaceGraph = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/buildablespace/";
    getOntoZoneFilterQuery(predicate, onto_class, sb, ontoZoneGraph);
    if (gfa_case) {
      getGFAFilterQuery(sb, buildableSpaceGraph);
    }
    return sb.build();
  }

  private void getOntoZoneFilterQuery(String predicate, ArrayList<String> onto_class, SelectBuilder sb, String graph) {
    WhereBuilder wb = new WhereBuilder()
        .addPrefix(ONTOZONING_PREFIX, onto_zoning);
    if (predicate.equals(ALLOWS_USE)) {
      for (String use_class: onto_class) {
        Path allow = PathFactory.pathLink(NodeFactory.createURI(onto_zoning + ALLOWS_USE));
        Path mayAllow = PathFactory.pathLink(NodeFactory.createURI(onto_zoning + MAY_ALLOW_USE));
        Path fullPath = PathFactory.pathAlt(allow, mayAllow);
        wb.addWhere(QM +ZONE, fullPath.toString(), onto_zoning + use_class);
      }
    } else if (predicate.equals(ALLOWS_PROGRAMME)) {
      wb.addWhere(QM +ZONE, NodeFactory.createURI(onto_zoning + ALLOWS_USE), QM + USE);
      for (String programme_class : onto_class) {
        Path allow = PathFactory.pathLink(NodeFactory.createURI(onto_zoning + ALLOWS_PROGRAMME));
        Path mayAllow = PathFactory.pathLink(NodeFactory.createURI(onto_zoning + MAY_ALLOW_PROGRAMME));
        Path fullPath = PathFactory.pathAlt(allow, mayAllow);
        wb.addWhere(QM + USE, fullPath.toString(), NodeFactory.createURI(onto_zoning + programme_class));
      }
    }
    wb.addWhere(QM + CITY_OBJECT_ID, NodeFactory.createURI(onto_zoning + HAS_ZONE_PREDICATE), QM +ZONE);

    sb.addVar(QM + CITY_OBJECT_ID)
        .addGraph(NodeFactory.createURI(graph), wb);
  }

  private void getGFAFilterQuery(SelectBuilder sb, String graph) {
    WhereBuilder w2b = new WhereBuilder()
        .addPrefix(ONTO_PLANCON_PREFIX, onto_planning_concept)
        .addPrefix(OM_PREFIX, om);
    Path measure = PathFactory.pathLink(NodeFactory.createURI(om + HAS_VALUE));
    Path numeric_value = PathFactory.pathLink(NodeFactory.createURI(om + HAS_NUMERIC_VALUE));
    Path fullPath_value = PathFactory.pathSeq(measure, numeric_value);
    w2b.addWhere(QM+ CITY_OBJECT_ID, PathFactory.pathLink(NodeFactory.createURI(onto_planning_concept + HAS_BUILDABLE_SPACE)), QM + BUILDABLE_SPACE);
    w2b.addWhere(QM + BUILDABLE_SPACE, PathFactory.pathLink(NodeFactory.createURI(onto_planning_concept + HAS_ALLOWED_GFA)), QM + ALLOWED_GFA);
    w2b.addWhere( QM + ALLOWED_GFA, fullPath_value.toString(), QM + GFA_VALUE);
    w2b.addOptional(QM + BUILDABLE_SPACE, PathFactory.pathLink(NodeFactory.createURI(onto_planning_concept + FOR_ZONING_CASE)), QM + ZONING_CASE);

    sb.addVar(QM + GFA_VALUE)
        .addVar(QM + ZONING_CASE)
        .addGraph(NodeFactory.createURI(graph), w2b);

  }

  private JSONArray getFilteredObjects (String predicate, HashMap<String, Double> gfas, double total_gfa) {
    boolean gfa_case = !gfas.isEmpty() | total_gfa > 0;

    Query query = getFilterQuery(predicate, new ArrayList<>(gfas.keySet()), gfa_case);
    JSONArray filteredCityobjects = new JSONArray();
    JSONArray query_result = AccessAgentCaller.queryStore(route, query.toString());

    if (gfa_case) {
      HashMap<String, HashMap<String, Double>> object_gfa_cases = new HashMap<>();

      for (int i = 0; i < query_result.length(); i++) {
        JSONObject row = (JSONObject) query_result.get(i);
        String cityObjectId = row.getString(CITY_OBJECT_ID);
        Double gfa = row.getDouble(GFA_VALUE);
        String zoning_case = DEFAULT_ZONING_CASE;
        if (row.keySet().contains(ZONING_CASE)) {
          zoning_case = row.getString(ZONING_CASE).split("#")[1];
        }
        if (object_gfa_cases.containsKey(cityObjectId)) {
          object_gfa_cases.get(cityObjectId).put(zoning_case, gfa);
        }
        else {
          HashMap<String, Double> newObjectGfas = new HashMap<>();
          newObjectGfas.put(zoning_case, gfa);
          object_gfa_cases.put(cityObjectId, newObjectGfas);
        }
      }

      for (String cityobject: object_gfa_cases.keySet()){
        double chosen_gfa = gfas.values().stream().mapToDouble(Double::doubleValue).sum();
        if (chosen_gfa == 0) {
          chosen_gfa = total_gfa;
        }
        HashMap<String, Double> current_gfas = object_gfa_cases.get(cityobject);
        if (!gfas.isEmpty()){
          ArrayList<Double> relevant_zoning_case_gfas = new ArrayList<>();
          for (String zoning_case: current_gfas.keySet()) {
            if (gfas.containsKey(zoning_case)) {
              relevant_zoning_case_gfas.add(current_gfas.get(zoning_case));
            }
          }
          if (relevant_zoning_case_gfas.isEmpty()) {
            if (current_gfas.containsKey(DEFAULT_ZONING_CASE)) {
              if (current_gfas.get(DEFAULT_ZONING_CASE) >= chosen_gfa) {
                filteredCityobjects.put(cityobject);
              }
            }
          }
          else {
            if (Collections.min(relevant_zoning_case_gfas) >= chosen_gfa) {
              filteredCityobjects.put(cityobject);
            }
          }
        }
        else{
          if (current_gfas.get(DEFAULT_ZONING_CASE) >= chosen_gfa){
            filteredCityobjects.put(cityobject);
          }
        }

      }
    }
    else {
      for (int i = 0; i < query_result.length(); i++) {
        JSONObject row = (JSONObject) query_result.get(i);
        filteredCityobjects.put(row.get(CITY_OBJECT_ID));
      }
    }
    return filteredCityobjects;
  }
}