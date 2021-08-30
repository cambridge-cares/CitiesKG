package uk.ac.cam.cares.twa.cities.agents.geo.test;

import junit.framework.TestCase;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cares.twa.cities.agents.geo.CityExportAgent;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CityExportAgentTest extends TestCase {


    @Test
    public void testNewCityExportAgent() {
        CityExportAgent agent;
        try {
            agent = new CityExportAgent();
            assertNotNull(agent);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testValidateInput()  {
        CityExportAgent agent = new CityExportAgent();
        Method validateInput = null;

        try {
            validateInput = agent.getClass().getDeclaredMethod("validateInput", JSONObject.class);
        } catch (Exception e) {
            fail();
        }

        JSONObject requestParams = new JSONObject();
        Set<String> keys = new HashSet<>();
        String localhostURL = "http://localhost:8081/agents";

        // General keys and value check
        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        // Add private attributes of the CityExportAgent class
        keys.add(CityExportAgent.URI_ACTION);
        keys.add(CityExportAgent.KEY_GMLID);
        keys.add(CityExportAgent.KEY_REQ_METHOD);
        keys.add(CityExportAgent.KEY_REQ_URL);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        // Create the HTTP request with a json body by the mean of the added keys
        requestParams.put(CityExportAgent.KEY_REQ_METHOD, HttpMethod.POST);
        requestParams.put(CityExportAgent.KEY_REQ_METHOD, HttpMethod.GET);
        requestParams.put(CityExportAgent.KEY_REQ_URL, "abc");
        requestParams.put(CityExportAgent.KEY_REQ_URL, localhostURL);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        //Action case:
        requestParams.put(CityExportAgent.KEY_REQ_URL, CityExportAgent.URI_ACTION);
        requestParams.put(CityExportAgent.KEY_REQ_URL, localhostURL + CityExportAgent.URI_ACTION);

        try {
            assertTrue((Boolean) validateInput.invoke(agent, requestParams));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testexportKml() {}

}