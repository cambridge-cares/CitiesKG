package uk.ac.cam.cares.twa.cities.agents.geo.test;

import junit.framework.TestCase;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.aws.AsynchronousWatcherService;
import uk.ac.cam.cares.twa.cities.agents.geo.CityImportAgent;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class CityImportAgentTest extends TestCase {

    public void testNewCityImportAgent() {
        CityImportAgent agent;

        try {
            agent = new CityImportAgent();
            assertNotNull(agent);
        }  catch (Exception e) {
            fail();
        }

    }

    public void testNewCityImportAgentFields() {
        CityImportAgent agent = new CityImportAgent();

        assertEquals(19, agent.getClass().getDeclaredFields().length);

        Field URI_LISTEN;
        Field  URI_ACTION;
        Field KEY_REQ_METHOD;
        Field KEY_REQ_URL;
        Field KEY_DIRECTORY;
        Field KEY_SPLIT;
        Field KEY_TARGET_URL;
        Field FS;
        Field CHUNK_SIZE;
        Field NUM_SERVER_THREADS;
        Field NUM_IMPORTER_THREADS;
        Field requestUrl;
        Field targetUrl;
        Field importDir;
        Field splitDir;
        Field serverExecutor;
        Field importerExecutor;
        Field nqExportExecutor;
        Field nqUploadExecutor;

        try {
            URI_LISTEN = agent.getClass().getDeclaredField("URI_LISTEN");
            assertEquals(URI_LISTEN.get(agent),"/import/source");
            URI_ACTION = agent.getClass().getDeclaredField("URI_ACTION");
            assertEquals(URI_ACTION.get(agent),"/import/citygml");
            KEY_REQ_METHOD = agent.getClass().getDeclaredField("KEY_REQ_METHOD");
            assertEquals(KEY_REQ_METHOD.get(agent),"method");
            KEY_REQ_URL = agent.getClass().getDeclaredField("KEY_REQ_URL");
            assertEquals(KEY_REQ_URL.get(agent),"requestUrl");
            KEY_DIRECTORY = agent.getClass().getDeclaredField("KEY_DIRECTORY");
            assertEquals(KEY_DIRECTORY.get(agent),"directory");
            KEY_SPLIT = agent.getClass().getDeclaredField("KEY_SPLIT");
            assertEquals(KEY_SPLIT.get(agent),"split");
            KEY_TARGET_URL = agent.getClass().getDeclaredField("KEY_TARGET_URL");
            assertEquals(KEY_TARGET_URL.get(agent),"targetURL");
            CHUNK_SIZE = agent.getClass().getDeclaredField("CHUNK_SIZE");
            assertEquals(CHUNK_SIZE.get(agent),100);
            NUM_SERVER_THREADS = agent.getClass().getDeclaredField("NUM_SERVER_THREADS");
            assertEquals(NUM_SERVER_THREADS.get(agent),2);
            NUM_IMPORTER_THREADS = agent.getClass().getDeclaredField("NUM_IMPORTER_THREADS");
            assertEquals(NUM_IMPORTER_THREADS.get(agent),1);
            FS = agent.getClass().getDeclaredField("FS");
            FS.setAccessible(true);
            assertEquals(FS.get(agent),System.getProperty("file.separator"));
            requestUrl = agent.getClass().getDeclaredField("requestUrl");
            requestUrl.setAccessible(true);
            assertNull(requestUrl.get(agent));
            targetUrl = agent.getClass().getDeclaredField("targetUrl");
            targetUrl.setAccessible(true);
            assertNull(targetUrl.get(agent));
            importDir = agent.getClass().getDeclaredField("importDir");
            importDir.setAccessible(true);
            assertNull(importDir.get(agent));
            splitDir = agent.getClass().getDeclaredField("splitDir");
            splitDir.setAccessible(true);
            assertNull(splitDir.get(agent));
            serverExecutor = agent.getClass().getDeclaredField("serverExecutor");
            serverExecutor.setAccessible(true);
            assertFalse(((ExecutorService)serverExecutor.get(agent)).isTerminated());
            importerExecutor = agent.getClass().getDeclaredField("importerExecutor");
            importerExecutor.setAccessible(true);
            assertFalse(((ExecutorService)importerExecutor.get(agent)).isTerminated());
            nqExportExecutor = agent.getClass().getDeclaredField("nqExportExecutor");
            nqExportExecutor.setAccessible(true);
            assertFalse(((ExecutorService)nqExportExecutor.get(agent)).isTerminated());
            nqUploadExecutor = agent.getClass().getDeclaredField("nqUploadExecutor");
            nqUploadExecutor.setAccessible(true);
            assertFalse(((ExecutorService)nqUploadExecutor.get(agent)).isTerminated());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    public void testNewCityImportAgentMethods() {
        CityImportAgent agent = new CityImportAgent();
        assertEquals(14, agent.getClass().getDeclaredMethods().length);
    }

    public void testValidateListenInput()  {
        CityImportAgent agent  = new CityImportAgent();
        Method validateListenInput = null;


        try {
            validateListenInput = agent.getClass().getDeclaredMethod(
                    "validateListenInput" , JSONObject.class, Set.class);
            validateListenInput.setAccessible(true);
        } catch (Exception e) {
           fail();
        }


        JSONObject requestParams = new JSONObject();
        Set<String> keys = new HashSet<>();

        try {
            assertTrue((Boolean) validateListenInput.invoke(agent, requestParams, keys));

            keys.add(CityImportAgent.KEY_DIRECTORY);
            requestParams.put(CityImportAgent.KEY_DIRECTORY,
                    System.getProperty("java.io.tmpdir") +
                            CityImportAgent.URI_LISTEN);

            assertFalse((Boolean) validateListenInput.invoke(agent, requestParams, keys));
        } catch (Exception e) {
            fail();
        }

    }

    public void testValidateActionInput()  {
        CityImportAgent agent  = new CityImportAgent();
        Method validateListenInput = null;

        try {
            validateListenInput = agent.getClass().getDeclaredMethod(
                    "validateActionInput", JSONObject.class, Set.class);
            validateListenInput.setAccessible(true);
        } catch (NoSuchMethodException e) {
            fail();
        }


        JSONObject requestParams = new JSONObject();
        Set<String> keys = new HashSet<>();

        try {
            assertTrue((Boolean) validateListenInput.invoke(agent, requestParams, keys));

            keys.add(AsynchronousWatcherService.KEY_WATCH);
            requestParams.put(AsynchronousWatcherService.KEY_WATCH,
                    System.getProperty("java.io.tmpdir"));

            assertFalse((Boolean) validateListenInput.invoke(agent, requestParams, keys));
        } catch (Exception e) {
            fail();
        }
    }

    public void testValidateInput()  {
        CityImportAgent agent  = new CityImportAgent();
        Method validateInput = null;

        try {
            validateInput = agent.getClass().getDeclaredMethod("validateInput", JSONObject.class);
        } catch (Exception e) {
            fail();
        }

        JSONObject requestParams = new JSONObject();
        Set<String> keys = new HashSet<>();
        String localhostURL = "http://localhost";

        //General keys and values check

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        keys.add(CityImportAgent.KEY_REQ_METHOD);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        keys.add(CityImportAgent.KEY_REQ_URL);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        keys.add(CityImportAgent.KEY_TARGET_URL);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityImportAgent.KEY_REQ_METHOD, HttpMethod.GET);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityImportAgent.KEY_REQ_METHOD, HttpMethod.POST);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityImportAgent.KEY_REQ_URL, "abc");

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityImportAgent.KEY_TARGET_URL, "abc");

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityImportAgent.KEY_TARGET_URL, localhostURL);

        //Listen case:

        requestParams.put(CityImportAgent.KEY_REQ_URL, CityImportAgent.URI_LISTEN);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityImportAgent.KEY_REQ_URL, localhostURL + CityImportAgent.URI_LISTEN);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityImportAgent.KEY_DIRECTORY, "");

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityImportAgent.KEY_DIRECTORY, System.getProperty("java.io.tmpdir"));

        try {
            assertTrue((Boolean) validateInput.invoke(agent, requestParams));
        } catch (Exception e) {
            fail();
        }

        //Action case:

        requestParams.put(CityImportAgent.KEY_REQ_URL, CityImportAgent.URI_ACTION);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityImportAgent.KEY_REQ_URL, localhostURL + CityImportAgent.URI_ACTION);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(AsynchronousWatcherService.KEY_WATCH, "");

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(AsynchronousWatcherService.KEY_WATCH, System.getProperty("java.io.tmpdir"));

        try {
            assertTrue((Boolean) validateInput.invoke(agent, requestParams));
        } catch (Exception e) {
            fail();
        }

    }

    public void testListenToImport() {
        CityImportAgent agent  = new CityImportAgent();
        Method listenToImport = null;
        String listenDir = System.getProperty("java.io.tmpdir") + "import";

        try {
            listenToImport = agent.getClass().getDeclaredMethod("listenToImport", String.class);
            listenToImport.setAccessible(true);
            Field requestUrl = agent.getClass().getDeclaredField("requestUrl");
            requestUrl.setAccessible(true);
            requestUrl.set(agent, CityImportAgent.URI_LISTEN);
        } catch (Exception e) {
            fail();
        }

        try {
            assertEquals(((File) listenToImport.invoke(agent, listenDir)).getAbsolutePath(), listenDir);
        } catch (Exception e) {
            fail();
        }

        // watching directory already tested in CreateFileWatcherTest
    }

    public void testImportFiles() {
        //@Todo: implementation
    }

    public void testSplitFile() {
        //@Todo: implementation
    }

    public void testImportChunk() {
        //@Todo: implementation
    }

    public void testStartBlazegraphInstance() {
        //@Todo: implementation
    }

    public void testImportToLocalBlazegraphInstance() {
        //@Todo: implementation
    }

    public void testExportToNquads() {
        //@Todo: implementation
    }

    public void testUploadNQuadsFileToBlazegraphInstance() {
        //@Todo: implementation
    }

    public void testWriteErrorLog() {
        //@Todo: implementation
    }

    public void testArchiveImportFiles() {
        //@Todo: implementation
    }


}