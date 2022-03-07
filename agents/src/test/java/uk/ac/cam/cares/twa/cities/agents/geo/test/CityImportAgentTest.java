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
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.ac.cam.cares.jps.aws.AsynchronousWatcherService;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.agents.geo.CityImportAgent;
import uk.ac.cam.cares.twa.cities.tasks.geo.BlazegraphServerTask;
import uk.ac.cam.cares.twa.cities.tasks.geo.ImporterTask;
import uk.ac.cam.cares.twa.cities.tasks.geo.NquadsExporterTask;
import uk.ac.cam.cares.twa.cities.tasks.geo.NquadsUploaderTask;
import uk.ac.cam.cares.twa.cities.tasks.geo.test.NquadsExporterTaskTest;

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

    assertEquals(33, agent.getClass().getDeclaredFields().length);

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
    Field KEY_SRID;
    Field KEY_SRSNAME;
    Field CTYPE_SPARQLUPDATE;
    Field srid;
    Field srsname;
    Field OCGML_PREFIX;
    Field OCGML_SCHEMA;
    Field GRAPH_DATABASESRS;
    Field QN_MARK;
    Field SUB_SRID;
    Field SUB_SRSNAME;
    Field OB_SRID;
    Field OB_SRSNAME;

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
      KEY_SRID = agent.getClass().getDeclaredField("KEY_SRID");
      assertEquals(KEY_SRID.get(agent), "srid");
      KEY_SRSNAME = agent.getClass().getDeclaredField("KEY_SRSNAME");
      assertEquals(KEY_SRSNAME.get(agent), "srsName");
      CTYPE_SPARQLUPDATE = agent.getClass().getDeclaredField("CTYPE_SPARQLUPDATE");
      assertEquals(CTYPE_SPARQLUPDATE.get(agent), "application/sparql-update");
      srid = agent.getClass().getDeclaredField("srid");
      srid.setAccessible(true);
      assertNull(srid.get(agent));
      srsname = agent.getClass().getDeclaredField("srsname");
      srsname.setAccessible(true);
      assertNull(srsname.get(agent));
      OCGML_PREFIX = agent.getClass().getDeclaredField("OCGML_PREFIX");
      assertEquals(OCGML_PREFIX.get(agent), "ocgml");
      OCGML_SCHEMA = agent.getClass().getDeclaredField("OCGML_SCHEMA");
      assertEquals(OCGML_SCHEMA.get(agent), "http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#");
      GRAPH_DATABASESRS = agent.getClass().getDeclaredField("GRAPH_DATABASESRS");
      assertEquals(GRAPH_DATABASESRS.get(agent), "/databasesrs/");
      QN_MARK = agent.getClass().getDeclaredField("QN_MARK");
      assertEquals(QN_MARK.get(agent), "?");
      SUB_SRID = agent.getClass().getDeclaredField("SUB_SRID");
      assertEquals(SUB_SRID.get(agent), "srid");
      SUB_SRSNAME = agent.getClass().getDeclaredField("SUB_SRSNAME");
      assertEquals(SUB_SRSNAME.get(agent), "srsname");
      OB_SRID = agent.getClass().getDeclaredField("OB_SRID");
      assertEquals(OB_SRID.get(agent), "currentSrid");
      OB_SRSNAME = agent.getClass().getDeclaredField("OB_SRSNAME");
      assertEquals(OB_SRSNAME.get(agent), "currentSrsname");
    } catch (NoSuchFieldException | IllegalAccessException e) {
      fail();
    }
  }

  public void testNewCityImportAgentMethods() {
    CityImportAgent agent = new CityImportAgent();
    assertEquals(18, agent.getClass().getDeclaredMethods().length);
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

  public void testValidateDatabaseSrsInput() {
    CityImportAgent agent = new CityImportAgent();
    Method validateDatabaseSrsInput = null;

    try {
      validateDatabaseSrsInput = agent.getClass().getDeclaredMethod("validateDatabaseSrsInput", JSONObject.class, Set.class);
      validateDatabaseSrsInput.setAccessible(true);
    } catch (NoSuchMethodException e) {
      fail();
    }

    JSONObject requestParams = new JSONObject();
    JSONObject requestParamsSridError = new JSONObject();
    JSONObject requestParamsSrsnameError = new JSONObject();
    Set<String> keys = new HashSet<>();

    try {
      //test case when KEY_SRID and KEY_SRSNAME not available
      assertTrue((Boolean) validateDatabaseSrsInput.invoke(agent, requestParams, keys));
      keys.add(CityImportAgent.KEY_SRID);
      keys.add(CityImportAgent.KEY_SRSNAME);

      //test case when both srid and srsname are correct
      requestParams.put(CityImportAgent.KEY_SRID, "123");
      requestParams.put(CityImportAgent.KEY_SRSNAME, "srsname");

      assertFalse((Boolean) validateDatabaseSrsInput.invoke(agent, requestParams, keys));
    } catch (Exception e) {
      fail();
    }

    try {
      //testcase when srid is not a number
      requestParamsSridError.put(CityImportAgent.KEY_SRID, "srid");
      requestParamsSridError.put(CityImportAgent.KEY_SRSNAME, "srsname");

      validateDatabaseSrsInput.invoke(agent, requestParamsSridError, keys);
    } catch (Exception e) {
      assert e instanceof InvocationTargetException;
      assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
              JPSRuntimeException.class);
    }

    try {
      //testcase when srsname is empty
      requestParamsSrsnameError.put(CityImportAgent.KEY_SRID, "123");
      requestParamsSrsnameError.put(CityImportAgent.KEY_SRSNAME, "");

      assertTrue((Boolean) validateDatabaseSrsInput.invoke(agent, requestParamsSrsnameError, keys));
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
    /*

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

     */

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

    requestParams.put(CityImportAgent.KEY_SRID, "123");
    requestParams.put(CityImportAgent.KEY_SRSNAME, "srsname");
    //requestParams.put(CityImportAgent.KEY_DIRECTORY, System.getProperty("java.io.tmpdir"));

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

    requestParams.put(CityImportAgent.KEY_SRSNAME, "");
    try {
      validateInput.invoke(agent, requestParams);
    } catch (Exception e) {
      assert e instanceof InvocationTargetException;
      assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
              BadRequestException.class);
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
    String forwardSlash = "/";
    CityImportAgent agent = new CityImportAgent();
    Field targetUrl = null;
    Field importDir = null;
    Method importFiles = null;
    File testFile = new File(Objects.requireNonNull(this.getClass().getResource(forwardSlash + "test.gml")).getFile());
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

    // make sure if the impD exists, it need to be deleted before the test
    if (impD.exists()){
      try {
        FileUtils.deleteDirectory(impD); // force delete any directory with content. File.delete() can not delete dir with content
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // Case: if the importDir doesn't exist
    // should return NullPointerException at Objects.requireNonNull(dirContent)
    try {
      importDir.set(agent, impD);
      importFiles.invoke(agent, impD);
    } catch (IllegalAccessException | InvocationTargetException e) {
      if (e.getClass() == InvocationTargetException.class) {
        assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
            NullPointerException.class);  // It should be nullpointer exception
      } else {
        fail();
      }
    }

    // Case: if the importDir exists but no content, the directory is deletable.
    // should return empty string as it skips the if block at if (Objects.requireNonNull(dirContent).length > 0)
    try {
      if (impD.mkdirs()) {  // FILE.mkdirs() only return true for the first time, otherwise false
        assertEquals(importFiles.invoke(agent, impD), "");
      }
    } catch (Exception e) {
      fail();
    } finally {
      NquadsExporterTaskTest.NquadsExporterTaskTestHelper.tearDown();
      try {
        FileUtils.deleteDirectory(impD);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    try (MockedConstruction<BlazegraphServerTask> serverTask = Mockito.mockConstruction(BlazegraphServerTask.class, (mock, context) -> {
      Mockito.when(mock.isRunning()).thenReturn(true);
    })) {

      // Case: if the importDir exists with content, but null targetUrl
      // should return JPSRuntimeException
      // caused by NullPointerException due to null targetUrl at new URI(targetUrl) in importChunk method
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
        try {
          FileUtils.deleteDirectory(impD);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      // Case: test file is successfully imported
      try {
        if (impD.mkdirs()) {
          Files.copy(testFile.toPath(), impF.toPath());
          targetUrl.set(agent, "http://localhost/test");
          assertEquals(importFiles.invoke(agent, impD), "file_part_1.gml \n");    // Initial code: Splitfiles returns null, chunks = null, assume the file is small
        }
      } catch (InvocationTargetException | IllegalAccessException | IOException e) {
        fail();
      } finally {
        NquadsExporterTaskTest.NquadsExporterTaskTestHelper.tearDown();
        try {
          FileUtils.deleteDirectory(impD);
        } catch (IOException e) {
          fail();
        }
      }
    }

    //other import functionality already tested in the corresponding tasks' tests
  }

  public void testSplitFile() {
    String fs = System.getProperty("file.separator");
    String forwardSlash = "/";
    CityImportAgent agent = new CityImportAgent();
    File testFile = new File(
        Objects.requireNonNull(this.getClass().getResource(forwardSlash + "test.gml")).getFile());
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

  }

  public void testImportChunk() {
    String fs = System.getProperty("file.separator");
    String forwardSlash = "/";
    CityImportAgent agent = new CityImportAgent();
    File testFile = new File(
        Objects.requireNonNull(this.getClass().getResource(forwardSlash + "test.gml")).getFile());
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
    String forwardSlash = "/";
    CityImportAgent agent = new CityImportAgent();
    File testFile = new File(
        Objects.requireNonNull(this.getClass().getResource(forwardSlash + "test.gml")).getFile());
    File impD = new File(System.getProperty("java.io.tmpdir") + "imptstdir");
    File impF = new File(impD.getAbsolutePath() + fs + "test.gml");

    Method startBlazegraphInstance;
    BlazegraphServerTask task = null;

    try {
      if (impD.mkdirs()) {
        Files.copy(testFile.toPath(), impF.toPath());
      }
      startBlazegraphInstance = agent.getClass()
          .getDeclaredMethod("startBlazegraphInstance", BlockingQueue.class, String.class);
      startBlazegraphInstance.setAccessible(true);
     task = (BlazegraphServerTask) startBlazegraphInstance.invoke(agent,
          new LinkedBlockingDeque<>(), impF.getAbsolutePath());
      Field stopF = task.getClass().getDeclaredField("stop");
      stopF.setAccessible(true);
      assertFalse((Boolean) stopF.get(task));
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IOException | NoSuchFieldException e) {
      fail();
    } finally {
      try {
        task.stop();
        FileUtils.deleteDirectory(impD);
        NquadsExporterTaskTest.NquadsExporterTaskTestHelper.tearDown();
      } catch (IOException e) {
        fail();
      }
    }
    //other functionality already tested in the corresponding task
  }

  public void testImportToLocalBlazegraphInstance() {
    String fs = System.getProperty("file.separator");
    String forwardSlash = "/";
    CityImportAgent agent = new CityImportAgent();
    File testFile = new File(
        Objects.requireNonNull(this.getClass().getResource(forwardSlash + "test.gml")).getFile());
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
    String forwardSlash = "/";
    CityImportAgent agent = new CityImportAgent();
    File testFile = new File(
        Objects.requireNonNull(this.getClass().getResource(forwardSlash + "test.gml")).getFile());
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

  public void testSetDatabaseSrs() {
    CityImportAgent agent = new CityImportAgent();
    Field targetUrl = null;
    Method setDatabaseSrs = null;

    try {
      targetUrl = agent.getClass().getDeclaredField("targetUrl");
      targetUrl.setAccessible(true);
      targetUrl.set(agent, "http://localhost");
      setDatabaseSrs = agent.getClass().getDeclaredMethod("setDatabaseSrs");
      setDatabaseSrs.setAccessible(true);
    } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
      fail();
    }

    try {
      setDatabaseSrs.invoke(agent);
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
        setDatabaseSrs.invoke(agent);
      } catch (Exception e) {
        assert e instanceof InvocationTargetException;
        assertEquals(((InvocationTargetException) e).getTargetException().getClass(), JPSRuntimeException.class);
        assertEquals(((InvocationTargetException) e).getTargetException().getCause().getMessage(), "http://localhost 0");
      }
    } catch (Exception e) {
      fail();
    }
  }

  public void testGetSetDatabaseSrsUpdate() {
    CityImportAgent agent = new CityImportAgent();

    try {
      Field targetUrl = agent.getClass().getDeclaredField("targetUrl");
      targetUrl.setAccessible(true);
      targetUrl.set(agent, "http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql");
      Field srid = agent.getClass().getDeclaredField("srid");
      srid.setAccessible(true);
      srid.set(agent, "123");
      Field srsname = agent.getClass().getDeclaredField("srsname");
      srsname.setAccessible(true);
      srsname.set(agent, "test");
    } catch (Exception e) {
      fail();
    }

    String updateString = "PREFIX  ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#>\n" +
            "\n" +
            "WITH <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql/databasesrs/>\n" +
            "DELETE {\n" +
            "  ?srid ocgml:srid ?currentSrid .\n" +
            "  ?srsname ocgml:srsname ?currentSrsname .\n" +
            "}\n" +
            "INSERT {\n" +
            "  <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql> ocgml:srid 123 .\n" +
            "  <http://127.0.0.1:9999/blazegraph/namespace/berlin/sparql> ocgml:srsname \"test\" .\n" +
            "}\n" +
            "WHERE\n" +
            "  { OPTIONAL\n" +
            "      { ?srid  ocgml:srid  ?currentSrid}\n" +
            "    OPTIONAL\n" +
            "      { ?srsname  ocgml:srsname  ?currentSrsname}\n" +
            "  }\n";
    try {
      Method getSetDatabaseSrsUpdate = agent.getClass().getDeclaredMethod("getSetDatabaseSrsUpdate");
      getSetDatabaseSrsUpdate.setAccessible(true);
      assertEquals(getSetDatabaseSrsUpdate.invoke(agent).toString(), updateString);
    } catch (Exception e) {
      fail();
    }
  }

  public void testArchiveImportFiles() {
    String fs = System.getProperty("file.separator");
    File archD = new File(System.getProperty("java.io.tmpdir") + "tstdir");
    File archF = new File(archD.getParentFile().getAbsolutePath() + fs + "tstdir.zip");
    File impFgml = new File(archD.getAbsolutePath() + fs + "test.gml");
    File impFgmlPart = new File(archD.getAbsolutePath() + fs + "test1.gml");
    File impFnq = new File(archD.getAbsolutePath() + fs + "test.nq");

    assertEquals(CityImportAgent.archiveImportFiles(impFnq), "");

    if (archD.mkdirs()) {
      assertEquals(CityImportAgent.archiveImportFiles(impFnq), "");
      try {
        if (impFgml.createNewFile()) {
          assertEquals(CityImportAgent.archiveImportFiles(impFnq), "");
        } else {
          fail();
        }
        if (impFnq.createNewFile()) {
          assertEquals(CityImportAgent.archiveImportFiles(impFnq), "");
        } else {
          fail();
        }
        if (impFgmlPart.createNewFile()) {
          /*CityImportAgent agent = new CityImportAgent();
          Field serverExecutor = agent.getClass().getDeclaredField("serverExecutor");
          serverExecutor.setAccessible(true);
          serverExecutor.set(agent, Executors.newFixedThreadPool(CityImportAgent.NUM_SERVER_THREADS)); */
          assertEquals(CityImportAgent.archiveImportFiles(impFnq), archF.getAbsolutePath());
        } else {
          fail();
        }

      } catch (IOException  e) {
        fail();
      } finally {
        if (archF.exists() && !archF.delete()) {
          fail();
        }
        if (archD.exists()) {
          try {
            FileUtils.deleteDirectory(archD);
          } catch (IOException e) {
            fail();
          }
        }
      }
    } else {
      fail();
    }

  }


}