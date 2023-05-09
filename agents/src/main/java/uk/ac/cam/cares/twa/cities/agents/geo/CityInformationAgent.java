package uk.ac.cam.cares.twa.cities.agents.geo;

import static java.lang.Boolean.parseBoolean;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
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
import org.apache.jena.graph.Node;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.config.JPSConstants;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.ogm.models.ModelContext;
import uk.ac.cam.cares.jps.base.http.Http;
import uk.ac.cam.cares.twa.cities.AccessAgentMapping;
import uk.ac.cam.cares.twa.cities.model.geo.CityObject;


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
  private static final String ONTOZONING_PREFIX = "zo";
  private static final String CITY_OBJECT_ID = "cityObjectId";
  private static final String ONTO_BUILDABLE_SPACE_PREFIX = "obs";
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
  private static final String MAX_CAP = "max_cap";
  private static final String MIN_CAP = "min_cap";

  @Getter private String route;
  private boolean lazyload;
  @Getter private String zo;
  @Getter private String om;

  @Getter private String obs;


  public CityInformationAgent() {
    super();
    readConfig();
  }

  @Override
  public JSONObject processRequestParameters(JSONObject requestParams) {

    if (validateInput(requestParams)){
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


          if ((agentURL.contains(JPSConstants.ACCESS_AGENT_PATH)) && !(agentKeyValuePairs.keySet().contains(JPSConstants.TARGETIRI))){
            JSONObject filters = requestParams.getJSONObject(KEY_CONTEXT).getJSONObject(agentURL);
            String predicate = "";
            if (filters.keySet().contains(ALLOWS_USE)) {
              predicate = ALLOWS_USE;
            } else if (filters.keySet().contains(ALLOWS_PROGRAMME)){
              predicate = ALLOWS_PROGRAMME;
            }
            boolean max_cap = parseBoolean(filters.get(MAX_CAP).toString());
            boolean min_cap = parseBoolean(filters.get(MIN_CAP).toString());

            HashMap<String, Double> gfas = new HashMap<>();
            if (!predicate.equals("")) {
              ArrayList<String> onto_elements = new ArrayList<>(filters.getJSONObject(predicate).keySet());
              for (String onto_element: onto_elements) {
                try {
                  gfas.put(onto_element, Double.parseDouble(filters.getJSONObject(predicate).getString(onto_element)));
                } catch (NumberFormatException exception) {
                  gfas.put(onto_element, 0.);
                }
              }
            }
            double total_gfa = 0.;
            if (filters.keySet().contains(TOTAL_GFA)) {
              try {
                total_gfa = Double.parseDouble(filters.getString(TOTAL_GFA));
              } catch (NumberFormatException ignored) {
              }
            }
            JSONArray filtered_objs = getFilteredObjects(predicate, gfas, total_gfa, min_cap, max_cap);
            response.put("filtered", filtered_objs);
            response.put("filteredCounts", countFilteredObjects(filtered_objs));
            requestParams.put(agentURL, response);
          }
          else {
            for (String key : agentKeyValuePairs.keySet()) {
              requestBody.put(key, agentKeyValuePairs.get(key));
            }
            String[] params = new String[0];
            HttpPost request =  Http.post(agentURL, requestBody, "application/json","application/json", params);
            try {
              response = new JSONObject(Http.execute(request));
              // specific agent response added to the city information response.
              requestParams.append(agentURL, response);
            } catch (Exception e){
              // ignore if no response from context endpoint
            }
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
    zo = config.getString("uri.ontology.ontozoning");
    om = config.getString("uri.ontology.om");
    obs =config.getString("uri.ontology.ontobuildablespace");
  }

  /*** Method combines both query blocks depending on the user input.
   * @param predicate efines weather uses or programmes to be queried
   * @param onto_class types of programmes or uses
   * @param gfa_case boolean defining weather gfa query block should be generated
   * @return query body
   */
  private Query getFilterQuery(String predicate, ArrayList<String> onto_class, boolean gfa_case) {
    SelectBuilder sb = new SelectBuilder();
    sb.addVar(QM + CITY_OBJECT_ID);
    sb.setDistinct(true);
    String namespaceURl = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql";
    String ontoZoneGraph = namespaceURl + "/ontozone/";
    String buildableSpaceGraph = namespaceURl + "/buildablespace/";
    getOntoZoneFilterQuery(predicate, onto_class, sb, ontoZoneGraph);
    if (gfa_case) {
      getGFAFilterQuery(sb, buildableSpaceGraph);
    }
    return sb.build();
  }

  /*** Method to build a query block to retrieve allowed uses or programmes.
   * @param predicate defines weather uses or programmes to be queried
   * @param onto_class types of programmes or uses
   * @param sb query body
   * @param graph storing zoning values
   */
  private void getOntoZoneFilterQuery(String predicate, ArrayList<String> onto_class, SelectBuilder sb, String graph) {
    Path fullPath_use = PathFactory.pathAlt(PathFactory.pathLink(NodeFactory.createURI(zo + ALLOWS_USE)),
        PathFactory.pathLink(NodeFactory.createURI(zo + MAY_ALLOW_USE)));
    Path fullPath_programme = PathFactory.pathAlt(PathFactory.pathLink(NodeFactory.createURI(zo + ALLOWS_PROGRAMME)),
        PathFactory.pathLink(NodeFactory.createURI(zo + MAY_ALLOW_PROGRAMME)));
    WhereBuilder wb = new WhereBuilder()
        .addPrefix(ONTOZONING_PREFIX, zo)
        .addWhere(QM + CITY_OBJECT_ID, NodeFactory.createURI(zo + HAS_ZONE_PREDICATE), QM +ZONE);
    if (predicate.equals(ALLOWS_USE)) {
      for (String use_class: onto_class) {
        wb.addWhere(QM +ZONE, fullPath_use.toString(), NodeFactory.createURI(zo + use_class));
      }
      sb.addGraph(NodeFactory.createURI(graph), wb);
    } else if (predicate.equals(ALLOWS_PROGRAMME)) {
      for (String programme_class : onto_class) {
        Node blank_node = NodeFactory.createBlankNode();
        wb.addWhere(QM +ZONE, fullPath_use.toString(), blank_node);
        wb.addWhere(blank_node, fullPath_programme.toString(), NodeFactory.createURI(zo + programme_class));
      }
      sb.addGraph(NodeFactory.createURI(graph), wb);
    }
  }

  /*** Method to build a query block to retrieve gfa values.
   * @param sb query body
   * @param graph storing the gfa values
   */
  private void getGFAFilterQuery(SelectBuilder sb, String graph) {
    Path fullPath_value = PathFactory.pathSeq(PathFactory.pathLink(NodeFactory.createURI(om + HAS_VALUE)),
        PathFactory.pathLink(NodeFactory.createURI(om + HAS_NUMERIC_VALUE)));
    WhereBuilder w2b = new WhereBuilder()
        .addPrefix(ONTO_BUILDABLE_SPACE_PREFIX, obs)
        .addPrefix(OM_PREFIX, om);
    w2b.addWhere(QM+ CITY_OBJECT_ID, PathFactory.pathLink(NodeFactory.createURI(obs + HAS_BUILDABLE_SPACE)), QM + BUILDABLE_SPACE);
    w2b.addWhere(QM + BUILDABLE_SPACE, PathFactory.pathLink(NodeFactory.createURI(obs + HAS_ALLOWED_GFA)), QM + ALLOWED_GFA);
    w2b.addWhere( QM + ALLOWED_GFA, fullPath_value.toString(), QM + GFA_VALUE);
    w2b.addOptional(QM + BUILDABLE_SPACE, PathFactory.pathLink(NodeFactory.createURI(obs + FOR_ZONING_CASE)), QM + ZONING_CASE);
    sb.addVar(QM + GFA_VALUE)
        .addVar(QM + ZONING_CASE)
        .addGraph(NodeFactory.createURI(graph), w2b);
  }

  /***
   * Method retrieves city objects that fits user input: programmes, uses and GFAs.
   * @param predicate defines weather uses or programmes to be queried
   * @param inputZoneCaseGFAValues defines what use or programme GFAs values have been input
   * @param totalGFAInputValue defines what totalGFA value has been input by the user
   * @param minCap boolean input determining if cityObjects with 10 smallest applicable GFA values should be returned.
   * @param maxCap boolean input determining if cityObjects with 10 largest applicable GFA values should be returned.
   * @return list of city object ids that pass use, programme and GFA filters.
   */
  private JSONArray getFilteredObjects (String predicate, HashMap<String, Double> inputZoneCaseGFAValues, double totalGFAInputValue, boolean minCap, boolean maxCap) {
    boolean gfaCase = inputZoneCaseGFAValues.values().stream().mapToDouble(Double::doubleValue).sum() > 0 | totalGFAInputValue > 0 | minCap | maxCap;
    Query query = getFilterQuery(predicate, new ArrayList<>(inputZoneCaseGFAValues.keySet()), gfaCase);
    JSONArray returnedCityObjects =  new JSONArray();
    JSONArray queryResult = AccessAgentCaller.queryStore(route, query.toString());
    if (gfaCase) {
      applyFiltersGFA(queryResult, totalGFAInputValue, inputZoneCaseGFAValues, returnedCityObjects, minCap, maxCap);
    }
    else {
      applyFiltersOnlyZoneCase(queryResult, returnedCityObjects);
    }
    return returnedCityObjects;
  }

  /***
   * method applies GFA filters on query results if GFA input has been provided to the CIA.
   * @param queryResult returns information from the knowledge graph with city objects that allow combinations of uses/programmes and their related GFAs (if applicable).
   * @param totalGFAInputValue user input value for "Total GFA" UI element.
   * @param inputZoneCaseGFAValues a hashmap containing programme/use names and respective user GFA inputs.
   * @param returnedCityObjects JSONArray of city objects that passed all filters and are returned by the CIA.
   */
  private void applyFiltersGFA(JSONArray queryResult, double totalGFAInputValue, HashMap<String, Double> inputZoneCaseGFAValues, JSONArray returnedCityObjects, boolean minCap, boolean maxCap){
    HashMap<String, Double> filteredCityObjects = new HashMap<>();
    HashMap<String, HashMap<String, Double>> cityObjectsGFAValues = orderGFAResults(queryResult);
    boolean totalGFA = totalGFAInputValue > 0;
    boolean zoneCaseGFA = !inputZoneCaseGFAValues.isEmpty();
    for (String cityObject: cityObjectsGFAValues.keySet()){
      HashMap<String, Double> currentCityObjectGFAValues = cityObjectsGFAValues.get(cityObject);
      if (zoneCaseGFA) {
        applyFiltersGFAWithZoneCase(cityObject, totalGFA, totalGFAInputValue, currentCityObjectGFAValues, inputZoneCaseGFAValues, filteredCityObjects);
      }
      else {
        if (totalGFA) {
          applyFiltersOnlyTotalGFA(cityObject, currentCityObjectGFAValues, totalGFAInputValue, filteredCityObjects);
        }
      }
    }
    applyCapToFilteredResults(filteredCityObjects, returnedCityObjects, minCap, maxCap);
  }

  /***
   * Method filters city objects based on "TotalGFA" input, individual programe/use-specific GFAs or default cityObject GFA.
   * @param cityObject object whose id is appended to returnedCityObject if all filtered passed.
   * @param totalGFA boolean determining if totalGFa input has been provided.
   * @param inputTotalGFAValue provided input value determining the total amount of GFA to filter by.
   * @param currentCityObjectGFA a hashmap storing programme/use-specific and default GFA values linked to the cityObject.
   * @param inputZoneCaseGFAValues a hashmap containing programme/use names and respective user GFA inputs.
   * @param filteredCityObjects hashmap of filtered city object IDs and allowed GFA values. Hashmap is needed if capping results to min or max 10 is applicable.
   */
  private void applyFiltersGFAWithZoneCase(String cityObject, boolean totalGFA, double inputTotalGFAValue, HashMap<String, Double> currentCityObjectGFA, HashMap<String, Double> inputZoneCaseGFAValues, HashMap<String, Double>  filteredCityObjects) {
    ArrayList<Double> cityObjectZoneCaseGFAValues = returnRelevantGFAs(currentCityObjectGFA, inputZoneCaseGFAValues);
    boolean defaultGFA = cityObjectZoneCaseGFAValues.isEmpty();
    double sumOfInputZoneCaseGFAValues = inputZoneCaseGFAValues.values().stream().mapToDouble(Double::doubleValue).sum();
    double cityObjectDefaultGFAValue = 0.;
    if (currentCityObjectGFA.containsKey(DEFAULT_ZONING_CASE)) {
      cityObjectDefaultGFAValue = currentCityObjectGFA.get(DEFAULT_ZONING_CASE);
    }
    if (totalGFA){
      applyFiltersGFAWithZoneCaseWithTotalGFA(cityObject, defaultGFA, inputTotalGFAValue, cityObjectZoneCaseGFAValues,
          sumOfInputZoneCaseGFAValues, cityObjectDefaultGFAValue, filteredCityObjects);
    }
    else {
      applyFiltersGFAWithZoneCaseWithoutTotalGFA(cityObject, defaultGFA, cityObjectZoneCaseGFAValues,
          sumOfInputZoneCaseGFAValues, cityObjectDefaultGFAValue, filteredCityObjects);
    }
  }

  /***
   * Method filters city objects when  input for programmes/uses with specific GFAs and totalGFa is provided.
   * @param cityObject object whose id is appended to returnedCityObject if all filtered passed.
   * @param defaultGFA boolean determining if city object has only one GFA values or programme/use-specific exceptions.
   * @param totalGFAValue provided input value determining the total amount of GFA to filter by.
   * @param cityObjectZoneCaseGFAValues cityObject GFA values for specific programmes/uses (exceptions).
   * @param sumOfInputZoneCaseGFAValues sum of user GFA inputs for chosen use/programme combinations.
   * @param cityObjectDefaultGFAValue GFA value linked to the cityObject when input programme/use is not a GFA exception.
   * @param filteredCityObjects hashmap of filtered city object IDs and allowed GFA values. Hashmap is needed if capping results to min or max 10 is applicable.
   */
  private void applyFiltersGFAWithZoneCaseWithTotalGFA(String cityObject, boolean defaultGFA, double totalGFAValue, ArrayList<Double> cityObjectZoneCaseGFAValues, double sumOfInputZoneCaseGFAValues, double cityObjectDefaultGFAValue, HashMap<String, Double> filteredCityObjects) {
    boolean zoneCaseGFA = sumOfInputZoneCaseGFAValues > 0;
    if (defaultGFA) {
      if (cityObjectDefaultGFAValue > totalGFAValue) {
        filteredCityObjects.put(cityObject, cityObjectDefaultGFAValue);
      }
    }
    else {
      if (zoneCaseGFA) {
        if ((Collections.min(cityObjectZoneCaseGFAValues) > sumOfInputZoneCaseGFAValues) &
            (totalGFAValue < Collections.max(cityObjectZoneCaseGFAValues))){
          filteredCityObjects.put(cityObject, Collections.max(cityObjectZoneCaseGFAValues));
        }
      }
      else {
        if (cityObjectZoneCaseGFAValues.stream().mapToDouble(Double::doubleValue).sum() > totalGFAValue) {
          filteredCityObjects.put(cityObject, cityObjectZoneCaseGFAValues.stream().mapToDouble(Double::doubleValue).sum());
        }
      }
    }
  }


  /***
   * Method filters city objects when user inputted programmes/uses have specific GFAs.
   * @param cityObject object whose id is appended to returnedCityObject if all filtered passed.
   * @param defaultGFA boolean determining if city object has only one GFA values or programme/use-specific exceptions.
   * @param cityObjectZoneCaseGFAValues cityObject GFA values for specific programmes/uses (exceptions).
   * @param sumOfInputZoneCaseGFAValues sum of user GFA inputs for chosen use/programme combinations.
   * @param cityObjectDefaultGFAValue GFA value linked to the cityObject when input programme/use is not a GFA exception.
   * @param filteredCityObjects hashmap of filtered city object IDs and allowed GFA values. Hashmap is needed if capping results to min or max 10 is applicable.
   */
  private void applyFiltersGFAWithZoneCaseWithoutTotalGFA(String cityObject, boolean defaultGFA, ArrayList<Double> cityObjectZoneCaseGFAValues, double sumOfInputZoneCaseGFAValues, double cityObjectDefaultGFAValue, HashMap<String, Double> filteredCityObjects) {
    if (!defaultGFA) {
      if (Collections.min(cityObjectZoneCaseGFAValues) > sumOfInputZoneCaseGFAValues) {
        filteredCityObjects.put(cityObject, Collections.min(cityObjectZoneCaseGFAValues));
      }
    }
    else {
      if (cityObjectDefaultGFAValue > sumOfInputZoneCaseGFAValues) {
        filteredCityObjects.put(cityObject, cityObjectDefaultGFAValue);
      }
    }
  }

  /***
   * Method called when provided input only contains totalGFA value without specifying particular programmes.
   * @param cityObject object whose id is appended to returnedCityObject if all filtered passed.
   * @param currentCityObjectGFA a hashmap storing programme/use-specific and default GFA values linked to the cityObject.
   * @param totalGFAValue totalGFAValue provided input value determining the total amount of GFA to filter by.
   * @param filteredCityObjects hashmap of filtered city object IDs and allowed GFA values. Hashmap is needed if capping results to min or max 10 is applicable.
   */
  private void applyFiltersOnlyTotalGFA(String cityObject, HashMap<String, Double> currentCityObjectGFA,
      double totalGFAValue, HashMap<String, Double> filteredCityObjects) {
    if (Collections.max(currentCityObjectGFA.values()) > totalGFAValue) {
      filteredCityObjects.put(cityObject, Collections.max(currentCityObjectGFA.values()));
    }
  }

  /***
   * Method is called when input for min_cap or max_cap is true. It filters cityObjects based on applicable GFA value (10 largest or smallest).
   * @param filteredCityObjects hashmap of filtered city object IDs and allowed GFA values. Hashmap is needed if capping results to min or max 10 is applicable.
   * @param returnedCityObjects list of cityObjects that passed all filters.
   * @param max_cap boolean input determining if cityObjects with 10 largest applicable GFA values should be returned.
   * @param min_cap boolean input determining if cityObjects with 10 smallest applicable GFA values should be returned.
   */
  private void applyCapToFilteredResults(HashMap<String, Double> filteredCityObjects, JSONArray returnedCityObjects, boolean min_cap, boolean max_cap) {
    if (max_cap || min_cap){
      List<Entry<String, Double>> filteredCityObjectsList = new LinkedList<>(filteredCityObjects.entrySet());
      filteredCityObjectsList.sort((o1, o2) -> {
        if (min_cap) {
          return o1.getValue().compareTo(o2.getValue());
        }
        else {
          return o2.getValue().compareTo(o1.getValue());

        }
      });
      for (int i = 0; i < filteredCityObjectsList.size() && i < 10; i++) {
        returnedCityObjects.put(filteredCityObjectsList.get(i).getKey());
      }
    }
    else {
      for (Object filteredCityObject: filteredCityObjects.keySet()) {
        returnedCityObjects.put(filteredCityObject);
      }
    }
  }

  /***
   * Method is called when only programme/use input is passed to CIA. Does not apply GFA filtering.
   * @param query_result returns information from the knowledge graph with city objects that allow combinations of uses/programmes and their related GFAs (if applicable).
   * @param returnedCityObjects list of cityObjects that passed all filters.
   */
  private void applyFiltersOnlyZoneCase(JSONArray query_result, JSONArray returnedCityObjects) {
    for (int i = 0; i < query_result.length(); i++) {
      JSONObject row = (JSONObject) query_result.get(i);
      returnedCityObjects.put(row.get(CITY_OBJECT_ID));
    }
  }

  /*** Method preselects the relevant GFAs for further comparison based on zoning cases.
   * @param cityObjectGFAs GFAs linked to a partocular cityObject.
   * @param inputGFAs GFAs provided by the user.
   * @return selection of relevant GFAs for the partcular city object.
   */
  private ArrayList<Double> returnRelevantGFAs(HashMap<String, Double> cityObjectGFAs, HashMap<String, Double> inputGFAs){
    ArrayList<Double> relevantGFAs = new ArrayList<>();
    for (String zoningCase: cityObjectGFAs.keySet()) {
      if (inputGFAs.containsKey(zoningCase)) {
        relevantGFAs.add(cityObjectGFAs.get(zoningCase));
      }
    }
    return relevantGFAs;
  }

  /*** Method extracts from query results gfa values and constructs a hashmap with city object and all gfas linked to it.
   * @param gfaQueryResult returned results with gfa values.
   * @return a hashmap with structured query results.
   */
  private HashMap<String, HashMap<String, Double>> orderGFAResults(JSONArray gfaQueryResult){
    HashMap<String, HashMap<String, Double>> orderedGFACases = new HashMap<>();
    for (int i = 0; i < gfaQueryResult.length(); i++) {
      JSONObject row = (JSONObject) gfaQueryResult.get(i);
      String cityObjectId = row.getString(CITY_OBJECT_ID);
      Double gfa = row.getDouble(GFA_VALUE);
      String zoneCase = DEFAULT_ZONING_CASE;
      if (row.keySet().contains(ZONING_CASE)) {
        zoneCase = row.getString(ZONING_CASE).split("#")[1];
      }
      if (orderedGFACases.containsKey(cityObjectId)) {
        orderedGFACases.get(cityObjectId).put(zoneCase, gfa);
      }
      else {
        HashMap<String, Double> newObjectGfa = new HashMap<>();
        newObjectGfa .put(zoneCase, gfa);
        orderedGFACases.put(cityObjectId, newObjectGfa );
      }
    }
    return orderedGFACases;
  }

  private int countFilteredObjects(JSONArray filteredObjects){
    return filteredObjects.length();
  }

}