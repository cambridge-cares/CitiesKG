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
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.model.IRI;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.tasks.CardinalityRestrictionCheckingTask;
import uk.ac.cam.cares.twa.cities.tasks.ClassDisjointnessCheckingTask;
import uk.ac.cam.cares.twa.cities.tasks.ClassMembershipCheckingTask;
import uk.ac.cam.cares.twa.cities.tasks.ClassSpecialisationCheckingTask;
import uk.ac.cam.cares.twa.cities.tasks.ConsistencyCheckingTask;
import uk.ac.cam.cares.twa.cities.tasks.EdgeBetweennessTask;
import uk.ac.cam.cares.twa.cities.tasks.PageRankTask;
import uk.ac.cam.cares.twa.cities.tasks.PropertyCheckingTask;
import uk.ac.cam.cares.twa.cities.tasks.UninitialisedDataAndResultQueueTask;
import uk.ac.cam.cares.twa.cities.tasks.UninitialisedDataQueueTask;
import uk.ac.cam.cares.twa.cities.tasks.UnweightedShortestPathTask;
import uk.ac.cam.cares.twa.cities.tasks.ValueRestrictionCheckingTask;

public abstract class InferenceAgent extends JPSAgent {

  public static final String KEY_REQ_METHOD = "method";
  public static final String KEY_REQ_URL = "requestUrl";
  public static final String KEY_TARGET_IRI = "targetIRI";
  public static final String KEY_SRC_IRI = "sourceIRI";
  public static final String KEY_DST_IRI = "destinationIRI";
  public static final String KEY_ALGO_IRI = "algorithmIRI";
  public static final String KEY_ONTO_IRI = "ontologyIRI";
  public static final String KEY_ASRT_IRI = "assertionsIRI";
  public static final String KEY_PROP_IRI = "propertyIRI";
  public static final String ONINF_PREFIX = "oninf";
  public static final String ONINF_SCHEMA = "http://www.theworldavatar.com/ontologies/OntoInfer.owl#";
  public static final String ONTOINFER_GRAPH = "OntoInfer/";
  public static final String ONINT_P_INOBJ = "hasInferenceObject";
  public static final String ONINT_P_INALG = "hasInferenceAlgorithm";
  public static final String ONINT_P_INVAL = "hasInferredValue";
  public static final String ONINT_C_PRALG = "PageRankAlgorithm";
  public static final String ONINT_C_EBALG = "EdgeBetweennessAlgorithm";
  public static final String ONINT_C_USPALG = "UnweightedShortestPathAlgorithm";
  public static final String TASK_PR = "PageRankTask";
  public static final String TASK_EB = "EdgeBetweennessTask";
  public static final String TASK_USP = "UnweightedShortestPathTask";
  public static final String TASK_CC = "ConsistencyCheckingTask";
  public static final String TASK_CMC = "ClassMembershipCheckingTask";
  public static final String TASK_CSC = "ClassSpecialisationCheckingTask";
  public static final String TASK_CDC = "ClassDisjointnessCheckingTask";
  public static final String TASK_PC = "PropertyCheckingTask";
  public static final String TASK_VRC = "ValueRestrictionCheckingTask";
  public static final String TASK_CRC = "CardinalityRestrictionCheckingTask";
  public static final String IRI_ONDEB_AX = "<http://ainf.aau.at/ontodebug#axiom>";
  public static final String IRI_ONDEB_TYP = "<http://ainf.aau.at/ontodebug#type>";
  public static final String IRI_ONDEB_TST = "<http://ainf.aau.at/ontodebug#testCase>";
  public static final String IRI_RDF_TYP = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
  public static final String IRI_RDF_NIL = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#nil>";
  public static final String IRI_OWL_THG = "<http://www.w3.org/2002/07/owl#Thing>";
  private static final ExecutorService taskExecutor = Executors.newFixedThreadPool(5);
  protected static LinkedBlockingDeque<Map<String, JSONArray>> dataQueue = new LinkedBlockingDeque<>();
  protected static LinkedBlockingDeque<Map<String, JSONArray>> resultQueue = new LinkedBlockingDeque<>();
  protected final Map<IRI, UninitialisedDataQueueTask> TASKS = Stream.of(new Object[][]{
      {IRI.create(ONINF_SCHEMA + TASK_PR), new PageRankTask()},
      {IRI.create(ONINF_SCHEMA + TASK_EB), new EdgeBetweennessTask()},
      {IRI.create(ONINF_SCHEMA + TASK_USP), new UnweightedShortestPathTask()},
      {IRI.create(ONINF_SCHEMA + TASK_CC), new ConsistencyCheckingTask()},
      {IRI.create(ONINF_SCHEMA + TASK_CMC), new ClassMembershipCheckingTask()},
      {IRI.create(ONINF_SCHEMA + TASK_CSC), new ClassSpecialisationCheckingTask()},
      {IRI.create(ONINF_SCHEMA + TASK_CDC), new ClassDisjointnessCheckingTask()},
      {IRI.create(ONINF_SCHEMA + TASK_PC), new PropertyCheckingTask()},
      {IRI.create(ONINF_SCHEMA + TASK_VRC), new ValueRestrictionCheckingTask()},
      {IRI.create(ONINF_SCHEMA + TASK_CRC), new CardinalityRestrictionCheckingTask()}
  }).collect(Collectors.toMap(data -> (IRI) data[0], data -> (UninitialisedDataQueueTask) data[1]));
  public String route;
  public static final String URI_ACTION = "/";

  @Override
  public boolean validateInput(JSONObject requestParams) throws BadRequestException {
    boolean error = true;

    if (!requestParams.isEmpty()) {
      Set<String> keys = requestParams.keySet();
      if (keys.contains(KEY_REQ_METHOD) && keys.contains(KEY_REQ_URL) && keys.contains(
          KEY_TARGET_IRI) && keys.contains(KEY_ALGO_IRI) && keys.contains(KEY_ONTO_IRI)) {
        if (requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.POST)) {
          try {
            URL reqUrl = new URL(requestParams.getString(KEY_REQ_URL));
            if (reqUrl.getPath().contains(URI_ACTION)) {
              IRI.create(requestParams.getString(KEY_TARGET_IRI));
              IRI.create(requestParams.getString(KEY_ALGO_IRI));
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
  public JSONObject processRequestParameters(JSONObject requestParams) {
    JSONObject responseParams = new JSONObject();
    if (validateInput(requestParams)) {
      try {
        // setup route for AccessAgent and check if targetIri has the trailing /
        route = ResourceBundle.getBundle("config").getString("uri.route");
        String targetIRI = requestParams.getString(KEY_TARGET_IRI).endsWith("/")
            ? requestParams.getString(KEY_TARGET_IRI)
            : requestParams.getString(KEY_TARGET_IRI).concat("/");

        //(1) Choose task based on algorithm IRI
        UninitialisedDataQueueTask task = chooseTask(
            IRI.create(requestParams.getString(KEY_ALGO_IRI)),
            IRI.create(targetIRI));
        String taskIRI = task.getTaskIri().toString();

        //(2) Add data to the queue and execute task
        dataQueue.put(prepareTaskData(targetIRI, taskIRI, requestParams));
        taskExecutor.execute(task);

        //(3) add task information to the response
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

  /**
   * Method to prepare task data.
   *
   * @param targetIRI IRI of the target SPARQL endpoint
   * @param taskIRI IRI of the task
   * @param requestParams Extracted HTTP request parameters
   * @return Data for the task
   * @throws ParseException Thrown when SPARQL query to fetch data could not be built
   */
  protected Map<String, JSONArray> prepareTaskData(String targetIRI, String taskIRI,
      JSONObject requestParams)
      throws ParseException {
    Map<String, JSONArray> taskData = new HashMap<>();
    String tBoxGraph = requestParams.getString(KEY_ONTO_IRI).replace(targetIRI, "");
    // Retrieve data from KG based on target IRI
    JSONArray targetData = getAllTargetData(IRI.create(targetIRI), tBoxGraph);

    //add aBox data if needed
    if (requestParams.keySet().contains(KEY_ASRT_IRI)) {
      String aBoxGraph = requestParams.getString(KEY_ASRT_IRI).replace(targetIRI, "");
      getAllTargetData(IRI.create(targetIRI), aBoxGraph, targetData);
    }

    //(2) Add data from request parameters
    taskData.put(KEY_ONTO_IRI, new JSONArray().put(tBoxGraph));
    taskData.put(taskIRI, targetData);

    return addRequestDataToTaskData(taskData, requestParams);
  }

  protected Map<String, JSONArray> addRequestDataToTaskData(Map<String, JSONArray> taskData,
      JSONObject requestParams) {
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
    UninitialisedDataQueueTask task = TASKS.get(taskIri);
    //some task configuration/initialisation code can go here.
    task.setStringMapQueue(dataQueue);
    task.setTargetGraph(sparqlEndpoint.toString());
    if (task instanceof UninitialisedDataAndResultQueueTask) {
      ((UninitialisedDataAndResultQueueTask) task).setResultQueue(resultQueue);
    }

    return task;
  }

  protected SelectBuilder getSparqlBuilder(IRI sparqlEndpoint, String tBoxGraph)
      throws ParseException {
    SelectBuilder sb = new SelectBuilder();
    sb.setBase(sparqlEndpoint.toString()).from(tBoxGraph)
        .addVar("?s").addVar("?p").addVar("?o")
        .addWhere("?s", "?p", "?o")
        .addFilter("?p != " + IRI_ONDEB_AX)
        .addFilter("?p != " + IRI_ONDEB_TYP)
        .addFilter("?o != " + IRI_ONDEB_TST);
    return sb;
  }

  protected JSONArray getAllTargetData(IRI sparqlEndpoint, String tBoxGraph) throws ParseException{
    return null;
  }

  protected void getAllTargetData(IRI sparqlEndpoint, String aBoxGraph, JSONArray targetData) {
    SelectBuilder sb = new SelectBuilder();
    sb.setBase(sparqlEndpoint.toString()).from(aBoxGraph)
        .addVar("?s").addVar("?p").addVar("?o")
        .addWhere("?s", "?p", "?o");
    JSONArray sparqlResult = AccessAgentCaller.queryStore(route, sb.buildString());

    for (Object triple : sparqlResult) {
      targetData.put(triple);
    }

  }

}
