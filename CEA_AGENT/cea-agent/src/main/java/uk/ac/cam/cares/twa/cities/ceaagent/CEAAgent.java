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
    public static final String THEMATIC_SURFACE = "thematicsurface";
    public static final String ENERGY_PROFILE = "energyprofile";
    public static final String DATABASE_SRS = "databasesrs";
    public static final String KEY_GRID_CONSUMPTION = "GridConsumption";
    public static final String KEY_ELECTRICITY_CONSUMPTION = "ElectricityConsumption";
    public static final String KEY_HEATING_CONSUMPTION = "HeatingConsumption";
    public static final String KEY_COOLING_CONSUMPTION = "CoolingConsumption";
    public static final String KEY_PV_ROOF_AREA = "PVRoofArea";
    public static final String KEY_PV_WALL_NORTH_AREA = "PVWallNorthArea";
    public static final String KEY_PV_WALL_SOUTH_AREA = "PVWallSouthArea";
    public static final String KEY_PV_WALL_EAST_AREA = "PVWallEastArea";
    public static final String KEY_PV_WALL_WEST_AREA = "PVWallWestArea";
    public static final String KEY_PV_ROOF_SUPPLY= "PVRoofSupply";
    public static final String KEY_PV_WALL_NORTH_SUPPLY= "PVWallNorthSupply";
    public static final String KEY_PV_WALL_SOUTH_SUPPLY= "PVWallSouthSupply";
    public static final String KEY_PV_WALL_EAST_SUPPLY= "PVWallEastSupply";
    public static final String KEY_PV_WALL_WEST_SUPPLY= "PVWallWestSupply";
    public static final String KEY_TIMES= "times";

    public List<String> TIME_SERIES = Arrays.asList(KEY_GRID_CONSUMPTION,KEY_ELECTRICITY_CONSUMPTION,KEY_HEATING_CONSUMPTION,KEY_COOLING_CONSUMPTION, KEY_PV_ROOF_SUPPLY,KEY_PV_WALL_SOUTH_SUPPLY, KEY_PV_WALL_NORTH_SUPPLY,KEY_PV_WALL_EAST_SUPPLY, KEY_PV_WALL_WEST_SUPPLY);
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
    private String ontoBuiltEnvUri;
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

                    String route = new String();
                    for (int i = 0; i < uriArray.length(); i++) {
                        LinkedHashMap<String,String> tsIris = new LinkedHashMap<>();
                        LinkedHashMap<String,String> scalarIris = new LinkedHashMap<>();

                        String uri = uriArray.getString(i);
                        // Only set routes once - assuming all iris passed have same namespace
                        // Will not be necessary if namespace is passed in request params
                        if(i==0) {
                            route = localRoute.isEmpty() ? getRoute(uri) : localRoute;
                        }
                        String building = checkBuildingInitialised(uri, route);
                        if(building.equals("")){
                            building = initialiseBuilding(uri, route);
                        }
                        if(!checkDataInitialised(uri, building, tsIris, scalarIris, route)) {
                            createTimeSeries(uri,tsIris);
                            initialiseData(uri, i, scalars, building, tsIris, scalarIris, route);
                        }
                        else{
                            updateScalars(uri, route, scalarIris, scalars, i);
                        }
                        addDataToTimeSeries(timeSeries.get(i), times, tsIris);
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
                        // Set default value of 10m if height can not be obtained from knowledge graph
                        // Will only require one height query if height is represented in data consistently
                        String height = getValue(uri, "HeightMeasuredHeigh", route);
                        height = height.length() == 0 ? getValue(uri, "HeightMeasuredHeight", route): height;
                        height = height.length() == 0 ? getValue(uri, "HeightGenAttr", route): height;
                        height = height.length() == 0 ? "10.0" : height;
                        // Get footprint from ground thematic surface or find from surface geometries depending on data
                        String footprint = getValue(uri, "FootprintThematicSurface", route);
                        footprint = footprint.length() == 0 ? getValue(uri, "FootprintSurfaceGeom", route) : footprint;
                        testData.add(new CEAInputData(footprint, height));
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
                    }
                    String building = checkBuildingInitialised(uri, route);
                    if(building.equals("")){
                        return requestParams;
                    }
                    JSONObject data = new JSONObject();
                    List<String> allMeasures = new ArrayList<>();
                    Stream.of(TIME_SERIES, SCALARS).forEach(allMeasures::addAll);
                    for (String measurement: allMeasures) {
                        ArrayList<String> result = getDataIRI(uri, building, measurement, route);
                        if (!result.isEmpty()) {
                            String value;
                            if (TIME_SERIES.contains(measurement)) {
                                value = calculateAnnual(retrieveData(result.get(0)), result.get(0));
                                measurement = "Annual "+ measurement;
                            } else {
                                value = getNumericalValue(uri, result.get(0), route);
                            }
                            // Return non-zero values
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
                timeSeriesList.add(Double.valueOf(timeDataArray.getString(i)));
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
                requestParams.get(KEY_GRID_CONSUMPTION).toString().isEmpty() ||
                requestParams.get(KEY_ELECTRICITY_CONSUMPTION).toString().isEmpty() ||
                requestParams.get(KEY_HEATING_CONSUMPTION).toString().isEmpty() ||
                requestParams.get(KEY_COOLING_CONSUMPTION).toString().isEmpty() ||
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
        timeSeriesUri=config.getString("uri.ontology.ts");
        ontoBuiltEnvUri=config.getString("uri.ontology.ontobuiltenv");
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
     * Creates and initialises a time series using the time series client
     *
     * @param uriString input city object id
     * @param fixedIris map containing time series iris mapped to measurement type
     */
    private void createTimeSeries(String uriString, LinkedHashMap<String,String> fixedIris ) {
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
            for(String measurement: TIME_SERIES){
                String iri = getGraph(uriString,ENERGY_PROFILE)+measurement+"_"+UUID.randomUUID()+ "/";
                iris.add(iri);
                fixedIris.put(measurement, iri);
            }

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
     * gets building uri from input city object uri
     *
     * @param uriString input city object id
     * @return Building uri
     */
    private String getBuildingUri(String uriString) {
        return getGraph(uriString,BUILDING)+getUUID(uriString)+"/";
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
            if(value!="FootprintSurfaceGeom") {
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
     * NB. On data TSDA has run on, the thematic surface is labelled with a ground surface id so this is not required
     * @param results array of building surfaces
     * @return footprint geometry as string
     */
    private String getFootprint(JSONArray results){
        String footprint="";
        ArrayList<String> z_values = new ArrayList<>();
        Double minimum=Double.MAX_VALUE;

        for(Integer i=0; i<results.length(); i++){
            String geom = results.getJSONObject(i).get("FootprintSurfaceGeom").toString();
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
            case "FootprintSurfaceGeom":
                return getGeometryQuerySurfaceGeom(uriString);
            case "FootprintThematicSurface":
                return getGeometryQueryThematicSurface(uriString);
            case "HeightMeasuredHeigh":
                return getHeightQueryMeasuredHeigh(uriString);
            case "HeightMeasuredHeight":
                return getHeightQueryMeasuredHeight(uriString);
            case "HeightGenAttr":
                return getHeightQueryGenAttr(uriString);
            case "CRS":
                return getCrsQuery(uriString);
        }
        return null;
    }

    /**
     * builds a SPARQL query for a specific URI to retrieve a footprint for building linked to surface geometries.
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getGeometryQuerySurfaceGeom(String uriString) {
        try {
            WhereBuilder wb = new WhereBuilder()
                    .addPrefix("ocgml", ocgmlUri)
                    .addWhere("?surf", "ocgml:cityObjectId", "?s")
                    .addWhere("?surf", "ocgml:GeometryType", "?FootprintSurfaceGeom")
                    .addFilter("!isBlank(?FootprintSurfaceGeom)");
            SelectBuilder sb = new SelectBuilder()
                    .addVar("?FootprintSurfaceGeom")
                    .addGraph(NodeFactory.createURI(getGraph(uriString,SURFACE_GEOMETRY)), wb);
            sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(getBuildingUri(uriString)));
            return sb.build();

        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * builds a SPARQL query for a specific URI to retrieve a footprint for building linked to thematic surfaces.
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getGeometryQueryThematicSurface(String uriString) {
        try {
            WhereBuilder wb1 = new WhereBuilder()
                    .addPrefix("ocgml", ocgmlUri)
                    .addWhere("?surf", "ocgml:cityObjectId", "?s")
                    .addWhere("?surf", "ocgml:GeometryType", "?FootprintThematicSurface")
                    .addFilter("!isBlank(?FootprintThematicSurface)");
            WhereBuilder wb2 = new WhereBuilder()
                    .addPrefix("ocgml", ocgmlUri)
                    .addWhere("?s", "ocgml:buildingId", "?building")
                    .addWhere("?s", "ocgml:objectClassId", "?groundSurfId")
                    .addFilter("?groundSurfId = 35"); //Thematic Surface Ids are 33:roof, 34:wall and 35:ground
            SelectBuilder sb = new SelectBuilder()
                    .addVar("?FootprintThematicSurface")
                    .addGraph(NodeFactory.createURI(getGraph(uriString,SURFACE_GEOMETRY)), wb1)
                    .addGraph(NodeFactory.createURI(getGraph(uriString,THEMATIC_SURFACE)), wb2);
            sb.setVar( Var.alloc( "building" ), NodeFactory.createURI(getBuildingUri(uriString)));
            return sb.build();

        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * builds a SPARQL query for a specific URI to retrieve the building height for data with ocgml:measuredHeight attribute
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getHeightQueryMeasuredHeight(String uriString) {
        WhereBuilder wb = new WhereBuilder();
        SelectBuilder sb = new SelectBuilder();

        wb.addPrefix("ocgml", ocgmlUri)
                .addWhere("?s", "ocgml:measuredHeight", "?HeightMeasuredHeight");
        sb.addVar("?HeightMeasuredHeight")
                .addGraph(NodeFactory.createURI(getGraph(uriString,BUILDING)), wb);
        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(getBuildingUri(uriString)));

        return sb.build();
    }

    /**
     * builds a SPARQL query for a specific URI to retrieve the building height for data with ocgml:measuredHeigh attribute
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getHeightQueryMeasuredHeigh(String uriString) {
        WhereBuilder wb = new WhereBuilder();
        SelectBuilder sb = new SelectBuilder();
        wb.addPrefix("ocgml", ocgmlUri)
                .addWhere("?s", "ocgml:measuredHeigh", "?HeightMeasuredHeigh");
        sb.addVar("?HeightMeasuredHeigh")
                .addGraph(NodeFactory.createURI(getGraph(uriString,BUILDING)), wb);
        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(getBuildingUri(uriString)));

        return sb.build();
    }

    /**
     * builds a SPARQL query for a specific URI to retrieve the building height for data with generic attribute with ocgml:attrName 'height'
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getHeightQueryGenAttr(String uriString) {
        WhereBuilder wb = new WhereBuilder();
        SelectBuilder sb = new SelectBuilder();

        wb.addPrefix("ocgml", ocgmlUri)
                .addWhere("?o", "ocgml:attrName", "height")
                .addWhere("?o", "ocgml:realVal", "?HeightGenAttr")
                .addWhere("?o", "ocgml:cityObjectId", "?s");
        sb.addVar("?HeightGenAttr")
                .addGraph(NodeFactory.createURI(getGraph(uriString,CITY_OBJECT_GEN_ATT)), wb);
        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(uriString));

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
     * Add Where for Building Consumption
     * @param builder update builder
     * @param type energy type in ontology
     */
    public void addBuildingConsumptionWhere(WhereBuilder builder, String type){
        builder.addWhere("?building", "purlEnaeq:consumesEnergy", "?grid")
                .addWhere("?grid", "rdf:type", type)
                .addWhere("?grid", "om:hasValue", "?measure")
                .addWhere("?measure", "om:hasUnit", "?unit");
    }

    /**
     * Add Where for Device Consumption
     * @param builder update builder
     * @param system type of device
     */
    public void addConsumptionDeviceWhere(WhereBuilder builder, String system){
        builder.addWhere("?building", "ontoubemmp:hasDevice", "?device")
                .addWhere("?device", "rdf:type", system)
                .addWhere("?device", "purlEnaeq:consumesEnergy", "?energy")
                .addWhere("?energy", "rdf:type", "ontoubemmp:ThermalConsumption")
                .addWhere("?energy", "om:hasValue", "?measure")
                .addWhere("?measure", "om:hasUnit", "?unit");
    }

    /**
     * Add Where for Device Supply
     * @param builder update builder
     * @param panelType type of panels
     */
    public void addSupplyDeviceWhere(WhereBuilder builder, String panelType){
        builder.addWhere("?building", "ontoubemmp:hasDevice", "?PVPanels")
                .addWhere("?PVPanels", "rdf:type", panelType)
                .addWhere("?PVPanels", "thinkhome:producesEnergy", "?supply")
                .addWhere("?supply", "rdf:type", "ontoubemmp:ElectricitySupply")
                .addWhere("?supply", "om:hasValue", "?measure")
                .addWhere("?measure", "om:hasUnit", "?unit");
    }

    /**
     * Add Where for Device Area
     * @param builder update builder
     * @param panelType type of panels
     */
    public void addSupplyDeviceAreaWhere(WhereBuilder builder, String panelType){
        builder.addWhere("?building", "ontoubemmp:hasDevice", "?PVPanels")
                .addWhere("?PVPanels", "rdf:type", panelType)
                .addWhere("?PVPanels", "ontoubemmp:hasArea", "?area")
                .addWhere("?area", "rdf:type", "ontoubemmp:PVPanelsArea")
                .addWhere("?area", "om:hasValue", "?measure")
                .addWhere("?measure", "om:hasNumericalValue", "?value")
                .addWhere("?measure", "om:hasUnit", "?unit");
    }

    /**
     * Retrieves iris from KG for the data type requested
     * @param uriString city object id
     * @param building uri of building in energyprofile graph
     * @param value type of data from TIME_SERIES or SCALARS
     * @param route route to pass to access agent
     * @return list of iris
     */
    public ArrayList<String> getDataIRI(String uriString, String building, String value, String route) {
        ArrayList<String> result = new ArrayList<>();

        SelectBuilder sb = new SelectBuilder();
        WhereBuilder wb = new WhereBuilder();

        if(building.equals("")) {
            return result;
        }

        wb.addPrefix("ocgml", ocgmlUri)
                .addPrefix("rdf", rdfUri)
                .addPrefix("om", unitOntologyUri)
                .addPrefix("purlEnaeq", purlEnaeqUri)
                .addPrefix("ontoubemmp", ontoUBEMMPUri)
                .addPrefix("thinkhome", thinkhomeUri);

        switch(value) {
            case KEY_GRID_CONSUMPTION:
                addBuildingConsumptionWhere(wb,"ontoubemmp:GridConsumption");
                break;
            case KEY_ELECTRICITY_CONSUMPTION:
                addBuildingConsumptionWhere(wb,"ontoubemmp:ElectricityConsumption");
                break;
            case KEY_HEATING_CONSUMPTION:
                addConsumptionDeviceWhere(wb, "purlEnaeq:HeatingSystem");
                break;
            case KEY_COOLING_CONSUMPTION:
                addConsumptionDeviceWhere(wb, "ontoubemmp:CoolingSystem");
                break;
            case KEY_PV_ROOF_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:RoofPVPanels");
                break;
            case KEY_PV_WALL_SOUTH_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:SouthWallPVPanels");
                break;
            case KEY_PV_WALL_NORTH_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:NorthWallPVPanels");
                break;
            case KEY_PV_WALL_EAST_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:EastWallPVPanels");
                break;
            case KEY_PV_WALL_WEST_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:WestWallPVPanels");
                break;
            case KEY_PV_ROOF_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:RoofPVPanels");
                break;
            case KEY_PV_WALL_SOUTH_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:SouthWallPVPanels");
                break;
            case KEY_PV_WALL_NORTH_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:NorthWallPVPanels");
                break;
            case KEY_PV_WALL_EAST_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:EastWallPVPanels");
                break;
            case KEY_PV_WALL_WEST_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:WestWallPVPanels");
                break;
            default:
                return result;

        }

        sb.addVar("?measure")
                .addVar("?unit")
                .addGraph(NodeFactory.createURI(getGraph(uriString,ENERGY_PROFILE)), wb);

        sb.setVar( Var.alloc( "building" ), NodeFactory.createURI(building));

        JSONArray queryResultArray = new JSONArray(this.queryStore(route, sb.build().toString()));

        if(!queryResultArray.isEmpty()){
            result.add(queryResultArray.getJSONObject(0).get("measure").toString());
            result.add(queryResultArray.getJSONObject(0).get("unit").toString());
        }
        return result;

    }

    /**
     * Gets numerical value of specified measurement
     * @param uriString city object id
     * @param measureUri Uri of the measurement with numerical value in KG
     * @param route route to pass to access agent
     * @return list of iris
     */
    public String getNumericalValue(String uriString, String measureUri, String route) {
        String result = "";

        WhereBuilder wb = new WhereBuilder().addPrefix("om", unitOntologyUri)
            .addWhere("?measure", "om:hasNumericalValue", "?value");

        SelectBuilder sb = new SelectBuilder().addVar("?value")
                .addGraph(NodeFactory.createURI(getGraph(uriString,ENERGY_PROFILE)), wb);

        sb.setVar( Var.alloc( "measure" ), NodeFactory.createURI(measureUri));

        JSONArray queryResultArray = new JSONArray(this.queryStore(route, sb.build().toString()));

        if(!queryResultArray.isEmpty()){
            result = queryResultArray.getJSONObject(0).get("value").toString();
        }
        return result;
    }

    /**
     * Check building linked to ontoCityGML is initialised in KG
     * @param uriString city object id
     * @param route route to pass to access agent
     * @return building
     */
    public String checkBuildingInitialised(String uriString, String route){
        WhereBuilder wb = new WhereBuilder();
        SelectBuilder sb = new SelectBuilder();

        wb.addPrefix("ontoBuiltEnv", ontoBuiltEnvUri)
                .addWhere("?building", "ontoBuiltEnv:hasOntoCityGMLRepresentation", "?s");

        sb.addVar("?building").addGraph(NodeFactory.createURI(getGraph(uriString,ENERGY_PROFILE)), wb);

        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(getBuildingUri(uriString)));

        JSONArray queryResultArray = new JSONArray(this.queryStore(route, sb.build().toString()));
        String building = "";
        if(!queryResultArray.isEmpty()){
            building = queryResultArray.getJSONObject(0).get("building").toString();
        }
        return building;
    }

    /**
     * Initialise building in KG and link to ontoCityGMLRepresentation
     * @param uriString city object id
     * @param route route to pass to access agent
     * @return building
     */
    public String initialiseBuilding(String uriString, String route){

        String outputGraphUri = getGraph(uriString,ENERGY_PROFILE);

        String buildingUri = outputGraphUri + "Building_" + UUID.randomUUID() + "/";

        UpdateBuilder ub =
                new UpdateBuilder()
                        .addPrefix("rdf", rdfUri)
                        .addPrefix("owl", owlUri)
                        .addPrefix("purlInf", purlInfrastructureUri)
                        .addPrefix("ontoBuiltEnv", ontoBuiltEnvUri)
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "rdf:type", "purlInf:Building")
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "ontoBuiltEnv:hasOntoCityGMLRepresentation", NodeFactory.createURI(getBuildingUri(uriString)));

        ub.setVar(Var.alloc("graph"), NodeFactory.createURI(outputGraphUri));

        UpdateRequest ur = ub.buildRequest();

        //Use access agent
        this.updateStore(route, ur.toString());

        return buildingUri;
    }

    /**
     * Check if energy profile data already exist in KG and get IRIs if they do
     * @param uriString city object uri
     * @param building building uri in energy profile graph
     * @param tsIris map of time series iris to data types
     * @param scalarIris map of iris in kg to data type
     * @param route route to pass to access agent
     * @return if time series are initialised
     */
    public Boolean checkDataInitialised(String uriString, String building, LinkedHashMap<String,String> tsIris, LinkedHashMap<String,String> scalarIris, String route){
        ArrayList<String> result;
        List<String> allMeasures = new ArrayList<>();
        Stream.of(TIME_SERIES, SCALARS).forEach(allMeasures::addAll);
        for (String measurement: allMeasures) {
            result = getDataIRI(uriString, building, measurement, route);
            if (!result.isEmpty()) {
                if (TIME_SERIES.contains(measurement)) {
                    tsIris.put(measurement, result.get(0));
                } else {
                    scalarIris.put(measurement, result.get(0));
                }
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Create update for energy consumption
     * @param builder update builder
     * @param consumer iri of building/device
     * @param consumptionType type in ontology
     * @param quantity om:quantity iri
     * @param measure om:measure iri
     */
    public void createConsumptionUpdate(UpdateBuilder builder, String consumer, String consumptionType, String quantity, String measure){
        builder.addInsert("?graph", NodeFactory.createURI(quantity), "rdf:type", consumptionType)
                .addInsert("?graph", NodeFactory.createURI(quantity), "rdf:type", "owl:NamedIndividual")
                .addInsert("?graph", NodeFactory.createURI(quantity), "om:hasDimension", "om:energy-Dimension")
                .addInsert("?graph", NodeFactory.createURI(quantity), "om:hasValue", NodeFactory.createURI(measure))
                .addInsert("?graph", NodeFactory.createURI(measure), "rdf:type", "om:Measure")
                .addInsert("?graph", NodeFactory.createURI(measure), "rdf:type", "owl:NamedIndividual")
                .addInsert("?graph", NodeFactory.createURI(measure), "om:hasUnit", "om:kilowattHour")
                .addInsert("?graph", NodeFactory.createURI(consumer), "purlEnaeq:consumesEnergy",NodeFactory.createURI(quantity));
    }

    /**
     * Create update for device energy consumption
     * @param builder update builder
     * @param building iri of building
     * @param device iri of device
     * @param deviceType device type in ontology
     * @param consumptionType onsumption type in ontology
     * @param quantity om:quantity iri
     * @param measure om:measure iri
     */
    public void createDeviceConsumptionUpdate(UpdateBuilder builder, String building, String device, String deviceType, String consumptionType, String quantity, String measure){
          builder.addInsert("?graph", NodeFactory.createURI(building), "ontoubemmp:hasDevice", NodeFactory.createURI(device))
                .addInsert("?graph", NodeFactory.createURI(device), "rdf:type", deviceType)
                .addInsert("?graph", NodeFactory.createURI(device), "rdf:type", "owl:NamedIndividual");
          createConsumptionUpdate(builder, device, consumptionType, quantity, measure);
    }

    /**
     * Create update for PV Panel Supply
     * @param builder update builder
     * @param PVPanels iri of PV panels
     * @param quantity om:quantity iri
     * @param measure om:measure iri
     */
    public void createPVPanelSupplyUpdate(UpdateBuilder builder, String PVPanels, String quantity, String measure){
        builder.addInsert("?graph", NodeFactory.createURI(PVPanels), "thinkhome:producesEnergy", NodeFactory.createURI(quantity))
                .addInsert("?graph", NodeFactory.createURI(quantity), "rdf:type", "ontoubemmp:ElectricitySupply")
                .addInsert("?graph", NodeFactory.createURI(quantity), "rdf:type", "owl:NamedIndividual")
                .addInsert("?graph", NodeFactory.createURI(quantity), "om:hasDimension", "om:energy-Dimension")
                .addInsert("?graph", NodeFactory.createURI(quantity), "om:hasValue", NodeFactory.createURI(measure))
                .addInsert("?graph", NodeFactory.createURI(measure), "rdf:type", "om:Measure")
                .addInsert("?graph", NodeFactory.createURI(measure), "rdf:type", "owl:NamedIndividual")
                .addInsert("?graph", NodeFactory.createURI(measure), "om:hasUnit", "om:kilowattHour");
    }

    /**
     * Create update for PV Panel areas
     * @param builder update builder
     * @param building iri of building
     * @param PVPanels iri of PV panels
     * @param panelType panel type in ontology
     * @param quantity om:quantity iri
     * @param measure om:measure iri
     * @param value numerical value
     */
    public void createPVPanelAreaUpdate(UpdateBuilder builder, String building, String PVPanels, String panelType, String quantity, String measure, String value){
        builder.addInsert("?graph", NodeFactory.createURI(building), "ontoubemmp:hasDevice", NodeFactory.createURI(PVPanels))
                .addInsert("?graph", NodeFactory.createURI(PVPanels), "rdf:type", panelType)
                .addInsert("?graph", NodeFactory.createURI(PVPanels), "rdf:type", "owl:NamedIndividual")
                .addInsert("?graph", NodeFactory.createURI(PVPanels), "ontoubemmp:hasArea", NodeFactory.createURI(quantity))
                .addInsert("?graph", NodeFactory.createURI(quantity), "rdf:type", "ontoubemmp:PVPanelsArea")
                .addInsert("?graph", NodeFactory.createURI(quantity), "rdf:type", "owl:NamedIndividual")
                .addInsert("?graph", NodeFactory.createURI(quantity), "om:hasDimension", "om:area-Dimension")
                .addInsert("?graph", NodeFactory.createURI(quantity), "om:hasValue", NodeFactory.createURI(measure))
                .addInsert("?graph", NodeFactory.createURI(measure), "rdf:type", "owl:NamedIndividual")
                .addInsert("?graph", NodeFactory.createURI(measure), "rdf:type", "om:Measure")
                .addInsert("?graph", NodeFactory.createURI(measure), "om:hasNumericalValue", value)
                .addInsert("?graph", NodeFactory.createURI(measure), "om:hasUnit", "om:squareMetre");
    }

    /**
     * Initialise energy profile data in KG
     * @param uriString city object uri
     * @param uriCounter keep track of uris
     * @param scalars map of scalar measurements
     * @param buildingUri building uri
     * @param tsIris map of time series iris to data types
     * @param scalarIris map of iris in kg to data types
     * @param route route to pass to access agent
     */
    public void initialiseData(String uriString, Integer uriCounter, LinkedHashMap<String, List<String>> scalars, String buildingUri, LinkedHashMap<String,String> tsIris, LinkedHashMap<String,String> scalarIris, String route){

        UpdateBuilder ub =
                new UpdateBuilder()
                        .addPrefix("ontoubemmp", ontoUBEMMPUri)
                        .addPrefix("rdf", rdfUri)
                        .addPrefix("owl", owlUri)
                        .addPrefix("purlEnaeq", purlEnaeqUri)
                        .addPrefix("om", unitOntologyUri)
                        .addPrefix("thinkhome", thinkhomeUri)
                        .addPrefix("purlInf", purlInfrastructureUri);

        String outputGraphUri = getGraph(uriString,ENERGY_PROFILE);

        //Device uris
        String heatingUri = outputGraphUri + "HeatingSystem_" + UUID.randomUUID() + "/";
        String coolingUri = outputGraphUri + "CoolingSystem_" + UUID.randomUUID() + "/";
        String pvRoofPanelsUri = outputGraphUri + "PVRoofPanels_" + UUID.randomUUID() + "/";
        String pvWallSouthPanelsUri = outputGraphUri + "PVWallSouthPanels_" + UUID.randomUUID() + "/";
        String pvWallNorthPanelsUri = outputGraphUri + "PVWallNorthPanels_" + UUID.randomUUID() + "/";
        String pvWallEastPanelsUri = outputGraphUri + "PVWallEastPanels_" + UUID.randomUUID() + "/";
        String pvWallWestPanelsUri = outputGraphUri + "PVWallWestPanels_" + UUID.randomUUID() + "/";

        // save om:measure uris for scalars and create om:quantity uris for scalars and time series
        // (time series om:measure iris already created in createTimeSeries)
        for (String measurement: SCALARS) {
            String measure = outputGraphUri + measurement+"Value_" + UUID.randomUUID() + "/";
            scalarIris.put(measurement, measure);
            String quantity = outputGraphUri + measurement+"Quantity_" + UUID.randomUUID() + "/";
            switch(measurement){
                case(KEY_PV_ROOF_AREA):
                    createPVPanelAreaUpdate(ub, buildingUri, pvRoofPanelsUri, "ontoubemmp:RoofPVPanels", quantity, measure, scalars.get(KEY_PV_ROOF_AREA).get(uriCounter));
                    break;
                case(KEY_PV_WALL_SOUTH_AREA):
                    createPVPanelAreaUpdate(ub, buildingUri, pvWallSouthPanelsUri, "ontoubemmp:SouthWallPVPanels", quantity, measure, scalars.get(KEY_PV_WALL_SOUTH_AREA).get(uriCounter));
                    break;
                case(KEY_PV_WALL_NORTH_AREA):
                    createPVPanelAreaUpdate(ub, buildingUri, pvWallNorthPanelsUri, "ontoubemmp:NorthWallPVPanels", quantity, measure, scalars.get(KEY_PV_WALL_NORTH_AREA).get(uriCounter));
                    break;
                case(KEY_PV_WALL_EAST_AREA):
                    createPVPanelAreaUpdate(ub, buildingUri, pvWallEastPanelsUri, "ontoubemmp:EastWallPVPanels", quantity, measure, scalars.get(KEY_PV_WALL_EAST_AREA).get(uriCounter));
                    break;
                case(KEY_PV_WALL_WEST_AREA):
                    createPVPanelAreaUpdate(ub, buildingUri, pvWallWestPanelsUri, "ontoubemmp:WestWallPVPanels", quantity, measure, scalars.get(KEY_PV_WALL_WEST_AREA).get(uriCounter));
                    break;
            }
        }

        for (String measurement: TIME_SERIES) {
            String quantity = outputGraphUri + measurement + "Quantity_" + UUID.randomUUID() + "/";
            if (measurement.equals(KEY_GRID_CONSUMPTION) || measurement.equals(KEY_ELECTRICITY_CONSUMPTION)) {
                createConsumptionUpdate(ub, buildingUri, "ontoubemmp:" + measurement, quantity, tsIris.get(measurement));
            }
            else if (measurement.equals(KEY_COOLING_CONSUMPTION)) {
                createDeviceConsumptionUpdate(ub, buildingUri, coolingUri, "ontoubemmp:CoolingSystem","ontoubemmp:ThermalConsumption" , quantity, tsIris.get(measurement));
            }
            else if (measurement.equals(KEY_HEATING_CONSUMPTION)) {
                createDeviceConsumptionUpdate(ub, buildingUri, heatingUri,"purlEnaeq:HeatingSystem","ontoubemmp:ThermalConsumption" , quantity, tsIris.get(measurement));
            }
            else if (measurement.equals(KEY_PV_ROOF_SUPPLY)){
                createPVPanelSupplyUpdate(ub, pvRoofPanelsUri, quantity, tsIris.get(measurement));
            }
            else if (measurement.equals(KEY_PV_WALL_SOUTH_SUPPLY)){
                createPVPanelSupplyUpdate(ub, pvWallSouthPanelsUri, quantity, tsIris.get(measurement));
            }
            else if (measurement.equals(KEY_PV_WALL_NORTH_SUPPLY)){
                createPVPanelSupplyUpdate(ub, pvWallNorthPanelsUri, quantity, tsIris.get(measurement));
            }
            else if (measurement.equals(KEY_PV_WALL_EAST_SUPPLY)){
                createPVPanelSupplyUpdate(ub, pvWallEastPanelsUri, quantity, tsIris.get(measurement));
            }
            else if (measurement.equals(KEY_PV_WALL_WEST_SUPPLY)){
                createPVPanelSupplyUpdate(ub, pvWallWestPanelsUri, quantity, tsIris.get(measurement));
            }

        }

        ub.setVar(Var.alloc("graph"), NodeFactory.createURI(outputGraphUri));

        UpdateRequest ur = ub.buildRequest();

        //Use access agent
        this.updateStore(route, ur.toString());
    }

    /**
     * Update numerical value of scalars in KG
     * @param scalars map of scalar measurements
     * @param scalarIris map of iris in kg to data types
     * @param route route to pass to access agent
     * @param uriCounter keep track of uris
     */
    public void updateScalars(String uriString, String route, LinkedHashMap<String,String> scalarIris, LinkedHashMap<String, List<String>> scalars, Integer uriCounter) {

        for (String measurement: SCALARS) {
            UpdateBuilder ub1 = new UpdateBuilder().addPrefix("om", unitOntologyUri)
                    .addDelete("?graph", NodeFactory.createURI(scalarIris.get(measurement)), "om:hasNumericalValue", "?s")
                    .addWhere(NodeFactory.createURI(scalarIris.get(measurement)), "om:hasNumericalValue", "?s");
            ub1.setVar(Var.alloc("graph"), NodeFactory.createURI(getGraph(uriString,ENERGY_PROFILE)));

            UpdateBuilder ub2 = new UpdateBuilder().addPrefix("om", unitOntologyUri)
                    .addInsert("?graph", NodeFactory.createURI(scalarIris.get(measurement)), "om:hasNumericalValue", scalars.get(measurement).get(uriCounter));
            ub2.setVar(Var.alloc("graph"), NodeFactory.createURI(getGraph(uriString,ENERGY_PROFILE)));

            UpdateRequest ur1 = ub1.buildRequest();
            UpdateRequest ur2 = ub2.buildRequest();

            //Use access agent
            this.updateStore(route, ur1.toString());
            this.updateStore(route, ur2.toString());
        }

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