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
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        keys.add(CityImportAgent.KEY_REQ_METHOD);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        keys.add(CityImportAgent.KEY_REQ_URL);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        keys.add(CityImportAgent.KEY_TARGET_URL);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityImportAgent.KEY_REQ_METHOD, HttpMethod.GET);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityImportAgent.KEY_REQ_METHOD, HttpMethod.POST);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityImportAgent.KEY_REQ_URL, "abc");

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityImportAgent.KEY_TARGET_URL, "abc");

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityImportAgent.KEY_TARGET_URL, localhostURL);

        //Listen case:

        requestParams.put(CityImportAgent.KEY_REQ_URL, CityImportAgent.URI_LISTEN);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityImportAgent.KEY_REQ_URL, localhostURL + CityImportAgent.URI_LISTEN);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityImportAgent.KEY_DIRECTORY, "");

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
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
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(CityImportAgent.KEY_REQ_URL, localhostURL + CityImportAgent.URI_ACTION);

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), BadRequestException.class);
        }

        requestParams.put(AsynchronousWatcherService.KEY_WATCH, "");

        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
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

    public void testStopBlazegraphInstance() {
        //@Todo: implementation
    }

    public void testWriteErrorLog() {
        //@Todo: implementation
    }

    public void testExportToNquads() {
        //@Todo: implementation
    }

    public void testChangeUrlsInNQuadsFile() {
        //@Todo: implementation
    }

    public void testUploadNQuadsFileToBlazegraphInstance() {
        //@Todo: implementation
    }

    public void testArchiveImportFiles() {
        //@Todo: implementation
    }


}