package uk.ac.cam.cares.twa.cities.agents.geo;

import org.apache.jena.sparql.core.Var;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.twa.cities.tasks.CEAInputData;
import uk.ac.cam.cares.twa.cities.tasks.CEAOutputData;
import uk.ac.cam.cares.twa.cities.tasks.RunCEATask;
import org.json.JSONObject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.util.ArrayList;
import java.util.Set;
import javax.servlet.annotation.WebServlet;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
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
    private StoreClientInterface kgClient;
    private static final String ROUTE = "http://kb/singapore-local";

    public final int NUM_IMPORTER_THREADS = 1;
    private final ThreadPoolExecutor CEAExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_IMPORTER_THREADS);

    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {

        if (validateInput(requestParams)) {
            String uri = requestParams.getString("iri");
            CEAInputData testData = new CEAInputData();
            testData.geometry = getValue(uri, "Envelope");
            testData.height = getValue(uri, "Height");
            CEAOutputData outputs = new CEAOutputData();
            runCEA(testData, outputs);
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

    private void runCEA(CEAInputData buildingData, CEAOutputData output) {
        RunCEATask task = new RunCEATask(buildingData, output);
        CEAExecutor.execute(task);
    }

    /**
     * executes query on SPARQL endpoint and retrieves requested value of building
     * @param uriString city object id
     * @param value building value requested
     * @return geometry as string
     */
    private String getValue(String uriString, String value){

        String result = "";
        setKGClient();

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
                .addPrefix( "ocgml", "http://locahost/ontocitygml/" )
                .addVar("?Envelope")
                .addGraph(NodeFactory.createURI("http://localhost/berlin/cityobject/"), "?s", "ocgml:EnvelopeType", "?Envelope");
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
                        .addPrefix("ocgml", "http://locahost/ontocitygml/")
                        .addWhere("?o", "ocgml:attrName", "height")
                        .addWhere("?o", "ocgml:realVal", "?Height")
                        .addWhere("?o", "ocgml:cityObjectId", "?s");
        SelectBuilder sb = new SelectBuilder()
                .addVar("?Height")
                .addGraph(NodeFactory.createURI("http://localhost/berlin/cityobjectgenericattrib/"), wb);
        sb.setVar( Var.alloc( "s" ), NodeFactory.createURI(uriString));

        return sb.build();
    }

    /**
     * sets KG Client for specific query endpoint.
     */
    private void setKGClient(){

        this.kgClient = StoreRouter.getStoreClient(ROUTE,
                true,
                false);
    }


}
