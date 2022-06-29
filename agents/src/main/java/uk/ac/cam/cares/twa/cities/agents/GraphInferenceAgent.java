package uk.ac.cam.cares.twa.cities.agents;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.http.HttpException;
import org.semanticweb.owlapi.model.IRI;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.tasks.PageRankTask;
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
  public static final String PREF_ONT_INFER = "http://www.theworldavatar.com/ontologies/OntoInfer.owl#";
  public static final String TASK_PR = "PageRankTask";
  public static final String URI_ACTION = "/inference/graph";
  public static final String KEY_REQ_METHOD = "method";
  public static final String KEY_REQ_URL = "requestUrl";
  public static final String KEY_TARGET_IRI = "targetIRI";
  public static final String KEY_ALGO_IRI = "algorithmIRI";
  private final Map<IRI, UninitialisedDataQueueTask> TASKS = Stream.of(new Object[][] {
      {IRI.create(PREF_ONT_INFER + TASK_PR), new PageRankTask()},
  }).collect(Collectors.toMap(data -> (IRI) data[0], data -> (UninitialisedDataQueueTask) data[1]));
  public static LinkedBlockingDeque<Map<String, String>> dataQueue = new LinkedBlockingDeque<>();
  private static final ExecutorService taskExecutor = Executors.newFixedThreadPool(5);

  @Override
  public JSONObject processRequestParameters(JSONObject requestParams) {
    JSONObject responseParams = new JSONObject();
    if (validateInput(requestParams)) {
      try {

        //(1) Choose task based on algorithm IRI
        UninitialisedDataQueueTask task = chooseTask(IRI.create(requestParams.getString(KEY_ALGO_IRI)),
            IRI.create(requestParams.getString(KEY_TARGET_IRI)));
        //(2) Retrieve data from target IRI
        String targetData = getAllTargetData(IRI.create(requestParams.getString(KEY_TARGET_IRI)));
        //(3) Pass target data (2) to the task (1) and run the task
        dataQueue.put(Collections.singletonMap(task.getTaskIri().toString(), targetData));
        taskExecutor.execute(task);
        //(4) add task information to the response
        responseParams.put(PREF_ONT_INFER + task.getTaskIri().toString(), "started");

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

    return error;
  }

  private UninitialisedDataQueueTask chooseTask(IRI algorithmIRI, IRI sparqlEndpoint) {

    //Retrieve task IRI by algorithm IRI from KG and assign to taskId
    String sparql =  "PREFIX oninf: <" + PREF_ONT_INFER + ">" +
    "BASE <" + sparqlEndpoint.toString() + ">" +
    "SELECT ?o" +
    "from <OntoInfer/>" +
    "WHERE { <" + algorithmIRI.toString() + "> oninf:appliedBy ?o }";
    // |@todo: change that to query builder and execute with access agent and it should return:
    String sparqlResult = "http://www.theworldavatar.com/ontologies/OntoInfer.owl#PageRankTask";


    //Get task from map by IRI
    IRI taskIri = IRI.create(sparqlResult);
    UninitialisedDataQueueTask task = (UninitialisedDataQueueTask) TASKS.get(taskIri);
    //some task configuration/initialisation code can go here.
    task.setStringMapQueue(dataQueue);

    return task;
  }

  private String getAllTargetData(IRI sparqlEndpoint) {
    String targetData = "";
    //retrieve data and replace empty string with it
    String sparql =  "SELECT ?s ?p ?o"
                      + "WHERE {?s ?p ?o}";
    // |@todo: change that to query builder and execute with access agent

    return targetData;
  }


}
