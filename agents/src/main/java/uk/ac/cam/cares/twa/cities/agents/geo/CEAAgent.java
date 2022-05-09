package uk.ac.cam.cares.twa.cities.agents.geo;

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
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.*;
import javax.servlet.annotation.WebServlet;
import java.util.concurrent.*;
import java.net.URISyntaxException;
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
    public static final String KEY_GRID_DEMAND = "grid_demand";
    public static final String KEY_ELECTRICITY_DEMAND = "electricity_demand";
    public static final String KEY_HEATING_DEMAND = "heating_demand";
    public static final String KEY_COOLING_DEMAND = "cooling_demand";
    public static final String KEY_PV_AREA = "PV_area";
    public static final String KEY_PV_SUPPLY= "PV_supply";
    public static final String KEY_TIME_SERIES= "timeSeries";
    public static final String KEY_TIMES= "times";

    public final int NUM_CEA_THREADS = 1;
    private final ThreadPoolExecutor CEAExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_CEA_THREADS);

    private static final String TIME_SERIES_CLIENT_PROPS = "timeseriesclient.properties";
    private static final String MAPPING_FOLDER = "mappings";
    private TimeSeriesClient<OffsetDateTime> tsClient;
    private List<JSONKeyToIriMapper> mappings;
    public static final String timeUnit = OffsetDateTime.class.getSimpleName();
    private List<List<String>> fixedIris = new ArrayList<>();

    // Variables fetched from CEAAgentConfig.properties file.
    private String ocgmlUri;
    private String ontoUBEMMPUri;
    private String rdfUri;
    private String owlUri;
    private String purlEnaeqUri;
    private String purlInfrastructureUri;
    private String thinkhomeUri;
    private static String unitOntologyUri;
    private static String QUERY_ROUTE;
    private static String UPDATE_ROUTE;
    private String requestUrl;
    private String targetUrl;
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
                    // parse time series data
                    String timesArrayString = requestParams.get(KEY_TIMES).toString();
                    JSONArray timesArray = new JSONArray(timesArrayString);
                    List<OffsetDateTime> times = new ArrayList<>();

                    for (int i = 0; i < timesArray.length(); i++) {
                        OffsetDateTime odt = OffsetDateTime.parse(timesArray.getString(i));
                        times.add(odt);
                    }

                    String timesSeriesArrayString = requestParams.get(KEY_TIME_SERIES).toString();
                    JSONArray timeSeriesArray = new JSONArray(timesSeriesArrayString);
                    List<List<List<?>>> timeSeries = new ArrayList<>();

                    for (int i = 0; i < timeSeriesArray.length(); i++) {
                        JSONArray timeSeriesIriArray = new JSONArray(timeSeriesArray.get(i).toString());
                        List<List<?>> timeSeriesMaps = new ArrayList<>();
                        for (int j = 0; j < timeSeriesIriArray.length(); j++) {
                            JSONArray timeSeriesMappingArray = new JSONArray(timeSeriesIriArray.get(j).toString());
                            List<Double> timeSeriesList = new ArrayList<>();
                            for (int k = 0; k < timeSeriesMappingArray.length(); k++) {
                                timeSeriesList.add(Double.valueOf(timeSeriesMappingArray.get(k).toString()));
                            }
                            timeSeriesMaps.add(timeSeriesList);
                        }
                        timeSeries.add(timeSeriesMaps);
                    }

                    // parse PV area data
                    String areaArrayString = requestParams.get(KEY_PV_AREA).toString();
                    JSONArray areaArray = new JSONArray(areaArrayString);
                    List<String> areas = new ArrayList<>();
                    for (int j = 0; j < areaArray.length(); j++) {
                        areas.add(areaArray.getString(j));
                    }

                    for (int i = 0; i < uriArray.length(); i++) {
                        String uri = uriArray.getString(i);
                        addDataToTimeSeries(timeSeries.get(i), times, fixedIris.get(i));
                        String buildingUri = checkEnergyProfileInitialised(mappings.get(i), uri);
                        if (buildingUri == "") {
                            buildingUri = sparqlUpdate(areas.get(i), mappings.get(i), uri);
                        }
                        if (!checkGenAttributeInitialised(uri)) sparqlGenAttributeUpdate(uri, buildingUri);
                    }
                } else if (requestUrl.contains(URI_ACTION)) {
                    ArrayList<CEAInputData> testData = new ArrayList<>();
                    ArrayList<String> uriStringArray = new ArrayList<>();
                    for (int i = 0; i < uriArray.length(); i++) {
                        String uri = uriArray.getString(i);
                        uriStringArray.add(uri);
                        createTimeSeries(uri);
                        testData.add(new CEAInputData(getValue(uri, "Envelope"), getValue(uri, "Height")));
                    }
                    runCEA(testData, uriStringArray, 0);
                }
            } else if (requestUrl.contains(URI_QUERY)) {
                for (int i = 0; i < uriArray.length(); i++) {
                    String uri = uriArray.getString(i);
                    JSONObject data = new JSONObject();
                    for (Field key : CEAOutputData.class.getDeclaredFields()) {
                        ArrayList<String> results = getDataIRI(uri, key.getName());
                        if (!results.isEmpty()) {
                            String value;
                            if (key.getName() != "PV_area") {
                                value = calculateAverage(retrieveData(results.get(0)), results.get(0));
                            } else {
                                value = results.get(0);
                            }
                            value += " " + getUnit(results.get(1));
                            data.put(key.getName(), value);
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
     * Validates input specific to requests coming to URI_UPDATE
     *
     * @param requestParams - request body in JSON format
     * @return boolean saying if request is valid or not
     */
    private boolean validateUpdateInput(JSONObject requestParams) {
        boolean error = true;
        if (!requestParams.get(KEY_IRI).toString().isEmpty() ||
                !requestParams.get(KEY_TARGET_URL).toString().isEmpty() ||
                !requestParams.get(KEY_GRID_DEMAND).toString().isEmpty() ||
                !requestParams.get(KEY_ELECTRICITY_DEMAND).toString().isEmpty() ||
                !requestParams.get(KEY_HEATING_DEMAND).toString().isEmpty() ||
                !requestParams.get(KEY_COOLING_DEMAND).toString().isEmpty() ||
                !requestParams.get(KEY_PV_AREA).toString().isEmpty() ||
                !requestParams.get(KEY_PV_SUPPLY).toString().isEmpty() ||
                !requestParams.get(KEY_TIMES).toString().isEmpty() ||
                !requestParams.get(KEY_TIME_SERIES).toString().isEmpty()){
            error = false;
        }

        return error;
    }

    /**
     * Validates input specific to requests coming to URI_ACTION
     *
     * @param requestParams - request body in JSON format
     * @return boolean saying if request is valid or not
     */
    private boolean validateActionInput(JSONObject requestParams) {
        boolean error = true;
        if (!requestParams.get(KEY_TARGET_URL).toString().isEmpty() || !requestParams.get(KEY_IRI).toString().isEmpty()){
            error = false;
        }
        return error;
    }

    /**
     * Validates input specific to requests coming to URI_QUERY
     *
     * @param requestParams - request body in JSON format
     * @return boolean saying if request is valid or not
     */
    private boolean validateQueryInput(JSONObject requestParams) {
        boolean error = true;
        if (!requestParams.get(KEY_IRI).toString().isEmpty()){
            error = false;
        }
        return error;
    }


    /**
     * Gets variables from config
     *
     */
    private void readConfig() {
        ResourceBundle config = ResourceBundle.getBundle("CEAAgentConfig");
        UPDATE_ROUTE = config.getString("uri.route.local");
        QUERY_ROUTE = config.getString("uri.route.local");
        ocgmlUri = config.getString("uri.ontology.ontocitygml");
        unitOntologyUri = config.getString("uri.ontology.om");
        ontoUBEMMPUri = config.getString("uri.ontology.ontoubemmp");
        rdfUri = config.getString("uri.ontology.rdf");
        owlUri = config.getString("uri.ontology.owl");
        purlEnaeqUri=config.getString("uri.ontology.purl.enaeq");
        thinkhomeUri=config.getString("uri.ontology.thinkhome");
        purlInfrastructureUri=config.getString("uri.ontology.purl.infrastructure");
    }

    /**
     * runs CEATask on CEAInputData and returns CEAOutputData
     *
     * @param buildingData input data on building envelope and height
     * @param threadNumber int tracking thread that is running
     */
    private void runCEA(ArrayList<CEAInputData> buildingData, ArrayList<String> uris, int threadNumber) {
        try {
            RunCEATask task = new RunCEATask(buildingData, new URI(targetUrl), uris, fixedIris, mappings, threadNumber);
            CEAExecutor.execute(task);
        }
        catch(URISyntaxException e){
        }
    }
    /**
     * Creates and intialises a time series using the time series client
     *
     * @param uriString input city object id
     */
    private void createTimeSeries(String uriString) {
        try{
            tsClient =  new TimeSeriesClient<>(OffsetDateTime.class, new File(
                    Objects.requireNonNull(getClass().getClassLoader().getResource(TIME_SERIES_CLIENT_PROPS)).toURI()).getAbsolutePath());
            String mappingFolder = new File(
                    Objects.requireNonNull(getClass().getClassLoader().getResource(MAPPING_FOLDER)).toURI()).getAbsolutePath();

            mappings = new ArrayList<>();
            File folder = new File(mappingFolder);
            File[] mappingFiles = folder.listFiles();

            // Make sure the folder exists and contains files
            if (mappingFiles == null) {
                throw new IOException("Folder does not exist: " + mappingFolder);
            }
            if (mappingFiles.length == 0) {
                throw new IOException("No files in the folder: " + mappingFolder);
            }

            // Create a mapper for each file
            else {
                for (File mappingFile: mappingFiles) {
                    JSONKeyToIriMapper mapper = new JSONKeyToIriMapper(getGraph(uriString,ENERGY_PROFILE), mappingFile.getAbsolutePath());
                    mappings.add(mapper);
                    // Save the mappings back to the file to ensure using same IRIs next time
                    mapper.saveToFile(mappingFile.getAbsolutePath());
                }
            }

            for (JSONKeyToIriMapper mapping: mappings) {
                // The IRIs used by the current mapping
                List<String> iris = mapping.getAllIRIs();
                fixedIris.add(iris);
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
     * @param iri iris for time series data
     */
    private void addDataToTimeSeries(List<List<?>> values, List<OffsetDateTime> times, List<String> iri) {
        TimeSeries<OffsetDateTime> currentTimeSeries = new TimeSeries<>(times, iri, values);
        OffsetDateTime endDataTime = tsClient.getMaxTime(currentTimeSeries.getDataIRIs().get(0));

        if (endDataTime == null) {
            tsClient.addTimeSeriesData(currentTimeSeries);
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
        return String.join("/", Arrays.copyOfRange(splitUri, 0, splitUri.length - 2));
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
        return namespace + "/" + graph + "/";
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
     * executes query on SPARQL endpoint and retrieves requested value of building
     * @param uriString city object id
     * @param value building value requested
     * @return geometry as string
     */
    private String getValue(String uriString, String value)  {

        String result = "";

        Query q = getQuery(uriString, value);

        //Use access agent
        String queryResultString = this.query(QUERY_ROUTE, q.toString());

        JSONObject queryResultObject = new JSONObject(queryResultString);
        String resultString = queryResultObject.get("result").toString();
        JSONArray queryResultArray = new JSONArray(resultString);

        if(!queryResultArray.isEmpty()){
            if(value!="Envelope") {
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
     * @param results array of building surfaces
     * @return footprint geometry as string
     */
    private String getFootprint(JSONArray results){
        String footprint="";
        ArrayList<String> z_values = new ArrayList<>();
        Double minimum=Double.MAX_VALUE;

        for(Integer i=0; i<results.length(); i++){
            String geom = results.getJSONObject(i).get("Envelope").toString();
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
            case "Envelope":
                return getGeometryQuery(uriString);
            case "Height":
                return getHeightQuery(uriString);
        }
        return null;
    }

    /**
     * builds a SPARQL query for a specific URI to retrieve an envelope.
     * @param uriString city object id
     * @return returns a query string
     */
    /*private Query getGeometryQuery(String uriString) {
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
                    .addWhere("?surf", "ocgml:GeometryType", "?Envelope")
                    .addFilter("!isBlank(?Envelope)");
            SelectBuilder sb = new SelectBuilder()
                    .addVar("?Envelope")
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
     * builds a SPARQL update to add cityobject generic attribute that links to energy profile graph
     * @param uriString city object id
     * @param energyProfileBuildingUri building id in energy profile namespace
     */
    public void sparqlGenAttributeUpdate( String uriString, String energyProfileBuildingUri) {
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
        this.update(UPDATE_ROUTE, ur.toString());
    }
    /**
     * builds a SPARQL update using output from CEA simulations
     * @param PV_area the area of PV Cells
     * @param uriString city object id
     */
    public String sparqlUpdate( String PV_area, JSONKeyToIriMapper mapper, String uriString) {
        String outputGraphUri = getGraph(uriString,ENERGY_PROFILE);

        String buildingUri = outputGraphUri + "Building_UUID_" + UUID.randomUUID() + "/";
        String heatingUri = outputGraphUri + "HeatingSystem_UUID_" + UUID.randomUUID() + "/";
        String coolingUri = outputGraphUri + "CoolingSystem_UUID_" + UUID.randomUUID() + "/";
        String pvCellsUri = outputGraphUri + "PV_Cells_UUID_" + UUID.randomUUID()+ "/";
        String PVAreaUri = outputGraphUri + "PVArea_UUID_" + UUID.randomUUID()+ "/";
        String PVAreaValueUri = outputGraphUri + "PVAreaValue_UUID_" + UUID.randomUUID()+ "/";
        String gridConsumptionUri = outputGraphUri + "GridConsumption_UUID_" + UUID.randomUUID()+ "/";
        String electricityConsumptionUri = outputGraphUri + "ElectricityConsumption_UUID_" + UUID.randomUUID()+ "/";
        String heatingConsumptionUri = outputGraphUri + "HeatingConsumption_UUID_" + UUID.randomUUID()+ "/";
        String coolingConsumptionUri = outputGraphUri + "CoolingConsumption_UUID_" + UUID.randomUUID()+ "/";
        String PVSupplyUri = outputGraphUri + "PVSupply_UUID_" + UUID.randomUUID()+ "/";

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
                        .addInsert("?graph", NodeFactory.createURI(gridConsumptionUri), "om:hasValue", NodeFactory.createURI(mapper.getIRI("GridConsumptionMeasure")))
                        .addInsert("?graph", NodeFactory.createURI(mapper.getIRI("GridConsumptionMeasure")), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(mapper.getIRI("GridConsumptionMeasure")), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(mapper.getIRI("GridConsumptionMeasure")), "om:hasUnit", "om:kilowattHour")
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "purlEnaeq:consumesEnergy", NodeFactory.createURI(gridConsumptionUri))
                        .addInsert("?graph", NodeFactory.createURI(electricityConsumptionUri), "rdf:type", "ontoubemmp:ElectricityConsumption")
                        .addInsert("?graph", NodeFactory.createURI(electricityConsumptionUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(electricityConsumptionUri), "om:hasDimension", "om:energy-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(electricityConsumptionUri), "om:hasValue", NodeFactory.createURI(mapper.getIRI("ElectricityConsumptionMeasure")))
                        .addInsert("?graph", NodeFactory.createURI(mapper.getIRI("ElectricityConsumptionMeasure")), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(mapper.getIRI("ElectricityConsumptionMeasure")), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(mapper.getIRI("ElectricityConsumptionMeasure")), "om:hasUnit", "om:kilowattHour")
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "purlEnaeq:consumesEnergy", NodeFactory.createURI(electricityConsumptionUri))
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "ontoubemmp:hasDevice", NodeFactory.createURI(heatingUri))
                        .addInsert("?graph", NodeFactory.createURI(heatingUri), "rdf:type", "purlEnaeq:HeatingSystem")
                        .addInsert("?graph", NodeFactory.createURI(heatingUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(heatingConsumptionUri), "rdf:type", "ontoubemmp:ThermalConsumption")
                        .addInsert("?graph", NodeFactory.createURI(heatingConsumptionUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(heatingConsumptionUri), "om:hasDimension", "om:energy-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(heatingConsumptionUri), "om:hasValue", NodeFactory.createURI(mapper.getIRI("HeatingConsumptionMeasure")))
                        .addInsert("?graph", NodeFactory.createURI(mapper.getIRI("HeatingConsumptionMeasure")), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(mapper.getIRI("HeatingConsumptionMeasure")), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(mapper.getIRI("HeatingConsumptionMeasure")), "om:hasUnit", "om:kilowattHour")
                        .addInsert("?graph", NodeFactory.createURI(heatingUri), "purlEnaeq:consumesEnergy", NodeFactory.createURI(heatingConsumptionUri))
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "ontoubemmp:hasDevice", NodeFactory.createURI(coolingUri))
                        .addInsert("?graph", NodeFactory.createURI(coolingUri), "rdf:type", "ontoubemmp:CoolingSystem")
                        .addInsert("?graph", NodeFactory.createURI(coolingUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(coolingConsumptionUri), "rdf:type", "ontoubemmp:ThermalConsumption")
                        .addInsert("?graph", NodeFactory.createURI(coolingConsumptionUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(coolingConsumptionUri), "om:hasDimension", "om:energy-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(coolingConsumptionUri), "om:hasValue", NodeFactory.createURI(mapper.getIRI("CoolingConsumptionMeasure")))
                        .addInsert("?graph", NodeFactory.createURI(mapper.getIRI("CoolingConsumptionMeasure")), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(mapper.getIRI("CoolingConsumptionMeasure")), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(mapper.getIRI("CoolingConsumptionMeasure")), "om:hasUnit", "om:kilowattHour")
                        .addInsert("?graph", NodeFactory.createURI(coolingUri), "purlEnaeq:consumesEnergy", NodeFactory.createURI(coolingConsumptionUri))
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "ontoubemmp:hasDevice", NodeFactory.createURI(pvCellsUri))
                        .addInsert("?graph", NodeFactory.createURI(pvCellsUri), "rdf:type", "ontoubemmp:PVSystem")
                        .addInsert("?graph", NodeFactory.createURI(pvCellsUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(pvCellsUri), "ontoubemmp:hasArea", NodeFactory.createURI(PVAreaUri))
                        .addInsert("?graph", NodeFactory.createURI(PVAreaUri), "rdf:type", "ontoubemmp:PVSystemArea")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaUri), "om:hasDimension", "om:area-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaUri), "om:hasValue", NodeFactory.createURI(PVAreaValueUri))
                        .addInsert("?graph", NodeFactory.createURI(PVAreaValueUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaValueUri), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(PVAreaValueUri), "om:hasNumericalValue", PV_area)
                        .addInsert("?graph", NodeFactory.createURI(PVAreaValueUri), "om:hasUnit", "om:squareMetre")
                        .addInsert("?graph", NodeFactory.createURI(pvCellsUri), "thinkhome:producesEnergy", NodeFactory.createURI(PVSupplyUri))
                        .addInsert("?graph", NodeFactory.createURI(PVSupplyUri), "rdf:type", "ontoubemmp:ElectricitySupply")
                        .addInsert("?graph", NodeFactory.createURI(PVSupplyUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(PVSupplyUri), "om:hasDimension", "om:energy-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(PVSupplyUri), "om:hasValue", NodeFactory.createURI(mapper.getIRI("PVSupplyMeasure")))
                        .addInsert("?graph", NodeFactory.createURI(mapper.getIRI("PVSupplyMeasure")), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(mapper.getIRI("PVSupplyMeasure")), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(mapper.getIRI("PVSupplyMeasure")), "om:hasUnit", "om:kilowattHour");

        ub.setVar(Var.alloc("graph"), NodeFactory.createURI(outputGraphUri));

        UpdateRequest ur = ub.buildRequest();

        //Use access agent
        this.update(UPDATE_ROUTE, ur.toString());

        return buildingUri;

    }

    public ArrayList<String> getDataIRI(String uriString, String value) {
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
            case "grid_demand":
                wb2.addWhere("?energyProfileBuilding", "purlEnaeq:consumesEnergy", "?grid")
                        .addWhere("?grid", "rdf:type", "ontoubemmp:GridConsumption")
                        .addWhere("?grid", "om:hasValue", "?measure")
                           .addWhere("?measure", "om:hasUnit", "?unit");
                break;
            case "electricity_demand":
                wb2.addWhere("?energyProfileBuilding", "purlEnaeq:consumesEnergy", "?electricity")
                        .addWhere("?electricity", "rdf:type", "ontoubemmp:ElectricityConsumption")
                        .addWhere("?electricity", "om:hasValue", "?measure")
                        .addWhere("?measure", "om:hasUnit", "?unit");
                break;
            case "heating_demand":
                wb2.addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?heatingDevice")
                        .addWhere("?heatingDevice", "rdf:type", "ontoubemmp:HeatingSystem")
                        .addWhere("?heatingDevice", "purlEnaeq:consumesEnergy", "?heating")
                        .addWhere("?heating", "rdf:type", "ontoubemmp:ThermalConsumption")
                        .addWhere("?heating", "om:hasValue", "?measure")
                        .addWhere("?measure", "om:hasUnit", "?unit");
                break;
            case "cooling_demand":
                wb2.addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?coolingDevice")
                        .addWhere("?coolingDevice", "rdf:type", "ontoubemmp:CoolingSystem")
                        .addWhere("?coolingDevice", "purlEnaeq:consumesEnergy", "?cooling")
                        .addWhere("?cooling", "rdf:type", "ontoubemmp:ThermalConsumption")
                        .addWhere("?cooling", "om:hasValue", "?measure")
                        .addWhere("?measure", "om:hasUnit", "?unit");
                break;
            case "PV_supply":
                wb2.addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?PVSystem")
                        .addWhere("?PVSystem", "rdf:type", "ontoubemmp:PVSystem")
                        .addWhere("?PVSystem", "thinkhome:producesEnergy", "?supply")
                        .addWhere("?supply", "rdf:type", "ontoubemmp:ElectricitySupply")
                        .addWhere("?supply", "om:hasValue", "?measure")
                        .addWhere("?measure", "om:hasUnit", "?unit");
                break;
            case "PV_area":
                wb2.addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?PVSystem")
                        .addWhere("?PVSystem", "rdf:type", "ontoubemmp:PVSystem")
                        .addWhere("?PVSystem", "ontoubemmp:hasArea", "?area")
                        .addWhere("?area", "rdf:type", "ontoubemmp:PVSystemArea")
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

        JSONObject queryResultObject = new JSONObject(this.query(QUERY_ROUTE, sb.build().toString()));
        JSONArray queryResultArray = new JSONArray(queryResultObject.get("result").toString());

        if(!queryResultArray.isEmpty()){
            result.add(queryResultArray.getJSONObject(0).get("measure").toString());
            result.add(queryResultArray.getJSONObject(0).get("unit").toString());
        }
        return result;

    }

    public boolean checkGenAttributeInitialised(String uriString){
        WhereBuilder wb = new WhereBuilder();
        SelectBuilder sb = new SelectBuilder();

        wb.addPrefix("ocgml", ocgmlUri)
                    .addWhere("?genAttribute", "ocgml:cityObjectId", "?s")
                    .addWhere("?genAttribute", "ocgml:attrName", "energyProfileBuildingRepresentation")
                    .addWhere("?genAttribute", "ocgml:uriVal", "?energyProfileBuilding");

        sb.addVar("?genAttribute")
                .addGraph(NodeFactory.createURI(getGraph(uriString,CITY_OBJECT_GEN_ATT)), wb);

        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(uriString));

        JSONObject queryResultObject = new JSONObject(this.query(QUERY_ROUTE, sb.build().toString()));
        JSONArray queryResultArray = new JSONArray(queryResultObject.get("result").toString());
        String genAtt = "";

        if(!queryResultArray.isEmpty()){
            genAtt = queryResultArray.getJSONObject(0).get("genAttribute").toString();

        }
        return genAtt != "";
    }

    public String checkEnergyProfileInitialised(JSONKeyToIriMapper mapper, String uriString){
        WhereBuilder wb = new WhereBuilder();
        SelectBuilder sb = new SelectBuilder();


        wb.addPrefix("ocgml", ocgmlUri)
                .addPrefix("rdf", rdfUri)
                .addPrefix("purlEnaeq", purlEnaeqUri)
                .addPrefix("ontoubemmp", ontoUBEMMPUri)
                .addPrefix("thinkhome", thinkhomeUri)
                .addPrefix("purlInf", purlInfrastructureUri)
                .addPrefix("om", unitOntologyUri)
                .addWhere("?energyProfileBuilding", "rdf:type", "purlInf:Building")
                .addWhere("?energyProfileBuilding", "purlEnaeq:consumesEnergy", "?grid")
                .addWhere("?grid", "om:hasValue", NodeFactory.createURI(mapper.getIRI("GridConsumptionMeasure")))
                .addWhere("?energyProfileBuilding", "purlEnaeq:consumesEnergy", "?electricity")
                .addWhere("?electricity", "om:hasValue", NodeFactory.createURI(mapper.getIRI("ElectricityConsumptionMeasure")))
                .addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?heating")
                .addWhere("?heating", "purlEnaeq:consumesEnergy", "?heatingConsumption")
                .addWhere("?heatingConsumption", "om:hasValue", NodeFactory.createURI(mapper.getIRI("HeatingConsumptionMeasure")))
                .addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?cooling")
                .addWhere("?cooling", "purlEnaeq:consumesEnergy", "?coolingConsumption")
                .addWhere("?coolingConsumption", "om:hasValue", NodeFactory.createURI(mapper.getIRI("CoolingConsumptionMeasure")))
                .addWhere("?energyProfileBuilding", "ontoubemmp:hasDevice", "?PVCells")
                .addWhere("?PVCells", "thinkhome:producesEnergy", "?PVSupply")
                .addWhere("?PVSupply", "om:hasValue", NodeFactory.createURI(mapper.getIRI("PVSupplyMeasure")));

        sb.addVar("?energyProfileBuilding")
                .addGraph(NodeFactory.createURI(getGraph(uriString,ENERGY_PROFILE)), wb);

        JSONObject queryResultObject = new JSONObject(this.query(QUERY_ROUTE, sb.build().toString()));
        JSONArray queryResultArray = new JSONArray(queryResultObject.get("result").toString());
        String building = "";

        if(!queryResultArray.isEmpty()){
            building = queryResultArray.getJSONObject(0).get("energyProfileBuilding").toString();

        }
        return building;
    }

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

    public TimeSeries<OffsetDateTime> retrieveData(String dataIri){
        try {
            tsClient = new TimeSeriesClient<>(OffsetDateTime.class, new File(
                    Objects.requireNonNull(getClass().getClassLoader().getResource(TIME_SERIES_CLIENT_PROPS)).toURI()).getAbsolutePath());
            List<String> iris = new ArrayList<>();
            iris.add(dataIri);
            TimeSeries<OffsetDateTime> data = tsClient.getTimeSeries(iris);
            return data;

        } catch(URISyntaxException | IOException e){
            e.printStackTrace();
            throw new JPSRuntimeException(e);
        }
    }

    public String calculateAverage(TimeSeries<OffsetDateTime> timeSeries, String dataIri){
        List<Double> values = timeSeries.getValuesAsDouble(dataIri);
        Double annualValue = 0.;
        for(Double value : values){
            annualValue += value;
        }
        return annualValue.toString();

    }

}
