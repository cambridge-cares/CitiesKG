package uk.ac.cam.cares.twa.cities.ceaagent;

import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementService;
import org.jooq.exception.DataAccessException;
import org.locationtech.jts.geom.util.GeometryFixer;
import uk.ac.cam.cares.jps.base.config.JPSConstants;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.timeseries.TimeSeries;
import uk.ac.cam.cares.jps.base.timeseries.TimeSeriesClient;
import uk.ac.cam.cares.jps.base.query.RemoteRDBStoreClient;
import uk.ac.cam.cares.jps.base.query.RemoteStoreClient;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.arq.querybuilder.Order;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.json.JSONArray;

import org.locationtech.jts.geom.*;

import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;

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
    public static final String KEY_GEOMETRY = "geometryEndpoint";
    public static final String KEY_USAGE = "usageEndpoint";
    public static final String KEY_CEA = "ceaEndpoint";
    public static final String KEY_GRAPH = "graphName";
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
    public static final String KEY_PV_WALL_NORTH_SUPPLY = "PVWallNorthSupply";
    public static final String KEY_PV_WALL_SOUTH_SUPPLY = "PVWallSouthSupply";
    public static final String KEY_PV_WALL_EAST_SUPPLY = "PVWallEastSupply";
    public static final String KEY_PV_WALL_WEST_SUPPLY = "PVWallWestSupply";
    public static final String KEY_PVT_PLATE_ROOF_AREA = "PVTPlateRoofArea";
    public static final String KEY_PVT_PLATE_WALL_NORTH_AREA = "PVTPlateWallNorthArea";
    public static final String KEY_PVT_PLATE_WALL_SOUTH_AREA = "PVTPlateWallSouthArea";
    public static final String KEY_PVT_PLATE_WALL_EAST_AREA = "PVTPlateWallEastArea";
    public static final String KEY_PVT_PLATE_WALL_WEST_AREA = "PVTPlateWallWestArea";
    public static final String KEY_PVT_PLATE_ROOF_E_SUPPLY = "PVTPlateRoofESupply";
    public static final String KEY_PVT_PLATE_WALL_NORTH_E_SUPPLY = "PVTPlateWallNorthESupply";
    public static final String KEY_PVT_PLATE_WALL_SOUTH_E_SUPPLY = "PVTPlateWallSouthESupply";
    public static final String KEY_PVT_PLATE_WALL_EAST_E_SUPPLY = "PVTPlateWallEastESupply";
    public static final String KEY_PVT_PLATE_WALL_WEST_E_SUPPLY = "PVTPlateWallWestESupply";
    public static final String KEY_PVT_PLATE_ROOF_Q_SUPPLY = "PVTPlateRoofQSupply";
    public static final String KEY_PVT_PLATE_WALL_NORTH_Q_SUPPLY = "PVTPlateWallNorthQSupply";
    public static final String KEY_PVT_PLATE_WALL_SOUTH_Q_SUPPLY = "PVTPlateWallSouthQSupply";
    public static final String KEY_PVT_PLATE_WALL_EAST_Q_SUPPLY = "PVTPlateWallEastQSupply";
    public static final String KEY_PVT_PLATE_WALL_WEST_Q_SUPPLY = "PVTPlateWallWestQSupply";
    public static final String KEY_PVT_TUBE_ROOF_AREA = "PVTTubeRoofArea";
    public static final String KEY_PVT_TUBE_WALL_NORTH_AREA = "PVTTubeWallNorthArea";
    public static final String KEY_PVT_TUBE_WALL_SOUTH_AREA = "PVTTubeWallSouthArea";
    public static final String KEY_PVT_TUBE_WALL_EAST_AREA = "PVTTubeWallEastArea";
    public static final String KEY_PVT_TUBE_WALL_WEST_AREA = "PVTTubeWallWestArea";
    public static final String KEY_PVT_TUBE_ROOF_E_SUPPLY = "PVTTubeRoofESupply";
    public static final String KEY_PVT_TUBE_WALL_NORTH_E_SUPPLY = "PVTTubeWallNorthESupply";
    public static final String KEY_PVT_TUBE_WALL_SOUTH_E_SUPPLY = "PVTTubeWallSouthESupply";
    public static final String KEY_PVT_TUBE_WALL_EAST_E_SUPPLY = "PVTTubeWallEastESupply";
    public static final String KEY_PVT_TUBE_WALL_WEST_E_SUPPLY = "PVTTubeWallWestESupply";
    public static final String KEY_PVT_TUBE_ROOF_Q_SUPPLY = "PVTTubeRoofQSupply";
    public static final String KEY_PVT_TUBE_WALL_NORTH_Q_SUPPLY = "PVTTubeWallNorthQSupply";
    public static final String KEY_PVT_TUBE_WALL_SOUTH_Q_SUPPLY = "PVTTubeWallSouthQSupply";
    public static final String KEY_PVT_TUBE_WALL_EAST_Q_SUPPLY = "PVTTubeWallEastQSupply";
    public static final String KEY_PVT_TUBE_WALL_WEST_Q_SUPPLY = "PVTTubeWallWestQSupply";
    public static final String KEY_THERMAL_PLATE_ROOF_AREA = "ThermalPlateRoofArea";
    public static final String KEY_THERMAL_PLATE_WALL_NORTH_AREA = "ThermalPlateWallNorthArea";
    public static final String KEY_THERMAL_PLATE_WALL_SOUTH_AREA = "ThermalPlateWallSouthArea";
    public static final String KEY_THERMAL_PLATE_WALL_EAST_AREA = "ThermalPlateWallEastArea";
    public static final String KEY_THERMAL_PLATE_WALL_WEST_AREA = "ThermalPlateWallWestArea";
    public static final String KEY_THERMAL_PLATE_ROOF_SUPPLY= "ThermalPlateRoofSupply";
    public static final String KEY_THERMAL_PLATE_WALL_NORTH_SUPPLY = "ThermalPlateWallNorthSupply";
    public static final String KEY_THERMAL_PLATE_WALL_SOUTH_SUPPLY = "ThermalPlateWallSouthSupply";
    public static final String KEY_THERMAL_PLATE_WALL_EAST_SUPPLY = "ThermalPlateWallEastSupply";
    public static final String KEY_THERMAL_PLATE_WALL_WEST_SUPPLY = "ThermalPlateWallWestSupply";
    public static final String KEY_THERMAL_TUBE_ROOF_AREA = "ThermalTubeRoofArea";
    public static final String KEY_THERMAL_TUBE_WALL_NORTH_AREA = "ThermalTubeWallNorthArea";
    public static final String KEY_THERMAL_TUBE_WALL_SOUTH_AREA = "ThermalTubeWallSouthArea";
    public static final String KEY_THERMAL_TUBE_WALL_EAST_AREA = "ThermalTubeWallEastArea";
    public static final String KEY_THERMAL_TUBE_WALL_WEST_AREA = "ThermalTubeWallWestArea";
    public static final String KEY_THERMAL_TUBE_ROOF_SUPPLY= "ThermalTubeRoofSupply";
    public static final String KEY_THERMAL_TUBE_WALL_NORTH_SUPPLY = "ThermalTubeWallNorthSupply";
    public static final String KEY_THERMAL_TUBE_WALL_SOUTH_SUPPLY = "ThermalTubeWallSouthSupply";
    public static final String KEY_THERMAL_TUBE_WALL_EAST_SUPPLY = "ThermalTubeWallEastSupply";
    public static final String KEY_THERMAL_TUBE_WALL_WEST_SUPPLY = "ThermalTubeWallWestSupply";
    public static final String KEY_TIMES = "times";
    public static final String CEA_OUTPUTS = "ceaOutputs";
    public String customDataType = "<http://localhost/blazegraph/literals/POLYGON-3-15>";
    public String customField = "X0#Y0#Z0#X1#Y1#Z1#X2#Y2#Z2#X3#Y3#Z3#X4#Y4#Z4";

    public List<String> TIME_SERIES = Arrays.asList(KEY_GRID_CONSUMPTION,KEY_ELECTRICITY_CONSUMPTION,KEY_HEATING_CONSUMPTION,KEY_COOLING_CONSUMPTION, KEY_PV_ROOF_SUPPLY, KEY_PV_WALL_NORTH_SUPPLY, KEY_PV_WALL_SOUTH_SUPPLY, KEY_PV_WALL_EAST_SUPPLY, KEY_PV_WALL_WEST_SUPPLY, KEY_PVT_PLATE_ROOF_E_SUPPLY, KEY_PVT_PLATE_WALL_NORTH_E_SUPPLY, KEY_PVT_PLATE_WALL_SOUTH_E_SUPPLY, KEY_PVT_PLATE_WALL_EAST_E_SUPPLY, KEY_PVT_PLATE_WALL_WEST_E_SUPPLY, KEY_PVT_PLATE_ROOF_Q_SUPPLY, KEY_PVT_PLATE_WALL_NORTH_Q_SUPPLY, KEY_PVT_PLATE_WALL_SOUTH_Q_SUPPLY, KEY_PVT_PLATE_WALL_EAST_Q_SUPPLY, KEY_PVT_PLATE_WALL_WEST_Q_SUPPLY, KEY_PVT_TUBE_ROOF_E_SUPPLY, KEY_PVT_TUBE_WALL_NORTH_E_SUPPLY, KEY_PVT_TUBE_WALL_SOUTH_E_SUPPLY, KEY_PVT_TUBE_WALL_EAST_E_SUPPLY, KEY_PVT_TUBE_WALL_WEST_E_SUPPLY, KEY_PVT_TUBE_ROOF_Q_SUPPLY, KEY_PVT_TUBE_WALL_NORTH_Q_SUPPLY, KEY_PVT_TUBE_WALL_SOUTH_Q_SUPPLY, KEY_PVT_TUBE_WALL_EAST_Q_SUPPLY, KEY_PVT_TUBE_WALL_WEST_Q_SUPPLY, KEY_THERMAL_PLATE_ROOF_SUPPLY, KEY_THERMAL_PLATE_WALL_NORTH_SUPPLY, KEY_THERMAL_PLATE_WALL_SOUTH_SUPPLY, KEY_THERMAL_PLATE_WALL_EAST_SUPPLY, KEY_THERMAL_PLATE_WALL_WEST_SUPPLY, KEY_THERMAL_TUBE_ROOF_SUPPLY, KEY_THERMAL_TUBE_WALL_NORTH_SUPPLY, KEY_THERMAL_TUBE_WALL_SOUTH_SUPPLY, KEY_THERMAL_TUBE_WALL_EAST_SUPPLY, KEY_THERMAL_TUBE_WALL_WEST_SUPPLY);
    public List<String> SCALARS = Arrays.asList(KEY_PV_ROOF_AREA, KEY_PV_WALL_NORTH_AREA, KEY_PV_WALL_SOUTH_AREA, KEY_PV_WALL_EAST_AREA, KEY_PV_WALL_WEST_AREA, KEY_PVT_PLATE_ROOF_AREA, KEY_PVT_PLATE_WALL_NORTH_AREA, KEY_PVT_PLATE_WALL_SOUTH_AREA, KEY_PVT_PLATE_WALL_EAST_AREA, KEY_PVT_PLATE_WALL_WEST_AREA, KEY_PVT_TUBE_ROOF_AREA, KEY_PVT_TUBE_WALL_NORTH_AREA, KEY_PVT_TUBE_WALL_SOUTH_AREA, KEY_PVT_TUBE_WALL_EAST_AREA, KEY_PVT_TUBE_WALL_WEST_AREA, KEY_THERMAL_PLATE_ROOF_AREA, KEY_THERMAL_PLATE_WALL_NORTH_AREA, KEY_THERMAL_PLATE_WALL_SOUTH_AREA, KEY_THERMAL_PLATE_WALL_EAST_AREA, KEY_THERMAL_PLATE_WALL_WEST_AREA, KEY_THERMAL_TUBE_ROOF_AREA, KEY_THERMAL_TUBE_WALL_NORTH_AREA, KEY_THERMAL_TUBE_WALL_SOUTH_AREA, KEY_THERMAL_TUBE_WALL_EAST_AREA, KEY_THERMAL_TUBE_WALL_WEST_AREA);

    public final int NUM_CEA_THREADS = 1;
    private final ThreadPoolExecutor CEAExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_CEA_THREADS);

    private static final String TIME_SERIES_CLIENT_PROPS = "timeseriesclient.properties";
    private TimeSeriesClient<OffsetDateTime> tsClient;
    public static final String timeUnit = OffsetDateTime.class.getSimpleName();
    private static final String FS = System.getProperty("file.separator");
    private RemoteRDBStoreClient rdbStoreClient;
    private RemoteStoreClient storeClient;
    // Variables fetched from CEAAgentConfig.properties file.
    private String ocgmlUri;
    private String ontoUBEMMPUri;
    private String rdfUri;
    private String owlUri;
    private String purlEnaeqUri;
    private String purlInfrastructureUri;
    private String thinkhomeUri;
    private String ontoBuiltEnvUri;
    private String geoUri;
    private static String unitOntologyUri;
    private String requestUrl;
    private String targetUrl;
    private String geometryRoute;
    private String usageRoute;
    private String ceaRoute;
    private String namedGraph;

    private Map<String, String> accessAgentRoutes = new HashMap<>();

    public CEAAgent() {
        readConfig();
    }

    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {
        if (validateInput(requestParams)) {
            setRDBClient(getTimeSeriesPropsPath());

            requestUrl = requestParams.getString(KEY_REQ_URL);
            String uriArrayString = requestParams.get(KEY_IRI).toString();
            JSONArray uriArray = new JSONArray(uriArrayString);

            if (requestUrl.contains(URI_UPDATE) || requestUrl.contains(URI_ACTION)) {
                targetUrl = requestUrl.replace(URI_ACTION, URI_UPDATE);

                if (isDockerized()) {
                    targetUrl = targetUrl.replace("localhost", "host.docker.internal");
                }

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

                    for (int i = 0; i < uriArray.length(); i++) {
                        LinkedHashMap<String,String> tsIris = new LinkedHashMap<>();
                        LinkedHashMap<String,String> scalarIris = new LinkedHashMap<>();

                        String uri = uriArray.getString(i);

                        String building = checkBuildingInitialised(uri, ceaRoute);
                        if(building.equals("")){
                            // Check if DABGEO:Building IRI has already been created in another endpoint
                            building = checkBuildingInitialised(uri, geometryRoute);
                            building = initialiseBuilding(uri, building, ceaRoute, namedGraph);
                        }
                        if(!checkDataInitialised(uri, building, tsIris, scalarIris, ceaRoute, namedGraph)) {
                            createTimeSeries(uri, tsIris, namedGraph);
                            initialiseData(uri, i, scalars, building, tsIris, scalarIris, ceaRoute, namedGraph);
                        }
                        else{
                            updateScalars(uri, ceaRoute, scalarIris, scalars, i, namedGraph);
                        }
                        addDataToTimeSeries(timeSeries.get(i), times, tsIris);
                    }
                }
                else if (requestUrl.contains(URI_ACTION)) {
                    ArrayList<CEAInputData> testData = new ArrayList<>();
                    ArrayList<String> uriStringArray = new ArrayList<>();
                    List<String> uniqueSurrounding = new ArrayList<>();
                    String crs= new String();

                    for (int i = 0; i < uriArray.length(); i++) {
                        String uri = uriArray.getString(i);
                        uniqueSurrounding.add(uri);

                        // Only set route once - assuming all iris passed in same namespace
                        // Will not be necessary if namespace is passed in request params
                        if(i==0) {
                            // if KEY_GEOMETRY is not specified in requestParams, geometryRoute defaults to TheWorldAvatar Blazegraph
                            geometryRoute = requestParams.has(KEY_GEOMETRY) ? requestParams.getString(KEY_GEOMETRY) : getRoute(uri);
                            // if KEY_USAGE is not specified in requestParams, geometryRoute defaults to TheWorldAvatar Blazegraph
                            usageRoute = requestParams.has(KEY_USAGE) ? requestParams.getString(KEY_USAGE) : geometryRoute;
                            if (!requestParams.has(KEY_CEA)){
                                // if KEY_CEA is not specified in requestParams, set ceaRoute to TheWorldAvatar Blazegraph
                                ceaRoute = getRoute(uri);
                                // default graph in TheWorldAvatar Blazegraph is energyprofile if no KEY_GRAPH specified in requestParams
                                namedGraph = requestParams.has(KEY_GRAPH) ? requestParams.getString(KEY_GRAPH) : getGraph(uri,ENERGY_PROFILE);

                            }
                            else{
                                ceaRoute = requestParams.getString(KEY_CEA);
                                // if KEY_CEA is specified, assume no graph if KEY_GRAPH is not specified in requestParams
                                if (requestParams.has(KEY_GRAPH)){
                                    namedGraph =  requestParams.getString(KEY_GRAPH);
                                    // ensures that graph ends with /
                                    if (!namedGraph.endsWith("/")) {namedGraph = namedGraph + "/";}
                                }
                                else{
                                    namedGraph = "";
                                }
                            }

                            // check if ceaRoute has quads enabled for querying and updating with graphs
                            if (!namedGraph.isEmpty()){
                                checkQuadsEnabled(ceaRoute);
                            }
                            setStoreClient(ceaRoute);
                        }
                        uriStringArray.add(uri);
                        // Set default value of 10m if height can not be obtained from knowledge graph
                        // Will only require one height query if height is represented in data consistently
                        String height = getValue(uri, "HeightMeasuredHeigh", geometryRoute);
                        height = height.length() == 0 ? getValue(uri, "HeightMeasuredHeight", geometryRoute): height;
                        height = height.length() == 0 ? getValue(uri, "HeightGenAttr", geometryRoute): height;
                        height = height.length() == 0 ? "10.0" : height;
                        // Get footprint from ground thematic surface or find from surface geometries depending on data
                        String footprint = getValue(uri, "Lod0FootprintId", geometryRoute);
                        footprint = footprint.length() == 0 ? getValue(uri, "FootprintThematicSurface", geometryRoute) : footprint;
                        footprint = footprint.length() == 0 ? getValue(uri, "FootprintSurfaceGeom", geometryRoute) : footprint;
                        // Get building usage, set default usage of MULTI_RES if not available in knowledge graph
                        Map<String, Double> usage = getBuildingUsages(uri, usageRoute);

                        ArrayList<CEAInputData> surrounding = getSurroundings(uri, geometryRoute, uniqueSurrounding);

                        testData.add(new CEAInputData(footprint, height, usage, surrounding));
                        if (i==0) {
                            crs = getValue(uri, "CRS", geometryRoute);
                            crs = crs == "" ? getValue(uri, "DatabasesrsCRS", geometryRoute) : crs;
                            if (crs == ""){crs = getNamespace(uri).split("EPSG").length == 2 ? getNamespace(uri).split("EPSG")[1].split("/")[0] : "27700";}
                        } //just get crs once - assuming all iris in same namespace
                    }
                    // Manually set thread number to 0 - multiple threads not working so needs investigating
                    // Potentially issue is CEA is already multi-threaded
                    runCEA(testData, uriStringArray, 0, crs);
                }
            } else if (requestUrl.contains(URI_QUERY)) {

                for (int i = 0; i < uriArray.length(); i++) {
                    String uri = uriArray.getString(i);

                    // Only set route once - assuming all iris passed in same namespace
                    if(i==0) {
                        if (!requestParams.has(KEY_CEA)){
                            // if KEY_CEA is not specified in requestParams, set ceaRoute to TheWorldAvatar Blazegraph
                            ceaRoute = getRoute(uri);
                            // default graph in TheWorldAvatar Blazegraph is energyprofile if no KEY_GRAPH specified in requestParams
                            namedGraph = requestParams.has(KEY_GRAPH) ? requestParams.getString(KEY_GRAPH) : getGraph(uri,ENERGY_PROFILE);
                        }
                        else{
                            ceaRoute = requestParams.getString(KEY_CEA);
                            // if KEY_CEA is specified, assume no graph if KEY_GRAPH is not specified in requestParams
                            if (requestParams.has(KEY_GRAPH)){
                                namedGraph =  requestParams.getString(KEY_GRAPH);
                                // ensures that graph ends with /
                                if (!namedGraph.endsWith("/")) {namedGraph = namedGraph + "/";}
                            }
                            else{
                                namedGraph = "";
                            }
                            // check if ceaRoute has quads enabled for querying and updating with graphs
                            if (!namedGraph.isEmpty()){
                                checkQuadsEnabled(ceaRoute);
                            }
                        }
                        setStoreClient(ceaRoute);
                    }
                    String building = checkBuildingInitialised(uri, ceaRoute);
                    if(building.equals("")){
                        return requestParams;
                    }
                    JSONObject data = new JSONObject();
                    List<String> allMeasures = new ArrayList<>();
                    Stream.of(TIME_SERIES, SCALARS).forEach(allMeasures::addAll);
                    for (String measurement: allMeasures) {
                        ArrayList<String> result = getDataIRI(uri, building, measurement, ceaRoute, namedGraph);
                        if (!result.isEmpty()) {
                            String value;
                            if (TIME_SERIES.contains(measurement)) {
                                value = calculateAnnual(retrieveData(result.get(0)), result.get(0));
                                measurement = "Annual "+ measurement;
                            } else {
                                value = getNumericalValue(uri, result.get(0), ceaRoute, namedGraph);
                            }
                            // Return non-zero values
                            if(!(value.equals("0") || value.equals("0.0"))){
                                value += " " + getUnit(result.get(1));
                                data.put(measurement, value);
                            }
                        }
                    }
                    requestParams.append(CEA_OUTPUTS, data);
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
        boolean error = requestParams.get(KEY_IRI).toString().isEmpty();

        if (requestParams.has(KEY_GEOMETRY)) {error = error || requestParams.get(KEY_GEOMETRY).toString().isEmpty();}
        if (requestParams.has(KEY_USAGE)) {error = error || requestParams.get(KEY_USAGE).toString().isEmpty();}
        if (requestParams.has(KEY_CEA)) {error = error || requestParams.get(KEY_CEA).toString().isEmpty();}
        if (requestParams.has(KEY_GRAPH)) {error = error || requestParams.get(KEY_GRAPH).toString().isEmpty();}

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

        if (requestParams.has(KEY_CEA)) {error = error || requestParams.get(KEY_CEA).toString().isEmpty();}
        if (requestParams.has(KEY_GRAPH)) {error = error || requestParams.get(KEY_GRAPH).toString().isEmpty();}
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
        ontoBuiltEnvUri=config.getString("uri.ontology.ontobuiltenv");
        geoUri=config.getString("uri.service.geo");
        accessAgentRoutes.put("http://www.theworldavatar.com:83/citieskg/namespace/berlin/sparql/", config.getString("berlin.targetresourceid"));
        accessAgentRoutes.put("http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG24500/sparql/", config.getString("singaporeEPSG24500.targetresourceid"));
        accessAgentRoutes.put("http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/", config.getString("singaporeEPSG4326.targetresourceid"));
        accessAgentRoutes.put("http://www.theworldavatar.com:83/citieskg/namespace/kingslynnEPSG3857/sparql/", config.getString("kingslynnEPSG3857.targetresourceid"));
        accessAgentRoutes.put("http://www.theworldavatar.com:83/citieskg/namespace/kingslynnEPSG27700/sparql/", config.getString("kingslynnEPSG27700.targetresourceid"));
        accessAgentRoutes.put("http://www.theworldavatar.com:83/citieskg/namespace/pirmasensEPSG32633/sparql/", config.getString("pirmasensEPSG32633.targetresourceid"));
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
    private void createTimeSeries(String uriString, LinkedHashMap<String,String> fixedIris, String graph ) {
        tsClient = new TimeSeriesClient<>(storeClient, OffsetDateTime.class);

        // Create a iri for each measurement
        List<String> iris = new ArrayList<>();
        for(String measurement: TIME_SERIES){
            String iri = measurement+"_"+UUID.randomUUID()+ "/";
            iri = !graph.isEmpty() ? graph + iri : ontoUBEMMPUri + iri ;
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
            try (Connection conn = rdbStoreClient.getConnection()) {
                // Initialize the time series
                tsClient.initTimeSeries(iris, classes, timeUnit, conn, TimeSeriesClient.Type.STEPWISECUMULATIVE, null, null);
                //LOGGER.info(String.format("Initialized time series with the following IRIs: %s", String.join(", ", iris)));
            }
            catch (SQLException e) {
                throw new JPSRuntimeException(e);
            }
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
            tsClient = new TimeSeriesClient<>(storeClient, OffsetDateTime.class);
        }
        TimeSeries<OffsetDateTime> currentTimeSeries = new TimeSeries<>(times, iris, values);

        try (Connection conn = rdbStoreClient.getConnection()) {
            OffsetDateTime endDataTime = tsClient.getMaxTime(currentTimeSeries.getDataIRIs().get(0), conn);
            OffsetDateTime beginDataTime = tsClient.getMinTime(currentTimeSeries.getDataIRIs().get(0), conn);

            // Delete old data if exists
            if (endDataTime != null) {
                for (Integer i = 0; i < currentTimeSeries.getDataIRIs().size(); i++) {
                    tsClient.deleteTimeSeriesHistory(currentTimeSeries.getDataIRIs().get(i), beginDataTime, endDataTime, conn);
                }
            }
            // Add New data
            tsClient.addTimeSeriesData(currentTimeSeries, conn);
        }
        catch (SQLException e) {
            throw new JPSRuntimeException(e);
        }
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
                try (Connection conn = rdbStoreClient.getConnection()) {
                    if (!tsClient.checkDataHasTimeSeries(iri, conn)) {
                        return false;
                    }
                }
                catch (SQLException e) {
                    throw new JPSRuntimeException(e);
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
            if (value == "Lod0FootprintId" || value == "FootprintThematicSurface"){
                result = extractFootprint(queryResultArray);
            }
            else if (value == "FootprintSurfaceGeom") {
                result = extractFootprint(getGroundGeometry(queryResultArray));
            }
            else{
                result = queryResultArray.getJSONObject(0).get(value).toString();
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
    private JSONArray getGroundGeometry(JSONArray results){
        ArrayList<Integer> ind = new ArrayList<>();
        ArrayList<Double> z;
        Double minZ = Double.MAX_VALUE;
        double eps = 0.5;
        boolean flag;
        String geom;
        String[] split;

        for (int i = 0; i < results.length(); i++){
            geom = results.getJSONObject(i).get("geometry").toString();
            split = geom.split("#");

            z = new ArrayList<>();
            z.add(Double.parseDouble(split[2]));

            flag = true;

            for (int j = 5; j < split.length; j += 3){
                for (int ji = 0;  ji < z.size(); ji++){
                    if (Math.abs(Double.parseDouble(split[j]) - z.get(ji)) > eps){
                        flag = false;
                        break;
                    }
                }

                if (flag){
                    z.add(Double.parseDouble(split[j]));
                }
                else{
                    break;
                }
            }

            if (!flag){
                ind.add(i);
            }
            else{
                if (z.get(0) < minZ){minZ = z.get(0);}
            }
        }

        // gets rid of geometry without constant z (up to eps tolerance)
        for (int i = ind.size() - 1; i >= 0; i--){
            results.remove(ind.get(i));
        }

        int i = 0;

        // gets rid of geometry without minimum z value (up to eps tolerance)
        while (i < results.length()){
            geom = results.getJSONObject(i).get("geometry").toString();
            split = geom.split("#");

            if (Double.parseDouble(split[2]) > minZ + eps){
                results.remove(i);
            }
            else{
                i++;
            }
        }

        return results;
    }

    /**
     * calls a SPARQL query for a specific URI for height or geometry.
     * @param uriString city object id
     * @param value building value requested
     * @return returns a query string
     */
    private Query getQuery(String uriString, String value) {
        switch(value) {
            case "Lod0FootprintId":
                return getLod0FootprintIdQuery(uriString);
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
            case "DatabasesrsCRS":
                return getDatabasesrsCrsQuery(uriString);
            case "CRS":
                return getCrsQuery(uriString);
            case "envelope":
                return getEnvelopeQuery(uriString);
        }
        return null;
    }

    /**
     * builds a SPARQL query for a specific URI to retrieve all surface geometries to a building
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getGeometryQuerySurfaceGeom(String uriString) {
        try {
            WhereBuilder wb = new WhereBuilder()
                    .addPrefix("ocgml", ocgmlUri)
                    .addWhere("?surf", "ocgml:cityObjectId", "?s")
                    .addWhere("?surf", "ocgml:GeometryType", "?geometry")
                    .addFilter("!isBlank(?geometry)");
            SelectBuilder sb = new SelectBuilder()
                    .addVar("?geometry")
                    .addVar("datatype(?geometry)", "?datatype")
                    .addGraph(NodeFactory.createURI(getGraph(uriString,SURFACE_GEOMETRY)), wb);
            sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(getBuildingUri(uriString)));
            return sb.build();

        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * builds a SPARQL query for a specific URI to retrieve ground surface geometries for building linked to thematic surfaces with ocgml:objectClassId 35
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getGeometryQueryThematicSurface(String uriString) {
        try {
            WhereBuilder wb1 = new WhereBuilder()
                    .addPrefix("ocgml", ocgmlUri)
                    .addWhere("?surf", "ocgml:cityObjectId", "?s")
                    .addWhere("?surf", "ocgml:GeometryType", "?geometry")
                    .addFilter("!isBlank(?geometry)");
            WhereBuilder wb2 = new WhereBuilder()
                    .addPrefix("ocgml", ocgmlUri)
                    .addWhere("?s", "ocgml:buildingId", "?building")
                    .addWhere("?s", "ocgml:objectClassId", "?groundSurfId")
                    .addFilter("?groundSurfId = 35"); //Thematic Surface Ids are 33:roof, 34:wall and 35:ground
            SelectBuilder sb = new SelectBuilder()
                    .addVar("?geometry")
                    .addVar("datatype(?geometry)", "?datatype")
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
        try {
            WhereBuilder wb = new WhereBuilder()
                    .addPrefix("ocgml", ocgmlUri)
                    .addWhere("?s", "ocgml:measuredHeight", "?HeightMeasuredHeight")
                    .addFilter("!isBlank(?HeightMeasuredHeight)");
            SelectBuilder sb = new SelectBuilder()
                    .addVar("?HeightMeasuredHeight")
                    .addGraph(NodeFactory.createURI(getGraph(uriString,BUILDING)), wb);
            sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(getBuildingUri(uriString)));
            return sb.build();
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * builds a SPARQL query for a specific URI to retrieve the building height for data with ocgml:measuredHeigh attribute
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getHeightQueryMeasuredHeigh(String uriString) {
        try {
            WhereBuilder wb = new WhereBuilder()
                    .addPrefix("ocgml", ocgmlUri)
                    .addWhere("?s", "ocgml:measuredHeigh", "?HeightMeasuredHeigh")
                    .addFilter("!isBlank(?HeightMeasuredHeigh)");
            SelectBuilder sb = new SelectBuilder()
                    .addVar("?HeightMeasuredHeigh")
                    .addGraph(NodeFactory.createURI(getGraph(uriString, BUILDING)), wb);
            sb.setVar(Var.alloc("s"), NodeFactory.createURI(getBuildingUri(uriString)));
            return sb.build();
        }catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
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
     * builds a SPARQL query for a CRS in the DatabaseSRS graph using namespace from uri
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getDatabasesrsCrsQuery(String uriString) {
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
     * builds a SPARQL query for a specific URI to retrieve ground surface geometry from lod0FootprintId
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getLod0FootprintIdQuery(String uriString) {
        try {
            WhereBuilder wb = new WhereBuilder()
                    .addPrefix("ocgml", ocgmlUri)
                    .addWhere("?building", "ocgml:lod0FootprintId", "?footprintSurface")
                    .addWhere("?surface", "ocgml:parentId", "?footprintSurface")
                    .addWhere("?surface", "ocgml:GeometryType", "?geometry");
            SelectBuilder sb = new SelectBuilder()
                    .addVar("?geometry")
                    .addVar("datatype(?geometry)", "?datatype")
                    .addWhere(wb);
            sb.setVar(Var.alloc("building"), NodeFactory.createURI(getBuildingUri(uriString)));
            return sb.build();
        }catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * builds a SPARQL query for a CRS not in the DatabaseSRS graph using namespace from uri
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getCrsQuery(String uriString) {
        WhereBuilder wb = new WhereBuilder()
                .addPrefix("ocgml", ocgmlUri)
                .addWhere("?s", "ocgml:srid", "?CRS");
        SelectBuilder sb = new SelectBuilder()
                .addVar("?CRS")
                .addWhere(wb);
        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(getNamespace(uriString)));
        return sb.build();
    }

    /**
     * builds a SPARQL query for a specific URI to retrieve the building usages and the building usage share with OntoBuiltEnv concepts
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getBuildingUsagesQuery(String uriString) {
        WhereBuilder wb = new WhereBuilder();
        SelectBuilder sb = new SelectBuilder();

        wb.addPrefix("ontoBuiltEnv", ontoBuiltEnvUri)
                .addPrefix("rdf", rdfUri)
                .addWhere("?building", "ontoBuiltEnv:hasOntoCityGMLRepresentation", "?s")
                .addWhere("?building", "ontoBuiltEnv:hasPropertyUsage", "?usage")
                .addWhere("?usage", "rdf:type", "?BuildingUsage")
                .addOptional("?usage", "ontoBuiltEnv:hasUsageShare", "?UsageShare");

        sb.addVar("?BuildingUsage").addVar("?UsageShare")
                .addWhere(wb)
                .addOrderBy("UsageShare", Order.DESCENDING);

        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(getBuildingUri(uriString)));

        return sb.build();
    }

    /**
     * builds a SPARQL query for the envelope of uriString
     * @param uriString city object id
     * @return returns a query string
     */
    private Query getEnvelopeQuery(String uriString){
        WhereBuilder wb = new WhereBuilder()
                .addPrefix("ocgml", ocgmlUri)
                .addWhere("?s", "ocgml:EnvelopeType", "?envelope");
        SelectBuilder sb = new SelectBuilder()
                .addVar("?envelope")
                .addWhere(wb);
        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(uriString));
        return sb.build();
    }

    /**
     * builds a SPARQL geospatial query for city object id of buildings whose envelope are within lowerBounds and upperBounds
     * @param uriString city object id of the target building
     * @param lowerBounds coordinates of customFieldsLowerBounds as a string
     * @param upperBounds coordinates of customFieldsUpperBounds as a string
     * @return returns a query string
     */
    private Query getBuildingsWithinBoundsQuery(String uriString, String lowerBounds, String upperBounds) throws ParseException {
        // where clause for geospatial search
        WhereBuilder wb = new WhereBuilder()
                .addPrefix("ocgml", ocgmlUri)
                .addPrefix("geo", geoUri)
                .addWhere("?cityObject", "geo:predicate", "ocgml:EnvelopeType")
                .addWhere("?cityObject", "geo:searchDatatype", customDataType)
                .addWhere("?cityObject", "geo:customFields", customField)
                // PLACEHOLDER because lowerBounds and upperBounds would be otherwise added as doubles, not strings
                .addWhere("?cityObject", "geo:customFieldsLowerBounds", "PLACEHOLDER" + lowerBounds)
                .addWhere("?cityObject", "geo:customFieldsUpperBounds", "PLACEHOLDER" + upperBounds);

        // where clause to check that the city object is a building
        WhereBuilder wb2 = new WhereBuilder()
                .addPrefix("ocgml", ocgmlUri)
                .addWhere("?cityObject", "ocgml:objectClassId", "?id")
                .addFilter("?id=26");

        SelectBuilder sb = new SelectBuilder()
                .addVar("?cityObject");

        Query query = sb.build();
        // add geospatial service
        ElementGroup body = new ElementGroup();
        body.addElement(new ElementService(geoUri + "search", wb.build().getQueryPattern()));
        body.addElement(wb2.build().getQueryPattern());
        query.setQueryPattern(body);

        WhereHandler wh = new WhereHandler(query.cloneQuery());

        // add city object graph
        WhereHandler wh2 = new WhereHandler(sb.build());
        wh2.addGraph(NodeFactory.createURI(getGraph(uriString,CITY_OBJECT)), wh);

        return wh2.getQuery();
    }

    /**
     * retrieves the surrounding buildings
     * @param uriString city object id
     * @param route route to pass to access agent
     * @param unique array list of unique surrounding buildings
     * @return the surrounding buildings as an ArrayList of CEAInputData
     */
    private ArrayList<CEAInputData> getSurroundings(String uriString, String route, List<String> unique) {
        try {
            CEAInputData temp;
            String uri;
            ArrayList<CEAInputData> surroundings = new ArrayList<>();
            String envelopeCoordinates = getValue(uriString, "envelope", route);

            Double buffer = 200.00;

            Polygon envelopePolygon = (Polygon) toPolygon(envelopeCoordinates);

            Geometry surroundingRing = ((Polygon) inflatePolygon(envelopePolygon, buffer)).getExteriorRing();

            Coordinate[] surroundingCoordinates = surroundingRing.getCoordinates();

            String boundingBox = coordinatesToString(surroundingCoordinates);

            String[] points = boundingBox.split("#");

            String lowerPoints= points[0] + "#" + points[1] + "#" + 0 + "#";

            String lowerBounds = lowerPoints + lowerPoints + lowerPoints + lowerPoints + lowerPoints;
            lowerBounds = lowerBounds.substring(0, lowerBounds.length() - 1 );

            String upperPoints = points[6] + "#" + points[7] + "#" + String.valueOf(Double.parseDouble(points[8])+100) + "#";

            String upperBounds = upperPoints + upperPoints + upperPoints + upperPoints + upperPoints;
            upperBounds = upperBounds.substring(0, upperBounds.length() - 1);

            Query query = getBuildingsWithinBoundsQuery(uriString, lowerBounds, upperBounds);

            String queryString = query.toString().replace("PLACEHOLDER", "");

            JSONArray queryResultArray = this.queryStore(route, queryString);

            for (int i = 0; i < queryResultArray.length(); i++) {
                uri = queryResultArray.getJSONObject(i).get("cityObject").toString();

                if (!unique.contains(uri)) {
                    // Set default value of 10m if height can not be obtained from knowledge graph
                    // Will only require one height query if height is represented in data consistently
                    String height = getValue(uri, "HeightMeasuredHeigh", route);
                    height = height.length() == 0 ? getValue(uri, "HeightMeasuredHeight", route) : height;
                    height = height.length() == 0 ? getValue(uri, "HeightGenAttr", route) : height;
                    height = height.length() == 0 ? "10.0" : height;
                    // Get footprint from ground thematic surface or find from surface geometries depending on data
                    String footprint = getValue(uri, "Lod0FootprintId", route);
                    footprint = footprint.length() == 0 ? getValue(uri, "FootprintThematicSurface", route) : footprint;
                    footprint = footprint.length() == 0 ? getValue(uri, "FootprintSurfaceGeom", route) : footprint;

                    temp = new CEAInputData(footprint, height, null, null);
                    unique.add(uri);
                    surroundings.add(temp);
                }
            }
            return surroundings;
        }
        catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * retrieves the usages of a building and each usage's corresponding weight, and returns the usages and their weight as a map
     * @param uriString city object id
     * @param route route to pass to access agent
     * @return the usages and their corresponding weighting
     */
    private Map<String, Double> getBuildingUsages(String uriString, String route) {
        Map<String, Double> result = new HashMap<>();
        Map<String, Double> temp = new HashMap<>();
        String usage;
        Query q = getBuildingUsagesQuery(uriString);

        JSONArray queryResultArray = this.queryStore(route, q.toString());

        // CEA only support up to three usages for each building
        // convert all usages to CEA defined usages first

        if (queryResultArray.length() == 1){
            usage = queryResultArray.getJSONObject(0).get("BuildingUsage").toString().split(ontoBuiltEnvUri)[1].split(">")[0].toUpperCase();
            usage = toCEAConvention(usage);
            result.put(usage, 1.00);
        }
        else {
            for (int i = 0; i < queryResultArray.length(); i++) {
                usage = queryResultArray.getJSONObject(i).get("BuildingUsage").toString().split(ontoBuiltEnvUri)[1].split(">")[0].toUpperCase();
                usage = toCEAConvention(usage);

                if (temp.containsKey(usage)) {
                    temp.put(usage, temp.get(usage) + queryResultArray.getJSONObject(i).getDouble("UsageShare"));
                } else {
                    temp.put(usage, queryResultArray.getJSONObject(i).getDouble("UsageShare"));
                }
            }

            // get the top 3 usages
            result = temp.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(3)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            // normalise the usage weights in result so that they sum up to 1
            Double sum = 0.00;

            for (Double val : result.values()){
                sum += val;
            }

            for (Map.Entry<String, Double> entry : result.entrySet()){
                result.put(entry.getKey(), entry.getValue() / sum);
            }
        }

        return result;
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
    public void addSupplyDeviceWhere(WhereBuilder builder, String panelType, String energyType){
        builder.addWhere("?building", "ontoubemmp:hasDevice", "?PVPanels")
                .addWhere("?PVPanels", "rdf:type", panelType)
                .addWhere("?PVPanels", "thinkhome:producesEnergy", "?supply")
                .addWhere("?supply", "rdf:type", energyType)
                .addWhere("?supply", "om:hasValue", "?measure")
                .addWhere("?measure", "om:hasUnit", "?unit");
    }

    /**
     * Add Where for Device Area
     * @param builder update builder
     * @param panelType type of panels
     */
    public void addSupplyDeviceAreaWhere(WhereBuilder builder, String panelType, String areaType){
        builder.addWhere("?building", "ontoubemmp:hasDevice", "?PVPanels")
                .addWhere("?PVPanels", "rdf:type", panelType)
                .addWhere("?PVPanels", "ontoubemmp:hasArea", "?area")
                .addWhere("?area", "rdf:type", areaType)
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
     * @param graph graph name
     * @return list of iris
     */
    public ArrayList<String> getDataIRI(String uriString, String building, String value, String route, String graph) {
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
                addSupplyDeviceWhere(wb,"ontoubemmp:RoofPVPanels", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_PV_WALL_SOUTH_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:SouthWallPVPanels", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_PV_WALL_NORTH_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:NorthWallPVPanels", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_PV_WALL_EAST_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:EastWallPVPanels", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_PV_WALL_WEST_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:WestWallPVPanels", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_PV_ROOF_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:RoofPVPanels", "ontoubemmp:PVPanelsArea");
                break;
            case KEY_PV_WALL_SOUTH_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:SouthWallPVPanels", "ontoubemmp:PVPanelsArea");
                break;
            case KEY_PV_WALL_NORTH_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:NorthWallPVPanels", "ontoubemmp:PVPanelsArea");
                break;
            case KEY_PV_WALL_EAST_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:EastWallPVPanels", "ontoubemmp:PVPanelsArea");
                break;
            case KEY_PV_WALL_WEST_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:WestWallPVPanels", "ontoubemmp:PVPanelsArea");
                break;
            case KEY_PVT_PLATE_ROOF_E_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:RoofPVTPlateCollectors", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_PVT_PLATE_WALL_SOUTH_E_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:SouthWallPVTPlateCollectors", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_PVT_PLATE_WALL_NORTH_E_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:NorthWallPVTPlateCollectors", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_PVT_PLATE_WALL_EAST_E_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:EastWallPVTPlateCollectors", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_PVT_PLATE_WALL_WEST_E_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:WestWallPVTPlateCollectors", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_PVT_PLATE_ROOF_Q_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:RoofPVTPlateCollectors", "ontoubemmp:HeatSupply");
                break;
            case KEY_PVT_PLATE_WALL_SOUTH_Q_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:SouthWallPVTPlateCollectors", "ontoubemmp:HeatSupply");
                break;
            case KEY_PVT_PLATE_WALL_NORTH_Q_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:NorthWallPVTPlateCollectors", "ontoubemmp:HeatSupply");
                break;
            case KEY_PVT_PLATE_WALL_EAST_Q_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:EastWallPVTPlateCollectors", "ontoubemmp:HeatSupply");
                break;
            case KEY_PVT_PLATE_WALL_WEST_Q_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:WestWallPVTPlateCollectors", "ontoubemmp:HeatSupply");
                break;
            case KEY_PVT_PLATE_ROOF_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:RoofPVTPlateCollectors", "ontoubemmp:PVTPlateCollectosArea");
                break;
            case KEY_PVT_PLATE_WALL_SOUTH_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:SouthWallPVTPlateCollectors", "ontoubemmp:PVTPlateCollectosArea");
                break;
            case KEY_PVT_PLATE_WALL_NORTH_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:NorthWallPVTPlateCollectors", "ontoubemmp:PVTPlateCollectosArea");
                break;
            case KEY_PVT_PLATE_WALL_EAST_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:EastWallPVTPlateCollectors", "ontoubemmp:PVTPlateCollectosArea");
                break;
            case KEY_PVT_PLATE_WALL_WEST_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:WestWallPVTPlateCollectors", "ontoubemmp:PVTPlateCollectosArea");
                break;
            case KEY_PVT_TUBE_ROOF_E_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:RoofPVTTubeCollectors", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_PVT_TUBE_WALL_SOUTH_E_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:SouthWallPVTTubeCollectors", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_PVT_TUBE_WALL_NORTH_E_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:NorthWallPVTTubeCollectors", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_PVT_TUBE_WALL_EAST_E_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:EastWallPVTTubeCollectors", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_PVT_TUBE_WALL_WEST_E_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:WestWallPVTTubeCollectors", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_PVT_TUBE_ROOF_Q_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:RoofPVTTubeCollectors", "ontoubemmp:HeatSupply");
                break;
            case KEY_PVT_TUBE_WALL_SOUTH_Q_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:SouthWallPVTTubeCollectors", "ontoubemmp:HeatSupply");
                break;
            case KEY_PVT_TUBE_WALL_NORTH_Q_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:NorthWallPVTTubeCollectors", "ontoubemmp:HeatSupply");
                break;
            case KEY_PVT_TUBE_WALL_EAST_Q_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:EastWallPVTTubeCollectors", "ontoubemmp:HeatSupply");
                break;
            case KEY_PVT_TUBE_WALL_WEST_Q_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:WestWallPVTTubeCollectors", "ontoubemmp:HeatSupply");
                break;
            case KEY_PVT_TUBE_ROOF_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:RoofPVTTubeCollectors", "ontoubemmp:PVTTubeCollectosArea");
                break;
            case KEY_PVT_TUBE_WALL_SOUTH_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:SouthWallPVTTubeCollectors", "ontoubemmp:PVTTubeCollectosArea");
                break;
            case KEY_PVT_TUBE_WALL_NORTH_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:NorthWallPVTTubeCollectors", "ontoubemmp:PVTTubeCollectosArea");
                break;
            case KEY_PVT_TUBE_WALL_EAST_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:EastWallPVTTubeCollectors", "ontoubemmp:PVTTubeCollectosArea");
                break;
            case KEY_PVT_TUBE_WALL_WEST_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:WestWallPVTTubeCollectors", "ontoubemmp:PVTTubeCollectosArea");
                break;
            case KEY_THERMAL_PLATE_ROOF_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:RoofThermalPlateCollectors", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_THERMAL_PLATE_WALL_SOUTH_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:SouthWallThermalPlateCollectors", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_THERMAL_PLATE_WALL_NORTH_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:NorthWallThermalPlateCollectors", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_THERMAL_PLATE_WALL_EAST_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:EastWallThermalPlateCollectors", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_THERMAL_PLATE_WALL_WEST_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:WestWallThermalPlateCollectors", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_THERMAL_PLATE_ROOF_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:RoofThermalPlateCollectors", "ontoubemmp:ThermalPlateCollectosArea");
                break;
            case KEY_THERMAL_PLATE_WALL_SOUTH_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:SouthWallThermalPlateCollectors", "ontoubemmp:ThermalPlateCollectosArea");
                break;
            case KEY_THERMAL_PLATE_WALL_NORTH_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:NorthWallThermalPlateCollectors", "ontoubemmp:ThermalPlateCollectosArea");
                break;
            case KEY_THERMAL_PLATE_WALL_EAST_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:EastWallThermalPlateCollectors", "ontoubemmp:ThermalPlateCollectosArea");
                break;
            case KEY_THERMAL_PLATE_WALL_WEST_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:WestWallThermalPlateCollectors", "ontoubemmp:ThermalPlateCollectosArea");
                break;
            case KEY_THERMAL_TUBE_ROOF_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:RoofThermalTubeCollectors", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_THERMAL_TUBE_WALL_SOUTH_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:SouthWallThermalTubeCollectors", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_THERMAL_TUBE_WALL_NORTH_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:NorthWallThermalTubeCollectors", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_THERMAL_TUBE_WALL_EAST_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:EastWallThermalTubeCollectors", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_THERMAL_TUBE_WALL_WEST_SUPPLY:
                addSupplyDeviceWhere(wb,"ontoubemmp:WestWallThermalTubeCollectors", "ontoubemmp:ElectricitySupply");
                break;
            case KEY_THERMAL_TUBE_ROOF_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:RoofThermalTubeCollectors", "ontoubemmp:ThermalTubeCollectosArea");
                break;
            case KEY_THERMAL_TUBE_WALL_SOUTH_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:SouthWallThermalTubeCollectors", "ontoubemmp:ThermalTubeCollectosArea");
                break;
            case KEY_THERMAL_TUBE_WALL_NORTH_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:NorthWallThermalTubeCollectors", "ontoubemmp:ThermalTubeCollectosArea");
                break;
            case KEY_THERMAL_TUBE_WALL_EAST_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:EastWallThermalTubeCollectors", "ontoubemmp:ThermalTubeCollectosArea");
                break;
            case KEY_THERMAL_TUBE_WALL_WEST_AREA:
                addSupplyDeviceAreaWhere(wb, "ontoubemmp:WestWallThermalTubeCollectors", "ontoubemmp:ThermalTubeCollectosArea");
                break;
            default:
                return result;
        }

        sb.addVar("?measure")
                .addVar("?unit");

        if (!graph.isEmpty()){
            sb.addGraph(NodeFactory.createURI(graph), wb);
        }
        else{
            sb.addWhere(wb);
        }


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
     * @param graph graph name
     * @return list of iris
     */
    public String getNumericalValue(String uriString, String measureUri, String route, String graph) {
        String result = "";

        WhereBuilder wb = new WhereBuilder().addPrefix("om", unitOntologyUri)
            .addWhere("?measure", "om:hasNumericalValue", "?value");

        SelectBuilder sb = new SelectBuilder().addVar("?value");

        if (!graph.isEmpty()){
            sb.addGraph(NodeFactory.createURI(graph), wb);
        }
        else{
            sb.addWhere(wb);
        }

        sb.setVar( Var.alloc( "measure" ), NodeFactory.createURI(measureUri));

        JSONArray queryResultArray = new JSONArray(this.queryStore(route, sb.build().toString()));

        if(!queryResultArray.isEmpty()){
            result = queryResultArray.getJSONObject(0).get("value").toString();
        }
        return result;
    }

    /**
     * Check building linked to ontoCityGML is initialised in KG and is a DABGEO:Building instance
     * @param uriString city object id
     * @param route route to pass to access agent
     * @return building
     */
    public String checkBuildingInitialised(String uriString, String route){
        WhereBuilder wb = new WhereBuilder();
        SelectBuilder sb = new SelectBuilder();

        wb.addPrefix("rdf", rdfUri)
                .addPrefix("ontoBuiltEnv", ontoBuiltEnvUri)
                .addPrefix("DABGEO", purlInfrastructureUri)
                .addWhere("?building", "ontoBuiltEnv:hasOntoCityGMLRepresentation", "?s")
                .addWhere("?building", "rdf:type", "DABGEO:Building");

        sb.addVar("?building").addWhere(wb);

        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(getBuildingUri(uriString)));

        JSONArray queryResultArray = new JSONArray(this.queryStore(route, sb.build().toString()));
        String building = "";
        if(!queryResultArray.isEmpty()){
            building = queryResultArray.getJSONObject(0).get("building").toString();
        }
        return building;
    }

    /**
     * Initialise building in KG with buildingUri as the DABGEO:Building IRI, and link to ontoCityGMLRepresentation
     * @param uriString city object id
     * @param buildingUri building IRI from other endpoints if exist
     * @param route route to pass to access agent
     * @param graph graph name
     * @return building
     */
    public String initialiseBuilding(String uriString, String buildingUri, String route, String graph){

        UpdateBuilder ub = new UpdateBuilder();

        if (buildingUri.isEmpty()) {
            if (!graph.isEmpty()) {
                buildingUri = graph + "Building_" + UUID.randomUUID() + "/";
            }
            else{
                buildingUri = ontoBuiltEnvUri + "Building_" + UUID.randomUUID() + "/";
            }
        }

        WhereBuilder wb =
                new WhereBuilder()
                        .addPrefix("rdf", rdfUri)
                        .addPrefix("owl", owlUri)
                        .addPrefix("purlInf", purlInfrastructureUri)
                        .addPrefix("ontoBuiltEnv", ontoBuiltEnvUri)
                        .addWhere(NodeFactory.createURI(buildingUri), "rdf:type", "purlInf:Building")
                        .addWhere(NodeFactory.createURI(buildingUri), "rdf:type", "owl:NamedIndividual")
                        .addWhere(NodeFactory.createURI(buildingUri), "ontoBuiltEnv:hasOntoCityGMLRepresentation", NodeFactory.createURI(getBuildingUri(uriString)));

        if (!graph.isEmpty()){
            ub.addInsert(NodeFactory.createURI(graph), wb);
        }
        else{
            ub.addInsert(wb);
        }

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
     * @param graph graph name
     * @return if time series are initialised
     */
    public Boolean checkDataInitialised(String uriString, String building, LinkedHashMap<String,String> tsIris, LinkedHashMap<String,String> scalarIris, String route, String graph){
        ArrayList<String> result;
        List<String> allMeasures = new ArrayList<>();
        Stream.of(TIME_SERIES, SCALARS).forEach(allMeasures::addAll);
        for (String measurement: allMeasures) {
            result = getDataIRI(uriString, building, measurement, route, graph);
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
    public void createConsumptionUpdate(WhereBuilder builder, String consumer, String consumptionType, String quantity, String measure){
        builder.addWhere(NodeFactory.createURI(quantity), "rdf:type", consumptionType)
                .addWhere(NodeFactory.createURI(quantity), "rdf:type", "owl:NamedIndividual")
                .addWhere(NodeFactory.createURI(quantity), "om:hasDimension", "om:energy-Dimension")
                .addWhere(NodeFactory.createURI(quantity), "om:hasValue", NodeFactory.createURI(measure))
                .addWhere(NodeFactory.createURI(measure), "rdf:type", "om:Measure")
                .addWhere(NodeFactory.createURI(measure), "rdf:type", "owl:NamedIndividual")
                .addWhere(NodeFactory.createURI(measure), "om:hasUnit", "om:kilowattHour")
                .addWhere(NodeFactory.createURI(consumer), "purlEnaeq:consumesEnergy",NodeFactory.createURI(quantity));
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
    public void createDeviceConsumptionUpdate(WhereBuilder builder, String building, String device, String deviceType, String consumptionType, String quantity, String measure){
          builder.addWhere(NodeFactory.createURI(building), "ontoubemmp:hasDevice", NodeFactory.createURI(device))
                .addWhere(NodeFactory.createURI(device), "rdf:type", deviceType)
                .addWhere(NodeFactory.createURI(device), "rdf:type", "owl:NamedIndividual");
          createConsumptionUpdate(builder, device, consumptionType, quantity, measure);
    }

    /**
     * Create update for PV Panel Supply
     * @param builder update builder
     * @param PVPanels iri of PV panels
     * @param quantity om:quantity iri
     * @param measure om:measure iri
     */
    public void createSolarGeneratorSupplyUpdate(WhereBuilder builder, String PVPanels, String quantity, String measure, String energySupply){
        builder.addWhere(NodeFactory.createURI(PVPanels), "thinkhome:producesEnergy", NodeFactory.createURI(quantity))
                .addWhere(NodeFactory.createURI(quantity), "rdf:type", energySupply)
                .addWhere(NodeFactory.createURI(quantity), "rdf:type", "owl:NamedIndividual")
                .addWhere(NodeFactory.createURI(quantity), "om:hasDimension", "om:energy-Dimension")
                .addWhere(NodeFactory.createURI(quantity), "om:hasValue", NodeFactory.createURI(measure))
                .addWhere(NodeFactory.createURI(measure), "rdf:type", "om:Measure")
                .addWhere(NodeFactory.createURI(measure), "rdf:type", "owl:NamedIndividual")
                .addWhere(NodeFactory.createURI(measure), "om:hasUnit", "om:kilowattHour");
    }

    /**
     * Create update for PV Panel areas
     * @param builder update builder
     * @param building iri of building
     * @param generator iri of PV panels
     * @param generatorType panel type in ontology
     * @param quantity om:quantity iri
     * @param measure om:measure iri
     * @param value numerical value
     */
    public void createSolarGeneratorAreaUpdate(WhereBuilder builder, String building, String generator, String generatorType, String quantity, String measure, String value, String areaType){
        builder.addWhere(NodeFactory.createURI(building), "ontoubemmp:hasDevice", NodeFactory.createURI(generator))
                .addWhere(NodeFactory.createURI(generator), "rdf:type", generatorType)
                .addWhere(NodeFactory.createURI(generator), "rdf:type", "owl:NamedIndividual")
                .addWhere(NodeFactory.createURI(generator), "ontoubemmp:hasArea", NodeFactory.createURI(quantity))
                .addWhere(NodeFactory.createURI(quantity), "rdf:type", areaType)
                .addWhere(NodeFactory.createURI(quantity), "rdf:type", "owl:NamedIndividual")
                .addWhere(NodeFactory.createURI(quantity), "om:hasDimension", "om:area-Dimension")
                .addWhere(NodeFactory.createURI(quantity), "om:hasValue", NodeFactory.createURI(measure))
                .addWhere(NodeFactory.createURI(measure), "rdf:type", "owl:NamedIndividual")
                .addWhere(NodeFactory.createURI(measure), "rdf:type", "om:Measure")
                .addWhere(NodeFactory.createURI(measure), "om:hasNumericalValue", value)
                .addWhere(NodeFactory.createURI(measure), "om:hasUnit", "om:squareMetre");
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
     * @param graph graph name
     */
    public void initialiseData(String uriString, Integer uriCounter, LinkedHashMap<String, List<String>> scalars, String buildingUri, LinkedHashMap<String,String> tsIris, LinkedHashMap<String,String> scalarIris, String route, String graph){

        WhereBuilder wb =
                new WhereBuilder()
                        .addPrefix("ontoubemmp", ontoUBEMMPUri)
                        .addPrefix("rdf", rdfUri)
                        .addPrefix("owl", owlUri)
                        .addPrefix("purlEnaeq", purlEnaeqUri)
                        .addPrefix("om", unitOntologyUri)
                        .addPrefix("thinkhome", thinkhomeUri)
                        .addPrefix("purlInf", purlInfrastructureUri);

        UpdateBuilder ub = new UpdateBuilder();

        //Device uris
        String heatingUri = "HeatingSystem_" + UUID.randomUUID() + "/";
        String coolingUri = "CoolingSystem_" + UUID.randomUUID() + "/";
        String pvRoofPanelsUri = "PVRoofPanels_" + UUID.randomUUID() + "/";
        String pvWallSouthPanelsUri = "PVWallSouthPanels_" + UUID.randomUUID() + "/";
        String pvWallNorthPanelsUri = "PVWallNorthPanels_" + UUID.randomUUID() + "/";
        String pvWallEastPanelsUri = "PVWallEastPanels_" + UUID.randomUUID() + "/";
        String pvWallWestPanelsUri = "PVWallWestPanels_" + UUID.randomUUID() + "/";
        String pvtPlateRoofPanelsUri = "PVTPlateRoofCollectors_" + UUID.randomUUID() + "/";
        String pvtPlateWallSouthPanelsUri = "PVTPlateWallSouthCollectors_" + UUID.randomUUID() + "/";
        String pvtPlateWallNorthPanelsUri = "PVTPlateWallNorthCollectors_" + UUID.randomUUID() + "/";
        String pvtPlateWallEastPanelsUri = "PVTPlateWallEastCollectors_" + UUID.randomUUID() + "/";
        String pvtPlateWallWestPanelsUri = "PVTPlateWallWestCollectors_" + UUID.randomUUID() + "/";
        String pvtTubeRoofPanelsUri = "PVTTubeRoofCollectors_" + UUID.randomUUID() + "/";
        String pvtTubeWallSouthPanelsUri = "PVTTubeWallSouthCollectors_" + UUID.randomUUID() + "/";
        String pvtTubeWallNorthPanelsUri = "PVTTubeWallNorthCollectors_" + UUID.randomUUID() + "/";
        String pvtTubeWallEastPanelsUri = "PVTTubeWallEastCollectors_" + UUID.randomUUID() + "/";
        String pvtTubeWallWestPanelsUri = "PVTTubeWallWestCollectors_" + UUID.randomUUID() + "/";
        String thermalPlateRoofPanelsUri = "ThermalPlateRoofCollectors_" + UUID.randomUUID() + "/";
        String thermalPlateWallSouthPanelsUri = "ThermalPlateWallSouthCollectors_" + UUID.randomUUID() + "/";
        String thermalPlateWallNorthPanelsUri = "ThermalPlateWallNorthCollectors_" + UUID.randomUUID() + "/";
        String thermalPlateWallEastPanelsUri = "ThermalPlateWallEastCollectors_" + UUID.randomUUID() + "/";
        String thermalPlateWallWestPanelsUri = "ThermalPlateWallWestCollectors_" + UUID.randomUUID() + "/";
        String thermalTubeRoofPanelsUri = "ThermalTubeRoofCollectors_" + UUID.randomUUID() + "/";
        String thermalTubeWallSouthPanelsUri = "ThermalTubeWallSouthCollectors_" + UUID.randomUUID() + "/";
        String thermalTubeWallNorthPanelsUri = "ThermalTubeWallNorthCollectors_" + UUID.randomUUID() + "/";
        String thermalTubeWallEastPanelsUri = "ThermalTubeWallEastCollectors_" + UUID.randomUUID() + "/";
        String thermalTubeWallWestPanelsUri = "ThermalTubeWallWestCollectors_" + UUID.randomUUID() + "/";


        if (!graph.isEmpty()){
            heatingUri = graph + heatingUri;
            coolingUri = graph + coolingUri;
            pvRoofPanelsUri = graph + pvRoofPanelsUri;
            pvWallSouthPanelsUri = graph + pvWallSouthPanelsUri;
            pvWallNorthPanelsUri = graph + pvWallNorthPanelsUri;
            pvWallEastPanelsUri = graph + pvWallEastPanelsUri;
            pvWallWestPanelsUri = graph + pvWallWestPanelsUri;
            pvtPlateRoofPanelsUri = graph + pvtPlateRoofPanelsUri;
            pvtPlateWallSouthPanelsUri = graph + pvtPlateWallSouthPanelsUri;
            pvtPlateWallNorthPanelsUri = graph + pvtPlateWallNorthPanelsUri;
            pvtPlateWallEastPanelsUri = graph + pvtPlateWallEastPanelsUri;
            pvtPlateWallWestPanelsUri = graph + pvtPlateWallWestPanelsUri;
            pvtTubeRoofPanelsUri = graph + pvtTubeRoofPanelsUri;
            pvtTubeWallSouthPanelsUri = graph + pvtTubeWallSouthPanelsUri;
            pvtTubeWallNorthPanelsUri = graph + pvtTubeWallNorthPanelsUri;
            pvtTubeWallEastPanelsUri = graph + pvtTubeWallEastPanelsUri;
            pvtTubeWallWestPanelsUri = graph + pvtTubeWallWestPanelsUri;
            thermalPlateRoofPanelsUri = graph + thermalPlateRoofPanelsUri;
            thermalPlateWallSouthPanelsUri = graph + thermalPlateWallSouthPanelsUri;
            thermalPlateWallNorthPanelsUri = graph + thermalPlateWallNorthPanelsUri;
            thermalPlateWallEastPanelsUri = graph + thermalPlateWallEastPanelsUri;
            thermalPlateWallWestPanelsUri = graph + thermalPlateWallWestPanelsUri;
            thermalTubeRoofPanelsUri = graph + thermalTubeRoofPanelsUri;
            thermalTubeWallSouthPanelsUri = graph + thermalTubeWallSouthPanelsUri;
            thermalTubeWallNorthPanelsUri = graph + thermalTubeWallNorthPanelsUri;
            thermalTubeWallEastPanelsUri = graph + thermalTubeWallEastPanelsUri;
            thermalTubeWallWestPanelsUri = graph + thermalTubeWallWestPanelsUri;
        }
        else{
            heatingUri = ontoUBEMMPUri + heatingUri;
            coolingUri = ontoUBEMMPUri + coolingUri;
            pvRoofPanelsUri = ontoUBEMMPUri + pvRoofPanelsUri;
            pvWallSouthPanelsUri = ontoUBEMMPUri + pvWallSouthPanelsUri;
            pvWallNorthPanelsUri = ontoUBEMMPUri + pvWallNorthPanelsUri;
            pvWallEastPanelsUri = ontoUBEMMPUri + pvWallEastPanelsUri;
            pvWallWestPanelsUri = ontoUBEMMPUri + pvWallWestPanelsUri;
            pvtPlateRoofPanelsUri = ontoUBEMMPUri + pvtPlateRoofPanelsUri;
            pvtPlateWallSouthPanelsUri = ontoUBEMMPUri + pvtPlateWallSouthPanelsUri;
            pvtPlateWallNorthPanelsUri = ontoUBEMMPUri + pvtPlateWallNorthPanelsUri;
            pvtPlateWallEastPanelsUri = ontoUBEMMPUri + pvtPlateWallEastPanelsUri;
            pvtPlateWallWestPanelsUri = ontoUBEMMPUri + pvtPlateWallWestPanelsUri;
            pvtTubeRoofPanelsUri = ontoUBEMMPUri + pvtTubeRoofPanelsUri;
            pvtTubeWallSouthPanelsUri = ontoUBEMMPUri + pvtTubeWallSouthPanelsUri;
            pvtTubeWallNorthPanelsUri = ontoUBEMMPUri + pvtTubeWallNorthPanelsUri;
            pvtTubeWallEastPanelsUri = ontoUBEMMPUri + pvtTubeWallEastPanelsUri;
            pvtTubeWallWestPanelsUri = ontoUBEMMPUri + pvtTubeWallWestPanelsUri;
            thermalPlateRoofPanelsUri = ontoUBEMMPUri + thermalPlateRoofPanelsUri;
            thermalPlateWallSouthPanelsUri = ontoUBEMMPUri + thermalPlateWallSouthPanelsUri;
            thermalPlateWallNorthPanelsUri = ontoUBEMMPUri + thermalPlateWallNorthPanelsUri;
            thermalPlateWallEastPanelsUri = ontoUBEMMPUri + thermalPlateWallEastPanelsUri;
            thermalPlateWallWestPanelsUri = ontoUBEMMPUri + thermalPlateWallWestPanelsUri;
            thermalTubeRoofPanelsUri = ontoUBEMMPUri + thermalTubeRoofPanelsUri;
            thermalTubeWallSouthPanelsUri = ontoUBEMMPUri + thermalTubeWallSouthPanelsUri;
            thermalTubeWallNorthPanelsUri = ontoUBEMMPUri + thermalTubeWallNorthPanelsUri;
            thermalTubeWallEastPanelsUri = ontoUBEMMPUri + thermalTubeWallEastPanelsUri;
            thermalTubeWallWestPanelsUri = ontoUBEMMPUri + thermalTubeWallWestPanelsUri;
        }

        // save om:measure uris for scalars and create om:quantity uris for scalars and time series
        // (time series om:measure iris already created in createTimeSeries)
        for (String measurement: SCALARS) {
            String measure = measurement+"Value_" + UUID.randomUUID() + "/";
            String quantity = measurement+"Quantity_" + UUID.randomUUID() + "/";
            if (!graph.isEmpty()){
                measure = graph + measure;
                quantity = graph + quantity;
            }
            else{
                measure = ontoUBEMMPUri + measure;
                quantity = ontoUBEMMPUri + quantity;
            }
            scalarIris.put(measurement, measure);

            switch(measurement){
                case(KEY_PV_ROOF_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, pvRoofPanelsUri, "ontoubemmp:RoofPVPanels", quantity, measure, scalars.get(KEY_PV_ROOF_AREA).get(uriCounter), "ontoubemmp:PVPanelsArea");
                    break;
                case(KEY_PV_WALL_SOUTH_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, pvWallSouthPanelsUri, "ontoubemmp:SouthWallPVPanels", quantity, measure, scalars.get(KEY_PV_WALL_SOUTH_AREA).get(uriCounter), "ontoubemmp:PVPanelsArea");
                    break;
                case(KEY_PV_WALL_NORTH_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, pvWallNorthPanelsUri, "ontoubemmp:NorthWallPVPanels", quantity, measure, scalars.get(KEY_PV_WALL_NORTH_AREA).get(uriCounter), "ontoubemmp:PVPanelsArea");
                    break;
                case(KEY_PV_WALL_EAST_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, pvWallEastPanelsUri, "ontoubemmp:EastWallPVPanels", quantity, measure, scalars.get(KEY_PV_WALL_EAST_AREA).get(uriCounter), "ontoubemmp:PVPanelsArea");
                    break;
                case(KEY_PV_WALL_WEST_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, pvWallWestPanelsUri, "ontoubemmp:WestWallPVPanels", quantity, measure, scalars.get(KEY_PV_WALL_WEST_AREA).get(uriCounter), "ontoubemmp:PVPanelsArea");
                    break;
                case(KEY_PVT_PLATE_ROOF_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, pvtPlateRoofPanelsUri, "ontoubemmp:RoofPVTPlateCollectors", quantity, measure, scalars.get(KEY_PVT_PLATE_ROOF_AREA).get(uriCounter), "ontoubemmp:PVTPlateCollectorsArea");
                    break;
                case(KEY_PVT_PLATE_WALL_SOUTH_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, pvtPlateWallSouthPanelsUri, "ontoubemmp:SouthWallPVTPlateCollectors", quantity, measure, scalars.get(KEY_PVT_PLATE_WALL_SOUTH_AREA).get(uriCounter), "ontoubemmp:PVTPlateCollectorsArea");
                    break;
                case(KEY_PVT_PLATE_WALL_NORTH_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, pvtPlateWallNorthPanelsUri, "ontoubemmp:NorthWallPVTPlateCollectors", quantity, measure, scalars.get(KEY_PVT_PLATE_WALL_NORTH_AREA).get(uriCounter), "ontoubemmp:PVTPlateCollectorsArea");
                    break;
                case(KEY_PVT_PLATE_WALL_EAST_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, pvtPlateWallEastPanelsUri, "ontoubemmp:EastWallPVTPlateCollectors", quantity, measure, scalars.get(KEY_PVT_PLATE_WALL_EAST_AREA).get(uriCounter), "ontoubemmp:PVTPlateCollectorsArea");
                    break;
                case(KEY_PVT_PLATE_WALL_WEST_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, pvtPlateWallWestPanelsUri, "ontoubemmp:WestWallPVTPlateCollectors", quantity, measure, scalars.get(KEY_PVT_PLATE_WALL_WEST_AREA).get(uriCounter), "ontoubemmp:PVTPlateCollectorsArea");
                    break;
                case(KEY_PVT_TUBE_ROOF_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, pvtTubeRoofPanelsUri, "ontoubemmp:RoofPVTTubeCollectors", quantity, measure, scalars.get(KEY_PVT_TUBE_ROOF_AREA).get(uriCounter), "ontoubemmp:PVTTubeCollectorsArea");
                    break;
                case(KEY_PVT_TUBE_WALL_SOUTH_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, pvtTubeWallSouthPanelsUri, "ontoubemmp:SouthWallPVTTubeCollectors", quantity, measure, scalars.get(KEY_PVT_TUBE_WALL_SOUTH_AREA).get(uriCounter), "ontoubemmp:PVTTubeCollectorsArea");
                    break;
                case(KEY_PVT_TUBE_WALL_NORTH_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, pvtTubeWallNorthPanelsUri, "ontoubemmp:NorthWallPVTTubeCollectors", quantity, measure, scalars.get(KEY_PVT_TUBE_WALL_NORTH_AREA).get(uriCounter), "ontoubemmp:PVTTubeCollectorsArea");
                    break;
                case(KEY_PVT_TUBE_WALL_EAST_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, pvtTubeWallEastPanelsUri, "ontoubemmp:EastWallPVTTubeCollectors", quantity, measure, scalars.get(KEY_PVT_TUBE_WALL_EAST_AREA).get(uriCounter), "ontoubemmp:PVTTubeCollectorsArea");
                    break;
                case(KEY_PVT_TUBE_WALL_WEST_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, pvtTubeWallWestPanelsUri, "ontoubemmp:WestWallPVTTubeCollectors", quantity, measure, scalars.get(KEY_PVT_TUBE_WALL_WEST_AREA).get(uriCounter), "ontoubemmp:PVTTubeCollectorsArea");
                    break;
                case(KEY_THERMAL_PLATE_ROOF_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, thermalPlateRoofPanelsUri, "ontoubemmp:RoofThermalPlateCollectors", quantity, measure, scalars.get(KEY_THERMAL_PLATE_ROOF_AREA).get(uriCounter), "ontoubemmp:ThermalPlateCollectorsArea");
                    break;
                case(KEY_THERMAL_PLATE_WALL_SOUTH_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, thermalPlateWallSouthPanelsUri, "ontoubemmp:SouthWallThermalPlateCollectors", quantity, measure, scalars.get(KEY_THERMAL_PLATE_WALL_SOUTH_AREA).get(uriCounter), "ontoubemmp:ThermalPlateCollectorsArea");
                    break;
                case(KEY_THERMAL_PLATE_WALL_NORTH_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, thermalPlateWallNorthPanelsUri, "ontoubemmp:NorthWallThermalPlateCollectors", quantity, measure, scalars.get(KEY_THERMAL_PLATE_WALL_NORTH_AREA).get(uriCounter), "ontoubemmp:ThermalPlateCollectorsArea");
                    break;
                case(KEY_THERMAL_PLATE_WALL_EAST_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, thermalPlateWallEastPanelsUri, "ontoubemmp:EastWallThermalPlateCollectors", quantity, measure, scalars.get(KEY_THERMAL_PLATE_WALL_EAST_AREA).get(uriCounter), "ontoubemmp:ThermalPlateCollectorsArea");
                    break;
                case(KEY_THERMAL_PLATE_WALL_WEST_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, thermalPlateWallWestPanelsUri, "ontoubemmp:WestWallThermalPlateCollectors", quantity, measure, scalars.get(KEY_THERMAL_PLATE_WALL_WEST_AREA).get(uriCounter), "ontoubemmp:ThermalPlateCollectorsArea");
                    break;
                case(KEY_THERMAL_TUBE_ROOF_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, thermalTubeRoofPanelsUri, "ontoubemmp:RoofThermalTubeCollectors", quantity, measure, scalars.get(KEY_THERMAL_TUBE_ROOF_AREA).get(uriCounter), "ontoubemmp:ThermalTubeCollectorsArea");
                    break;
                case(KEY_THERMAL_TUBE_WALL_SOUTH_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, thermalTubeWallSouthPanelsUri, "ontoubemmp:SouthWallThermalTubeCollectors", quantity, measure, scalars.get(KEY_THERMAL_TUBE_WALL_SOUTH_AREA).get(uriCounter), "ontoubemmp:ThermalTubeCollectorsArea");
                    break;
                case(KEY_THERMAL_TUBE_WALL_NORTH_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, thermalTubeWallNorthPanelsUri, "ontoubemmp:NorthWallThermalTubeCollectors", quantity, measure, scalars.get(KEY_THERMAL_TUBE_WALL_NORTH_AREA).get(uriCounter), "ontoubemmp:ThermalTubeCollectorsArea");
                    break;
                case(KEY_THERMAL_TUBE_WALL_EAST_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, thermalTubeWallEastPanelsUri, "ontoubemmp:EastWallThermalTubeCollectors", quantity, measure, scalars.get(KEY_THERMAL_TUBE_WALL_EAST_AREA).get(uriCounter), "ontoubemmp:ThermalTubeCollectorsArea");
                    break;
                case(KEY_THERMAL_TUBE_WALL_WEST_AREA):
                    createSolarGeneratorAreaUpdate(wb, buildingUri, thermalTubeWallWestPanelsUri, "ontoubemmp:WestWallThermalTubeCollectors", quantity, measure, scalars.get(KEY_THERMAL_TUBE_WALL_WEST_AREA).get(uriCounter), "ontoubemmp:ThermalTubeCollectorsArea");
                    break;
            }
        }

        for (String measurement: TIME_SERIES) {
            String quantity = measurement+"Quantity_" + UUID.randomUUID() + "/";
            quantity = !graph.isEmpty() ? graph + quantity : ontoUBEMMPUri + quantity;
            if (measurement.equals(KEY_GRID_CONSUMPTION) || measurement.equals(KEY_ELECTRICITY_CONSUMPTION)) {
                createConsumptionUpdate(wb, buildingUri, "ontoubemmp:" + measurement, quantity, tsIris.get(measurement));
            }
            else if (measurement.equals(KEY_COOLING_CONSUMPTION)) {
                createDeviceConsumptionUpdate(wb, buildingUri, coolingUri, "ontoubemmp:CoolingSystem","ontoubemmp:ThermalConsumption" , quantity, tsIris.get(measurement));
            }
            else if (measurement.equals(KEY_HEATING_CONSUMPTION)) {
                createDeviceConsumptionUpdate(wb, buildingUri, heatingUri,"purlEnaeq:HeatingSystem","ontoubemmp:ThermalConsumption" , quantity, tsIris.get(measurement));
            }
            else if (measurement.equals(KEY_PV_ROOF_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvRoofPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:ElectricitySupply");
            }
            else if (measurement.equals(KEY_PV_WALL_SOUTH_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvWallSouthPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:ElectricitySupply");
            }
            else if (measurement.equals(KEY_PV_WALL_NORTH_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvWallNorthPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:ElectricitySupply");
            }
            else if (measurement.equals(KEY_PV_WALL_EAST_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvWallEastPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:ElectricitySupply");
            }
            else if (measurement.equals(KEY_PV_WALL_WEST_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvWallWestPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:ElectricitySupply");
            }
            else if (measurement.equals(KEY_PVT_PLATE_ROOF_E_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvtPlateRoofPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:ElectricitySupply");
            }
            else if (measurement.equals(KEY_PVT_PLATE_WALL_SOUTH_E_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvtPlateWallSouthPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:ElectricitySupply");
            }
            else if (measurement.equals(KEY_PVT_PLATE_WALL_NORTH_E_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvtPlateWallNorthPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:ElectricitySupply");
            }
            else if (measurement.equals(KEY_PVT_PLATE_WALL_EAST_E_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvtPlateWallEastPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:ElectricitySupply");
            }
            else if (measurement.equals(KEY_PVT_PLATE_WALL_WEST_E_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvtPlateWallWestPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:ElectricitySupply");
            }
            else if (measurement.equals(KEY_PVT_PLATE_ROOF_Q_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvtPlateRoofPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:HeatSupply");
            }
            else if (measurement.equals(KEY_PVT_PLATE_WALL_SOUTH_Q_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvtPlateWallSouthPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:HeatSupply");
            }
            else if (measurement.equals(KEY_PVT_PLATE_WALL_NORTH_Q_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvtPlateWallNorthPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:HeatSupply");
            }
            else if (measurement.equals(KEY_PVT_PLATE_WALL_EAST_Q_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvtPlateWallEastPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:HeatSupply");
            }
            else if (measurement.equals(KEY_PVT_PLATE_WALL_WEST_Q_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvtPlateWallWestPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:HeatSupply");
            }
            else if (measurement.equals(KEY_PVT_TUBE_ROOF_E_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvtTubeRoofPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:ElectricitySupply");
            }
            else if (measurement.equals(KEY_PVT_TUBE_WALL_SOUTH_E_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvtTubeWallSouthPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:ElectricitySupply");
            }
            else if (measurement.equals(KEY_PVT_TUBE_WALL_NORTH_E_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvtTubeWallNorthPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:ElectricitySupply");
            }
            else if (measurement.equals(KEY_PVT_TUBE_WALL_EAST_E_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvtTubeWallEastPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:ElectricitySupply");
            }
            else if (measurement.equals(KEY_PVT_TUBE_WALL_WEST_E_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvtTubeWallWestPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:ElectricitySupply");
            }
            else if (measurement.equals(KEY_PVT_TUBE_ROOF_Q_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvtTubeRoofPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:HeatSupply");
            }
            else if (measurement.equals(KEY_PVT_TUBE_WALL_SOUTH_Q_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvtTubeWallSouthPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:HeatSupply");
            }
            else if (measurement.equals(KEY_PVT_TUBE_WALL_NORTH_Q_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvtTubeWallNorthPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:HeatSupply");
            }
            else if (measurement.equals(KEY_PVT_TUBE_WALL_EAST_Q_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvtTubeWallEastPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:HeatSupply");
            }
            else if (measurement.equals(KEY_PVT_TUBE_WALL_WEST_Q_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, pvtTubeWallWestPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:HeatSupply");
            }
            else if (measurement.equals(KEY_THERMAL_PLATE_ROOF_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, thermalPlateRoofPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:HeatSupply");
            }
            else if (measurement.equals(KEY_THERMAL_PLATE_WALL_SOUTH_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, thermalPlateWallSouthPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:HeatSupply");
            }
            else if (measurement.equals(KEY_THERMAL_PLATE_WALL_NORTH_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, thermalPlateWallNorthPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:HeatSupply");
            }
            else if (measurement.equals(KEY_THERMAL_PLATE_WALL_EAST_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, thermalPlateWallEastPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:HeatSupply");
            }
            else if (measurement.equals(KEY_THERMAL_PLATE_WALL_WEST_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, thermalPlateWallWestPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:HeatSupply");
            }
            else if (measurement.equals(KEY_THERMAL_TUBE_ROOF_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, thermalTubeRoofPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:HeatSupply");
            }
            else if (measurement.equals(KEY_THERMAL_TUBE_WALL_SOUTH_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, thermalTubeWallSouthPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:HeatSupply");
            }
            else if (measurement.equals(KEY_THERMAL_TUBE_WALL_NORTH_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, thermalTubeWallNorthPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:HeatSupply");
            }
            else if (measurement.equals(KEY_THERMAL_TUBE_WALL_EAST_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, thermalTubeWallEastPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:HeatSupply");
            }
            else if (measurement.equals(KEY_THERMAL_TUBE_WALL_WEST_SUPPLY)){
                createSolarGeneratorSupplyUpdate(wb, thermalTubeWallWestPanelsUri, quantity, tsIris.get(measurement), "ontoubemmp:HeatSupply");
            }
        }

        if (graph.isEmpty()){
            ub.addInsert(wb);
        }
        else{
            ub.addInsert(NodeFactory.createURI(graph), wb);
        }

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
     * @param graph graph name
     */
    public void updateScalars(String uriString, String route, LinkedHashMap<String,String> scalarIris, LinkedHashMap<String, List<String>> scalars, Integer uriCounter, String graph) {

        for (String measurement: SCALARS) {
            WhereBuilder wb1 = new WhereBuilder().addPrefix("om", unitOntologyUri)
                    .addWhere(NodeFactory.createURI(scalarIris.get(measurement)), "om:hasNumericalValue", "?s");
            UpdateBuilder ub1 = new UpdateBuilder().addPrefix("om", unitOntologyUri)
                    .addWhere(wb1);

            WhereBuilder wb2 = new WhereBuilder().addPrefix("om", unitOntologyUri)
                    .addWhere(NodeFactory.createURI(scalarIris.get(measurement)), "om:hasNumericalValue", scalars.get(measurement).get(uriCounter));
            UpdateBuilder ub2 = new UpdateBuilder().addPrefix("om", unitOntologyUri);

            if (!graph.isEmpty()){
                ub1.addDelete(NodeFactory.createURI(graph), wb1);
                ub2.addInsert(NodeFactory.createURI(graph), wb2);
            }
            else{
                ub1.addDelete(wb1);
                ub2.addInsert(wb2);
            }

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
        tsClient = new TimeSeriesClient<>(storeClient, OffsetDateTime.class);
        List<String> iris = new ArrayList<>();
        iris.add(dataIri);
        try (Connection conn = rdbStoreClient.getConnection()) {
            TimeSeries<OffsetDateTime> data = tsClient.getTimeSeries(iris, conn);
            return data;
        }
        catch (SQLException e) {
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

    /**
     * Extracts the footprint of the building from its ground surface geometries
     * @param results JSONArray of the query results for ground surface geometries
     * @return footprint as a string
     */
    private String extractFootprint(JSONArray results){
        double distance = 0.00001;
        double increment = 0.00001;

        Polygon footprintPolygon;
        Geometry footprintRing;
        Coordinate[] footprintCoordinates;
        ArrayList<Geometry> geometries = new ArrayList<>();
        GeometryFactory geoFac = new GeometryFactory();
        GeometryCollection geoCol;
        Geometry merged;
        Geometry temp;
        String geoType;

        if (results.length() == 1){
            footprintPolygon = (Polygon) toPolygon(ignoreHole(results.getJSONObject(0).get("geometry").toString(), results.getJSONObject(0).get("datatype").toString()));
        }

        else {
            for (int i = 0; i < results.length(); i++) {
                temp = toPolygon(ignoreHole(results.getJSONObject(i).get("geometry").toString(), results.getJSONObject(i).get("datatype").toString()));
                if (!temp.isValid()){
                    temp = GeometryFixer.fix(temp);
                }
                geometries.add(temp);
            }

            geoCol = (GeometryCollection) geoFac.buildGeometry(geometries);

            merged = geoCol.union();

            geoType = merged.getGeometryType();

            while (geoType != "Polygon" || deflatePolygon(merged, distance).getGeometryType() != "Polygon"){
                distance += increment;

                for (int i = 0; i < geometries.size(); i++){
                    temp = inflatePolygon(geometries.get(i), distance);
                    if (!temp.isValid()){
                        temp = GeometryFixer.fix(temp);
                    }
                    geometries.set(i, temp);
                }

                geoCol = (GeometryCollection) geoFac.buildGeometry(geometries);
                merged = geoCol.union();
                geoType = merged.getGeometryType();
            }

            footprintPolygon = (Polygon) deflatePolygon(merged, distance);

            if (!footprintPolygon.isValid()){
                footprintPolygon = (Polygon) GeometryFixer.fix(footprintPolygon);
            }
        }

        footprintRing = footprintPolygon.getExteriorRing();

        footprintCoordinates = footprintRing.getCoordinates();

        return coordinatesToString(footprintCoordinates);
    }
    /**
     * Create a polygon with the given points
     * @param points points of the polygon as a string
     * @return a polygon
     */
    private Geometry toPolygon(String points){
        int ind = 0;
        GeometryFactory gF = new GeometryFactory();

        String[] arr = points.split("#");

        Coordinate[] coordinates = new Coordinate[(arr.length) / 3];

        for (int i = 0; i < arr.length; i += 3){
            coordinates[ind] = new Coordinate(Double.valueOf(arr[i]), Double.valueOf(arr[i+1]), Double.valueOf(arr[i+2]));
            ind++;
        }

        return gF.createPolygon(coordinates);
    }

    /**
     * Converts an array of coordinates into a string
     * @param coordinates array of footprint coordinates
     * @return coordinates as a string
     */
    private String coordinatesToString(Coordinate[] coordinates){
        String output = "";

        for (int i = 0; i < coordinates.length; i++){
            output = output + "#" + Double.toString(coordinates[i].getX()) + "#" + Double.toString(coordinates[i].getY()) + "#" + Double.toString(coordinates[i].getZ());
        }

        return output.substring(1, output.length());
    }

    /**
     * Inflates a polygon
     * @param geom polygon geometry
     * @param distance buffer distance
     * @return inflated polygon
     */
    private Geometry inflatePolygon(Geometry geom, Double distance){
        ArrayList<Double> zCoordinate = getPolygonZ(geom);
        BufferParameters bufferParameters = new BufferParameters();
        bufferParameters.setEndCapStyle(BufferParameters.CAP_ROUND);
        bufferParameters.setJoinStyle(BufferParameters.JOIN_MITRE);
        Geometry buffered = BufferOp.bufferOp(geom, distance, bufferParameters);
        buffered.setUserData(geom.getUserData());
        setPolygonZ(buffered, zCoordinate);
        return buffered;
    }

    /**
     * Deflates a polygon
     * @param geom polygon geometry
     * @param distance buffer distance
     * @return deflated polygon
     */
    private Geometry deflatePolygon(Geometry geom, Double distance){
        ArrayList<Double> zCoordinate = getPolygonZ(geom);
        BufferParameters bufferParameters = new BufferParameters();
        bufferParameters.setEndCapStyle(BufferParameters.CAP_ROUND);
        bufferParameters.setJoinStyle(BufferParameters.JOIN_MITRE);
        Geometry buffered = BufferOp.bufferOp(geom, distance * -1, bufferParameters);
        buffered.setUserData(geom.getUserData());
        setPolygonZ(buffered, zCoordinate);
        return buffered;
    }

    /**
     * Extract the z coordinates of the polygon vertices
     * @param geom polygon geometry
     * @return the z coordinates of the polygon vertices
     */
    private static ArrayList<Double> getPolygonZ(Geometry geom){
        Coordinate[] coordinates = geom.getCoordinates();
        ArrayList<Double> output = new ArrayList<>();

        for (int i = 0; i < coordinates.length; i++){
            output.add(coordinates[i].getZ());
        }

        return output;
    }

    /**
     * Sets a polygon's z coordinates to the values from zInput
     * @param geom polygon geometry
     * @param zInput ArrayList of values representing z coordinates
     */
    private void setPolygonZ(Geometry geom, ArrayList<Double> zInput){
        Double newZ = Double.NaN;

        for (int i = 0; i < zInput.size(); i++){
            if (!zInput.get(i).isNaN()){
                newZ = zInput.get(i);
                break;
            }
        }

        if(newZ.isNaN()){newZ = 10.0;}

        if (geom.getNumPoints() < zInput.size()) {
            while (geom.getNumPoints() != zInput.size()) {
                zInput.remove(zInput.size()-1);
            }
        }
        else {
            while (geom.getNumPoints() != zInput.size()) {
                zInput.add(1, newZ);
            }
        }

        Collections.replaceAll(zInput, Double.NaN, newZ);
        geom.apply(new CoordinateSequenceFilter() {
            @Override
            public void filter(CoordinateSequence cSeq, int i) {
                cSeq.getCoordinate(i).setZ(zInput.get(i));
            }
            @Override
            public boolean isDone() {
                return false;
            }
            @Override
            public boolean isGeometryChanged() {
                return false;
            }
        });
    }

    /**
     * Returns the ground geometry's exterior ring
     * @param geometry ground geometry
     * @param polygonType polygon datatype, such as "<...\POLYGON-3-45-15>"
     * @return ground geometry with no holes
     */
    private String ignoreHole(String geometry, String polygonType){
        int num;
        int ind;
        int count = 1;

        String[] split = polygonType.split("-");

        if (split.length < 4){return geometry;}

        num = Integer.parseInt(split[2]);

        ind = geometry.indexOf("#");

        while (count != num){
            ind = geometry.indexOf("#", ind + 1);
            count++;
        }
        return geometry.substring(0, ind);
    }

    /**
     * Returns timeseriesclient.properties path as string
     * @return timeseriesclient.properties path as string
     */
    private String getTimeSeriesPropsPath(){
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                return new File(
                        Objects.requireNonNull(getClass().getClassLoader().getResource(TIME_SERIES_CLIENT_PROPS)).toURI()).getAbsolutePath();
            } else {
                return FS + "target" + FS + "classes" + FS + TIME_SERIES_CLIENT_PROPS;
            }
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            throw new JPSRuntimeException(e);
        }
    }

    /**
     * Sets rdbStoreClient with the database url, username, and password from the file at path
     * @param path timeseriesclient.properties path as string
     */
    protected void setRDBClient(String path){
        try {
            FileInputStream in = new FileInputStream(path);
            Properties props = new Properties();
            props.load(in);
            in.close();

            rdbStoreClient = new RemoteRDBStoreClient(props.getProperty("db.url"), props.getProperty("db.user"), props.getProperty("db.password"));
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new JPSRuntimeException(e);
        }
    }

    /**
     * Sets store client to the query and update endpoint of route, so that the time series client queries and updates from the same endpoint as route
     * @param route access agent route
     */
    private void setStoreClient(String route){
        JSONObject queryResult = this.getEndpoints(route);

        String queryEndpoint = queryResult.getString(JPSConstants.QUERY_ENDPOINT);
        String updateEndpoint = queryResult.getString(JPSConstants.UPDATE_ENDPOINT);

        if (!isDockerized()){
            queryEndpoint = queryEndpoint.replace("host.docker.internal", "localhost");
            updateEndpoint = updateEndpoint.replace("host.docker.internal", "localhost");
        }

        storeClient = new RemoteStoreClient(queryEndpoint, updateEndpoint);
    }

    /**
     * Convert OntoBuiltEnv building usage type to convention used by CEA
     * @param usage OntoBuiltEnv building usage type
     * @return building usage per CEA convention
     */
    public String toCEAConvention(String usage){
        switch(usage){
            case("DOMESTIC"):
                return "MULTI_RES";
            case("SINGLERESIDENTIAL"):
                return "SINGLE_RES";
            case("MULTIRESIDENTIAL"):
                return "MULTI_RES";
            case("EMERGENCYSERVICE"):
                return "HOSPITAL";
            case("FIRESTATION"):
                return "HOSPITAL";
            case("POLICESTATION"):
                return "HOSPITAL";
            case("MEDICALCARE"):
                return "HOSPITAL";
            case("HOSPITAL"):
                return usage;
            case("CLINIC"):
                return "HOSPITAL";
            case("EDUCATION"):
                return "UNIVERSITY";
            case("SCHOOL"):
                return usage;
            case("UNIVERSITYFACILITY"):
                return "UNIVERSITY";
            case("OFFICE"):
                return usage;
            case("RETAILESTABLISHMENT"):
                return "RETAIL";
            case("RELIGIOUSFACILITY"):
                return "MUSEUM";
            case("INDUSTRIALFACILITY"):
                return "INDUSTRIAL";
            case("EATINGESTABLISHMENT"):
                return "RESTAURANT";
            case("DRINKINGESTABLISHMENT"):
                return "RESTAURANT";
            case("HOTEL"):
                return usage;
            case("SPORTSFACILITY"):
                return "GYM";
            case("CULTURALFACILITY"):
                return "MUSEUM";
            case("TRANSPORTFACILITY"):
                return "INDUSTRIAL";
            case("NON-DOMESTIC"):
                return "INDUSTRIAL";
            default:
                return "MULTI_RES";
        }
    }

    /**
     * Checks if route is enabled to support quads
     * @param route endpoint to check
     */
    public void checkQuadsEnabled(String route){
        WhereBuilder wb = new WhereBuilder()
                .addGraph("?g","?s", "?p", "?o");
        SelectBuilder sb = new SelectBuilder()
                .addVar("?g")
                .addWhere(wb)
                .setLimit(1);
        
        // first check that querying from route works
        checkEndpoint(route);
        // check if query with graph works for route
        try{
            this.queryStore(route, sb.build().toString());
        }
        catch (Exception e){
            e.printStackTrace();
            throw new JPSRuntimeException("ceaEndpoint does not support graph");
        }
    }

    /**
     * Basic query to check if route is queryable
     * @param route endpoint to check
     */
    public void checkEndpoint(String route){
        WhereBuilder wb = new WhereBuilder()
                .addWhere("?s", "?p", "?o");
        SelectBuilder sb = new SelectBuilder()
                .addVar("?g")
                .addWhere(wb)
                .setLimit(1);

        try{
            this.queryStore(route, sb.build().toString());
        }
        catch (Exception e){
            e.printStackTrace();
            throw new JPSRuntimeException(e);
        }
    }

    /**
     * Checks if the agent is running in Docker
     * @return true if running in Docker, false otherwise
     */
    private boolean isDockerized(){
        File f = new File("/.dockerenv");

        return f.exists();
    }
}
