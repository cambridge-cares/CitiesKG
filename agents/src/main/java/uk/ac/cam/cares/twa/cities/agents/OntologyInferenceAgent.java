package uk.ac.cam.cares.twa.cities.agents;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.model.IRI;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.tasks.UninitialisedDataAndResultQueueTask;
import uk.ac.cam.cares.twa.cities.tasks.UninitialisedDataQueueTask;

/**
 * A JPSAgent framework based Ontology Inference class used to infer semantic information
 * with knowledge graphs.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 */
@WebServlet(
    urlPatterns = {
        uk.ac.cam.cares.twa.cities.agents.OntologyInferenceAgent.URI_ACTION
    })
public class OntologyInferenceAgent extends InferenceAgent {
  public static final String URI_ACTION = "/inference/ontology";
  private static final ExecutorService taskExecutor = Executors.newFixedThreadPool(5);

  @Override //@todo refactor to remove repetition in GraphInferenceAgent
  public JSONObject processRequestParameters(JSONObject requestParams) {
    JSONObject responseParams = new JSONObject();
    if (validateInput(requestParams)) {
      try {
        // setup route for AccessAgent and check if targetIri has the trailing /
        route = ResourceBundle.getBundle("config").getString("uri.route");
        String targetIRI = requestParams.getString(KEY_TARGET_IRI).endsWith("/")
            ? requestParams.getString(KEY_TARGET_IRI) :  requestParams.getString(KEY_TARGET_IRI).concat("/");

        //(1) Choose task based on algorithm IRI
        UninitialisedDataQueueTask task = chooseTask(IRI.create(requestParams.getString(KEY_ALGO_IRI)),
            IRI.create(targetIRI));

        String taskIRI = task.getTaskIri().toString();
        //(2) Retrieve data from target IRI
        JSONArray targetData = getAllTargetData(IRI.create(targetIRI), ONTOZONING_GRAPH);
        //(3) Pass target data (2) to the task (1) and run the task
        Map<String,JSONArray> taskData = new HashMap<>();
        taskData.put(taskIRI, targetData);

        dataQueue.put(taskData);
        taskExecutor.execute(task);
        //(4) add task information to the response
        if (task instanceof UninitialisedDataAndResultQueueTask) {
          while (resultQueue.isEmpty()) {
            if (!task.isRunning()) {
              return responseParams.put(taskIRI, "failed");
            }
          }
          responseParams.put(taskIRI, resultQueue.take().get(taskIRI));
        } else {
          responseParams.put(taskIRI, "started");
        }

      } catch (Exception e) {
        throw new JPSRuntimeException(e);
      }
    }

    return responseParams;
  }

  @Override
  public boolean validateInput(JSONObject requestParams) throws BadRequestException {
    boolean error = true;

    if (!requestParams.isEmpty()) {
      Set<String> keys = requestParams.keySet();
      if (keys.contains(KEY_REQ_METHOD) && keys.contains(KEY_REQ_URL) && keys.contains(
          KEY_TARGET_IRI) && keys.contains(KEY_ONTO_IRI)) {
        if (requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.POST)) {
          try {
            URL reqUrl = new URL(requestParams.getString(KEY_REQ_URL));
            if (reqUrl.getPath().contains(URI_ACTION)) {
              IRI.create(requestParams.getString(KEY_TARGET_IRI));
              IRI.create(requestParams.getString(KEY_ONTO_IRI));
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

  private JSONArray getAllTargetData(IRI sparqlEndpoint, String tBoxGraph) throws ParseException {
    //retrieve data and replace empty string with it
    // limit to ontozone and OntoZoning graphs
    SelectBuilder sb = new SelectBuilder();
    sb.setBase(sparqlEndpoint.toString()).from(tBoxGraph)
        .addVar("?s").addVar("?p").addVar("?o")
        .addWhere("?s", "?p", "?o")
        .addFilter("?p != <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")
        .addFilter("?p != <http://ainf.aau.at/ontodebug#axiom>")
        .addFilter("?p != <http://ainf.aau.at/ontodebug#type>")
        .addFilter("?o != \"only\"")
        .addFilter("?o != <http://ainf.aau.at/ontodebug#testCase>")
        .addFilter("?o != <http://www.w3.org/1999/02/22-rdf-syntax-ns#nil>")
        .addFilter("?o != <http://www.w3.org/2002/07/owl#Thing>")
    ;

    JSONArray sparqlResult = AccessAgentCaller.queryStore(route, sb.buildString());

    return sparqlResult;
  }

}
