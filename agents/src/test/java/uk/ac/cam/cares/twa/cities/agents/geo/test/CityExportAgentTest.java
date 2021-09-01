package uk.ac.cam.cares.twa.cities.agents.geo.test;

import junit.framework.TestCase;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cares.twa.cities.agents.geo.CityExportAgent;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Assertions.*;

class CityExportAgentTest extends TestCase {

    public String testgmlIds = "abc";
    public String outFileName = "/test.kml";
    public String outTmpDir = "java.io.tmpdir";

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
            validateInput.setAccessible(true);
        } catch (NoSuchMethodException e) {
            fail();
        }

        JSONObject requestParams = new JSONObject();
        Set<String> keys = new HashSet<>();
        String localhostURL = "http://localhost";

        // General keys and value check
        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        // Add private attributes of the CityExportAgent class
        keys.add(CityExportAgent.URI_ACTION);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        keys.add(CityExportAgent.KEY_GMLID);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        keys.add(CityExportAgent.KEY_REQ_METHOD);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        keys.add(CityExportAgent.KEY_REQ_URL);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        // Create the HTTP request with a json body by the mean of the added keys
        requestParams.put(CityExportAgent.KEY_REQ_METHOD, HttpMethod.GET);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityExportAgent.KEY_REQ_URL, localhostURL);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        //Action case:
        requestParams.put(CityExportAgent.KEY_REQ_METHOD, HttpMethod.POST);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityExportAgent.KEY_REQ_URL, CityExportAgent.URI_ACTION);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityExportAgent.KEY_REQ_URL, localhostURL + CityExportAgent.URI_ACTION);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityExportAgent.KEY_GMLID, testgmlIds);

        try {
            assertTrue((Boolean) validateInput.invoke(agent, requestParams));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testExportKml() {
        CityExportAgent agent = new CityExportAgent();
        Method exportKml = null;
        try {
            exportKml = agent.getClass().getDeclaredMethod("exportKml", String.class, String.class);
            exportKml.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        String gmlIds = testgmlIds;
        File outputFile = new File(System.getProperty(outTmpDir) + outFileName);
        String outputPath= outputFile.getAbsolutePath();

        try {
            assertEquals(exportKml.invoke(agent, gmlIds, outputPath), outputPath);
        } catch (Exception e) {
            fail();
        }

    }

}