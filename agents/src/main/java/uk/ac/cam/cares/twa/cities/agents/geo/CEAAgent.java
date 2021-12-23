package uk.ac.cam.cares.twa.cities.agents.geo;

import org.apache.jena.sparql.core.Var;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.tasks.CEAInputData;
import uk.ac.cam.cares.twa.cities.tasks.CEAOutputData;
import uk.ac.cam.cares.twa.cities.tasks.RunCEATask;
import org.json.JSONObject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.util.Set;
import javax.servlet.annotation.WebServlet;
import java.util.concurrent.*;
import java.util.UUID;
import java.util.ResourceBundle;
import java.util.Arrays;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.json.JSONArray;
import uk.ac.cam.cares.jps.base.interfaces.StoreClientInterface;
import uk.ac.cam.cares.jps.base.query.StoreRouter;

@WebServlet(
        urlPatterns = {
                CEAAgent.URI_ACTION
        })
public class CEAAgent extends JPSAgent {
    public static final String KEY_REQ_METHOD = "method";
    public static final String URI_ACTION = "/cea";
    public static final String KEY_IRI = "iri";
    public static final String CITY_OBJECT = "cityobject";
    public static final String CITY_OBJECT_GEN_ATT = "cityobjectgenericattrib";
    public static final String ENERGY_PROFILE = "energyprofile";

    private StoreClientInterface kgClient;

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

    public CEAAgent() {
        readConfig();
    }

    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {

        if (validateInput(requestParams)) {
            String uri = requestParams.getString("iri");

            CEAInputData testData = new CEAInputData();
            testData.geometry = getValue(uri, "Envelope");
            testData.height = getValue(uri, "Height");
            CEAOutputData outputs = runCEA(testData);

            sparqlUpdate(outputs, uri);
        }
        return requestParams;
    }

    @Override
    public boolean validateInput(JSONObject requestParams) throws BadRequestException {
        boolean error = true;

        if (!requestParams.isEmpty()) {
            Set<String> keys = requestParams.keySet();
            if (keys.contains(KEY_REQ_METHOD) && keys.contains(KEY_IRI)) {
                if (requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.POST)) {
                    try {
                        if (!requestParams.getString(KEY_IRI).isEmpty()){
                            error = false;
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

    private void readConfig() {
        ResourceBundle config = ResourceBundle.getBundle("CEAAgentConfig");
        UPDATE_ROUTE = config.getString("uri.route.local");
        QUERY_ROUTE = config.getString("uri.route.wa");
        ocgmlUri = config.getString("uri.ontology.ontocitygml");
        unitOntology = config.getString("uri.ontology.om");
        ontoUBEMMPUri = config.getString("uri.ontology.ontoubemmp");
        rdfUri = config.getString("uri.ontology.rdf");
        owlUri = config.getString("uri.ontology.owl");

    }

    private CEAOutputData runCEA(CEAInputData buildingData) {
        RunCEATask task = new RunCEATask(buildingData);
        Future<CEAOutputData> future = CEAExecutor.submit(task);
        CEAOutputData result;
        try {
             result = future.get();
        } catch( InterruptedException | ExecutionException| CancellationException e) {
            e.printStackTrace();
            throw new JPSRuntimeException(e);
        }
        return result;
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
     * executes query on SPARQL endpoint and retrieves requested value of building
     * @param uriString city object id
     * @param value building value requested
     * @return geometry as string
     */
    private String getValue(String uriString, String value){

        String result = "";
        setKGClient(true, false);

        Query q = getQuery(uriString, value);
        String queryResultString = kgClient.execute(q.toString());
        JSONArray queryResult = new JSONArray(queryResultString);

        if(!queryResult.isEmpty()){
            result = queryResult.getJSONObject(0).get(value).toString();
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
        WhereBuilder wb =
                new WhereBuilder()
                        .addPrefix("ocgml", ocgmlUri)
                        .addWhere("?o", "ocgml:attrName", "height")
                        .addWhere("?o", "ocgml:realVal", "?Height")
                        .addWhere("?o", "ocgml:cityObjectId", "?s");
        SelectBuilder sb = new SelectBuilder()
                .addVar("?Height")
                .addGraph(NodeFactory.createURI(getGraph(uriString,CITY_OBJECT_GEN_ATT)), wb);
        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(uriString));

        return sb.build();
    }

    /**
     * sets KG Client for specific query or update endpoint
     * @param isQuery
     * @param isUpdate
     */
    private void setKGClient(boolean isQuery, boolean isUpdate){
        if(isQuery) {
            this.kgClient = StoreRouter.getStoreClient(QUERY_ROUTE,
                    isQuery,
                    isUpdate);
        }
        else if(isUpdate){
            this.kgClient = StoreRouter.getStoreClient(UPDATE_ROUTE,
                    isQuery,
                    isUpdate);
        }
    }

    public int sparqlUpdate(CEAOutputData output, String uriString) {
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
        setKGClient(false, true);
        return kgClient.executeUpdate(ur);
    }

}
