package uk.ac.cam.cares.twa.cities.agents;

import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.json.JSONArray;
import org.semanticweb.owlapi.model.IRI;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.tasks.ConsistencyCheckingTask;
import uk.ac.cam.cares.twa.cities.tasks.EdgeBetweennessTask;
import uk.ac.cam.cares.twa.cities.tasks.PageRankTask;
import uk.ac.cam.cares.twa.cities.tasks.UninitialisedDataAndResultQueueTask;
import uk.ac.cam.cares.twa.cities.tasks.UninitialisedDataQueueTask;
import uk.ac.cam.cares.twa.cities.tasks.UnweightedShortestPathTask;

public abstract class InferenceAgent extends JPSAgent {
  public String route;
  public static final String KEY_REQ_METHOD = "method";
  public static final String KEY_REQ_URL = "requestUrl";
  public static final String KEY_TARGET_IRI = "targetIRI";
  public static final String KEY_ALGO_IRI = "algorithmIRI";
  public static final String KEY_ONTO_IRI = "ontologyIRI";
  public static final String ONINF_PREFIX = "oninf";
  public static final String ONINF_SCHEMA = "http://www.theworldavatar.com/ontologies/OntoInfer.owl#";
  public static final String ONTOINFER_GRAPH = "OntoInfer/";
  public static final String ONTOZONE_GRAPH = "ontozone/";
  public static final String ONTOZONING_GRAPH = "OntoZoning/";
  public static final String ONINT_P_INOBJ = "hasInferenceObject";
  public static final String ONINT_P_INALG = "hasInferenceAlgorithm";
  public static final String ONINT_P_INVAL = "hasInferredValue";
  public static final String ONINT_C_PRALG = "PageRankAlgorithm";
  public static final String ONINT_C_EBALG = "EdgeBetweennessAlgorithm";
  public static final String ONINT_C_USPALG = "UnweightedShortestPathAlgorithm";
  public static final String ONINT_C_CCALG = "ConsistencyCheckingAlgorithm";
  public static final String TASK_PR = "PageRankTask";
  public static final String TASK_EB = "EdgeBetweennessTask";
  public static final String TASK_USP = "UnweightedShortestPathTask";
  public static final String TASK_CC = "ConsistencyCheckingTask";
  protected final Map<IRI, UninitialisedDataQueueTask> TASKS = Stream.of(new Object[][] {
      {IRI.create(ONINF_SCHEMA + TASK_PR), new PageRankTask()},
      {IRI.create(ONINF_SCHEMA + TASK_EB), new EdgeBetweennessTask()},
      {IRI.create(ONINF_SCHEMA + TASK_USP), new UnweightedShortestPathTask()},
      {IRI.create(ONINF_SCHEMA + TASK_CC), new ConsistencyCheckingTask()}
  }).collect(Collectors.toMap(data -> (IRI) data[0], data -> (UninitialisedDataQueueTask) data[1]));
  protected static LinkedBlockingDeque<Map<String, JSONArray>> dataQueue = new LinkedBlockingDeque<>();
  protected static LinkedBlockingDeque<Map<String, JSONArray>> resultQueue = new LinkedBlockingDeque<>();


  protected UninitialisedDataQueueTask chooseTask(IRI algorithmIRI, IRI sparqlEndpoint) {

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
    task.setTargetGraph(sparqlEndpoint.toString());
    if (task instanceof UninitialisedDataAndResultQueueTask) {
      ((UninitialisedDataAndResultQueueTask) task).setResultQueue(resultQueue);
    }

    return task;
  }


}
