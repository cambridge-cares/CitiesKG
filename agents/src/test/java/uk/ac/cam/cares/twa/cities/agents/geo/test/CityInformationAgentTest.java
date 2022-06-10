package uk.ac.cam.cares.twa.cities.agents.geo.test;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cares.twa.cities.agents.geo.CityInformationAgent;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;

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

        assertEquals(5, agent.getClass().getDeclaredMethods().length);
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

        requestParams.put(CityInformationAgent.KEY_REQ_METHOD, HttpMethod.GET);
        // test case where keys has KEY_REQ_METHOD but no KEY_IRIS
        try {
            agent.validateInput(requestParams);
        } catch (Exception e) {
            assertEquals(BadRequestException.class, e.getClass());
        }

        JSONArray iris = new JSONArray();
        iris.put(0, "test");
        requestParams.put(CityInformationAgent.KEY_IRIS, iris);
        // test case where KEY_REQ_METHOD is not POST
        try {
            agent.validateInput(requestParams);
        } catch (Exception e) {
            assertEquals(BadRequestException.class, e.getClass());
        }

        requestParams.put(CityInformationAgent.KEY_REQ_METHOD, HttpMethod.POST);
        // test case where iri is not a valid URL
        try {
            agent.validateInput(requestParams);
        } catch (Exception e) {
            assertEquals(BadRequestException.class, e.getClass());
        }

        iris.remove(0);
        iris.put(0, "http://localhost:9999/blazegraph/namespace/berlin/sparql/cityobject/UUID_123/");
        requestParams.put(CityInformationAgent.KEY_IRIS, iris);
        // test case where request params with iris pass
        try {
            assertTrue(agent.validateInput(requestParams));
        } catch (Exception e) {
            fail();
        }

        JSONObject agentIri = new JSONObject();
        agentIri.put("test", "agent");
        requestParams.put(CityInformationAgent.KEY_CONTEXT, agentIri);
        // test case where agent iri is not a valid URL
        try {
            agent.validateInput(requestParams);
        } catch (Exception e) {
            assertEquals(BadRequestException.class, e.getClass());
        }

        agentIri.remove("test");
        agentIri.put("http://localhost:9999/agent", "agent");
        // test case where request params with agent pass
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

    @Test
    public void testGetNamespace() {
        String uriString = "http://localhost:9999/blazegraph/namespace/berlin/sparql/cityobject/UUID_123/";
        CityInformationAgent agent = new CityInformationAgent();

        try {
            Method getNamespace = agent.getClass().getDeclaredMethod("getNamespace", String.class);
            getNamespace.setAccessible(true);

            assertEquals("http://localhost:9999/blazegraph/namespace/berlin/sparql", getNamespace.invoke(agent, uriString));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            fail();
        }
    }
}
