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

  @Override
  protected Map<String,JSONArray> prepareTaskData(String targetIRI, String taskIRI, JSONObject requestParams)
      throws ParseException {
    Map<String,JSONArray> taskData = new HashMap<>();

    //(1) Retrieve data from KG based on target IRI
    String tBoxGraph = requestParams.getString(KEY_ONTO_IRI).replace(targetIRI, "");
    JSONArray targetData = getAllTargetData(IRI.create(targetIRI), tBoxGraph);
    //add aBox data if needed
    if (requestParams.keySet().contains(KEY_ASRT_IRI)) {
      String aBoxGraph = requestParams.getString(KEY_ASRT_IRI).replace(targetIRI, "");
      targetData = getAllTargetData(IRI.create(targetIRI), aBoxGraph, targetData);
    }

    taskData.put(taskIRI, targetData);

    //(2) Add data from request parameters
    taskData.put(KEY_ONTO_IRI, new JSONArray().put(tBoxGraph));

    if (requestParams.keySet().contains(KEY_SRC_IRI)) {
      JSONArray srcIriArr = new JSONArray().put(requestParams.get(KEY_SRC_IRI));
      taskData.put(KEY_SRC_IRI, srcIriArr);
    }

    if (requestParams.keySet().contains(KEY_DST_IRI)) {
      JSONArray srcIriArr = new JSONArray().put(requestParams.get(KEY_DST_IRI));
      taskData.put(KEY_DST_IRI, srcIriArr);
    }

    return taskData;
  }

  private JSONArray getAllTargetData(IRI sparqlEndpoint, String tBoxGraph) throws ParseException {
    //retrieve data and replace empty string with it
    SelectBuilder sb = new SelectBuilder();
    sb.setBase(sparqlEndpoint.toString()).from(tBoxGraph)
        .addVar("?s").addVar("?p").addVar("?o")
        .addWhere("?s", "?p", "?o")
        .addFilter("?p != <http://ainf.aau.at/ontodebug#axiom>")
        .addFilter("?p != <http://ainf.aau.at/ontodebug#type>")
        .addFilter("?o != <http://ainf.aau.at/ontodebug#testCase>");

    JSONArray sparqlResult = AccessAgentCaller.queryStore(route, sb.buildString());

    return sparqlResult;
  }

  private JSONArray getAllTargetData(IRI sparqlEndpoint, String aBoxGraph, JSONArray targetData) {

    SelectBuilder sb = new SelectBuilder();
    sb.setBase(sparqlEndpoint.toString()).from(aBoxGraph)
        .addVar("?s").addVar("?p").addVar("?o")
        .addWhere("?s", "?p", "?o");
    JSONArray sparqlResult = AccessAgentCaller.queryStore(route, sb.buildString());

    for (Object triple : sparqlResult) {
      targetData.put(triple);
    }

    return targetData;
  }

}
