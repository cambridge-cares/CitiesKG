package uk.ac.cam.cares.twa.cities.agents.geo.test;

import java.lang.reflect.Field;
import java.nio.file.Path;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.agents.geo.CityExportAgent;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import uk.ac.cam.cares.twa.cities.tasks.KMLTilingTask;
import uk.ac.cam.cares.twa.cities.tasks.geo.ExporterTask;

import static org.junit.jupiter.api.Assertions.*;


public class CityExportAgentTest {

    public String[] data = {"abc", "def"};
    public JSONArray testGmlIds = new JSONArray(data);

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

        assertEquals(20, agent.getClass().getDeclaredFields().length);

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
            outFileName.setAccessible(true);
            assertEquals(outFileName.get(agent), "test");
            outFileExtension = agent.getClass().getDeclaredField("outFileExtension");
            outFileExtension.setAccessible(true);
            assertEquals(outFileExtension.get(agent), ".kml");
            tmpDirsLocation = agent.getClass().getDeclaredField("tmpDirsLocation");
            tmpDirsLocation.setAccessible(true);
            assertEquals(tmpDirsLocation.get(agent), System.getProperty("java.io.tmpdir"));
            NUM_EXPORTER_THREADS = agent.getClass().getDeclaredField("NUM_EXPORTER_THREADS");
            NUM_EXPORTER_THREADS.setAccessible(true);
            assertEquals(NUM_EXPORTER_THREADS.get(agent), 1);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testNewCityExportAgentMethods() {
        CityExportAgent agent = new CityExportAgent();
        assertEquals(10, agent.getClass().getDeclaredMethods().length);
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
        String localhostURL = "http://localhost:8080/agents/";

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
            assertFalse((Boolean) validateInput.invoke(agent, requestParams));
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(BadRequestException.class, ((InvocationTargetException) e).getTargetException().getClass());
        }

        requestParams.put(CityExportAgent.KEY_NAMESPACE, localhostURL);
        requestParams.put(CityExportAgent.KEY_LOD, 2);
        requestParams.put(CityExportAgent.KEY_DISPLAYFORM, "extruded");

        try {
            assertTrue((Boolean) validateInput.invoke(agent, requestParams));
        } catch (Exception e) {
            fail();
        }

    }

    @Test
    public void testGetGmlidFromFile(){
        String namespace = "berlin";
        File gmlidF = new File(System.getProperty("java.io.tmpdir") + namespace);
        Path gmlidFile = gmlidF.toPath();
        CityExportAgent agent = new CityExportAgent();

        Method getGmlidFromFile = null;

        try {
            getGmlidFromFile = agent.getClass().getDeclaredMethod("getGmlidFromFile", Path.class);
            getGmlidFromFile.setAccessible(true);
        } catch (NoSuchMethodException e) {
            fail();
        }

        if (gmlidF.exists()){
            try {
                getGmlidFromFile.invoke(agent, gmlidFile);
            } catch (InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

    }

    @Test
    public void testGetCrsInfo(){
        CityExportAgent agent = new CityExportAgent();
        Method getCrsInfo = null;

        try {
            getCrsInfo = agent.getClass().getDeclaredMethod("getCrsInfo", String.class);
            getCrsInfo.setAccessible(true);
        } catch (NoSuchMethodException e) {
            fail();
        }

        // Test with correct namespace
        String testNamespace = "http://localhost";
        try {
            getCrsInfo.invoke(agent, testNamespace);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), JPSRuntimeException.class);
        }

        try {
            HttpResponse<?> response = Mockito.mock(HttpResponse.class);
            try (MockedStatic<Unirest> unirest = Mockito.mockStatic(Unirest.class, Mockito.RETURNS_MOCKS)) {
                unirest.when(() -> Unirest.post(ArgumentMatchers.anyString())
                        .header(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
                        .body(ArgumentMatchers.anyString())
                        .socketTimeout(ArgumentMatchers.anyInt())
                        .asEmpty())
                    .thenReturn(response);
                getCrsInfo.invoke(agent, testNamespace);
            } catch (Exception e) {
                assert e instanceof InvocationTargetException;
                assertEquals(((InvocationTargetException) e).getTargetException().getClass(), JPSRuntimeException.class);
                assertEquals(((InvocationTargetException) e).getTargetException().getCause().getMessage(), "http://localhost 0");
            }
        } catch (Exception e) {
            fail();
        }

    }

    @Test
    public void testGetOutputName(){
        Path inputFile = new File(System.getProperty("java.io.tmpdir") + "chunk_0.txt").toPath();
        Field outputDir;
        Field outFileName;
        Field outFileExtension;
        Method getOutputName = null;
        CityExportAgent agent = new CityExportAgent();

        try {
            outputDir = agent.getClass().getDeclaredField("outputDir");
            outputDir.setAccessible(true);
            outputDir.set(agent,System.getProperty("java.io.tmpdir") );
            assertEquals(outputDir.get(agent), System.getProperty("java.io.tmpdir"));

            outFileName = agent.getClass().getDeclaredField("outFileName");
            outFileName.setAccessible(true);
            assertEquals(outFileName.get(agent), "test");

            outFileExtension = agent.getClass().getDeclaredField("outFileExtension");
            outFileExtension.setAccessible(true);
            assertEquals(outFileExtension.get(agent), ".kml");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        try {
            getOutputName = agent.getClass().getDeclaredMethod("getOutputName", Path.class);
            getOutputName.setAccessible(true);
        } catch (NoSuchMethodException e) {
            fail();
        }

        try {
            String outputName = (String) getOutputName.invoke(agent, inputFile);
            String actualOutput = System.getProperty("java.io.tmpdir") + "test" + "_0.kml";
            assertEquals(outputName, actualOutput);
        } catch (Exception e) {
            fail();
        }

    }

    @Test
    public void testGetServerInfo(){
        String namespaceIri = "http://www.theworldavatar.com:83/citieskg/namespace/berlin/sparql/";
        CityExportAgent agent = new CityExportAgent();

        Method getServerInfo;
        try {
            getServerInfo = agent.getClass().getDeclaredMethod("getServerInfo", String.class);
            getServerInfo.setAccessible(true);
            JSONObject serverInfo = (JSONObject) getServerInfo.invoke(agent, namespaceIri);
            assertEquals(serverInfo.get("port"), "83");
            assertEquals(serverInfo.get("host"), "www.theworldavatar.com");
            assertEquals(serverInfo.get("namespace"), "/citieskg/namespace/berlin/sparql/");
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testExportKml() {
        CityExportAgent agent = new CityExportAgent();
        Method exportKml;
        Method getServerInfo;
        ExporterTask task = null;
        // Prepare the testTaskParams
        String namespaceIri = "http://www.theworldavatar.com:83/citieskg/namespace/testdata/sparql/";
        JSONObject serverInfo = null;
        try {
            getServerInfo = agent.getClass().getDeclaredMethod("getServerInfo", String.class);
            getServerInfo.setAccessible(true);
            serverInfo = (JSONObject) getServerInfo.invoke(agent, namespaceIri);
        } catch (NoSuchMethodException | InvocationTargetException |IllegalAccessException e) {
            e.printStackTrace();
            fail();
        }

        int srid = 4326;
        String outputDir = Paths.get(System.getProperty("java.io.tmpdir"), "export").toString();
        String outputPath = Paths.get(outputDir, "test.kml").toString();
        String[] displayMode = {"false","true", "false", "false"}; // extruded
        int lod = 2;
        String[] gmlIds = data;

        CityExportAgent.Params testTaskParams = new CityExportAgent.Params(namespaceIri, serverInfo, srid, outputDir, outputPath, displayMode, lod, gmlIds);
        try {
            exportKml = agent.getClass().getDeclaredMethod("exportKml", CityExportAgent.Params.class);
            exportKml.setAccessible(true);
            task = (ExporterTask) exportKml.invoke(agent, testTaskParams);
            assertTrue(task.isRunning());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            fail();
        }finally {
            assert task != null;
            task.stop();
        }

    }

    @Test
    public void testTilingKML(){
        CityExportAgent agent = new CityExportAgent();
        Field outputDir;
        Method tilingKml ;
        String outputFolder = Paths.get(System.getProperty("java.io.tmpdir"), "export").toString();
        KMLTilingTask task = null;
        try{
            outputDir = agent.getClass().getDeclaredField("outputDir");
            outputDir.setAccessible(true);
            assertNull(outputDir.get(agent));
            outputDir.set(agent, outputFolder);
            assertEquals(outputDir.get(agent), outputFolder);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            fail();
        }


        Field inputDisplayForm;
        Field namespaceIri;

        String testNamespace = "http://www.theworldavatar.com:83/citieskg/namespace/testdata/sparql/";
        String testDisplayForm = "extruded";
        try{
            namespaceIri = agent.getClass().getDeclaredField("namespaceIri");
            namespaceIri.setAccessible(true);
            namespaceIri.set(agent, testNamespace);

            inputDisplayForm = agent.getClass().getDeclaredField("inputDisplayForm");
            inputDisplayForm.setAccessible(true);
            inputDisplayForm.set(agent, testDisplayForm);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        try {
            tilingKml = agent.getClass().getDeclaredMethod("tilingKml");
            tilingKml.setAccessible(true);
            task = (KMLTilingTask) tilingKml.invoke(agent);
            assertTrue(task.isRunning());
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException E) {
            fail();
        } finally {
            assert task != null;
            task.stop();
        }
    }
}