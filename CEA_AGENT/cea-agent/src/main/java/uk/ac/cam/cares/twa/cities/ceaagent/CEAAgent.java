package uk.ac.cam.cares.twa.cities.ceaagent;

import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.jooq.exception.DataAccessException;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.timeseries.TimeSeries;
import uk.ac.cam.cares.jps.base.timeseries.TimeSeriesClient;
import uk.ac.cam.cares.twa.cities.tasks.*;
import org.json.JSONObject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.*;
import javax.servlet.annotation.WebServlet;
import java.util.concurrent.*;
import java.net.URISyntaxException;
import java.util.stream.Stream;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.json.JSONArray;

@WebServlet(
        urlPatterns = {
                CEAAgent.URI_ACTION,
                CEAAgent.URI_UPDATE,
                CEAAgent.URI_QUERY
        })
public class CEAAgent extends JPSAgent {
    public static final String KEY_REQ_METHOD = "method";
    public static final String URI_ACTION = "/cea/run";
    public static final String URI_UPDATE = "/cea/update";
    public static final String URI_QUERY = "/cea/query";
    public static final String KEY_REQ_URL = "requestUrl";
    public static final String KEY_TARGET_URL = "targetUrl";
    public static final String KEY_IRI = "iris";
    public static final String CITY_OBJECT = "cityobject";
    public static final String CITY_OBJECT_GEN_ATT = "cityobjectgenericattrib";
    public static final String BUILDING = "building";
    public static final String SURFACE_GEOMETRY = "surfacegeometry";
    public static final String ENERGY_PROFILE = "energyprofile";
    public static final String DATABASE_SRS = "databasesrs";
    public static final String KEY_GRID_DEMAND = "grid_demand";
    public static final String KEY_ELECTRICITY_DEMAND = "electricity_demand";
    public static final String KEY_HEATING_DEMAND = "heating_demand";
    public static final String KEY_COOLING_DEMAND = "cooling_demand";
    public static final String KEY_PV_ROOF_AREA = "PV_area_roof";
    public static final String KEY_PV_WALL_NORTH_AREA = "PV_area_wall_north";
    public static final String KEY_PV_WALL_SOUTH_AREA = "PV_area_wall_south";
    public static final String KEY_PV_WALL_EAST_AREA = "PV_area_wall_east";
    public static final String KEY_PV_WALL_WEST_AREA = "PV_area_wall_west";
    public static final String KEY_PV_ROOF_SUPPLY= "PV_supply_roof";
    public static final String KEY_PV_WALL_NORTH_SUPPLY= "PV_supply_wall_north";
    public static final String KEY_PV_WALL_SOUTH_SUPPLY= "PV_supply_wall_south";
    public static final String KEY_PV_WALL_EAST_SUPPLY= "PV_supply_wall_east";
    public static final String KEY_PV_WALL_WEST_SUPPLY= "PV_supply_wall_west";
    public static final String KEY_TIMES= "times";

    public List<String> TIME_SERIES = Arrays.asList(KEY_GRID_DEMAND,KEY_ELECTRICITY_DEMAND,KEY_HEATING_DEMAND,KEY_COOLING_DEMAND, KEY_PV_ROOF_SUPPLY,KEY_PV_WALL_SOUTH_SUPPLY, KEY_PV_WALL_NORTH_SUPPLY,KEY_PV_WALL_EAST_SUPPLY, KEY_PV_WALL_WEST_SUPPLY);
    public List<String> SCALARS = Arrays.asList(KEY_PV_ROOF_AREA,KEY_PV_WALL_NORTH_AREA,KEY_PV_WALL_SOUTH_AREA,KEY_PV_WALL_EAST_AREA, KEY_PV_WALL_WEST_AREA);

    public final int NUM_CEA_THREADS = 1;
    private final ThreadPoolExecutor CEAExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_CEA_THREADS);

    private static final String TIME_SERIES_CLIENT_PROPS = "timeseriesclient.properties";
    private TimeSeriesClient<OffsetDateTime> tsClient;
    public static final String timeUnit = OffsetDateTime.class.getSimpleName();
    private static final String FS = System.getProperty("file.separator");

    // Variables fetched from CEAAgentConfig.properties file.
    private String ocgmlUri;
    private String ontoUBEMMPUri;
    private String rdfUri;
    private String owlUri;
    private String purlEnaeqUri;
    private String purlInfrastructureUri;
    private String timeSeriesUri;
    private String thinkhomeUri;
    private static String unitOntologyUri;
    private String requestUrl;
    private String targetUrl;
    private String localRoute;

    private Map<String, String> accessAgentRoutes = new HashMap<>();

    public CEAAgent() {
        readConfig();
    }

    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {
        if (validateInput(requestParams)) {
            requestUrl = requestParams.getString(KEY_REQ_URL);
            String uriArrayString = requestParams.get(KEY_IRI).toString();
            JSONArray uriArray = new JSONArray(uriArrayString);

            if (requestUrl.contains(URI_UPDATE) || requestUrl.contains(URI_ACTION)) {
                targetUrl = requestParams.getString(KEY_TARGET_URL);

                if (requestUrl.contains(URI_UPDATE)) {
                    // parse times
                    List<OffsetDateTime> times = getTimesList(requestParams, KEY_TIMES);

                    // parse times series data;
                    List<List<List<?>>> timeSeries = new ArrayList<>();
                    for (int i = 0; i < uriArray.length(); i++) {
                        List<List<?>> iriList = new ArrayList<>();
                        for(String ts: TIME_SERIES) {
                            iriList.add(getTimeSeriesList(requestParams, ts, i));
                        }
                        timeSeries.add(iriList);
                    }

                    LinkedHashMap<String, List<String>> scalars = new LinkedHashMap<>();

                    // parse PV area data
                    for(String scalar: SCALARS){
                        scalars.put(scalar, getList(requestParams, scalar));
                    }

                    List<LinkedHashMap<String,String>> fixedIris = new ArrayList<>();
                    String route = new String();
                    for (int i = 0; i < uriArray.length(); i++) {
                        String uri = uriArray.getString(i);
                        // Only set routes once - assuming all iris passed have same namespace
                        // Will not be necessary if namespace is passed in request params
                        if(i==0) {
                            route = localRoute.isEmpty() ? getRoute(uri) : localRoute;
                            setTimeSeriesClientProperties(getNamespace(uri));
                        }
                        String buildingUri = checkBlazegraphAndTimeSeriesInitialised(uri, fixedIris, route);
                        if (buildingUri=="") {
                            createTimeSeries(uri,fixedIris);
                            buildingUri = sparqlUpdate(scalars, fixedIris.get(i), uri, i, route);
                            sparqlGenAttributeUpdate(uri, buildingUri, route);
                        }
                        addDataToTimeSeries(timeSeries.get(i), times, fixedIris.get(i));
                    }
                } else if (requestUrl.contains(URI_ACTION)) {
                    ArrayList<CEAInputData> testData = new ArrayList<>();
                    ArrayList<String> uriStringArray = new ArrayList<>();
                    String crs= new String();
                    String route = new String();

                    for (int i = 0; i < uriArray.length(); i++) {
                        String uri = uriArray.getString(i);
                        // Only set route once - assuming all iris passed in same namespace
                        // Will not be necessary if namespace is passed in request params
                        if(i==0) route = localRoute.isEmpty() ? getRoute(uri) : localRoute;
                        uriStringArray.add(uri);
                        //Set default value if height can not be obtained from knowledge graph
                        String height = getValue(uri, "Height", route);
                        height = height.length() == 0 ? "10.0" : height;
                        testData.add(new CEAInputData(getValue(uri, "Footprint", route), height));
                        if(i==0) crs = getValue(uri, "CRS", route); //just get crs once - assuming all iris in same namespace
                    }
                    // Manually set thread number to 0 - multiple threads not working so needs investigating
                    // Potentially issue is CEA is already multi-threaded
                    runCEA(testData, uriStringArray, 0, crs);
                }
            } else if (requestUrl.contains(URI_QUERY)) {
                String route = new String();
                for (int i = 0; i < uriArray.length(); i++) {
                    String uri = uriArray.getString(i);
                    // Only set route once - assuming all iris passed in same namespace
                    if(i==0) {
                        route = localRoute.isEmpty() ? getRoute(uri) : localRoute;
                        setTimeSeriesClientProperties(getNamespace(uri));
                    }
                    JSONObject data = new JSONObject();
                    List<String> allMeasures = new ArrayList<>();
                    Stream.of(TIME_SERIES, SCALARS).forEach(allMeasures::addAll);
                    for (String measurement: allMeasures) {
                        ArrayList<String> result = getDataIRI(uri, measurement, route);
                        if (!result.isEmpty()) {
                            String value;
                            if (TIME_SERIES.contains(measurement)) {
                                value = calculateAnnual(retrieveData(result.get(0)), result.get(0));
                                measurement = "Annual "+ measurement;
                            } else {
                                value = result.get(0);
                            }
                            if(!(value.equals("0") || value.equals("0.0"))){
                                value += " " + getUnit(result.get(1));
                                data.put(measurement, value);
                            }
                        }
                    }
                    requestParams.append(ENERGY_PROFILE, data);
                }
            }

        }
        return requestParams;
    }

    @Override
    public boolean validateInput(JSONObject requestParams) throws BadRequestException {
        boolean error = true;

        if (!requestParams.isEmpty()) {
            Set<String> keys = requestParams.keySet();
            if (keys.contains(KEY_REQ_METHOD) && keys.contains(KEY_REQ_URL) && keys.contains(KEY_IRI)) {
                if (requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.POST)) {
                    try {
                        URL reqUrl = new URL(requestParams.getString(KEY_REQ_URL));
                        if (reqUrl.getPath().contains(URI_UPDATE)) {
                            error = validateUpdateInput(requestParams);
                        } else if (reqUrl.getPath().contains(URI_ACTION)) {
                            error = validateActionInput(requestParams);
                        } else if (reqUrl.getPath().contains(URI_QUERY)) {
                            error = validateQueryInput(requestParams);
                        }
                    } catch (Exception e) {
                        throw new BadRequestException();
                    }
                }
            }
        }

        if (error) {
            throw new BadRequestException();
        }

        return true;
    }

    /**
     * Parses input JSONObject into a list of strings
     *
     * @param requestParams - request body in JSON format
     * @param key - requested data
     * @return List of data
     */
    private List<String> getList (JSONObject requestParams, String key) {
        JSONArray array = (JSONArray) requestParams.get(key);
        List<String> list = new ArrayList<>();
        for (int j = 0; j < array.length(); j++) {
            list.add(array.getString(j));
        }
        return list;
    }

    /**
     * Parses input JSONObject into a list of time series data
     *
     * @param requestParams - request body in JSON format
     * @param key - requested data
     * @return List of data
     */
    private List<Double> getTimeSeriesList (JSONObject requestParams, String key, Integer index) {
        List<Double> timeSeriesList = new ArrayList<>();

        if (requestParams.has(key)) {
            JSONArray array = (JSONArray) requestParams.get(key);
            JSONArray timeDataArray = (JSONArray) array.get(index);

            for (int i = 0; i < timeDataArray.length(); i++) {
                timeSeriesList.add(Double.valueOf(timeDataArray.get(i).toString()));
            }
        }
        return timeSeriesList;
    }

    /**
     * Parses input JSONObject into a list of times
     *
     * @param requestParams - request body in JSON format
     * @param key - requested data
     * @return List of times
     */
    private List<OffsetDateTime> getTimesList (JSONObject requestParams, String key) {
        JSONArray array = (JSONArray) requestParams.get(key);
        List<OffsetDateTime> list = new ArrayList<>();
        for (int j = 0; j < array.length(); j++) {
            OffsetDateTime odt = OffsetDateTime.parse(array.getString(j));
            list.add(odt);
        }
        return list;
    }

    /**
     * Validates input specific to requests coming to URI_UPDATE
     *
     * @param requestParams - request body in JSON format
     * @return boolean saying if request is valid or not
     */
    private boolean validateUpdateInput(JSONObject requestParams) {
        boolean error = requestParams.get(KEY_IRI).toString().isEmpty() ||
                requestParams.get(KEY_TARGET_URL).toString().isEmpty() ||
                requestParams.get(KEY_GRID_DEMAND).toString().isEmpty() ||
                requestParams.get(KEY_ELECTRICITY_DEMAND).toString().isEmpty() ||
                requestParams.get(KEY_HEATING_DEMAND).toString().isEmpty() ||
                requestParams.get(KEY_COOLING_DEMAND).toString().isEmpty() ||
                requestParams.get(KEY_PV_ROOF_AREA).toString().isEmpty() ||
                requestParams.get(KEY_PV_ROOF_SUPPLY).toString().isEmpty() ||
                requestParams.get(KEY_PV_WALL_SOUTH_AREA).toString().isEmpty() ||
                requestParams.get(KEY_PV_WALL_SOUTH_SUPPLY).toString().isEmpty() ||
                requestParams.get(KEY_PV_WALL_NORTH_AREA).toString().isEmpty() ||
                requestParams.get(KEY_PV_WALL_NORTH_SUPPLY).toString().isEmpty() ||
                requestParams.get(KEY_PV_WALL_EAST_AREA).toString().isEmpty() ||
                requestParams.get(KEY_PV_WALL_EAST_SUPPLY).toString().isEmpty() ||
                requestParams.get(KEY_PV_WALL_WEST_AREA).toString().isEmpty() ||
                requestParams.get(KEY_PV_WALL_WEST_SUPPLY).toString().isEmpty() ||
                requestParams.get(KEY_TIMES).toString().isEmpty();
        return error;
    }

    /**
     * Validates input specific to requests coming to URI_ACTION
     *
     * @param requestParams - request body in JSON format
     * @return boolean saying if request is valid or not
     */
    private boolean validateActionInput(JSONObject requestParams) {
        boolean error = requestParams.get(KEY_TARGET_URL).toString().isEmpty() || requestParams.get(KEY_IRI).toString().isEmpty();
        return error;
    }

    /**
     * Validates input specific to requests coming to URI_QUERY
     *
     * @param requestParams - request body in JSON format
     * @return boolean saying if request is valid or not
     */
    private boolean validateQueryInput(JSONObject requestParams) {
        boolean error = requestParams.get(KEY_IRI).toString().isEmpty();
        return error;
    }


    /**
     * Gets variables from config
     *
     */
    private void readConfig() {
        ResourceBundle config = ResourceBundle.getBundle("CEAAgentConfig");
        ocgmlUri = config.getString("uri.ontology.ontocitygml");
        unitOntologyUri = config.getString("uri.ontology.om");
        ontoUBEMMPUri = config.getString("uri.ontology.ontoubemmp");
        rdfUri = config.getString("uri.ontology.rdf");
        owlUri = config.getString("uri.ontology.owl");
        purlEnaeqUri=config.getString("uri.ontology.purl.enaeq");
        thinkhomeUri=config.getString("uri.ontology.thinkhome");
        purlInfrastructureUri=config.getString("uri.ontology.purl.infrastructure");
        timeSeriesUri=config.getString("uri.ts");
        accessAgentRoutes.put("http://www.theworldavatar.com:83/citieskg/namespace/berlin/sparql/", config.getString("berlin.targetresourceid"));
        accessAgentRoutes.put("http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG24500/sparql/", config.getString("singaporeEPSG24500.targetresourceid"));
        accessAgentRoutes.put("http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/", config.getString("singaporeEPSG4326.targetresourceid"));
        accessAgentRoutes.put("http://www.theworldavatar.com:83/citieskg/namespace/kingslynnEPSG3857/sparql/", config.getString("kingslynnEPSG3857.targetresourceid"));
        accessAgentRoutes.put("http://www.theworldavatar.com:83/citieskg/namespace/kingslynnEPSG27700/sparql/", config.getString("kingslynnEPSG27700.targetresourceid"));
        localRoute = config.getString("uri.route.local");
    }

    /**
     * runs CEATask on CEAInputData and returns CEAOutputData
     *
     * @param buildingData input data on building footprint and height
     * @param uris list of input uris
     * @param threadNumber int tracking thread that is running
     */
    private void runCEA(ArrayList<CEAInputData> buildingData, ArrayList<String> uris, Integer threadNumber, String crs) {
        try {
            RunCEATask task = new RunCEATask(buildingData, new URI(targetUrl), uris, threadNumber, crs);
            CEAExecutor.execute(task);
        }
        catch(URISyntaxException e){
            e.printStackTrace();
            throw new JPSRuntimeException(e);
        }
    }
    /**
     * Creates and intialises a time series using the time series client
     *
     * @param uriString input city object id
     * @param fixedIris list of maps containing time series iris mapped to measurement type
     */
    private void createTimeSeries(String uriString, List<LinkedHashMap<String,String>> fixedIris ) {
        try{
            String timeseries_props;
            if(System.getProperty("os.name").toLowerCase().contains("win")){
                timeseries_props = new File(
                        Objects.requireNonNull(getClass().getClassLoader().getResource(TIME_SERIES_CLIENT_PROPS)).toURI()).getAbsolutePath();
            }
            else{
                timeseries_props = FS+"target"+FS+"classes"+FS+TIME_SERIES_CLIENT_PROPS;
            }
            tsClient =  new TimeSeriesClient<>(OffsetDateTime.class, timeseries_props);

            // Create a iri for each measurement
            List<String> iris = new ArrayList<>();
            LinkedHashMap<String, String> iriMapping = new LinkedHashMap<>();
            for(String measurement: TIME_SERIES){
                String iri = getGraph(uriString,ENERGY_PROFILE)+measurement+"_"+UUID.randomUUID();
                iris.add(iri);
                iriMapping.put(measurement, iri);
            }
            fixedIris.add(iriMapping);

            // Check whether IRIs have a time series linked and if not initialize the corresponding time series
            if(!timeSeriesExist(iris)) {
                // All values are doubles
                List<Class<?>> classes =  new ArrayList<>();
                for(int i=0; i<iris.size(); i++){
                    classes.add(Double.class);
                }
                // Initialize the time series
                tsClient.initTimeSeries(iris, classes, timeUnit);
                //LOGGER.info(String.format("Initialized time series with the following IRIs: %s", String.join(", ", iris)));
            }


        } catch (URISyntaxException | IOException e)
        {
            e.printStackTrace();
            throw new JPSRuntimeException(e);
        }


    }

    /**
     *  Adds new data to time series
     *
     * @param values output CEA data
     * @param times times for output time series data
     * @param iriMap iri map containing time series iris
     */
    private void addDataToTimeSeries(List<List<?>> values, List<OffsetDateTime> times, LinkedHashMap<String,String> iriMap) {
        List<String> iris = new ArrayList<>();
        for (String iri : iriMap.values()){
            iris.add(iri);
        }
        // If CreateTimeSeries has not been run, get time series client
        if(tsClient==null){
            try{
                String timeseries_props;
                if(System.getProperty("os.name").toLowerCase().contains("win")){
                    timeseries_props = new File(
                            Objects.requireNonNull(getClass().getClassLoader().getResource(TIME_SERIES_CLIENT_PROPS)).toURI()).getAbsolutePath();
                }
                else{
                    timeseries_props = FS+"target"+FS+"classes"+FS+TIME_SERIES_CLIENT_PROPS;
                }
                tsClient =  new TimeSeriesClient<>(OffsetDateTime.class, timeseries_props);
            } catch (URISyntaxException | IOException e)
            {
                e.printStackTrace();
                throw new JPSRuntimeException(e);
            }
        }
        TimeSeries<OffsetDateTime> currentTimeSeries = new TimeSeries<>(times, iris, values);
        OffsetDateTime endDataTime = tsClient.getMaxTime(currentTimeSeries.getDataIRIs().get(0));
        OffsetDateTime beginDataTime = tsClient.getMinTime(currentTimeSeries.getDataIRIs().get(0));

        // Delete old data if exists
        if (endDataTime != null) {
            for(Integer i=0; i<currentTimeSeries.getDataIRIs().size(); i++){
                tsClient.deleteTimeSeriesHistory(currentTimeSeries.getDataIRIs().get(i), beginDataTime, endDataTime);
            }
        }
        // Add New data
        tsClient.addTimeSeriesData(currentTimeSeries);
    }

    /**
     * Checks whether a time series exists by checking whether any of the IRIs that should be attached to
     * the time series is not initialised in the central RDB lookup table using the time series client.
     *
     * @param iris The IRIs that should be attached to the same time series provided as list of strings.
     * @return True if all IRIs have a time series attached, false otherwise.
     */
    private boolean timeSeriesExist(List<String> iris) {
        // If any of the IRIs does not have a time series the time series does not exist
        for(String iri: iris) {
            try {
                if (!tsClient.checkDataHasTimeSeries(iri)) {
                    return false;
                }
                // If central RDB lookup table ("dbTable") has not been initialised, the time series does not exist
            } catch (DataAccessException e) {
                if (e.getMessage().contains("ERROR: relation \"dbTable\" does not exist")) {
                    return false;
                }
                else {
                    throw e;
                }
            }
        }
        return true;
    }

    /**
     * gets namespace uri from input city object uri.
     *
     * @param uriString input city object id
     * @return Object's namespace
     */
    private String getNamespace(String uriString) {
        String[] splitUri = uriString.split("/");
        return String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length - 2))+"/";
    }

    /**
     * creates graph uri from input city object uri and graph name tag.
     *
     * @param uriString input city object id
     * @param graph name tag of graph wanted
     * @return Requested graph with correct namespace
     */
    private String getGraph(String uriString, String graph) {
        String namespace = getNamespace(uriString);
        return namespace + graph + "/";
    }

    /**
     * gets UUID from input city object uri
     *
     * @param uriString input city object id
     * @return Requested UUID
     */
    private String getUUID(String uriString) {
        String[] splitUri = uriString.split("/");
        return splitUri[splitUri.length-1];
    }

    /**
     * Returns route for use with AccessAgent
     *
     * @param iriString iri of object to be queried
     * @return route of endpoint that iri belongs to
     */
    private String getRoute(String iriString) {
        String namespaceEndpoint = getNamespace(iriString);
        String route = accessAgentRoutes.get(namespaceEndpoint);
        return route;
    }

    /**
     * Sets the endpoint in the time series client properties file using
     * the namespace provided
     *
     * @param namespace endpoint for querying/updating
     */
    private String setTimeSeriesClientProperties(String namespace) {
        try {

            String timeseries_props;
            Boolean isDocker = false;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                timeseries_props = new File(
                        Objects.requireNonNull(getClass().getClassLoader().getResource(TIME_SERIES_CLIENT_PROPS)).toURI()).getAbsolutePath();
            } else {
                timeseries_props = FS + "target" + FS + "classes" + FS + TIME_SERIES_CLIENT_PROPS;
                isDocker = true;
            }
            File PropertiesFile = new File(timeseries_props);

            String oldFileContent = "";

            BufferedReader reader;
            FileWriter writer;

            reader = new BufferedReader(new FileReader(PropertiesFile));

            // Read all the lines of times series client properties file into oldFileContent
            String line = reader.readLine();
            String oldQueryString="";
            String oldUpdateString="";

            while (line != null)
            {
                oldFileContent = oldFileContent + line + System.lineSeparator();

                if (line.contains("sparql.query.endpoint")) oldQueryString = line;
                if (line.contains("sparql.update.endpoint")) oldUpdateString = line;

                line = reader.readLine();
            }

            // If building docker image, transform localhost in namespace to host.docker.internal
            String[] localHostStrings = {"localhost", "127.0.0.1"};
            if(isDocker) {
                for(String s : localHostStrings){
                    if(namespace.contains(s)) namespace = namespace.replace(s, "host.docker.internal");
                }
            }

            // Replace oldQueryString and oldUpdateString with namespace given
            String newFileContent = oldFileContent.replace(oldQueryString, "sparql.query.endpoint="+namespace);
            newFileContent = newFileContent.replace(oldUpdateString, "sparql.update.endpoint="+namespace);

            //Rewrite the input text file with newFileContent
            writer = new FileWriter(PropertiesFile);
            writer.write(newFileContent);

            reader.close();
            writer.close();

            return newFileContent;

        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
            throw new JPSRuntimeException(e);
        }
    }

    /**
     * executes query on SPARQL endpoint and retrieves requested value of building
     * @param uriString city object id
     * @param value building value requested
     * @param route route to pass to access agent
     * @return geometry as string
     */
    private String getValue(String uriString, String value, String route)  {

        String result = "";

        Query q = getQuery(uriString, value);

        //Use access agent
        JSONArray queryResultArray = this.queryStore(route, q.toString());

        if(!queryResultArray.isEmpty()){
            if(value!="Footprint") {
                result = queryResultArray.getJSONObject(0).get(value).toString();
            }
            else{
                result=getFootprint(queryResultArray);
            }
        }
        return result;
    }

    /**
     * finds footprint of building from array of building surfaces by searching for constant minimum z in geometries
     * NB. On data TSDA has run on, the surface should be labelled with a ground surface id so this won't be necessary
     * @param results array of building surfaces
     * @return footprint geometry as string
     */
    private String getFootprint(JSONArray results){
        String footprint="";
        ArrayList<String> z_values = new ArrayList<>();
        Double minimum=Double.MAX_VALUE;

        for(Integer i=0; i<results.length(); i++){
            String geom = results.getJSONObject(i).get("Footprint").toString();
            String[] split = geom.split("#");
            // store z values of surface
            for(Integer j=1; j<=split.length; j++) {
                if(j%3==0){
                    z_values.add(split[j-1]);
                }
            }
            // find surfaces with constant z value
            Boolean zIsConstant = true;
            for(Integer k=1; k<z_values.size(); k++) {
                if (!z_values.get(k).equals(z_values.get(k - 1))) {
                    zIsConstant = false;
                }
            }
            // store geometry with the minimum constant z as footprint
            if (zIsConstant && Double.valueOf(z_values.get(0)) < minimum) {
                minimum = Double.valueOf(z_values.get(0));
                footprint=geom;
            }
            z_values.clear();
        }
        return footprint;
    }

    /**
     * calls a SPARQL query for a specific URI for height or geometry.
     * @param uriString city object id
     * @param value building value requested
     * @return returns a query string
     */
    private Query getQuery(String uriString, String value) {
        switch(value) {
            case "Footprint":
                return getGeometryQuery(uriString);
            case "Height":
                return getHeightQuery(uriString);
            case "CRS":
                return getCrsQuery(uriString);
        }
        return null;
    }

    /**
     * builds a SPARQL query for a specific URI to retrieve a footprint.
     * @param uriString city object id
     * @return returns a query string
     */
    /*
    // Old implementation using Envelope
    private Query getGeometryQuery(String uriString) {
        SelectBuilder sb = new SelectBuilder()
                .addPrefix( "ocgml", ocgmlUri )
                .addVar("?Envelope")
                .addGraph(NodeFactory.createURI(getGraph(uriString,CITY_OBJECT)), "?s", "ocgml:EnvelopeType", "?Envelope");
        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(uriString));

        return sb.build();
    }*/
    private Query getGeometryQuery(String uriString) {
        try {
            WhereBuilder wb = new WhereBuilder()
                    .addPrefix("ocgml", ocgmlUri)
                    .addWhere("?surf", "ocgml:cityObjectId", "?s")
                    .addWhere("?surf", "ocgml:GeometryType", "?Footprint")
                    .addFilter("!isBlank(?Footprint)");
            SelectBuilder sb = new SelectBuilder()
                    .addVar("?Footprint")
                    .addGraph(NodeFactory.createURI(getGraph(uriString,SURFACE_GEOMETRY)), wb);
            sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(getGraph(uriString,BUILDING)+getUUID(uriString)+"/"));
            return sb.build();

        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * builds a SPARQL query for a specific URI to retrieve the building height.
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getHeightQuery(String uriString) {
        WhereBuilder wb = new WhereBuilder();
        SelectBuilder sb = new SelectBuilder();
        if(uriString.contains("kings-lynn-open-data"))
        {
            wb.addPrefix("ocgml", ocgmlUri)
                    .addWhere("?s", "ocgml:measuredHeight", "?Height");
            sb.addVar("?Height")
                    .addGraph(NodeFactory.createURI(getGraph(uriString,BUILDING)), wb);
            sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(getGraph(uriString,BUILDING)+getUUID(uriString)+"/"));
        }
        else{
            wb.addPrefix("ocgml", ocgmlUri)
                    .addWhere("?o", "ocgml:attrName", "height")
                    .addWhere("?o", "ocgml:realVal", "?Height")
                    .addWhere("?o", "ocgml:cityObjectId", "?s");
            sb.addVar("?Height")
                    .addGraph(NodeFactory.createURI(getGraph(uriString,CITY_OBJECT_GEN_ATT)), wb);
            sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(uriString));
        }

        return sb.build();
    }

    /**
     * builds a SPARQL query for a CRS using namespace from uri
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getCrsQuery(String uriString) {
        WhereBuilder wb = new WhereBuilder()
                .addPrefix("ocgml", ocgmlUri)
                .addWhere("?s", "ocgml:srid", "?CRS");
        SelectBuilder sb = new SelectBuilder()
                .addVar("?CRS")
                .addGraph(NodeFactory.createURI(getGraph(uriString,DATABASE_SRS)), wb);
        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(getNamespace(uriString)));
        return sb.build();
    }

    /**
     * builds a SPARQL update to add cityobject generic attribute that links to energy profile graph
     * @param uriString city object id
     * @param energyProfileBuildingUri building id in energy profile namespace
     * @param route route to pass to access agent
     */
    public void sparqlGenAttributeUpdate( String uriString, String energyProfileBuildingUri, String route) {
        String genAttributeGraphUri = getGraph(uriString, CITY_OBJECT_GEN_ATT);

        String genAttributeUri = genAttributeGraphUri + "UUID_" + UUID.randomUUID() + "/";

        UpdateBuilder ub =
                new UpdateBuilder()
                        .addPrefix("ocgml", ocgmlUri)
                        .addInsert("?graph", NodeFactory.createURI(genAttributeUri), "ocgml:id", NodeFactory.createURI(genAttributeUri))
                        .addInsert("?graph", NodeFactory.createURI(genAttributeUri), "ocgml:cityObjectId", NodeFactory.createURI(uriString))
                        .addInsert("?graph", NodeFactory.createURI(genAttributeUri), "ocgml:attrName", "energyProfileBuildingRepresentation")
                        .addInsert("?graph", NodeFactory.createURI(genAttributeUri), "ocgml:dataType", 4)
                        .addInsert("?graph", NodeFactory.createURI(genAttributeUri), "ocgml:uriVal", NodeFactory.createURI(energyProfileBuildingUri))
                        .addInsert("?graph", NodeFactory.createURI(genAttributeUri), "ocgml:dateVal", NodeFactory.createBlankNode())
                        .addInsert("?graph", NodeFactory.createURI(genAttributeUri), "ocgml:intVal", NodeFactory.createBlankNode())
                        .addInsert("?graph", NodeFactory.createURI(genAttributeUri), "ocgml:realVal", NodeFactory.createBlankNode())
                        .addInsert("?graph", NodeFactory.createURI(genAttributeUri), "ocgml:strVal", NodeFactory.createBlankNode())
                        .addInsert("?graph", NodeFactory.createURI(genAttributeUri), "ocgml:parentGenattribId", NodeFactory.createBlankNode())
                        .addInsert("?graph", NodeFactory.createURI(genAttributeUri), "ocgml:rootGenattribId", NodeFactory.createBlankNode());
        ub.setVar(Var.alloc("graph"), NodeFactory.createURI(genAttributeGraphUri));

        UpdateRequest ur = ub.buildRequest();

        //Use access agent
        this.updateStore(route, ur.toString());
    }
    /**
     * builds a SPARQL update using output from CEA simulations
     * @param scalars map of scalar measurements
     * @param tsIris map of time series iris
     * @param uriString city object id
     * @param uriCounter keep track of uris
     * @param route route to pass to access agent
     * @return building uri in energy profile graph
     */
    public String sparqlUpdate( LinkedHashMap<String, List<String>> scalars, LinkedHashMap<String, String> tsIris, String uriString, Integer uriCounter, String route) {
        String outputGraphUri = getGraph(uriString,ENERGY_PROFILE);

        String buildingUri = outputGraphUri + "Building_UUID_" + UUID.randomUUID() + "/";
        String heatingUri = outputGraphUri + "HeatingSystem_UUID_" + UUID.randomUUID() + "/";
        String coolingUri = outputGraphUri + "CoolingSystem_UUID_" + UUID.randomUUID() + "/";
        String pvRoofPanelsUri = outputGraphUri + "PV_RoofPanels_UUID_" + UUID.randomUUID()+ "/";
        String pvWallSouthPanelsUri = outputGraphUri + "PV_WallSouthPanels_UUID_" + UUID.randomUUID()+ "/";
        String pvWallNorthPanelsUri = outputGraphUri + "PV_WallNorthPanels_UUID_" + UUID.randomUUID()+ "/";
        String pvWallEastPanelsUri = outputGraphUri + "PV_WallEastPanels_UUID_" + UUID.randomUUID()+ "/";
        String pvWallWestPanelsUri = outputGraphUri + "PV_WallWestPanels_UUID_" + UUID.randomUUID()+ "/";

        String PVAreaRoofUri = outputGraphUri + "PVAreaRoof_UUID_" + UUID.randomUUID()+ "/";
        String PVAreaRoofValueUri = outputGraphUri + "PVAreaRoofValue_UUID_" + UUID.randomUUID()+ "/";
        String PVAreaWallSouthUri = outputGraphUri + "PVAreaWallSouth_UUID_" + UUID.randomUUID()+ "/";
        String PVAreaWallSouthValueUri = outputGraphUri + "PVAreaWallSouthValue_UUID_" + UUID.randomUUID()+ "/";
        String PVAreaWallNorthUri = outputGraphUri + "PVAreaWallNorth_UUID_" + UUID.randomUUID()+ "/";
        String PVAreaWallNorthValueUri = outputGraphUri + "PVAreaWallNorthValue_UUID_" + UUID.randomUUID()+ "/";
        String PVAreaWallEastUri = outputGraphUri + "PVAreaWallEast_UUID_" + UUID.randomUUID()+ "/";
        String PVAreaWallEastValueUri = outputGraphUri + "PVAreaWallEastValue_UUID_" + UUID.randomUUID()+ "/";
        String PVAreaWallWestUri = outputGraphUri + "PVAreaWallWest_UUID_" + UUID.randomUUID()+ "/";
        String PVAreaWallWestValueUri = outputGraphUri + "PVAreaWallWestValue_UUID_" + UUID.randomUUID()+ "/";
        String gridConsumptionUri = outputGraphUri + "GridConsumption_UUID_" + UUID.randomUUID()+ "/";
        String electricityConsumptionUri = outputGraphUri + "ElectricityConsumption_UUID_" + UUID.randomUUID()+ "/";
        String heatingConsumptionUri = outputGraphUri + "HeatingConsumption_UUID_" + UUID.randomUUID()+ "/";
        String coolingConsumptionUri = outputGraphUri + "CoolingConsumption_UUID_" + UUID.randomUUID()+ "/";
        String PVRoofSupplyUri = outputGraphUri + "PVRoofSupply_UUID_" + UUID.randomUUID()+ "/";
        String PVWallSouthSupplyUri = outputGraphUri + "PVWallSouthSupply_UUID_" + UUID.randomUUID()+ "/";
        String PVWallNorthSupplyUri = outputGraphUri + "PVWallNorthSupply_UUID_" + UUID.randomUUID()+ "/";
        String PVWallEastSupplyUri = outputGraphUri + "PVWallEastSupply_UUID_" + UUID.randomUUID()+ "/";
        String PVWallWestSupplyUri = outputGraphUri + "PVWallWestSupply_UUID_" + UUID.randomUUID()+ "/";

        UpdateBuilder ub =
                new UpdateBuilder()
                        .addPrefix("ontoubemmp", ontoUBEMMPUri)
                        .addPrefix("rdf", rdfUri)
                        .addPrefix("owl", owlUri)
                        .addPrefix("purlEnaeq", purlEnaeqUri)
                        .addPrefix("om", unitOntologyUri)
                        .addPrefix("thinkhome", thinkhomeUri)
                        .addPrefix("purlInf", purlInfrastructureUri)
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "rdf:type", "purlInf:Building")
                        .addInsert("?graph", NodeFactory.createURI(gridConsumptionUri), "rdf:type", "ontoubemmp:GridConsumption")
                        .addInsert("?graph", NodeFactory.createURI(gridConsumptionUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(gridConsumptionUri), "om:hasDimension", "om:energy-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(gridConsumptionUri), "om:hasValue", NodeFactory.createURI(tsIris.get("grid_demand")))
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("grid_demand")), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("grid_demand")), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("grid_demand")), "om:hasUnit", "om:kilowattHour")
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "purlEnaeq:consumesEnergy", NodeFactory.createURI(gridConsumptionUri))
                        .addInsert("?graph", NodeFactory.createURI(electricityConsumptionUri), "rdf:type", "ontoubemmp:ElectricityConsumption")
                        .addInsert("?graph", NodeFactory.createURI(electricityConsumptionUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(electricityConsumptionUri), "om:hasDimension", "om:energy-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(electricityConsumptionUri), "om:hasValue", NodeFactory.createURI(tsIris.get("electricity_demand")))
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("electricity_demand")), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("electricity_demand")), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("electricity_demand")), "om:hasUnit", "om:kilowattHour")
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "purlEnaeq:consumesEnergy", NodeFactory.createURI(electricityConsumptionUri))
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "ontoubemmp:hasDevice", NodeFactory.createURI(heatingUri))
                        .addInsert("?graph", NodeFactory.createURI(heatingUri), "rdf:type", "purlEnaeq:HeatingSystem")
                        .addInsert("?graph", NodeFactory.createURI(heatingUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(heatingConsumptionUri), "rdf:type", "ontoubemmp:ThermalConsumption")
                        .addInsert("?graph", NodeFactory.createURI(heatingConsumptionUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(heatingConsumptionUri), "om:hasDimension", "om:energy-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(heatingConsumptionUri), "om:hasValue", NodeFactory.createURI(tsIris.get("heating_demand")))
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("heating_demand")), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("heating_demand")), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("heating_demand")), "om:hasUnit", "om:kilowattHour")
                        .addInsert("?graph", NodeFactory.createURI(heatingUri), "purlEnaeq:consumesEnergy", NodeFactory.createURI(heatingConsumptionUri))
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "ontoubemmp:hasDevice", NodeFactory.createURI(coolingUri))
                        .addInsert("?graph", NodeFactory.createURI(coolingUri), "rdf:type", "ontoubemmp:CoolingSystem")
                        .addInsert("?graph", NodeFactory.createURI(coolingUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(coolingConsumptionUri), "rdf:type", "ontoubemmp:ThermalConsumption")
                        .addInsert("?graph", NodeFactory.createURI(coolingConsumptionUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(coolingConsumptionUri), "om:hasDimension", "om:energy-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(coolingConsumptionUri), "om:hasValue", NodeFactory.createURI(tsIris.get("cooling_demand")))
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("cooling_demand")), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("cooling_demand")), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("cooling_demand")), "om:hasUnit", "om:kilowattHour")
                        .addInsert("?graph", NodeFactory.createURI(coolingUri), "purlEnaeq:consumesEnergy", NodeFactory.createURI(coolingConsumptionUri))
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "ontoubemmp:hasDevice", NodeFactory.createURI(pvRoofPanelsUri))
                        .addInsert("?graph", NodeFactory.createURI(pvRoofPanelsUri), "rdf:type", "ontoubemmp:RoofPVPanels")
                        .addInsert("?graph", NodeFactory.createURI(pvRoofPanelsUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(pvRoofPanelsUri), "ontoubemmp:hasArea", NodeFactory.createURI(PVAreaRoofUri))
                        .addInsert("?graph", NodeFactory.createURI(PVAreaRoofUri), "rdf:type", "ontoubemmp:PVPanelsArea")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaRoofUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaRoofUri), "om:hasDimension", "om:area-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaRoofUri), "om:hasValue", NodeFactory.createURI(PVAreaRoofValueUri))
                        .addInsert("?graph", NodeFactory.createURI(PVAreaRoofValueUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaRoofValueUri), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaRoofValueUri), "om:hasNumericalValue", scalars.get(KEY_PV_ROOF_AREA).get(uriCounter))
                        .addInsert("?graph", NodeFactory.createURI(PVAreaRoofValueUri), "om:hasUnit", "om:squareMetre")
                        .addInsert("?graph", NodeFactory.createURI(pvRoofPanelsUri), "thinkhome:producesEnergy", NodeFactory.createURI(PVRoofSupplyUri))
                        .addInsert("?graph", NodeFactory.createURI(PVRoofSupplyUri), "rdf:type", "ontoubemmp:ElectricitySupply")
                        .addInsert("?graph", NodeFactory.createURI(PVRoofSupplyUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(PVRoofSupplyUri), "om:hasDimension", "om:energy-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(PVRoofSupplyUri), "om:hasValue", NodeFactory.createURI(tsIris.get("PV_supply_roof")))
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("PV_supply_roof")), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("PV_supply_roof")), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("PV_supply_roof")), "om:hasUnit", "om:kilowattHour")
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "ontoubemmp:hasDevice", NodeFactory.createURI(pvWallSouthPanelsUri))
                        .addInsert("?graph", NodeFactory.createURI(pvWallSouthPanelsUri), "rdf:type", "ontoubemmp:SouthWallPVPanels")
                        .addInsert("?graph", NodeFactory.createURI(pvWallSouthPanelsUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(pvWallSouthPanelsUri), "ontoubemmp:hasArea", NodeFactory.createURI(PVAreaWallSouthUri))
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallSouthUri), "rdf:type", "ontoubemmp:PVPanelsArea")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallSouthUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallSouthUri), "om:hasDimension", "om:area-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallSouthUri), "om:hasValue", NodeFactory.createURI(PVAreaWallSouthValueUri))
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallSouthValueUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallSouthValueUri), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallSouthValueUri), "om:hasNumericalValue", scalars.get(KEY_PV_WALL_SOUTH_AREA).get(uriCounter))
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallSouthValueUri), "om:hasUnit", "om:squareMetre")
                        .addInsert("?graph", NodeFactory.createURI(pvWallSouthPanelsUri), "thinkhome:producesEnergy", NodeFactory.createURI(PVWallSouthSupplyUri))
                        .addInsert("?graph", NodeFactory.createURI(PVWallSouthSupplyUri), "rdf:type", "ontoubemmp:ElectricitySupply")
                        .addInsert("?graph", NodeFactory.createURI(PVWallSouthSupplyUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(PVWallSouthSupplyUri), "om:hasDimension", "om:energy-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(PVWallSouthSupplyUri), "om:hasValue", NodeFactory.createURI(tsIris.get("PV_supply_wall_south")))
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("PV_supply_wall_south")), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("PV_supply_wall_south")), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("PV_supply_wall_south")), "om:hasUnit", "om:kilowattHour")
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "ontoubemmp:hasDevice", NodeFactory.createURI(pvWallNorthPanelsUri))
                        .addInsert("?graph", NodeFactory.createURI(pvWallNorthPanelsUri), "rdf:type", "ontoubemmp:NorthWallPVPanels")
                        .addInsert("?graph", NodeFactory.createURI(pvWallNorthPanelsUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(pvWallNorthPanelsUri), "ontoubemmp:hasArea", NodeFactory.createURI(PVAreaWallNorthUri))
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallNorthUri), "rdf:type", "ontoubemmp:PVPanelsArea")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallNorthUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallNorthUri), "om:hasDimension", "om:area-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallNorthUri), "om:hasValue", NodeFactory.createURI(PVAreaWallNorthValueUri))
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallNorthValueUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallNorthValueUri), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallNorthValueUri), "om:hasNumericalValue", scalars.get(KEY_PV_WALL_NORTH_AREA).get(uriCounter))
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallNorthValueUri), "om:hasUnit", "om:squareMetre")
                        .addInsert("?graph", NodeFactory.createURI(pvWallNorthPanelsUri), "thinkhome:producesEnergy", NodeFactory.createURI(PVWallNorthSupplyUri))
                        .addInsert("?graph", NodeFactory.createURI(PVWallNorthSupplyUri), "rdf:type", "ontoubemmp:ElectricitySupply")
                        .addInsert("?graph", NodeFactory.createURI(PVWallNorthSupplyUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(PVWallNorthSupplyUri), "om:hasDimension", "om:energy-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(PVWallNorthSupplyUri), "om:hasValue", NodeFactory.createURI(tsIris.get("PV_supply_wall_north")))
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("PV_supply_wall_north")), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("PV_supply_wall_north")), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("PV_supply_wall_north")), "om:hasUnit", "om:kilowattHour")
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "ontoubemmp:hasDevice", NodeFactory.createURI(pvWallEastPanelsUri))
                        .addInsert("?graph", NodeFactory.createURI(pvWallEastPanelsUri), "rdf:type", "ontoubemmp:EastWallPVPanels")
                        .addInsert("?graph", NodeFactory.createURI(pvWallEastPanelsUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(pvWallEastPanelsUri), "ontoubemmp:hasArea", NodeFactory.createURI(PVAreaWallEastUri))
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallEastUri), "rdf:type", "ontoubemmp:PVPanelsArea")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallEastUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallEastUri), "om:hasDimension", "om:area-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallEastUri), "om:hasValue", NodeFactory.createURI(PVAreaWallEastValueUri))
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallEastValueUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallEastValueUri), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallEastValueUri), "om:hasNumericalValue", scalars.get(KEY_PV_WALL_EAST_AREA).get(uriCounter))
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallEastValueUri), "om:hasUnit", "om:squareMetre")
                        .addInsert("?graph", NodeFactory.createURI(pvWallEastPanelsUri), "thinkhome:producesEnergy", NodeFactory.createURI(PVWallEastSupplyUri))
                        .addInsert("?graph", NodeFactory.createURI(PVWallEastSupplyUri), "rdf:type", "ontoubemmp:ElectricitySupply")
                        .addInsert("?graph", NodeFactory.createURI(PVWallEastSupplyUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(PVWallEastSupplyUri), "om:hasDimension", "om:energy-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(PVWallEastSupplyUri), "om:hasValue", NodeFactory.createURI(tsIris.get("PV_supply_wall_east")))
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("PV_supply_wall_east")), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("PV_supply_wall_east")), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("PV_supply_wall_east")), "om:hasUnit", "om:kilowattHour")
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "ontoubemmp:hasDevice", NodeFactory.createURI(pvWallWestPanelsUri))
                        .addInsert("?graph", NodeFactory.createURI(pvWallWestPanelsUri), "rdf:type", "ontoubemmp:WestWallPVPanels")
                        .addInsert("?graph", NodeFactory.createURI(pvWallWestPanelsUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(pvWallWestPanelsUri), "ontoubemmp:hasArea", NodeFactory.createURI(PVAreaWallWestUri))
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallWestUri), "rdf:type", "ontoubemmp:PVPanelsArea")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallWestUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallWestUri), "om:hasDimension", "om:area-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallWestUri), "om:hasValue", NodeFactory.createURI(PVAreaWallWestValueUri))
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallWestValueUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallWestValueUri), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallWestValueUri), "om:hasNumericalValue", scalars.get(KEY_PV_WALL_WEST_AREA).get(uriCounter))
                        .addInsert("?graph", NodeFactory.createURI(PVAreaWallWestValueUri), "om:hasUnit", "om:squareMetre")
                        .addInsert("?graph", NodeFactory.createURI(pvWallWestPanelsUri), "thinkhome:producesEnergy", NodeFactory.createURI(PVWallWestSupplyUri))
                        .addInsert("?graph", NodeFactory.createURI(PVWallWestSupplyUri), "rdf:type", "ontoubemmp:ElectricitySupply")
                        .addInsert("?graph", NodeFactory.createURI(PVWallWestSupplyUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(PVWallWestSupplyUri), "om:hasDimension", "om:energy-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(PVWallWestSupplyUri), "om:hasValue", NodeFactory.createURI(tsIris.get("PV_supply_wall_west")))
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("PV_supply_wall_west")), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("PV_supply_wall_west")), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(tsIris.get("PV_supply_wall_west")), "om:hasUnit", "om:kilowattHour");

        ub.setVar(Var.alloc("graph"), NodeFactory.createURI(outputGraphUri));

        UpdateRequest ur = ub.buildRequest();

        //Use access agent
        this.updateStore(route, ur.toString());

        return buildingUri;

    }

    /**
     * Retrieves iris from KG for the data type requested
     * @param uriString city object id
     * @param value type of data from TIME_SERIES
     * @param route route to pass to access agent
     * @return list of iris
     */
    public ArrayList<String> getDataIRI(String uriString, String value, String route) {
        ArrayList<String> result = new ArrayList<>();

        WhereBuilder wb1 = new WhereBuilder();
        SelectBuilder sb = new SelectBuilder();
        WhereBuilder wb2 = new WhereBuilder();

        wb1.addPrefix("ocgml", ocgmlUri)
                .addWhere("?genAttribute", "ocgml:cityObjectId", "?s")
                .addWhere("?genAttribute", "ocgml:attrName", "energyProfileBuildingRepresentation")
                .addWhere("?genAttribute", "ocgml:uriVal", "?energyProfileBuilding");

        sb.addGraph(NodeFactory.createURI(getGraph(uriString,CITY_OBJECT_GEN_ATT)), wb1);

        wb2.addPrefix("ocgml", ocgmlUri)
                .addPrefix("rdf", rdfUri)
                .addPrefix("om", unitOntologyUri)
                .addPrefix("purlEnaeq", purlEnaeqUri)
                .addPrefix("ontoubemmp", ontoUBEMMPUri)
                .addPrefix("thinkhome", thinkhomeUri);

        switch(value) {
            case KEY_GRID_DEMAND:
                wb2.addWhere("?energyProfileBuilding", "purlEnaeq:consumesEnergy", "?grid")
                        .addWhere("?grid", "rdf:type", "ontoubemmp:GridConsumption")
                        .addWhere("?grid", "om:hasValue", "?measure")
                        .addWhere("?measure", "om:hasUnit", "?unit");
                break;
            case KEY_ELECTRICITY_DEMAND:
                wb2.addWhere("?energyProfileBuilding", "purlEnaeq:consumesEnergy", "?electricity")
                        .addWhere("?electricity", "rdf:type", "ontoubemmp:ElectricityConsumption")
                        .addWhere("?electricity", "om:hasValue", "?measure")
                        .addWhere("?measure", "om:hasUnit", "?unit");
                break;
            case KEY_HEATING_DEMAND:
                wb2.addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?heatingDevice")
                        .addWhere("?heatingDevice", "rdf:type", "purlEnaeq:HeatingSystem")
                        .addWhere("?heatingDevice", "purlEnaeq:consumesEnergy", "?heating")
                        .addWhere("?heating", "rdf:type", "ontoubemmp:ThermalConsumption")
                        .addWhere("?heating", "om:hasValue", "?measure")
                        .addWhere("?measure", "om:hasUnit", "?unit");
                break;
            case KEY_COOLING_DEMAND:
                wb2.addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?coolingDevice")
                        .addWhere("?coolingDevice", "rdf:type", "ontoubemmp:CoolingSystem")
                        .addWhere("?coolingDevice", "purlEnaeq:consumesEnergy", "?cooling")
                        .addWhere("?cooling", "rdf:type", "ontoubemmp:ThermalConsumption")
                        .addWhere("?cooling", "om:hasValue", "?measure")
                        .addWhere("?measure", "om:hasUnit", "?unit");
                break;
            case KEY_PV_ROOF_SUPPLY:
                wb2.addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?PVPanels")
                        .addWhere("?PVPanels", "rdf:type", "ontoubemmp:RoofPVPanels")
                        .addWhere("?PVPanels", "thinkhome:producesEnergy", "?supply")
                        .addWhere("?supply", "rdf:type", "ontoubemmp:ElectricitySupply")
                        .addWhere("?supply", "om:hasValue", "?measure")
                        .addWhere("?measure", "om:hasUnit", "?unit");
                break;
            case KEY_PV_WALL_SOUTH_SUPPLY:
                wb2.addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?PVPanels")
                        .addWhere("?PVPanels", "rdf:type", "ontoubemmp:SouthWallPVPanels")
                        .addWhere("?PVPanels", "thinkhome:producesEnergy", "?supply")
                        .addWhere("?supply", "rdf:type", "ontoubemmp:ElectricitySupply")
                        .addWhere("?supply", "om:hasValue", "?measure")
                        .addWhere("?measure", "om:hasUnit", "?unit");
                break;
            case KEY_PV_WALL_NORTH_SUPPLY:
                wb2.addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?PVPanels")
                        .addWhere("?PVPanels", "rdf:type", "ontoubemmp:NorthWallPVPanels")
                        .addWhere("?PVPanels", "thinkhome:producesEnergy", "?supply")
                        .addWhere("?supply", "rdf:type", "ontoubemmp:ElectricitySupply")
                        .addWhere("?supply", "om:hasValue", "?measure")
                        .addWhere("?measure", "om:hasUnit", "?unit");
                break;
            case KEY_PV_WALL_EAST_SUPPLY:
                wb2.addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?PVPanels")
                        .addWhere("?PVPanels", "rdf:type", "ontoubemmp:EastWallPVPanels")
                        .addWhere("?PVPanels", "thinkhome:producesEnergy", "?supply")
                        .addWhere("?supply", "rdf:type", "ontoubemmp:ElectricitySupply")
                        .addWhere("?supply", "om:hasValue", "?measure")
                        .addWhere("?measure", "om:hasUnit", "?unit");
                break;
            case KEY_PV_WALL_WEST_SUPPLY:
                wb2.addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?PVPanels")
                        .addWhere("?PVPanels", "rdf:type", "ontoubemmp:WestWallPVPanels")
                        .addWhere("?PVPanels", "thinkhome:producesEnergy", "?supply")
                        .addWhere("?supply", "rdf:type", "ontoubemmp:ElectricitySupply")
                        .addWhere("?supply", "om:hasValue", "?measure")
                        .addWhere("?measure", "om:hasUnit", "?unit");
                break;
            case KEY_PV_ROOF_AREA:
                wb2.addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?PVPanels")
                        .addWhere("?PVPanels", "rdf:type", "ontoubemmp:RoofPVPanels")
                        .addWhere("?PVPanels", "ontoubemmp:hasArea", "?area")
                        .addWhere("?area", "rdf:type", "ontoubemmp:PVPanelsArea")
                        .addWhere("?area", "om:hasValue", "?value")
                        .addWhere("?value", "om:hasNumericalValue", "?measure")
                        .addWhere("?value", "om:hasUnit", "?unit");
                break;
            case KEY_PV_WALL_SOUTH_AREA:
                wb2.addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?PVPanels")
                        .addWhere("?PVPanels", "rdf:type", "ontoubemmp:SouthWallPVPanels")
                        .addWhere("?PVPanels", "ontoubemmp:hasArea", "?area")
                        .addWhere("?area", "rdf:type", "ontoubemmp:PVPanelsArea")
                        .addWhere("?area", "om:hasValue", "?value")
                        .addWhere("?value", "om:hasNumericalValue", "?measure")
                        .addWhere("?value", "om:hasUnit", "?unit");
                break;
            case KEY_PV_WALL_NORTH_AREA:
                wb2.addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?PVPanels")
                        .addWhere("?PVPanels", "rdf:type", "ontoubemmp:NorthWallPVPanels")
                        .addWhere("?PVPanels", "ontoubemmp:hasArea", "?area")
                        .addWhere("?area", "rdf:type", "ontoubemmp:PVPanelsArea")
                        .addWhere("?area", "om:hasValue", "?value")
                        .addWhere("?value", "om:hasNumericalValue", "?measure")
                        .addWhere("?value", "om:hasUnit", "?unit");
                break;
            case KEY_PV_WALL_EAST_AREA:
                wb2.addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?PVPanels")
                        .addWhere("?PVPanels", "rdf:type", "ontoubemmp:EastWallPVPanels")
                        .addWhere("?PVPanels", "ontoubemmp:hasArea", "?area")
                        .addWhere("?area", "rdf:type", "ontoubemmp:PVPanelsArea")
                        .addWhere("?area", "om:hasValue", "?value")
                        .addWhere("?value", "om:hasNumericalValue", "?measure")
                        .addWhere("?value", "om:hasUnit", "?unit");
                break;
            case KEY_PV_WALL_WEST_AREA:
                wb2.addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?PVPanels")
                        .addWhere("?PVPanels", "rdf:type", "ontoubemmp:WestWallPVPanels")
                        .addWhere("?PVPanels", "ontoubemmp:hasArea", "?area")
                        .addWhere("?area", "rdf:type", "ontoubemmp:PVPanelsArea")
                        .addWhere("?area", "om:hasValue", "?value")
                        .addWhere("?value", "om:hasNumericalValue", "?measure")
                        .addWhere("?value", "om:hasUnit", "?unit");
                break;
            default:
                return result;

        }

        sb.addVar("?measure")
                .addVar("?unit")
                .addGraph(NodeFactory.createURI(getGraph(uriString,ENERGY_PROFILE)), wb2);

        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(uriString));

        JSONArray queryResultArray = new JSONArray(this.queryStore(route, sb.build().toString()));

        if(!queryResultArray.isEmpty()){
            result.add(queryResultArray.getJSONObject(0).get("measure").toString());
            result.add(queryResultArray.getJSONObject(0).get("unit").toString());
        }
        return result;

    }

    /**
     * Check generic attribute is initialised in KG
     * @param uriString city object id
     * @param route route to pass to access agent
     * @return building in energy profile graph
     */
    public String checkGenAttributeInitialised(String uriString, String route){
        WhereBuilder wb = new WhereBuilder();
        SelectBuilder sb = new SelectBuilder();

        wb.addPrefix("ocgml", ocgmlUri)
                .addWhere("?genAttribute", "ocgml:cityObjectId", "?s")
                .addWhere("?genAttribute", "ocgml:attrName", "energyProfileBuildingRepresentation")
                .addWhere("?genAttribute", "ocgml:uriVal", "?energyProfileBuilding");

        sb.addVar("?energyProfileBuilding")
                .addGraph(NodeFactory.createURI(getGraph(uriString,CITY_OBJECT_GEN_ATT)), wb);

        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(uriString));

        JSONArray queryResultArray = new JSONArray(this.queryStore(route, sb.build().toString()));
        String building = "";

        if(!queryResultArray.isEmpty()){
            building = queryResultArray.getJSONObject(0).get("energyProfileBuilding").toString();
        }
        return building;
    }

    /**
     * Check if energy profile in KG has been initialised already for given building and time series already exist
     * @param uriString city object id
     * @param fixedIris map of time series iris to data types
     * @param route route to pass to access agent
     * @return building in energy profile graph
     */
    public String checkBlazegraphAndTimeSeriesInitialised(String uriString, List<LinkedHashMap<String,String>> fixedIris, String route){
        String building = checkGenAttributeInitialised(uriString, route);
        if(building.equals("")){
            return "";
        }
        WhereBuilder wb1 = new WhereBuilder();
        WhereBuilder wb2 = new WhereBuilder();
        SelectBuilder sb = new SelectBuilder();

        wb1.addPrefix("ocgml", ocgmlUri)
                .addPrefix("rdf", rdfUri)
                .addPrefix("purlEnaeq", purlEnaeqUri)
                .addPrefix("ontoubemmp", ontoUBEMMPUri)
                .addPrefix("thinkhome", thinkhomeUri)
                .addPrefix("purlInf", purlInfrastructureUri)
                .addPrefix("om", unitOntologyUri)
                .addWhere("?energyProfileBuilding", "rdf:type", "purlInf:Building")
                .addWhere("?energyProfileBuilding", "purlEnaeq:consumesEnergy", "?grid")
                .addWhere("?grid", "rdf:type", "ontoubemmp:GridConsumption")
                .addWhere("?grid", "om:hasValue", "?grid_demand")
                .addWhere("?energyProfileBuilding", "purlEnaeq:consumesEnergy", "?electricity")
                .addWhere("?electricity", "rdf:type", "ontoubemmp:ElectricityConsumption")
                .addWhere("?electricity", "om:hasValue", "?electricity_demand")
                .addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?heating")
                .addWhere("?heating", "rdf:type", "purlEnaeq:HeatingSystem")
                .addWhere("?heating", "purlEnaeq:consumesEnergy", "?heatingConsumption")
                .addWhere("?heatingConsumption", "om:hasValue", "?heating_demand")
                .addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?cooling")
                .addWhere("?cooling", "rdf:type", "ontoubemmp:CoolingSystem")
                .addWhere("?cooling", "purlEnaeq:consumesEnergy", "?coolingConsumption")
                .addWhere("?coolingConsumption", "om:hasValue", "?cooling_demand")
                .addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?PVPanelsRoof")
                .addWhere("?PVPanelsRoof", "rdf:type", "ontoubemmp:RoofPVPanels")
                .addWhere("?PVPanelsRoof", "thinkhome:producesEnergy", "?PVSupplyRoof")
                .addWhere("?PVSupplyRoof", "om:hasValue", "?PV_supply_roof")
                .addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?PVPanelsSouthWalls")
                .addWhere("?PVPanelsSouthWalls", "rdf:type", "ontoubemmp:SouthWallPVPanels")
                .addWhere("?PVPanelsSouthWalls", "thinkhome:producesEnergy", "?PVSupplySouthWalls")
                .addWhere("?PVSupplySouthWalls", "om:hasValue", "?PV_supply_wall_south")
                .addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?PVPanelsNorthWalls")
                .addWhere("?PVPanelsNorthWalls", "rdf:type", "ontoubemmp:NorthWallPVPanels")
                .addWhere("?PVPanelsNorthWalls", "thinkhome:producesEnergy", "?PVSupplyNorthWalls")
                .addWhere("?PVSupplyNorthWalls", "om:hasValue", "?PV_supply_wall_north")
                .addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?PVPanelsEastWalls")
                .addWhere("?PVPanelsEastWalls", "rdf:type", "ontoubemmp:EastWallPVPanels")
                .addWhere("?PVPanelsEastWalls", "thinkhome:producesEnergy", "?PVSupplyEastWalls")
                .addWhere("?PVSupplyEastWalls", "om:hasValue", "?PV_supply_wall_east")
                .addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?PVPanelsWestWalls")
                .addWhere("?PVPanelsWestWalls", "rdf:type", "ontoubemmp:WestWallPVPanels")
                .addWhere("?PVPanelsWestWalls", "thinkhome:producesEnergy", "?PVSupplyWestWalls")
                .addWhere("?PVSupplyWestWalls", "om:hasValue", "?PV_supply_wall_west");
        wb2.addPrefix("ts", timeSeriesUri)
                .addWhere("?grid_demand", "ts:hasTimeSeries", "?gridTS")
                .addWhere("?electricity_demand", "ts:hasTimeSeries", "?electricityTS")
                .addWhere("?heating_demand", "ts:hasTimeSeries", "?heatingTS")
                .addWhere("?cooling_demand", "ts:hasTimeSeries", "?coolingTS")
                .addWhere("?PV_supply_roof", "ts:hasTimeSeries", "?PVSupplyRoofTS")
                .addWhere("?PV_supply_wall_south", "ts:hasTimeSeries", "?PVSupplySouthWallsTS")
                .addWhere("?PV_supply_wall_north", "ts:hasTimeSeries", "?PVSupplyNorthWallsTS")
                .addWhere("?PV_supply_wall_east", "ts:hasTimeSeries", "?PVSupplyEastWallsTS")
                .addWhere("?PV_supply_wall_west", "ts:hasTimeSeries", "?PVSupplyWestWallsTS");


        sb.addVar("?energyProfileBuilding")
                .addGraph(NodeFactory.createURI(getGraph(uriString,ENERGY_PROFILE)), wb1);
        sb.addVar("?grid_demand").addVar("?electricity_demand").addVar("?heating_demand").addVar("?cooling_demand")
                .addVar("?PV_supply_roof").addVar("?PV_supply_wall_south").addVar("?PV_supply_wall_north")
                .addVar("?PV_supply_wall_east").addVar("?PV_supply_wall_west").addWhere(wb2);
        sb.setVar( Var.alloc( "energyProfileBuilding" ), NodeFactory.createURI(building));

        JSONArray queryResultArray = new JSONArray(this.queryStore(route, sb.build().toString()));
        LinkedHashMap<String, String> tsIris = new LinkedHashMap<>();

        if(!queryResultArray.isEmpty()){
            for(String measure: TIME_SERIES){
                if (queryResultArray.getJSONObject(0).has(measure)) {
                    tsIris.put(measure, queryResultArray.getJSONObject(0).get(measure).toString());
                }
            }
            fixedIris.add(tsIris);
            return building;
        }
        return "";
    }

    /**
     * Return readable unit from ontology iri
     * @param ontologyUnit unit iri in ontology
     * @return unit as a String
     */
    public String getUnit(String ontologyUnit){
        switch(ontologyUnit) {
            case("http://www.ontology-of-units-of-measure.org/resource/om-2/kilowattHour"):
                return "kWh";
            case("http://www.ontology-of-units-of-measure.org/resource/om-2/squareMetre"):
                return "m^2";
            default:
                return "";
        }
    }

    /**
     * Return data using time series client for given data iri
     * @param dataIri iri in time series database
     * @return time series data
     */
    public TimeSeries<OffsetDateTime> retrieveData(String dataIri){
        try {
            String timeseries_props;
            if(System.getProperty("os.name").toLowerCase().contains("win")){
                timeseries_props = new File(
                        Objects.requireNonNull(getClass().getClassLoader().getResource(TIME_SERIES_CLIENT_PROPS)).toURI()).getAbsolutePath();
            }
            else{
                timeseries_props = FS+"target"+FS+"classes"+FS+TIME_SERIES_CLIENT_PROPS;
            }
            tsClient =  new TimeSeriesClient<>(OffsetDateTime.class, timeseries_props);
            List<String> iris = new ArrayList<>();
            iris.add(dataIri);
            TimeSeries<OffsetDateTime> data = tsClient.getTimeSeries(iris);
            return data;

        } catch(URISyntaxException | IOException e){
            e.printStackTrace();
            throw new JPSRuntimeException(e);
        }
    }

    /**
     * Calculate annual value by summing all data in column in time series and rounding to 2dp
     * @param timeSeries time series data
     * @param dataIri iri in time series database
     * @return annualValue as a String
     */
    public String calculateAnnual(TimeSeries<OffsetDateTime> timeSeries, String dataIri){
        List<Double> values = timeSeries.getValuesAsDouble(dataIri);
        Double annualValue = 0.;
        for(Double value : values){
            annualValue += value;
        }
        annualValue = Math.round(annualValue*Math.pow(10,2))/Math.pow(10,2);
        return annualValue.toString();

    }

}
