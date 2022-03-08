package uk.ac.cam.cares.twa.cities.agents.geo.test;

import junit.framework.TestCase;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.twa.cities.agents.geo.CityExportAgent;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;


public class CityExportAgentTest extends TestCase {

    public String[] data = {"abc", "def"};
    public JSONArray testGmlIds = new JSONArray(data);

    public String outFileName = "/test.kml";
    public String outTmpDir = "java.io.tmpdir";


    public void testNewCityExportAgent() {
        CityExportAgent agent;
        try {
            agent = new CityExportAgent();
            assertNotNull(agent);
        } catch (Exception e) {
            fail();
        }
    }


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
            assert e instanceof InvocationTargetException;
            assertEquals(BadRequestException.class, ((InvocationTargetException) e).getTargetException().getClass());
        }

        // Add private attributes of the CityExportAgent class
        keys.add(CityExportAgent.URI_ACTION);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(BadRequestException.class, ((InvocationTargetException) e).getTargetException().getClass());
        }

        keys.add(CityExportAgent.KEY_GMLID);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(BadRequestException.class, ((InvocationTargetException) e).getTargetException().getClass());
        }

        keys.add(CityExportAgent.KEY_REQ_METHOD);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(BadRequestException.class, ((InvocationTargetException) e).getTargetException().getClass());
        }

        keys.add(CityExportAgent.KEY_REQ_URL);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(BadRequestException.class, ((InvocationTargetException) e).getTargetException().getClass());
        }

        // Create the HTTP request with a json body by the mean of the added keys
        requestParams.put(CityExportAgent.KEY_REQ_METHOD, HttpMethod.GET);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(BadRequestException.class, ((InvocationTargetException) e).getTargetException().getClass());
        }

        requestParams.put(CityExportAgent.KEY_REQ_URL, localhostURL);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(BadRequestException.class, ((InvocationTargetException) e).getTargetException().getClass());
        }

        //Action case:
        requestParams.put(CityExportAgent.KEY_REQ_METHOD, HttpMethod.POST);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(BadRequestException.class, ((InvocationTargetException) e).getTargetException().getClass());
        }

        requestParams.put(CityExportAgent.KEY_REQ_URL, CityExportAgent.URI_ACTION);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(BadRequestException.class, ((InvocationTargetException) e).getTargetException().getClass());
        }

        requestParams.put(CityExportAgent.KEY_REQ_URL, localhostURL + CityExportAgent.URI_ACTION);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(BadRequestException.class, ((InvocationTargetException) e).getTargetException().getClass());
        }

        requestParams.put(CityExportAgent.KEY_GMLID, testGmlIds);

        try {
            assertTrue((Boolean) validateInput.invoke(agent, requestParams));
        } catch (Exception e) {
            fail();
        }
    }


    public void testExportKml() {
        CityExportAgent agent = new CityExportAgent();
        Method exportKml = null;
        try {
            exportKml = agent.getClass().getDeclaredMethod("exportKml", String[].class, String.class, JSONObject.class);
            exportKml.setAccessible(true);
        } catch (NoSuchMethodException e) {
            fail();
        }

        String[] gmlIds = data;
        JSONObject serverInfo = new JSONObject();

        File outputFile = new File(System.getProperty(outTmpDir) + outFileName);
        String outputPath = outputFile.getAbsolutePath();
        String actualPath = outputPath.replace(".kml", "_extruded.kml");

        try {
            assert exportKml != null;
            assertEquals(actualPath, exportKml.invoke(agent, gmlIds, outputPath, serverInfo));
        } catch (Exception e) {
            fail();
        }

    }

}