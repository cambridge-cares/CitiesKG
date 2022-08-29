package uk.ac.cam.cares.twa.cities.agents;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.semanticweb.owlapi.model.IRI;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.tasks.EdgeBetweennessTask;
import uk.ac.cam.cares.twa.cities.tasks.PageRankTask;
import uk.ac.cam.cares.twa.cities.tasks.UninitialisedDataQueueTask;
import uk.ac.cam.cares.twa.cities.tasks.UnweightedShortestPathTask;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import static org.junit.jupiter.api.Assertions.*;

class GraphInferenceAgentTest {

    @Test
    public void testNewGraphInferenceAgent() {
        GraphInferenceAgent agent = new GraphInferenceAgent();
        assertNotNull(agent);
    }

    @Test
    public void testNewGraphInferenceAgentFields() {
        GraphInferenceAgent agent = new GraphInferenceAgent();
        assertEquals(22, agent.getClass().getDeclaredFields().length);

        try {
            assertEquals("oninf", agent.getClass().getDeclaredField("ONINF_PREFIX").get(agent));
            assertEquals("http://www.theworldavatar.com/ontologies/OntoInfer.owl#", agent.getClass().getDeclaredField("ONINF_SCHEMA").get(agent));
            assertEquals("OntoInfer/", agent.getClass().getDeclaredField("ONTOINFER_GRAPH").get(agent));
            assertEquals("ontozone/", agent.getClass().getDeclaredField("ONTOZONE_GRAPH").get(agent));
            assertEquals("OntoZoning/", agent.getClass().getDeclaredField("ONTOZONING_GRAPH").get(agent));
            assertEquals("hasInferenceObject", agent.getClass().getDeclaredField("ONINT_P_INOBJ").get(agent));
            assertEquals("hasInferenceAlgorithm", agent.getClass().getDeclaredField("ONINT_P_INALG").get(agent));
            assertEquals("hasInferredValue", agent.getClass().getDeclaredField("ONINT_P_INVAL").get(agent));
            assertEquals("PageRankAlgorithm", agent.getClass().getDeclaredField("ONINT_C_PRALG").get(agent));
            assertEquals("EdgeBetweennessAlgorithm", agent.getClass().getDeclaredField("ONINT_C_EBALG").get(agent));

            assertNull(agent.getClass().getDeclaredField("route").get(agent));
            assertEquals("PageRankTask", agent.getClass().getDeclaredField("TASK_PR").get(agent));
            assertEquals("EdgeBetweennessTask", agent.getClass().getDeclaredField("TASK_EB").get(agent));
            assertEquals("UnweightedShortestPathTask", agent.getClass().getDeclaredField("TASK_USP").get(agent));
            assertEquals("/inference/graph", agent.getClass().getDeclaredField("URI_ACTION").get(agent));
            assertEquals("method", agent.getClass().getDeclaredField("KEY_REQ_METHOD").get(agent));
            assertEquals("requestUrl", agent.getClass().getDeclaredField("KEY_REQ_URL").get(agent));
            assertEquals("targetIRI", agent.getClass().getDeclaredField("KEY_TARGET_IRI").get(agent));
            assertEquals("algorithmIRI", agent.getClass().getDeclaredField("KEY_ALGO_IRI").get(agent));

            Field TASKS = agent.getClass().getDeclaredField("TASKS");
            TASKS.setAccessible(true);
            Map<IRI, UninitialisedDataQueueTask> map = (HashMap) TASKS.get(agent);
            assertNotNull(map);
            assertEquals(PageRankTask.class, map.get(IRI.create("http://www.theworldavatar.com/ontologies/OntoInfer.owl#PageRankTask")).getClass());
            assertEquals(EdgeBetweennessTask.class, map.get(IRI.create("http://www.theworldavatar.com/ontologies/OntoInfer.owl#EdgeBetweennessTask")).getClass());
            assertEquals(UnweightedShortestPathTask.class, map.get(IRI.create("http://www.theworldavatar.com/ontologies/OntoInfer.owl#UnweightedShortestPathTask")).getClass());

            assertNotNull(agent.getClass().getDeclaredField("dataQueue").get(agent));

            Field taskExecutor = agent.getClass().getDeclaredField("taskExecutor");
            taskExecutor.setAccessible(true);
            assertNotNull(taskExecutor.get(agent));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testNewGraphInferenceAgentMethods() {
        GraphInferenceAgent agent = new GraphInferenceAgent();
        assertEquals(6, agent.getClass().getDeclaredMethods().length);
    }

    @Test
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

    @Test
    public void testValidateInput() {
        GraphInferenceAgent agent = new GraphInferenceAgent();
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

        // test case when requestUrl does not contain /inference/graph
        requestParams.put("requestUrl", "http://localhost:8080/");
        try {
            agent.validateInput(requestParams);
        } catch (Exception e) {
            assert (e instanceof BadRequestException);
        }

        // test case when request params are valid
        requestParams.put("requestUrl", "http://localhost:8080/agents/inference/graph");
        assertTrue(agent.validateInput(requestParams));
    }

    @Test
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
    public void testGetAllTargetData() {
        GraphInferenceAgent agent = new GraphInferenceAgent();
        JSONArray result = new JSONArray();
        try (MockedStatic<AccessAgentCaller> aacMock = Mockito.mockStatic(AccessAgentCaller.class)) {
            aacMock.when(() -> AccessAgentCaller.queryStore(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                    .thenReturn(result);
            agent.getClass().getDeclaredField("route").set(agent, "http://localhost:48080/test");
            Method getAllTargetData = agent.getClass().getDeclaredMethod("getAllTargetData", IRI.class);
            getAllTargetData.setAccessible(true);
            assertEquals(result, getAllTargetData.invoke(agent, IRI.create("http://127.0.0.1:9999/blazegraph/namespace/test/sparql/")));
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            fail();
        }
    }

}