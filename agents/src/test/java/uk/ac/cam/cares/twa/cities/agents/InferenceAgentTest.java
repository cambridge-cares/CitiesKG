package uk.ac.cam.cares.twa.cities.agents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.semanticweb.owlapi.model.IRI;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.tasks.CardinalityRestrictionCheckingTask;
import uk.ac.cam.cares.twa.cities.tasks.ClassDisjointnessCheckingTask;
import uk.ac.cam.cares.twa.cities.tasks.ClassMembershipCheckingTask;
import uk.ac.cam.cares.twa.cities.tasks.ClassSpecialisationCheckingTask;
import uk.ac.cam.cares.twa.cities.tasks.ConsistencyCheckingTask;
import uk.ac.cam.cares.twa.cities.tasks.EdgeBetweennessTask;
import uk.ac.cam.cares.twa.cities.tasks.PageRankTask;
import uk.ac.cam.cares.twa.cities.tasks.PropertyCheckingTask;
import uk.ac.cam.cares.twa.cities.tasks.UninitialisedDataQueueTask;
import uk.ac.cam.cares.twa.cities.tasks.UnweightedShortestPathTask;
import uk.ac.cam.cares.twa.cities.tasks.ValueRestrictionCheckingTask;

public class InferenceAgentTest {

  @Test
  public void testInferenceAgentFields() {
    InferenceAgent agent = Mockito.mock(
        InferenceAgent.class,
        Mockito.CALLS_REAL_METHODS);
    assertEquals(40, InferenceAgent.class.getDeclaredFields().length);

    try {
      assertEquals(InferenceAgent.class.getDeclaredField("KEY_REQ_METHOD").get(agent), "method");
      assertEquals(InferenceAgent.class.getDeclaredField("KEY_REQ_URL").get(agent), "requestUrl");
      assertEquals(InferenceAgent.class.getDeclaredField("KEY_TARGET_IRI").get(agent), "targetIRI");
      assertEquals(InferenceAgent.class.getDeclaredField("KEY_SRC_IRI").get(agent), "sourceIRI");
      assertEquals(InferenceAgent.class.getDeclaredField("KEY_DST_IRI").get(agent), "destinationIRI");
      assertEquals(InferenceAgent.class.getDeclaredField("KEY_ALGO_IRI").get(agent), "algorithmIRI");
      assertEquals(InferenceAgent.class.getDeclaredField("KEY_ONTO_IRI").get(agent), "ontologyIRI");
      assertEquals(InferenceAgent.class.getDeclaredField("KEY_ASRT_IRI").get(agent), "assertionsIRI");
      assertEquals(InferenceAgent.class.getDeclaredField("KEY_PROP_IRI").get(agent), "propertyIRI");
      assertEquals(InferenceAgent.class.getDeclaredField("ONINF_PREFIX").get(agent), "oninf");
      assertEquals(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent), "http://www.theworldavatar.com/ontologies/OntoInfer.owl#");
      assertEquals(InferenceAgent.class.getDeclaredField("ONTOINFER_GRAPH").get(agent), "OntoInfer/");
      assertEquals(InferenceAgent.class.getDeclaredField("ONINT_P_INOBJ").get(agent), "hasInferenceObject");
      assertEquals(InferenceAgent.class.getDeclaredField("ONINT_P_INALG").get(agent), "hasInferenceAlgorithm");
      assertEquals(InferenceAgent.class.getDeclaredField("ONINT_P_INVAL").get(agent), "hasInferredValue");
      assertEquals(InferenceAgent.class.getDeclaredField("ONINT_C_PRALG").get(agent), "PageRankAlgorithm");
      assertEquals(InferenceAgent.class.getDeclaredField("ONINT_C_EBALG").get(agent), "EdgeBetweennessAlgorithm");
      assertEquals(InferenceAgent.class.getDeclaredField("ONINT_C_USPALG").get(agent), "UnweightedShortestPathAlgorithm");
      assertEquals(InferenceAgent.class.getDeclaredField("TASK_PR").get(agent), "PageRankTask");
      assertEquals(InferenceAgent.class.getDeclaredField("TASK_EB").get(agent), "EdgeBetweennessTask");
      assertEquals(InferenceAgent.class.getDeclaredField("TASK_USP").get(agent), "UnweightedShortestPathTask");
      assertEquals(InferenceAgent.class.getDeclaredField("TASK_CC").get(agent), "ConsistencyCheckingTask");
      assertEquals(InferenceAgent.class.getDeclaredField("TASK_CMC").get(agent), "ClassMembershipCheckingTask");
      assertEquals(InferenceAgent.class.getDeclaredField("TASK_CSC").get(agent), "ClassSpecialisationCheckingTask");
      assertEquals(InferenceAgent.class.getDeclaredField("TASK_CDC").get(agent), "ClassDisjointnessCheckingTask");
      assertEquals(InferenceAgent.class.getDeclaredField("TASK_PC").get(agent), "PropertyCheckingTask");
      assertEquals(InferenceAgent.class.getDeclaredField("TASK_VRC").get(agent), "ValueRestrictionCheckingTask");
      assertEquals(InferenceAgent.class.getDeclaredField("TASK_CRC").get(agent), "CardinalityRestrictionCheckingTask");
      assertEquals(InferenceAgent.class.getDeclaredField("IRI_ONDEB_AX").get(agent), "<http://ainf.aau.at/ontodebug#axiom>");
      assertEquals(InferenceAgent.class.getDeclaredField("IRI_ONDEB_TYP").get(agent), "<http://ainf.aau.at/ontodebug#type>");
      assertEquals(InferenceAgent.class.getDeclaredField("IRI_ONDEB_TST").get(agent), "<http://ainf.aau.at/ontodebug#testCase>");
      assertEquals(InferenceAgent.class.getDeclaredField("IRI_RDF_TYP").get(agent), "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>");
      assertEquals(InferenceAgent.class.getDeclaredField("IRI_RDF_NIL").get(agent), "<http://www.w3.org/1999/02/22-rdf-syntax-ns#nil>");
      assertEquals(InferenceAgent.class.getDeclaredField("IRI_OWL_THG").get(agent), "<http://www.w3.org/2002/07/owl#Thing>");
      assertEquals(InferenceAgent.class.getDeclaredField("URI_ACTION").get(agent), "/");
      Field taskExecutor = InferenceAgent.class.getDeclaredField("taskExecutor");
      taskExecutor.setAccessible(true);
      assertFalse(((ExecutorService) taskExecutor.get(agent)).isShutdown());
      Field dataQueue = InferenceAgent.class.getDeclaredField("dataQueue");
      dataQueue.setAccessible(true);
      assertTrue(((LinkedBlockingDeque) dataQueue.get(agent)).isEmpty());
      Field resultQueue = InferenceAgent.class.getDeclaredField("resultQueue");
      resultQueue.setAccessible(true);
      assertTrue(((LinkedBlockingDeque) dataQueue.get(agent)).isEmpty());
      Field tasks = InferenceAgent.class.getDeclaredField("TASKS");
      tasks.setAccessible(true);
      assertTrue(((Map<IRI, UninitialisedDataQueueTask>) tasks.get(agent)).containsKey(
          IRI.create(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent)
              + ((String) InferenceAgent.class.getDeclaredField("TASK_PR").get(agent)))));
      assertTrue(((Map<IRI, UninitialisedDataQueueTask>) tasks.get(agent)).containsKey(
          IRI.create(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent)
              + ((String) InferenceAgent.class.getDeclaredField("TASK_EB").get(agent)))));
      assertTrue(((Map<IRI, UninitialisedDataQueueTask>) tasks.get(agent)).containsKey(
          IRI.create(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent)
              + ((String) InferenceAgent.class.getDeclaredField("TASK_USP").get(agent)))));
      assertTrue(((Map<IRI, UninitialisedDataQueueTask>) tasks.get(agent)).containsKey(
          IRI.create(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent)
              + ((String) InferenceAgent.class.getDeclaredField("TASK_CC").get(agent)))));
      assertTrue(((Map<IRI, UninitialisedDataQueueTask>) tasks.get(agent)).containsKey(
          IRI.create(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent)
              + ((String) InferenceAgent.class.getDeclaredField("TASK_CMC").get(agent)))));
      assertTrue(((Map<IRI, UninitialisedDataQueueTask>) tasks.get(agent)).containsKey(
          IRI.create(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent)
              + ((String) InferenceAgent.class.getDeclaredField("TASK_CSC").get(agent)))));
      assertTrue(((Map<IRI, UninitialisedDataQueueTask>) tasks.get(agent)).containsKey(
          IRI.create(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent)
              + ((String) InferenceAgent.class.getDeclaredField("TASK_CDC").get(agent)))));
      assertTrue(((Map<IRI, UninitialisedDataQueueTask>) tasks.get(agent)).containsKey(
          IRI.create(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent)
              + ((String) InferenceAgent.class.getDeclaredField("TASK_PC").get(agent)))));
      assertTrue(((Map<IRI, UninitialisedDataQueueTask>) tasks.get(agent)).containsKey(
          IRI.create(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent)
              + ((String) InferenceAgent.class.getDeclaredField("TASK_VRC").get(agent)))));
      assertTrue(((Map<IRI, UninitialisedDataQueueTask>) tasks.get(agent)).containsKey(
          IRI.create(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent)
              + ((String) InferenceAgent.class.getDeclaredField("TASK_CRC").get(agent)))));
      assertEquals(((Map<IRI, UninitialisedDataQueueTask>) tasks.get(agent)).get(
          IRI.create(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent)
              + ((String) InferenceAgent.class.getDeclaredField("TASK_PR").get(agent))))
          .getClass(), PageRankTask.class);
     assertEquals(((Map<IRI, UninitialisedDataQueueTask>) tasks.get(agent)).get(
          IRI.create(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent)
              + ((String) InferenceAgent.class.getDeclaredField("TASK_EB").get(agent))))
         .getClass(), EdgeBetweennessTask.class);
      assertEquals(((Map<IRI, UninitialisedDataQueueTask>) tasks.get(agent)).get(
          IRI.create(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent)
              + ((String) InferenceAgent.class.getDeclaredField("TASK_USP").get(agent))))
          .getClass(), UnweightedShortestPathTask.class);
      assertEquals(((Map<IRI, UninitialisedDataQueueTask>) tasks.get(agent)).get(
          IRI.create(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent)
              + ((String) InferenceAgent.class.getDeclaredField("TASK_CC").get(agent))))
          .getClass(), ConsistencyCheckingTask.class);
      assertEquals(((Map<IRI, UninitialisedDataQueueTask>) tasks.get(agent)).get(
          IRI.create(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent)
              + ((String) InferenceAgent.class.getDeclaredField("TASK_CMC").get(agent))))
          .getClass(), ClassMembershipCheckingTask.class);
      assertEquals(((Map<IRI, UninitialisedDataQueueTask>) tasks.get(agent)).get(
          IRI.create(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent)
              + ((String) InferenceAgent.class.getDeclaredField("TASK_CSC").get(agent))))
          .getClass(), ClassSpecialisationCheckingTask.class);
      assertEquals(((Map<IRI, UninitialisedDataQueueTask>) tasks.get(agent)).get(
          IRI.create(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent)
              + ((String) InferenceAgent.class.getDeclaredField("TASK_CDC").get(agent))))
          .getClass(), ClassDisjointnessCheckingTask.class);
      assertEquals(((Map<IRI, UninitialisedDataQueueTask>) tasks.get(agent)).get(
          IRI.create(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent)
              + ((String) InferenceAgent.class.getDeclaredField("TASK_PC").get(agent))))
          .getClass(), PropertyCheckingTask.class);
      assertEquals(((Map<IRI, UninitialisedDataQueueTask>) tasks.get(agent)).get(
          IRI.create(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent)
              + ((String) InferenceAgent.class.getDeclaredField("TASK_VRC").get(agent))))
          .getClass(), ValueRestrictionCheckingTask.class);
      assertEquals(((Map<IRI, UninitialisedDataQueueTask>) tasks.get(agent)).get(
          IRI.create(InferenceAgent.class.getDeclaredField("ONINF_SCHEMA").get(agent)
              + ((String) InferenceAgent.class.getDeclaredField("TASK_CRC").get(agent))))
          .getClass(), CardinalityRestrictionCheckingTask.class);
      assertNull(InferenceAgent.class.getDeclaredField("route").get(agent));
    } catch (NoSuchFieldException | IllegalAccessException e) {
      fail();
    }

  }

  @Test
  public void testInferenceAgentMethods() {
    assertEquals(10, InferenceAgent.class.getDeclaredMethods().length);
  }

  @Test
  public void testValidateInput() {
    InferenceAgent agent = Mockito.mock(
        InferenceAgent.class,
        Mockito.CALLS_REAL_METHODS);
    JSONObject requestParams = new JSONObject();

    // test case when request params is empty
    try {
      agent.validateInput(requestParams);
    } catch (Exception e) {
      assert (e instanceof BadRequestException);
    }

    // test case when one key is missing
    requestParams.put("requestUrl", "test");
    requestParams.put("targetIRI", "http://127.0.0.1:9999/blazegraph/namespace/test/sparql/");
    requestParams.put("algorithmIRI", "http://www.theworldavatar.com/ontologies/OntoInfer.owl#EdgeBetweennessAlgorithm");
    requestParams.put("ontologyIRI", "http://www.theworldavatar.com/ontologies/OntoInfer.owl");
    requestParams.put("sourceIRI", "http://127.0.0.1:9999/blazegraph/namespace/test/sparql/source");
    requestParams.put("destinationIRI", "http://127.0.0.1:9999/blazegraph/namespace/test/sparql/destination");
    try {
      agent.validateInput(requestParams);
    } catch (Exception e) {
      assert (e instanceof BadRequestException);
    }

    // test case when http method is not POST
    requestParams.put("method", HttpMethod.GET);
    try {
      agent.validateInput(requestParams);
    } catch (Exception e) {
      assert (e instanceof BadRequestException);
    }

    // test case when requestUrl is not a valid url
    requestParams.put("method", HttpMethod.POST);
    try {
      agent.validateInput(requestParams);
    } catch (Exception e) {
      assert (e instanceof BadRequestException);
    }

    // test case when request params are valid
    requestParams.put("requestUrl", "http://localhost:8080/agents/inference/graph");
    assertTrue(agent.validateInput(requestParams));
  }

  @Test //@ TODO: rewrite
  public void testProcessRequestParameters() {
    JSONObject requestParams = new JSONObject();
    requestParams.put("requestUrl", "http://localhost:8080/agents/inference/graph");
    requestParams.put("targetIRI", "http://127.0.0.1:9999/blazegraph/namespace/test/sparql/");
    requestParams.put("algorithmIRI", "http://www.theworldavatar.com/ontologies/OntoInfer.owl#EdgeBetweennessAlgorithm");
    requestParams.put("method", HttpMethod.POST);

    JSONArray chooseTaskResult = new JSONArray().put(new JSONObject().put("o", "http://www.theworldavatar.com/ontologies/OntoInfer.owl#PageRankTask"));
    JSONArray getAllTargetDataResult = new JSONArray().put(new JSONObject().put("result key", "result"));

    ExecutorService spy = Mockito.spy(Executors.newFixedThreadPool(5));

    try (MockedStatic<Executors> exMock = Mockito.mockStatic(Executors.class)) {
      exMock.when(() -> Executors.newFixedThreadPool(ArgumentMatchers.anyInt())).thenReturn(spy);
      Mockito.doNothing().when(spy).execute(ArgumentMatchers.any());
      GraphInferenceAgent agent = new GraphInferenceAgent();

      try (MockedStatic<AccessAgentCaller> aacMock = Mockito.mockStatic(AccessAgentCaller.class)) {
        aacMock.when(() -> AccessAgentCaller.queryStore(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
            .thenReturn(chooseTaskResult)
            .thenReturn(getAllTargetDataResult);

        // check value of responseParams
        JSONObject response = agent.processRequestParameters(requestParams);
        assertEquals("started", response.get("http://www.theworldavatar.com/ontologies/OntoInfer.owl#PageRankTask"));

        // check if route is set
        Field route = agent.getClass().getDeclaredField("route");
        assertEquals(ResourceBundle.getBundle("config").getString("uri.route"), route.get(agent));

        // check if array is added to dataQueue
        Field dataQueue = agent.getClass().getDeclaredField("dataQueue");
        Map map = (Map) ((LinkedBlockingDeque) dataQueue.get(agent)).poll();
        assertEquals(getAllTargetDataResult, map.get("http://www.theworldavatar.com/ontologies/OntoInfer.owl#PageRankTask"));
      }
    } catch (NoSuchFieldException | IllegalAccessException e) {
      fail();
    }
  }

  @Test //@ TODO: rewrite
  public void testChooseTask() {
    GraphInferenceAgent agent = new GraphInferenceAgent();
    JSONArray result = new JSONArray().put(new JSONObject().put("o", "http://www.theworldavatar.com/ontologies/OntoInfer.owl#PageRankTask"));
    try (MockedStatic<AccessAgentCaller> aacMock = Mockito.mockStatic(AccessAgentCaller.class)) {
      aacMock.when(() -> AccessAgentCaller.queryStore(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
          .thenReturn(result);
      agent.getClass().getDeclaredField("route").set(agent, "http://localhost:48080/test");
      Method chooseTask = agent.getClass().getDeclaredMethod("chooseTask", IRI.class, IRI.class);
      chooseTask.setAccessible(true);
      UninitialisedDataQueueTask task = (UninitialisedDataQueueTask) chooseTask.invoke(agent, IRI.create("http://www.theworldavatar.com/ontologies/OntoInfer.owl#PageRankAlgorithm"),
          IRI.create("http://127.0.0.1:9999/blazegraph/namespace/test/sparql/"));
      assertEquals(PageRankTask.class, task.getClass());

      Field agentDataQueueField = agent.getClass().getDeclaredField("dataQueue");
      Field taskDataQueueField = task.getClass().getDeclaredField("dataQueue");
      taskDataQueueField.setAccessible(true);
      assertEquals(agentDataQueueField.get(agent), taskDataQueueField.get(task));

      Field targetGraph = task.getClass().getDeclaredField("targetGraph");
      targetGraph.setAccessible(true);
      assertEquals("http://127.0.0.1:9999/blazegraph/namespace/test/sparql/OntoInfer/", targetGraph.get(task).toString());
    } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }
  }

  @Test
  public void testGetSparqlBuilder() {
    InferenceAgent agent = Mockito.mock(
        InferenceAgent.class,
        Mockito.CALLS_REAL_METHODS);
    String query = "BASE    <http://127.0.0.1:9999/blazegraph/namespace/test/sparql/>\n"
        + "\n"
        + "SELECT  ?s ?p ?o\n"
        + "FROM <testgraph>\n"
        + "WHERE\n"
        + "  { ?s  ?p  ?o\n"
        + "    FILTER ( ?p != <http://ainf.aau.at/ontodebug#axiom> )\n"
        + "    FILTER ( ?p != <http://ainf.aau.at/ontodebug#type> )\n"
        + "    FILTER ( ?o != <http://ainf.aau.at/ontodebug#testCase> )\n"
        + "  }\n";

    try {
      assertEquals(agent.getSparqlBuilder(IRI.create("http://127.0.0.1:9999/blazegraph/namespace/test/sparql/"),
          "testgraph").buildString(), query);
    } catch (ParseException e) {
      fail();
    }
  }

  @Test
  public void testGetAllTargetData() {
    InferenceAgent agent = Mockito.mock(
        InferenceAgent.class,
        Mockito.CALLS_REAL_METHODS);

    JSONArray result = new JSONArray();
    try (MockedStatic<AccessAgentCaller> aacMock = Mockito.mockStatic(AccessAgentCaller.class)) {
      aacMock.when(() -> AccessAgentCaller.queryStore(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
          .thenReturn(result);
      //@ TODO: implementation
    }
  }

}
