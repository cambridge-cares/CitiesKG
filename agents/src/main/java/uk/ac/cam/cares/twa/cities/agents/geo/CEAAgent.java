package uk.ac.cam.cares.twa.cities.agents.geo;

import org.apache.jena.sparql.core.Var;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.twa.cities.tasks.*;
import org.json.JSONObject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.net.URI;
import java.net.URL;
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
                CEAAgent.URI_UPDATE
        })
public class CEAAgent extends JPSAgent {
    public static final String KEY_REQ_METHOD = "method";
    public static final String URI_ACTION = "/cea/run";
    public static final String URI_UPDATE = "/cea/update";
    public static final String KEY_REQ_URL = "requestUrl";
    public static final String KEY_TARGET_URL = "targetUrl";
    public static final String KEY_IRI = "iri";
    public static final String CITY_OBJECT = "cityobject";
    public static final String CITY_OBJECT_GEN_ATT = "cityobjectgenericattrib";
    public static final String BUILDING = "building";
    public static final String ENERGY_PROFILE = "energyprofile";
    public static final String KEY_GRID_DEMAND = "grid_demand";
    public static final String KEY_ELECTRICITY_DEMAND = "electricity_demand";
    public static final String KEY_HEATING_DEMAND = "heating_demand";
    public static final String KEY_COOLING_DEMAND = "cooling_demand";
    public static final String KEY_PV_AREA = "PV_area";
    public static final String KEY_PV_SUPPLY= "PV_supply";

    public final int NUM_CEA_THREADS = 1;
    private final ThreadPoolExecutor CEAExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_CEA_THREADS);

    // Variables fetched from CEAAgentConfig.properties file.
    private String ocgmlUri;
    private String ontoUBEMMPUri;
    private String rdfUri;
    private String owlUri;
    private String purlInfrastructureUri;
    private String purlEnaeqUri;
    private String semancoUri;
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
            targetUrl = requestParams.getString(KEY_TARGET_URL);
            String uriArrayString = requestParams.get(KEY_IRI).toString();
            JSONArray uriArray = new JSONArray(uriArrayString);

            if (requestUrl.contains(URI_UPDATE)) {
                for(int i=0; i<uriArray.length(); i++) {
                    String uri = uriArray.getString(i);
                    sparqlUpdate(requestParams.get(KEY_GRID_DEMAND).toString(),
                            requestParams.get(KEY_ELECTRICITY_DEMAND).toString(),
                            requestParams.get(KEY_HEATING_DEMAND).toString(),
                            requestParams.get(KEY_COOLING_DEMAND).toString(),
                            requestParams.get(KEY_PV_AREA).toString(),
                            requestParams.get(KEY_PV_SUPPLY).toString(), uri);
                }
            } else if (requestUrl.contains(URI_ACTION)) {
                ArrayList<CEAInputData> testData = new ArrayList<>();
                ArrayList<String> uriStringArray = new ArrayList<>();
                for(int i=0; i<uriArray.length(); i++) {
                    String uri = uriArray.getString(i);
                    uriStringArray.add(uri);
                    testData.add(new CEAInputData(getValue(uri, "Envelope"), getValue(uri, "Height")));
                }
                runCEA(testData, uriStringArray,0);
            }

        }
        return requestParams;
    }

    @Override
    public boolean validateInput(JSONObject requestParams) throws BadRequestException {
        boolean error = true;

        if (!requestParams.isEmpty()) {
            Set<String> keys = requestParams.keySet();
            if (keys.contains(KEY_REQ_METHOD) && keys.contains(KEY_REQ_URL) && keys.contains(
                    KEY_TARGET_URL) && keys.contains(KEY_IRI)) {
                if (requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.POST)) {
                    try {
                        URL reqUrl = new URL(requestParams.getString(KEY_REQ_URL));
                        new URL(requestParams.getString(KEY_TARGET_URL));
                        if (reqUrl.getPath().contains(URI_UPDATE)) {
                            error = validateUpdateInput(requestParams);
                        } else if (reqUrl.getPath().contains(URI_ACTION)) {
                            error = validateActionInput(requestParams);
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
        if (!requestParams.get(KEY_GRID_DEMAND).toString().isEmpty() ||
                !requestParams.get(KEY_ELECTRICITY_DEMAND).toString().isEmpty() ||
                !requestParams.get(KEY_HEATING_DEMAND).toString().isEmpty() ||
                !requestParams.get(KEY_COOLING_DEMAND).toString().isEmpty() ||
                !requestParams.get(KEY_PV_AREA).toString().isEmpty() ||
                !requestParams.get(KEY_PV_SUPPLY).toString().isEmpty() ){
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
        purlInfrastructureUri=config.getString("uri.ontology.purl.infrastructure");
        purlEnaeqUri=config.getString("uri.ontology.purl.enaeq");
        semancoUri=config.getString("uri.ontology.semanco");
        thinkhomeUri=config.getString("uri.ontology.thinkhome");
    }

    /**
     * runs CEATask on CEAInputData and returns CEAOutputData
     *
     * @param buildingData input data on building envelope and height
     * @param threadNumber int tracking thread that is running
     */
    private void runCEA(ArrayList<CEAInputData> buildingData, ArrayList<String> uris, int threadNumber) {
        try {
            RunCEATask task = new RunCEATask(buildingData, new URI(targetUrl), uris, threadNumber);
            CEAExecutor.execute(task);
        }
        catch(URISyntaxException e){
        }
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
    private String getValue(String uriString, String value){

        String result = "";

        Query q = getQuery(uriString, value);

        //Use access agent
        String queryResultString = this.query(QUERY_ROUTE, q.toString());

        JSONObject queryResultObject = new JSONObject(queryResultString);
        String resultString = queryResultObject.get("result").toString();
        JSONArray queryResultArray = new JSONArray(resultString);

        if(!queryResultArray.isEmpty()){
            result = queryResultArray.getJSONObject(0).get(value).toString();
        }
        return result;
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
    private Query getGeometryQuery(String uriString) {
        SelectBuilder sb = new SelectBuilder()
                .addPrefix( "ocgml", ocgmlUri )
                .addVar("?Envelope")
                .addGraph(NodeFactory.createURI(getGraph(uriString,CITY_OBJECT)), "?s", "ocgml:EnvelopeType", "?Envelope");
        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(uriString));

        return sb.build();
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
     * builds a SPARQL update using output from CEA simulations
     * @param grid_demand the output data from the CEA
     * @param uriString city object id
     */
    public void sparqlUpdate(String grid_demand, String electricity_demand, String heating_demand, String cooling_demand, String PV_area, String PV_supply, String uriString) {
        String outputGraphUri = getGraph(uriString,ENERGY_PROFILE);
        String buildingUri = outputGraphUri + "Building_UUID_" + UUID.randomUUID() + "/";
        String heatingUri = outputGraphUri + "HeatingSystem_UUID_" + UUID.randomUUID() + "/";
        String coolingUri = outputGraphUri + "CoolingSystem_UUID_" + UUID.randomUUID() + "/";
        String pvCellsUri = outputGraphUri + "PV_Cells_UUID_" + UUID.randomUUID()+ "/";
        String gridConsumptionUri = outputGraphUri + "GridConsumption_UUID_" + UUID.randomUUID()+ "/";
        String gridConsumptionValueUri = outputGraphUri + "GridConsumptionValue_UUID_" + UUID.randomUUID()+ "/";
        String electricityConsumptionUri = outputGraphUri + "ElectricityConsumption_UUID_" + UUID.randomUUID()+ "/";
        String electricityConsumptionValueUri = outputGraphUri + "ElectricityConsumptionValue_UUID_" + UUID.randomUUID()+ "/";
        String heatingConsumptionUri = outputGraphUri + "HeatingConsumption_UUID_" + UUID.randomUUID()+ "/";
        String heatingConsumptionValueUri = outputGraphUri + "HeatingConsumptionValue_UUID_" + UUID.randomUUID()+ "/";
        String coolingConsumptionUri = outputGraphUri + "CoolingConsumption_UUID_" + UUID.randomUUID()+ "/";
        String coolingConsumptionValueUri = outputGraphUri + "CoolingConsumptionValue_UUID_" + UUID.randomUUID()+ "/";
        String PVAreaUri = outputGraphUri + "PVArea_UUID_" + UUID.randomUUID()+ "/";
        String PVAreaValueUri = outputGraphUri + "PVAreaValue_UUID_" + UUID.randomUUID()+ "/";
        String PVSupplyUri = outputGraphUri + "PVSupply_UUID_" + UUID.randomUUID()+ "/";
        String PVSupplyValueUri = outputGraphUri + "PVSupplyValue_UUID_" + UUID.randomUUID()+ "/";

        UpdateBuilder ub =
                new UpdateBuilder()
                        .addPrefix("ontoubemmp", ontoUBEMMPUri)
                        .addPrefix("rdf", rdfUri)
                        .addPrefix("owl", owlUri)
                        .addPrefix("purlInfrastructure", purlInfrastructureUri)
                        .addPrefix("purlEnaeq", purlEnaeqUri)
                        .addPrefix("om", unitOntologyUri)
                        .addPrefix("semanco", semancoUri)
                        .addPrefix("thinkhome", thinkhomeUri)
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "rdf:type", "purlInfrastructure:Building")
                        .addInsert("?graph", NodeFactory.createURI(gridConsumptionUri), "rdf:type", "ontoubemmp:GridConsumption")
                        .addInsert("?graph", NodeFactory.createURI(gridConsumptionUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(gridConsumptionUri), "om:hasDimension", "om:energy-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(gridConsumptionUri), "om:hasValue", NodeFactory.createURI(gridConsumptionValueUri))
                        .addInsert("?graph", NodeFactory.createURI(gridConsumptionValueUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(gridConsumptionValueUri), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(gridConsumptionValueUri), "om:hasNumericalValue", grid_demand)
                        .addInsert("?graph", NodeFactory.createURI(gridConsumptionValueUri), "om:hasUnit", "om:kilowattHour")
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "purlEnaeq:consumesEnergy", NodeFactory.createURI(gridConsumptionUri))
                        .addInsert("?graph", NodeFactory.createURI(electricityConsumptionUri), "rdf:type", "ontoubemmp:ElectricityConsumption")
                        .addInsert("?graph", NodeFactory.createURI(electricityConsumptionUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(electricityConsumptionUri), "om:hasDimension", "om:energy-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(electricityConsumptionUri), "om:hasValue", NodeFactory.createURI(electricityConsumptionValueUri))
                        .addInsert("?graph", NodeFactory.createURI(electricityConsumptionValueUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(electricityConsumptionValueUri), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(electricityConsumptionValueUri), "om:hasNumericalValue", electricity_demand)
                        .addInsert("?graph", NodeFactory.createURI(electricityConsumptionValueUri), "om:hasUnit", "om:kilowattHour")
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "purlEnaeq:consumesEnergy", NodeFactory.createURI(electricityConsumptionUri))
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "ontoubemmp:hasDevice", NodeFactory.createURI(heatingUri))
                        .addInsert("?graph", NodeFactory.createURI(heatingUri), "rdf:type", "purlEnaeq:HeatingSystem")
                        .addInsert("?graph", NodeFactory.createURI(heatingUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(heatingConsumptionUri), "rdf:type", "ontoubemmp:ThermalConsumption")
                        .addInsert("?graph", NodeFactory.createURI(heatingConsumptionUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(heatingConsumptionUri), "om:hasDimension", "om:energy-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(heatingConsumptionUri), "om:hasValue", NodeFactory.createURI(heatingConsumptionValueUri))
                        .addInsert("?graph", NodeFactory.createURI(heatingConsumptionValueUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(heatingConsumptionValueUri), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(heatingConsumptionValueUri), "om:hasNumericalValue", heating_demand)
                        .addInsert("?graph", NodeFactory.createURI(heatingConsumptionValueUri), "om:hasUnit", "om:kilowattHour")
                        .addInsert("?graph", NodeFactory.createURI(heatingUri), "purlEnaeq:consumesEnergy", NodeFactory.createURI(heatingConsumptionUri))
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "ontoubemmp:hasDevice", NodeFactory.createURI(coolingUri))
                        .addInsert("?graph", NodeFactory.createURI(coolingUri), "rdf:type", "ontoubemmp:CoolingSystem")
                        .addInsert("?graph", NodeFactory.createURI(coolingUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(coolingConsumptionUri), "rdf:type", "ontoubemmp:ThermalConsumption")
                        .addInsert("?graph", NodeFactory.createURI(coolingConsumptionUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(coolingConsumptionUri), "om:hasDimension", "om:energy-Dimension")
                        .addInsert("?graph", NodeFactory.createURI(coolingConsumptionUri), "om:hasValue", NodeFactory.createURI(coolingConsumptionValueUri))
                        .addInsert("?graph", NodeFactory.createURI(coolingConsumptionValueUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(coolingConsumptionValueUri), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(coolingConsumptionValueUri), "om:hasNumericalValue", cooling_demand)
                        .addInsert("?graph", NodeFactory.createURI(coolingConsumptionValueUri), "om:hasUnit", "om:kilowattHour")
                        .addInsert("?graph", NodeFactory.createURI(coolingUri), "purlEnaeq:consumesEnergy", NodeFactory.createURI(coolingConsumptionUri))
                        .addInsert("?graph", NodeFactory.createURI(buildingUri), "ontoubemmp:hasDevice", NodeFactory.createURI(pvCellsUri))
                        .addInsert("?graph", NodeFactory.createURI(pvCellsUri), "rdf:type", "semanco:PVSystem")
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
                        .addInsert("?graph", NodeFactory.createURI(PVSupplyUri), "om:hasValue", NodeFactory.createURI(PVSupplyValueUri))
                        .addInsert("?graph", NodeFactory.createURI(PVSupplyValueUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(PVSupplyValueUri), "rdf:type", "om:Measure")
                        .addInsert("?graph", NodeFactory.createURI(PVSupplyValueUri), "om:hasNumericalValue", PV_supply)
                        .addInsert("?graph", NodeFactory.createURI(PVSupplyValueUri), "om:hasUnit", "om:kilowattHour");
        ub.setVar(Var.alloc("graph"), NodeFactory.createURI(outputGraphUri));

        UpdateRequest ur = ub.buildRequest();

        //Use access agent
        this.update(UPDATE_ROUTE, ur.toString());

    }

}
