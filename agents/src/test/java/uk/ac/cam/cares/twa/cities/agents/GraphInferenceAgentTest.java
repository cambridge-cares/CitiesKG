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
        assertEquals(2, agent.getClass().getDeclaredFields().length);

        try {
            assertEquals("/inference/graph", agent.getClass().getDeclaredField("URI_ACTION").get(agent));
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
        assertEquals(3, agent.getClass().getDeclaredMethods().length);
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
    public void testGetAllTargetData() {
        GraphInferenceAgent agent = new GraphInferenceAgent();
        JSONArray result = new JSONArray();
        try (MockedStatic<AccessAgentCaller> aacMock = Mockito.mockStatic(AccessAgentCaller.class)) {
            aacMock.when(() -> AccessAgentCaller.queryStore(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                    .thenReturn(result);
            agent.getClass().getSuperclass().getDeclaredField("route").set(agent, "http://localhost:48080/test");
            Method getAllTargetData = agent.getClass().getDeclaredMethod("getAllTargetData", IRI.class);
            getAllTargetData.setAccessible(true);
            assertEquals(result, getAllTargetData.invoke(agent, IRI.create("http://127.0.0.1:9999/blazegraph/namespace/test/sparql/")));
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            fail();
        }
    }

}