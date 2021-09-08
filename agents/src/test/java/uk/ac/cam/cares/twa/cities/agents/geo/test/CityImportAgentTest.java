package uk.ac.cam.cares.twa.cities.agents.geo.test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import junit.framework.TestCase;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.aws.AsynchronousWatcherService;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.agents.geo.CityImportAgent;
import uk.ac.cam.cares.twa.cities.tasks.BlazegraphServerTask;
import uk.ac.cam.cares.twa.cities.tasks.ImporterTask;
import uk.ac.cam.cares.twa.cities.tasks.NquadsExporterTask;
import uk.ac.cam.cares.twa.cities.tasks.NquadsUploaderTask;
import uk.ac.cam.cares.twa.cities.tasks.test.NquadsExporterTaskTest;

public class CityImportAgentTest extends TestCase {

  public void testNewCityImportAgent() {
    CityImportAgent agent;

    try {
      agent = new CityImportAgent();
      assertNotNull(agent);
    } catch (Exception e) {
      fail();
    }

  }

  public void testNewCityImportAgentFields() {
    CityImportAgent agent = new CityImportAgent();

    assertEquals(20, agent.getClass().getDeclaredFields().length);

    Field URI_LISTEN;
    Field URI_ACTION;
    Field KEY_REQ_METHOD;
    Field KEY_REQ_URL;
    Field KEY_DIRECTORY;
    Field KEY_SPLIT;
    Field SPLIT_SCRIPT;
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
      assertEquals(URI_LISTEN.get(agent), "/import/source");
      URI_ACTION = agent.getClass().getDeclaredField("URI_ACTION");
      assertEquals(URI_ACTION.get(agent), "/import/citygml");
      KEY_REQ_METHOD = agent.getClass().getDeclaredField("KEY_REQ_METHOD");
      assertEquals(KEY_REQ_METHOD.get(agent), "method");
      KEY_REQ_URL = agent.getClass().getDeclaredField("KEY_REQ_URL");
      assertEquals(KEY_REQ_URL.get(agent), "requestUrl");
      KEY_DIRECTORY = agent.getClass().getDeclaredField("KEY_DIRECTORY");
      assertEquals(KEY_DIRECTORY.get(agent), "directory");
      KEY_SPLIT = agent.getClass().getDeclaredField("KEY_SPLIT");
      assertEquals(KEY_SPLIT.get(agent), "split");
      SPLIT_SCRIPT = agent.getClass().getDeclaredField("SPLIT_SCRIPT");
      SPLIT_SCRIPT.setAccessible(true);
      assertEquals(SPLIT_SCRIPT.get(agent), "citygml_splitter.py");
      KEY_TARGET_URL = agent.getClass().getDeclaredField("KEY_TARGET_URL");
      assertEquals(KEY_TARGET_URL.get(agent), "targetURL");
      CHUNK_SIZE = agent.getClass().getDeclaredField("CHUNK_SIZE");
      assertEquals(CHUNK_SIZE.get(agent), 50);
      NUM_SERVER_THREADS = agent.getClass().getDeclaredField("NUM_SERVER_THREADS");
      assertEquals(NUM_SERVER_THREADS.get(agent), 2);
      NUM_IMPORTER_THREADS = agent.getClass().getDeclaredField("NUM_IMPORTER_THREADS");
      assertEquals(NUM_IMPORTER_THREADS.get(agent), 1);
      FS = agent.getClass().getDeclaredField("FS");
      FS.setAccessible(true);
      assertEquals(FS.get(agent), System.getProperty("file.separator"));
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
      assertFalse(((ExecutorService) serverExecutor.get(agent)).isTerminated());
      importerExecutor = agent.getClass().getDeclaredField("importerExecutor");
      importerExecutor.setAccessible(true);
      assertFalse(((ExecutorService) importerExecutor.get(agent)).isTerminated());
      nqExportExecutor = agent.getClass().getDeclaredField("nqExportExecutor");
      nqExportExecutor.setAccessible(true);
      assertFalse(((ExecutorService) nqExportExecutor.get(agent)).isTerminated());
      nqUploadExecutor = agent.getClass().getDeclaredField("nqUploadExecutor");
      nqUploadExecutor.setAccessible(true);
      assertFalse(((ExecutorService) nqUploadExecutor.get(agent)).isTerminated());
    } catch (NoSuchFieldException | IllegalAccessException e) {
      fail();
    }
  }

  public void testNewCityImportAgentMethods() {
    CityImportAgent agent = new CityImportAgent();
    assertEquals(14, agent.getClass().getDeclaredMethods().length);
  }

  public void testValidateListenInput() {
    CityImportAgent agent = new CityImportAgent();
    Method validateListenInput = null;

    try {
      validateListenInput = agent.getClass().getDeclaredMethod(
          "validateListenInput", JSONObject.class, Set.class);
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

  public void testValidateActionInput() {
    CityImportAgent agent = new CityImportAgent();
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

  public void testValidateInput() {
    CityImportAgent agent = new CityImportAgent();
    Method validateInput = null;

    try {
      validateInput = agent.getClass().getDeclaredMethod("validateInput", JSONObject.class);
    } catch (Exception e) {
      fail();
    }

    JSONObject requestParams = new JSONObject();
    String localhostURL = "http://localhost";

    //General keys and values check

    try {
      validateInput.invoke(agent, requestParams);
    } catch (Exception e) {
      assert e instanceof InvocationTargetException;
      assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
          BadRequestException.class);
    }

    try {
      validateInput.invoke(agent, requestParams);
    } catch (Exception e) {
      assert e instanceof InvocationTargetException;
      assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
          BadRequestException.class);
    }

    try {
      validateInput.invoke(agent, requestParams);
    } catch (Exception e) {
      assert e instanceof InvocationTargetException;
      assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
          BadRequestException.class);
    }

    try {
      validateInput.invoke(agent, requestParams);
    } catch (Exception e) {
      assert e instanceof InvocationTargetException;
      assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
          BadRequestException.class);
    }

    requestParams.put(CityImportAgent.KEY_REQ_METHOD, HttpMethod.GET);

    try {
      validateInput.invoke(agent, requestParams);
    } catch (Exception e) {
      assert e instanceof InvocationTargetException;
      assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
          BadRequestException.class);
    }

    requestParams.put(CityImportAgent.KEY_REQ_METHOD, HttpMethod.POST);

    try {
      validateInput.invoke(agent, requestParams);
    } catch (Exception e) {
      assert e instanceof InvocationTargetException;
      assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
          BadRequestException.class);
    }

    requestParams.put(CityImportAgent.KEY_REQ_URL, "abc");

    try {
      validateInput.invoke(agent, requestParams);
    } catch (Exception e) {
      assert e instanceof InvocationTargetException;
      assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
          BadRequestException.class);
    }

    requestParams.put(CityImportAgent.KEY_TARGET_URL, "abc");

    try {
      validateInput.invoke(agent, requestParams);
    } catch (Exception e) {
      assert e instanceof InvocationTargetException;
      assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
          BadRequestException.class);
    }

    requestParams.put(CityImportAgent.KEY_TARGET_URL, localhostURL);

    //Listen case:

    requestParams.put(CityImportAgent.KEY_REQ_URL, CityImportAgent.URI_LISTEN);

    try {
      validateInput.invoke(agent, requestParams);
    } catch (Exception e) {
      assert e instanceof InvocationTargetException;
      assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
          BadRequestException.class);
    }

    requestParams.put(CityImportAgent.KEY_REQ_URL, localhostURL + CityImportAgent.URI_LISTEN);

    try {
      validateInput.invoke(agent, requestParams);
    } catch (Exception e) {
      assert e instanceof InvocationTargetException;
      assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
          BadRequestException.class);
    }

    requestParams.put(CityImportAgent.KEY_DIRECTORY, "");

    try {
      validateInput.invoke(agent, requestParams);
    } catch (Exception e) {
      assert e instanceof InvocationTargetException;
      assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
          BadRequestException.class);
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
      assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
          BadRequestException.class);
    }

    requestParams.put(CityImportAgent.KEY_REQ_URL, localhostURL + CityImportAgent.URI_ACTION);

    try {
      validateInput.invoke(agent, requestParams);
    } catch (Exception e) {
      assert e instanceof InvocationTargetException;
      assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
          BadRequestException.class);
    }

    requestParams.put(AsynchronousWatcherService.KEY_WATCH, "");

    try {
      validateInput.invoke(agent, requestParams);
    } catch (Exception e) {
      assert e instanceof InvocationTargetException;
      assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
          BadRequestException.class);
    }

    requestParams.put(AsynchronousWatcherService.KEY_WATCH, System.getProperty("java.io.tmpdir"));

    try {
      assertTrue((Boolean) validateInput.invoke(agent, requestParams));
    } catch (Exception e) {
      fail();
    }

  }

  public void testListenToImport() {
    CityImportAgent agent = new CityImportAgent();
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
    String fs = System.getProperty("file.separator");
    CityImportAgent agent = new CityImportAgent();
    Field targetUrl = null;
    Field importDir = null;
    Method importFiles = null;
    File testFile = new File(
        Objects.requireNonNull(this.getClass().getResource(fs + "test.gml")).getFile());
    File impD = new File(System.getProperty("java.io.tmpdir") + "imptstdir");
    File impF = new File(impD.getAbsolutePath() + fs + "test.gml");

    try {
      targetUrl = agent.getClass().getDeclaredField("targetUrl");
      targetUrl.setAccessible(true);
      assertNull(targetUrl.get(agent));
      importDir = agent.getClass().getDeclaredField("importDir");
      importDir.setAccessible(true);
      importFiles = agent.getClass().getDeclaredMethod("importFiles", File.class);
      importFiles.setAccessible(true);
    } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
      fail();
    }

    try {
      importDir.set(agent, impD);
      importFiles.invoke(agent, impD);
    } catch (IllegalAccessException | InvocationTargetException e) {
      if (e.getClass() == InvocationTargetException.class) {
        assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
            JPSRuntimeException.class);
      } else {
        fail();
      }
    }

    try {
      if (impD.mkdirs()) {
        importFiles.invoke(agent, impD);
      }
    } catch (InvocationTargetException | IllegalAccessException e) {
      if (e.getClass() == InvocationTargetException.class) {
        assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
            JPSRuntimeException.class);
      } else {
        fail();
      }
    } finally {
      NquadsExporterTaskTest.NquadsExporterTaskTestHelper.tearDown();
    }

    try {
      if (impD.mkdirs()) {
        Files.copy(testFile.toPath(), impF.toPath());
        importFiles.invoke(agent, impD);
      }
    } catch (InvocationTargetException | IllegalAccessException | IOException e) {
      if (e.getClass() == InvocationTargetException.class) {
        assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
            JPSRuntimeException.class);
      } else {
        fail();
      }
    } finally {
      NquadsExporterTaskTest.NquadsExporterTaskTestHelper.tearDown();
    }

    try {
      if (impD.mkdirs()) {
        Files.copy(testFile.toPath(), impF.toPath());
        targetUrl.set(agent, "test");
        importFiles.invoke(agent, impD);
      }
    } catch (InvocationTargetException | IllegalAccessException | IOException e) {
      if (e.getClass() == InvocationTargetException.class) {
        assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
            JPSRuntimeException.class);
      } else {
        fail();
      }
    } finally {
      NquadsExporterTaskTest.NquadsExporterTaskTestHelper.tearDown();
    }

    try {
      if (impD.mkdirs()) {
        Files.copy(testFile.toPath(), impF.toPath());
        targetUrl.set(agent, "http://localhost/test");
        assertEquals(importFiles.invoke(agent, impD), "file_part_1.gml \n");
      }
    } catch (InvocationTargetException | IllegalAccessException | IOException e) {
      fail();
    } finally {
      NquadsExporterTaskTest.NquadsExporterTaskTestHelper.tearDown();
    }

    //other import functionality already tested in the corresponding tasks' tests
  }

  public void testSplitFile() {
    String fs = System.getProperty("file.separator");
    CityImportAgent agent = new CityImportAgent();
    File testFile = new File(
        Objects.requireNonNull(this.getClass().getResource(fs + "test.gml")).getFile());
    File impF = new File(System.getProperty("java.io.tmpdir") + fs + "test.gml");
    Method splitFile;

    try {
      splitFile = agent.getClass().getDeclaredMethod("splitFile", File.class);
      splitFile.setAccessible(true);

      try {
        splitFile.invoke(agent, impF);
      } catch (IllegalAccessException | InvocationTargetException e) {
        if (e.getClass() == InvocationTargetException.class) {
          assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
              JPSRuntimeException.class);
        } else {
          fail();
        }
      }

      Files.copy(testFile.toPath(), impF.toPath());
      try {
        File splF = (File) ((ArrayList<?>) splitFile.invoke(agent, impF)).iterator().next();
        File splD = new File(splF.getParent());
        assertTrue(splF.exists());
        assertEquals(splF.getName(), "file_part_1.gml");
        assertTrue(splF.delete());
        assertTrue(new File(splD.getAbsolutePath() + fs + "test.gml").delete());
        assertTrue(splD.delete());
      } catch (IllegalAccessException | InvocationTargetException e) {
        fail();
      }

    } catch (NoSuchMethodException | IOException e) {
      fail();
    }

    //@Todo: implementation

  }

  public void testImportChunk() {
    String fs = System.getProperty("file.separator");
    CityImportAgent agent = new CityImportAgent();
    File testFile = new File(
        Objects.requireNonNull(this.getClass().getResource(fs + "test.gml")).getFile());
    File impD = new File(System.getProperty("java.io.tmpdir") + "imptstdir");
    File impF = new File(impD.getAbsolutePath() + fs + "test.gml");

    Field targetUrl = null;
    Method importChunk = null;

    try {
      targetUrl = agent.getClass().getDeclaredField("targetUrl");
      targetUrl.setAccessible(true);
      assertNull(targetUrl.get(agent));
      importChunk = agent.getClass().getDeclaredMethod("importChunk", File.class);
      importChunk.setAccessible(true);
    } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
      fail();
    }

    try {
      if (impD.mkdirs()) {
        Files.copy(testFile.toPath(), impF.toPath());
      }
      targetUrl.set(agent, "\\test");
      importChunk.invoke(agent, impF);
    } catch (IllegalAccessException | InvocationTargetException | IOException e) {
      if (e.getClass() == InvocationTargetException.class) {
        assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
            URISyntaxException.class);
      } else {
        fail();
      }
    } finally {
      NquadsExporterTaskTest.NquadsExporterTaskTestHelper.tearDown();
    }

    try {
      if (impD.mkdirs()) {
        Files.copy(testFile.toPath(), impF.toPath());
      }
      targetUrl.set(agent, "http://localhost/test");
      assertNull(importChunk.invoke(agent, impF));
    } catch (IllegalAccessException | InvocationTargetException | IOException e) {
      fail();
    } finally {
      NquadsExporterTaskTest.NquadsExporterTaskTestHelper.tearDown();
    }

    //other import functionality already tested in the corresponding tasks' tests
  }

  public void testStartBlazegraphInstance() {
    String fs = System.getProperty("file.separator");
    CityImportAgent agent = new CityImportAgent();
    File testFile = new File(
        Objects.requireNonNull(this.getClass().getResource(fs + "test.gml")).getFile());
    File impD = new File(System.getProperty("java.io.tmpdir") + "imptstdir");
    File impF = new File(impD.getAbsolutePath() + fs + "test.gml");

    Method startBlazegraphInstance;

    try {
      if (impD.mkdirs()) {
        Files.copy(testFile.toPath(), impF.toPath());
      }
      startBlazegraphInstance = agent.getClass()
          .getDeclaredMethod("startBlazegraphInstance", BlockingQueue.class, String.class);
      startBlazegraphInstance.setAccessible(true);
      BlazegraphServerTask task = (BlazegraphServerTask) startBlazegraphInstance.invoke(agent,
          new LinkedBlockingDeque<>(), impF.getAbsolutePath());
      Field stopF = task.getClass().getDeclaredField("stop");
      stopF.setAccessible(true);
      assertFalse((Boolean) stopF.get(task));
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IOException | NoSuchFieldException e) {
      fail();
    } finally {
      NquadsExporterTaskTest.NquadsExporterTaskTestHelper.tearDown();
    }
    //other functionality already tested in the corresponding task
  }

  public void testImportToLocalBlazegraphInstance() {
    String fs = System.getProperty("file.separator");
    CityImportAgent agent = new CityImportAgent();
    File testFile = new File(
        Objects.requireNonNull(this.getClass().getResource(fs + "test.gml")).getFile());
    File impD = new File(System.getProperty("java.io.tmpdir") + "imptstdir");
    File impF = new File(impD.getAbsolutePath() + fs + "test.gml");

    Method importToLocalBlazegraphInstance;

    try {
      if (impD.mkdirs()) {
        Files.copy(testFile.toPath(), impF.toPath());
      }
      importToLocalBlazegraphInstance = agent.getClass()
          .getDeclaredMethod("importToLocalBlazegraphInstance", BlockingQueue.class, File.class);
      importToLocalBlazegraphInstance.setAccessible(true);
      ImporterTask task = (ImporterTask) importToLocalBlazegraphInstance.invoke(agent,
          new LinkedBlockingDeque<>(), impF);
      Field stopF = task.getClass().getDeclaredField("stop");
      stopF.setAccessible(true);
      assertFalse((Boolean) stopF.get(task));
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IOException | NoSuchFieldException e) {
      fail();
    } finally {
      NquadsExporterTaskTest.NquadsExporterTaskTestHelper.tearDown();
    }
    //other functionality already tested in the corresponding task
  }

  public void testExportToNquads() {
    String fs = System.getProperty("file.separator");
    CityImportAgent agent = new CityImportAgent();
    File testFile = new File(
        Objects.requireNonNull(this.getClass().getResource(fs + "test.gml")).getFile());
    File impD = new File(System.getProperty("java.io.tmpdir") + "imptstdir");
    File impF = new File(impD.getAbsolutePath() + fs + "test.gml");

    Method exportToNquads;

    try {
      if (impD.mkdirs()) {
        Files.copy(testFile.toPath(), impF.toPath());
      }
      exportToNquads = agent.getClass()
          .getDeclaredMethod("exportToNquads", BlockingQueue.class, File.class);
      exportToNquads.setAccessible(true);
      NquadsExporterTask task = (NquadsExporterTask) exportToNquads.invoke(agent,
          new LinkedBlockingDeque<>(), impF);
      Field stopF = task.getClass().getDeclaredField("stop");
      stopF.setAccessible(true);
      assertFalse((Boolean) stopF.get(task));
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IOException | NoSuchFieldException e) {
      fail();
    } finally {
      NquadsExporterTaskTest.NquadsExporterTaskTestHelper.tearDown();
    }
    //other functionality already tested in the corresponding task
  }

  public void testUploadNQuadsFileToBlazegraphInstance() {
    CityImportAgent agent = new CityImportAgent();
    URI impUri;
    Method uploadNQuadsFileToBlazegraphInstance;

    try {
      impUri = new URI("http://localhost/test");
      uploadNQuadsFileToBlazegraphInstance = agent.getClass()
          .getDeclaredMethod("uploadNQuadsFileToBlazegraphInstance", BlockingQueue.class,
              URI.class);
      uploadNQuadsFileToBlazegraphInstance.setAccessible(true);
      NquadsUploaderTask task = (NquadsUploaderTask) uploadNQuadsFileToBlazegraphInstance.invoke(
          agent, new LinkedBlockingDeque<>(), impUri);
      Field stopF = task.getClass().getDeclaredField("stop");
      stopF.setAccessible(true);
      assertFalse((Boolean) stopF.get(task));
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException | URISyntaxException e) {
      fail();
    }

    //other functionality already tested in the corresponding task
  }

  public void testWriteErrorLog() {
    //@Todo: implementation
  }

  public void testArchiveImportFiles() {
    //@Todo: implementation
  }


}