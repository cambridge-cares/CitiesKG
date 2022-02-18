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
    private static String unitOntology;
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

            if (requestUrl.contains(URI_UPDATE)) {
                //sparqlUpdate(outputs, uri);
            } else if (requestUrl.contains(URI_ACTION)) {
                String uriArrayString = requestParams.get("iri").toString();
                JSONArray uriArray = new JSONArray(uriArrayString);
                ArrayList<CEAInputData> testData = new ArrayList<CEAInputData>();
                for(int i=0; i<uriArray.length(); i++) {
                    String uri = uriArray.getString(i);
                    testData.add(new CEAInputData(getValue(uri, "Envelope"), getValue(uri, "Height")));
                }
                runCEA(testData, 0);
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
                    KEY_TARGET_URL)) {
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
        unitOntology = config.getString("uri.ontology.om");
        ontoUBEMMPUri = config.getString("uri.ontology.ontoubemmp");
        rdfUri = config.getString("uri.ontology.rdf");
        owlUri = config.getString("uri.ontology.owl");
    }

    /**
     * runs CEATask on CEAInputData and returns CEAOutputData
     *
     * @param buildingData input data on building envelope and height
     * @param threadNumber int tracking thread that is running
     */
    private void runCEA(ArrayList<CEAInputData> buildingData, int threadNumber) {
        try {
            RunCEATask task = new RunCEATask(buildingData, new URI(targetUrl), threadNumber);
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
     * @param output the output data from the CEA
     * @param uriString city object id
     */
    public void sparqlUpdate(CEAOutputData output, String uriString) {
        String outputGraphUri = getGraph(uriString,ENERGY_PROFILE);
        String heatingUri = outputGraphUri + "UUID_" + UUID.randomUUID() + "/";
        String coolingUri = outputGraphUri + "UUID_" + UUID.randomUUID() + "/";
        String pvCellsUri = outputGraphUri + "UUID_" + UUID.randomUUID()+ "/";

        UpdateBuilder ub =
                new UpdateBuilder()
                        .addPrefix("ontoubemmp", ontoUBEMMPUri)
                        .addPrefix("rdf", rdfUri)
                        .addPrefix("owl", owlUri)
                        .addInsert("?graph", NodeFactory.createURI(uriString), "rdf:type", "ontoubemmp:building")
                        .addInsert("?graph", NodeFactory.createURI(uriString), "ontoubemmp:hasYearlyEnergyDemand", output.grid_demand)
                        .addInsert("?graph", NodeFactory.createURI(uriString), "ontoubemmp:hasEnergyUnit", "megawattHour")
                        .addInsert("?graph", NodeFactory.createURI(heatingUri), "rdf:type", "ontoubemmp:EnergyConsumer")
                        .addInsert("?graph", NodeFactory.createURI(heatingUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(heatingUri), "ontoubemmp:isConsumerType", "heating")
                        .addInsert("?graph", NodeFactory.createURI(uriString), "ontoubemmp:hasEnergyConsumer", NodeFactory.createURI(heatingUri))
                        .addInsert("?graph", NodeFactory.createURI(heatingUri), "ontoubemmp:hasEnergyUnit", "megawattHour")
                        .addInsert("?graph", NodeFactory.createURI(heatingUri), "ontoubemmp:hasYearlyEnergyDemand", output.heating_demand)
                        .addInsert("?graph", NodeFactory.createURI(coolingUri), "rdf:type", "ontoubemmp:EnergyConsumer")
                        .addInsert("?graph", NodeFactory.createURI(coolingUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(coolingUri), "ontoubemmp:isConsumerType", "cooling")
                        .addInsert("?graph", NodeFactory.createURI(uriString), "ontoubemmp:hasEnergyConsumer", NodeFactory.createURI(coolingUri))
                        .addInsert("?graph", NodeFactory.createURI(coolingUri), "ontoubemmp:hasEnergyUnit", "megawattHour")
                        .addInsert("?graph", NodeFactory.createURI(coolingUri), "ontoubemmp:hasYearlyEnergyDemand", output.cooling_demand)
                        .addInsert("?graph", NodeFactory.createURI(pvCellsUri), "rdf:type", "ontoubemmp:PVCells")
                        .addInsert("?graph", NodeFactory.createURI(pvCellsUri), "rdf:type", "owl:NamedIndividual")
                        .addInsert("?graph", NodeFactory.createURI(uriString), "ontoubemmp:hasPotentialPVCells", NodeFactory.createURI(pvCellsUri))
                        .addInsert("?graph", NodeFactory.createURI(pvCellsUri), "ontoubemmp:hasEnergyUnit", "kilowattHour")
                        .addInsert("?graph", NodeFactory.createURI(pvCellsUri), "ontoubemmp:hasYearlyEnergySupply", output.PV_supply)
                        .addInsert("?graph", NodeFactory.createURI(pvCellsUri), "ontoubemmp:hasAreaUnit", "squareMeter")
                        .addInsert("?graph", NodeFactory.createURI(pvCellsUri), "ontoubemmp:hasSurfaceArea", output.PV_area);
        ub.setVar(Var.alloc("graph"), NodeFactory.createURI(outputGraphUri));

        UpdateRequest ur = ub.buildRequest();

        //Use access agent
        this.update(UPDATE_ROUTE, ur.toString());
    }

}
