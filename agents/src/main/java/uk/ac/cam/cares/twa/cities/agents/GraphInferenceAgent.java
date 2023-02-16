package uk.ac.cam.cares.twa.cities.agents;

import java.net.URL;
import java.util.HashMap;
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
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.json.JSONArray;
import org.semanticweb.owlapi.model.IRI;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.tasks.EdgeBetweennessTask;
import uk.ac.cam.cares.twa.cities.tasks.PageRankTask;
import uk.ac.cam.cares.twa.cities.tasks.UninitialisedDataAndResultQueueTask;
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
public class GraphInferenceAgent extends InferenceAgent {
  public static final String URI_ACTION = "/inference/graph";

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
              if (keys.contains(KEY_SRC_IRI)) {
                IRI.create(requestParams.getString(KEY_SRC_IRI));
              }
              if (keys.contains(KEY_DST_IRI)) {
                IRI.create(requestParams.getString(KEY_SRC_IRI));
              }
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
  protected Map<String,JSONArray> prepareTaskData(String targetIRI, String taskIRI,
      JSONObject requestParams)
      throws ParseException {
      Map<String,JSONArray> taskData = new HashMap<>();

      // Retrieve data from KG based on target IRI
      JSONArray targetData = getAllTargetData(IRI.create(targetIRI));

      taskData.put(taskIRI, targetData);
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

  private JSONArray getAllTargetData(IRI sparqlEndpoint) throws ParseException {
    //retrieve data and replace empty string with it
    // limit to ontozone and OntoZoning graphs
    SelectBuilder sb = new SelectBuilder();
    sb.setBase(sparqlEndpoint.toString()).from(ONTOZONING_GRAPH)
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


    sb = new SelectBuilder();
    sb.setBase(sparqlEndpoint.toString()).from(ONTOZONE_GRAPH)
        .addVar("?s").addVar("?p").addVar("?o")
        .addWhere("?s", "?p", "?o")
    ;

    JSONArray sparqlResultTwo = AccessAgentCaller.queryStore(route, sb.buildString());

    for (Object data : sparqlResultTwo) {
      sparqlResult.put(data);
    }


    return sparqlResult;
  }


}
