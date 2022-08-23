package uk.ac.cam.cares.twa.cities.agents;

import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.json.JSONArray;
import org.semanticweb.owlapi.model.IRI;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.tasks.EdgeBetweennessTask;
import uk.ac.cam.cares.twa.cities.tasks.PageRankTask;
import uk.ac.cam.cares.twa.cities.tasks.UnweightedShortestPathTask;
import uk.ac.cam.cares.twa.cities.tasks.UninitialisedDataQueueTask;

/**
 * A JPSAgent framework based Graph Inference class used to infer structural information about graphs.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 */
@WebServlet(
    urlPatterns = {
        uk.ac.cam.cares.twa.cities.agents.GraphInferenceAgent.URI_ACTION
    })
public class GraphInferenceAgent extends JPSAgent {
  public static final String ONINF_PREFIX = "oninf";
  public static final String ONINF_SCHEMA = "http://www.theworldavatar.com/ontologies/OntoInfer.owl#";
  public static final String ONTOINFER_GRAPH = "OntoInfer/";
  public static final String ONTOZONE_GRAPH = "ontozone/";
  public static final String ONTOZONING_GRAPH = "OntoZoning/";
  public static final String ONINT_P_INOBJ = "hasInferenceObject";
  public static final String ONINT_P_INALG = "hasInferenceAlgorithm";
  public static final String ONINT_P_INVAL = "hasInferredValue";
  public static final String ONINT_C_PRALG = "PageRankAlgorithm";

  public String route;
  public static final String TASK_PR = "PageRankTask";
  public static final String TASK_EB = "EdgeBetweennessTask";
  public static final String TASK_USP = "UnweightedShortestPathTask";
  public static final String URI_ACTION = "/inference/graph";
  public static final String KEY_REQ_METHOD = "method";
  public static final String KEY_REQ_URL = "requestUrl";
  public static final String KEY_TARGET_IRI = "targetIRI";
  public static final String KEY_ALGO_IRI = "algorithmIRI";
  private final Map<IRI, UninitialisedDataQueueTask> TASKS = Stream.of(new Object[][] {
      {IRI.create(ONINF_SCHEMA + TASK_PR), new PageRankTask()},
          {IRI.create(ONINF_SCHEMA + TASK_EB), new EdgeBetweennessTask()},
          {IRI.create(ONINF_SCHEMA + TASK_USP), new UnweightedShortestPathTask()}
  }).collect(Collectors.toMap(data -> (IRI) data[0], data -> (UninitialisedDataQueueTask) data[1]));
  public static LinkedBlockingDeque<Map<String, JSONArray>> dataQueue = new LinkedBlockingDeque<>();
  private static final ExecutorService taskExecutor = Executors.newFixedThreadPool(5);

  @Override
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
        //(2) Retrieve data from target IRI
        JSONArray targetData = getAllTargetData(IRI.create(targetIRI));
        //(3) Pass target data (2) to the task (1) and run the task
        dataQueue.put(Collections.singletonMap(task.getTaskIri().toString(), targetData));
        taskExecutor.execute(task);
        //(4) add task information to the response
        responseParams.put(ONINF_SCHEMA + task.getTaskIri().toString(), "started");

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
          KEY_TARGET_IRI) && keys.contains(KEY_ALGO_IRI)) {
        if (requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.POST)) {
          try {
            URL reqUrl = new URL(requestParams.getString(KEY_REQ_URL));
            if (reqUrl.getPath().contains(URI_ACTION)) {
              IRI.create(requestParams.getString(KEY_TARGET_IRI));
              IRI.create(requestParams.getString(KEY_ALGO_IRI));
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

  private UninitialisedDataQueueTask chooseTask(IRI algorithmIRI, IRI sparqlEndpoint) {

    //Retrieve task IRI by algorithm IRI from KG and assign to taskId
    SelectBuilder sb = new SelectBuilder();
    sb.addPrefix(ONINF_PREFIX, ONINF_SCHEMA)
            .setBase(sparqlEndpoint.toString()).from(ONTOINFER_GRAPH)
            .addVar("?o")
            .addWhere("<" + algorithmIRI.toString() + ">", ONINF_PREFIX + ":appliedBy", "?o");

    JSONArray sparqlResult = AccessAgentCaller.queryStore(route, sb.buildString());

    //Get task from map by IRI
    IRI taskIri = IRI.create(sparqlResult.getJSONObject(0).get("o").toString());
    UninitialisedDataQueueTask task = (UninitialisedDataQueueTask) TASKS.get(taskIri);
    //some task configuration/initialisation code can go here.
    task.setStringMapQueue(dataQueue);

    return task;
  }

  private JSONArray getAllTargetData(IRI sparqlEndpoint) {
    //retrieve data and replace empty string with it
    // limit to ontozone and OntoZoning graphs
    SelectBuilder sb = new SelectBuilder();
    sb.setBase(sparqlEndpoint.toString()).from(ONTOZONE_GRAPH).from(ONTOZONING_GRAPH)
            .addVar("?s").addVar("?p").addVar("?o")
            .addWhere("?s", "?p", "?o");

    JSONArray sparqlResult = AccessAgentCaller.queryStore(route, sb.buildString());

    return sparqlResult;
  }


}
