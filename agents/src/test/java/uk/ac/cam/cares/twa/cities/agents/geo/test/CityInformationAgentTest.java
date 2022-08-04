package uk.ac.cam.cares.twa.cities.agents.geo.test;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import uk.ac.cam.cares.jps.base.http.Http;
import uk.ac.cam.cares.twa.cities.agents.geo.CityInformationAgent;
import uk.ac.cam.cares.ogm.models.ModelContext;
import uk.ac.cam.cares.twa.cities.model.geo.CityObject;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CityInformationAgentTest {

    @Test
    public void testNewCityInformationAgent() {
        CityInformationAgent agent;

        try {
            agent = new CityInformationAgent();
            assertNotNull(agent);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewCityInformationAgentFields() {
        CityInformationAgent agent = new CityInformationAgent();

        assertEquals(7, agent.getClass().getDeclaredFields().length);

        try {
            assertEquals("/cityobjectinformation", agent.getClass().getDeclaredField("URI_CITY_OBJECT_INFORMATION").get(agent));
            assertEquals("method", agent.getClass().getDeclaredField("KEY_REQ_METHOD").get(agent));
            assertEquals("iris", agent.getClass().getDeclaredField("KEY_IRIS").get(agent));
            assertEquals("context", agent.getClass().getDeclaredField("KEY_CONTEXT").get(agent));
            assertEquals("cityobjectinformation", agent.getClass().getDeclaredField("KEY_CITY_OBJECT_INFORMATION").get(agent));

            // test readConfig
            Field route = agent.getClass().getDeclaredField("route");
            route.setAccessible(true);
            assertEquals(ResourceBundle.getBundle("config").getString("uri.route"), route.get(agent));
            Field lazyload = agent.getClass().getDeclaredField("lazyload");
            lazyload.setAccessible(true);
            assertEquals(Boolean.getBoolean(ResourceBundle.getBundle("config").getString("loading.status")), lazyload.get(agent));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testNewCityInformationAgentMethods() {
        CityInformationAgent agent = new CityInformationAgent();

        assertEquals(4, agent.getClass().getDeclaredMethods().length);
    }

    @Test
    public void testProcessRequestParameters() {
        CityInformationAgent agent = new CityInformationAgent();
        JSONObject requestParams = new JSONObject();

        // set lazyload to true for mocking purposes
        try {
            Field lazyload = agent.getClass().getDeclaredField("lazyload");
            lazyload.setAccessible(true);
            lazyload.set(agent, true);
        } catch (Exception e) {
            fail();
        }

        // test case when request parameters are empty, validateInput should throw BadRequestException
        try {
            agent.processRequestParameters(requestParams);
        } catch (Exception e) {
            assertTrue(e instanceof BadRequestException);
        }

        // tests for sending iris to agent
        // test case when iris are empty, should return empty cityObjectInformation json array
        requestParams.put(CityInformationAgent.KEY_REQ_METHOD, HttpMethod.POST);
        JSONArray iris = new JSONArray();
        requestParams.put(CityInformationAgent.KEY_IRIS, iris);
        try {
            assertTrue(agent.processRequestParameters(requestParams).getJSONArray(CityInformationAgent.KEY_CITY_OBJECT_INFORMATION).getJSONArray(0).isEmpty());
        } catch (Exception e) {
            fail();
        } finally {
            requestParams.remove(CityInformationAgent.KEY_CITY_OBJECT_INFORMATION);
        }

        // test case when route is not overridden
        iris.put("http://www.theworldavatar.com:83/citieskg/namespace/example/sparql/cityobject/UUID_1/");
        try (MockedConstruction<ModelContext> modelContext = Mockito.mockConstruction(ModelContext.class, (mock, context) -> {
            when(mock.createHollowModel(any(Class.class), anyString())).thenReturn(new CityObject());
            doNothing().when(mock).pullAll(any(CityObject.class));
        })) {
            Field route = agent.getClass().getDeclaredField("route");
            route.setAccessible(true);
            agent.processRequestParameters(requestParams);
            assertEquals(ResourceBundle.getBundle("config").getString("uri.route"), route.get(agent));
        } catch (Exception e) {
            fail();
        } finally {
            requestParams.remove(CityInformationAgent.KEY_CITY_OBJECT_INFORMATION);
        }

        // test case when route is overridden
        iris.remove(0);
        iris.put("http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG24500/sparql/cityobject/UUID_1/");
        try (MockedConstruction<ModelContext> modelContext = Mockito.mockConstruction(ModelContext.class, (mock, context) -> {
            when(mock.createHollowModel(any(Class.class), anyString())).thenReturn(new CityObject());
            doNothing().when(mock).pullAll(ArgumentMatchers.any(CityObject.class));
        })) {
            Field route = agent.getClass().getDeclaredField("route");
            route.setAccessible(true);
            agent.processRequestParameters(requestParams);
            assertEquals("singaporeEPSG24500", route.get(agent));
        } catch (Exception e) {
            fail();
        } finally {
            requestParams.remove(CityInformationAgent.KEY_CITY_OBJECT_INFORMATION);
        }

        // test case when lazyload is true
        try (MockedConstruction<ModelContext> modelContext = Mockito.mockConstruction(ModelContext.class, (mock, context) -> {
            when(mock.createHollowModel(any(Class.class), anyString())).thenReturn(new CityObject());
            Mockito.doNothing().when(mock).pullAll(ArgumentMatchers.any(CityObject.class));
        })) {
            JSONObject response = agent.processRequestParameters(requestParams);
            verify(modelContext.constructed().get(0), times(1)).pullAll(any(CityObject.class));
            assertEquals(1, response.getJSONArray(CityInformationAgent.KEY_CITY_OBJECT_INFORMATION).getJSONArray(0).getJSONArray(0).length());
        } catch (Exception e) {
            fail();
        } finally {
            requestParams.remove(CityInformationAgent.KEY_CITY_OBJECT_INFORMATION);
        }

        // test case when lazyload is false
        try (MockedConstruction<ModelContext> modelContext = Mockito.mockConstruction(ModelContext.class, (mock, context) -> {
            when(mock.createHollowModel(any(Class.class), anyString())).thenReturn(new CityObject());
            Mockito.doNothing().when(mock).recursivePullAll(any(CityObject.class), anyInt());
        })) {
            Field lazyload = agent.getClass().getDeclaredField("lazyload");
            lazyload.setAccessible(true);
            lazyload.set(agent, false);
            JSONObject response =  agent.processRequestParameters(requestParams);
            verify(modelContext.constructed().get(0), times(1)).recursivePullAll(any(CityObject.class), anyInt());
            assertEquals(1, response.getJSONArray(CityInformationAgent.KEY_CITY_OBJECT_INFORMATION).getJSONArray(0).getJSONArray(0).length());
        } catch (Exception e) {
            fail();
        } finally {
            requestParams.remove(CityInformationAgent.KEY_CITY_OBJECT_INFORMATION);
        }

        // tests for sending context to agent
        // test case when agent iri is not valid, should return BadRequestException
        JSONObject otherAgents = new JSONObject();
        HashMap<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        otherAgents.put("testAgentIri", map);
        requestParams.put(CityInformationAgent.KEY_CONTEXT, otherAgents);
        try {
            agent.processRequestParameters(requestParams);
        } catch (Exception e) {
            assertTrue(e instanceof BadRequestException);
        }

        // test case when agent successfully sends request to another agent
        otherAgents.remove("testAgentIri");
        otherAgents.put("http://www.theworldavatar.com:83/citieskg/otheragentIRI", map);
        try (MockedConstruction<ModelContext> modelContext = Mockito.mockConstruction(ModelContext.class, (mock, context) -> {
            when(mock.createHollowModel(any(Class.class), anyString())).thenReturn(new CityObject());
            Mockito.doNothing().when(mock).recursivePullAll(any(CityObject.class), anyInt());
        })) {
            try (MockedStatic<Http> http = Mockito.mockStatic(Http.class, CALLS_REAL_METHODS)) {
                http.when(() -> Http.execute(ArgumentMatchers.any(HttpPost.class))).thenReturn("{otherAgentKey: result}");
                JSONObject response = agent.processRequestParameters(requestParams);
                assertEquals("result", response
                        .getJSONArray("http://www.theworldavatar.com:83/citieskg/otheragentIRI")
                        .getJSONObject(0).get("otherAgentKey"));
                assertEquals(1, response.getJSONArray(CityInformationAgent.KEY_CITY_OBJECT_INFORMATION).getJSONArray(0).getJSONArray(0).length());
            }
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testValidateInput() {
        CityInformationAgent agent = new CityInformationAgent();
        JSONObject requestParams = new JSONObject();

        // test case where request params is empty
        try {
            agent.validateInput(requestParams);
        } catch (Exception e) {
            assertEquals(BadRequestException.class, e.getClass());
        }

        // test case where keys have KEY_REQ_METHOD but no KEY_IRIS
        requestParams.put(CityInformationAgent.KEY_REQ_METHOD, HttpMethod.GET);
        try {
            agent.validateInput(requestParams);
        } catch (Exception e) {
            assertEquals(BadRequestException.class, e.getClass());
        }

        // test case where KEY_REQ_METHOD is not POST
        JSONArray iris = new JSONArray();
        iris.put(0, "test");
        requestParams.put(CityInformationAgent.KEY_IRIS, iris);
        try {
            agent.validateInput(requestParams);
        } catch (Exception e) {
            assertEquals(BadRequestException.class, e.getClass());
        }

        // test case where iri is not a valid URL
        requestParams.put(CityInformationAgent.KEY_REQ_METHOD, HttpMethod.POST);
        try {
            agent.validateInput(requestParams);
        } catch (Exception e) {
            assertEquals(BadRequestException.class, e.getClass());
        }

        // test case where request params with iris pass
        iris.remove(0);
        iris.put(0, "http://localhost:9999/blazegraph/namespace/berlin/sparql/cityobject/UUID_123/");
        requestParams.put(CityInformationAgent.KEY_IRIS, iris);
        try {
            assertTrue(agent.validateInput(requestParams));
        } catch (Exception e) {
            fail();
        }

        // test case where agent iri is not a valid URL
        JSONObject agentIri = new JSONObject();
        agentIri.put("test", "agent");
        requestParams.put(CityInformationAgent.KEY_CONTEXT, agentIri);
        try {
            agent.validateInput(requestParams);
        } catch (Exception e) {
            assertEquals(BadRequestException.class, e.getClass());
        }

        // test case where request params with agent pass
        agentIri.remove("test");
        agentIri.put("http://localhost:9999/agent", "agent");
        try {
            assertTrue(agent.validateInput(requestParams));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testReadConfig() {
        // this test is deliberately left blank
        // method is already tested in testNewCityInformationAgentFields
    }
}
