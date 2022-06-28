package uk.ac.cam.cares.twa.cities.agents.geo.test;

import java.lang.reflect.Field;
import org.checkerframework.checker.units.qual.C;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cares.twa.cities.agents.geo.CityExportAgent;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import uk.ac.cam.cares.twa.cities.agents.geo.CityImportAgent;

import static org.junit.jupiter.api.Assertions.*;


public class CityExportAgentTest {

    public String[] data = {"abc", "def"};
    public JSONArray testGmlIds = new JSONArray(data);

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
    public void testNewCityExportAgentFields() {
        CityExportAgent agent = new CityExportAgent();

        assertEquals(11, agent.getClass().getDeclaredFields().length);

        Field URI_ACTION;
        Field KEY_GMLID;
        Field KEY_REQ_URL;
        Field KEY_REQ_METHOD;
        Field KEY_NAMESPACE;
        Field KEY_DISPLAYFORM;
        Field KEY_LOD;
        Field outFileName;
        Field outFileExtension;
        Field tmpDirsLocation;
        Field NUM_EXPORTER_THREADS;

        try {
            URI_ACTION = agent.getClass().getDeclaredField("URI_ACTION");
            assertEquals(URI_ACTION.get(agent), "/export/kml");
            KEY_GMLID = agent.getClass().getDeclaredField("KEY_GMLID");
            assertEquals(KEY_GMLID.get(agent), "gmlid");
            KEY_REQ_URL = agent.getClass().getDeclaredField("KEY_REQ_URL");
            assertEquals(KEY_REQ_URL.get(agent), "requestUrl");
            KEY_REQ_METHOD = agent.getClass().getDeclaredField("KEY_REQ_METHOD");
            assertEquals(KEY_REQ_METHOD.get(agent), "method");
            KEY_NAMESPACE = agent.getClass().getDeclaredField("KEY_NAMESPACE");
            assertEquals(KEY_NAMESPACE.get(agent), "namespace");
            KEY_DISPLAYFORM = agent.getClass().getDeclaredField("KEY_DISPLAYFORM");
            assertEquals(KEY_DISPLAYFORM.get(agent), "displayform");
            KEY_LOD = agent.getClass().getDeclaredField("KEY_LOD");
            assertEquals(KEY_LOD.get(agent), "lod");

            outFileName = agent.getClass().getDeclaredField("outFileName");
            assertEquals(outFileName.get(agent), "test");
            outFileExtension = agent.getClass().getDeclaredField("outFileExtension");
            assertEquals(outFileExtension.get(agent), ".kml");
            tmpDirsLocation = agent.getClass().getDeclaredField("tmpDirsLocation");
            assertEquals(tmpDirsLocation.get(agent), System.getProperty("java.io.tmpdir"));
            NUM_EXPORTER_THREADS = agent.getClass().getDeclaredField("NUM_EXPORTER_THREADS");
            assertEquals(NUM_EXPORTER_THREADS.get(agent), 1);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testNewCityExportAgentMethods() {
        CityExportAgent agent = new CityExportAgent();
        assertEquals(11, agent.getClass().getDeclaredMethods().length);
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

        requestParams.put(CityExportAgent.KEY_NAMESPACE, localhostURL);
        requestParams.put(CityExportAgent.KEY_LOD, 2);
        requestParams.put(CityExportAgent.KEY_DISPLAYFORM, "extruded");

        try {
            assertTrue((Boolean) validateInput.invoke(agent, requestParams));
        } catch (Exception e) {
            fail();
        }

        // For illegal arguments
        requestParams.put(CityExportAgent.KEY_LOD, 100);
        requestParams.put(CityExportAgent.KEY_DISPLAYFORM, "xxx");

        try {
            assertFalse((Boolean) validateInput.invoke(agent, requestParams));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testGetGmlidFromFile(){

    }

    @Test
    public void testGetCrsInfo(){

    }

    @Test
    public void testGetOutputName(){

    }

    @Test
    public void testGetServerInfo(){

    }

    @Test
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


    @Test
    public void testTilingKML(){

    }
}