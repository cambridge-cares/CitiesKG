package uk.ac.cam.cares.twa.cities.agents;

import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import org.semanticweb.owlapi.model.IRI;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.twa.cities.tasks.PageRankTask;

/**
 * A JPSAgent framework based Graph Inference class used to infer structural information about graphs.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 * @version $Id$
 */
@WebServlet(
    urlPatterns = {
        uk.ac.cam.cares.twa.cities.agents.GraphInferenceAgent.URI_ACTION
    })
public class GraphInferenceAgent extends JPSAgent {
  public static final String PREF_ONTO_INFER = "http://www.theworldavatar.com/ontologies/OntoInfer.owl";
  public static final String URI_ACTION = "/inference/graph";
  public static final String KEY_REQ_METHOD = "method";
  public static final String KEY_REQ_URL = "requestUrl";
  public static final String KEY_TARGET_IRI = "targetIRI";
  public static final String KEY_ALGO_IRI = "algorithmIRI";
  private final Map<IRI, Runnable> TASKS = Stream.of(new Object[][] {
      {IRI.create(PREF_ONTO_INFER + "#PageRankTask"), new PageRankTask()},
  }).collect(Collectors.toMap(data -> (IRI) data[0], data -> (Runnable) data[1]));


  @Override
  public JSONObject processRequestParameters(JSONObject requestParams) {
    JSONObject responseParams = new JSONObject();
    if (validateInput(requestParams)) {
      //(1) Choose task based on algorithm IRI
      Runnable task = chooseTask(IRI.create(requestParams.getString(KEY_ALGO_IRI)));
      //(2) Retrieve data from target IRI

      //(3) Pass target data (2) to the task (1) and run the task

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

  private Runnable chooseTask(IRI algorithmIRI) {

    String taskId = "";
    //Retrieve task IRI by algorithm IRI from KG and assign to taskId


    //Get task from map by IRI
    IRI taskIri = IRI.create(taskId);
    Runnable task = TASKS.get(taskIri);
    //some task configuration/initialisation code can go here.

    return task;
  }



}
