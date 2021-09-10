package uk.ac.cam.cares.twa.cities.tasks.test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import junit.framework.TestCase;
import org.eclipse.jetty.server.Server;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.tasks.BlazegraphServerTask;
import uk.ac.cam.cares.twa.cities.tasks.ImporterTask;
import uk.ac.cam.cares.twa.cities.tasks.NquadsExporterTask;

public class ImporterTaskTest extends TestCase {

  public void testNewImporterTask() {
    ImporterTask task;

    try {
      task = new ImporterTask(new LinkedBlockingDeque<>(), new File("test.gml"));
      assertNotNull(task);
    } catch (Exception e) {
      fail();
    }
  }

  public void testNewImporterTaskFields() {
    ImporterTask task = new ImporterTask(new LinkedBlockingDeque<>(), new File("test.gml"));
    assertEquals(15, task.getClass().getDeclaredFields().length);

    Field PROJECT_CONFIG;
    Field PLACEHOLDER_HOST;
    Field PLACEHOLDER_PORT;
    Field PLACEHOLDER_NS;
    Field URL_BLZG_NS;
    Field URL_SPARQL;
    Field EXT_FILE_JNL;
    Field EXT_FILE_GML;
    Field EXT_FILE_NQUADS;
    Field ARG_SHELL;
    Field ARG_IMPORT;
    Field ARG_CFG;
    Field serverInstances;
    Field importFile;
    Field stop;

    try {
      PROJECT_CONFIG = task.getClass().getDeclaredField("PROJECT_CONFIG");
      assertEquals(PROJECT_CONFIG.get(task), "project.xml");
      PLACEHOLDER_HOST = task.getClass().getDeclaredField("PLACEHOLDER_HOST");
      assertEquals(PLACEHOLDER_HOST.get(task), "{{host}}");
      PLACEHOLDER_PORT = task.getClass().getDeclaredField("PLACEHOLDER_PORT");
      assertEquals(PLACEHOLDER_PORT.get(task), "{{port}}");
      PLACEHOLDER_NS = task.getClass().getDeclaredField("PLACEHOLDER_NS");
      assertEquals(PLACEHOLDER_NS.get(task), "{{namespace}}");
      URL_BLZG_NS = task.getClass().getDeclaredField("URL_BLZG_NS");
      assertEquals(URL_BLZG_NS.get(task), "/blazegraph/namespace/");
      URL_SPARQL = task.getClass().getDeclaredField("URL_SPARQL");
      assertEquals(URL_SPARQL.get(task), "/sparql/");
      EXT_FILE_JNL = task.getClass().getDeclaredField("EXT_FILE_JNL");
      assertEquals(EXT_FILE_JNL.get(task), ".jnl");
      EXT_FILE_GML = task.getClass().getDeclaredField("EXT_FILE_GML");
      assertEquals(EXT_FILE_GML.get(task), ".gml");
      EXT_FILE_NQUADS = task.getClass().getDeclaredField("EXT_FILE_NQUADS");
      assertEquals(EXT_FILE_NQUADS.get(task), ".nq");
      ARG_SHELL = task.getClass().getDeclaredField("ARG_SHELL");
      assertEquals(ARG_SHELL.get(task), "-shell");
      ARG_IMPORT = task.getClass().getDeclaredField("ARG_IMPORT");
      assertEquals(ARG_IMPORT.get(task), "-import=");
      ARG_CFG = task.getClass().getDeclaredField("ARG_CFG");
      assertEquals(ARG_CFG.get(task), "-config=");
      serverInstances = task.getClass().getDeclaredField("serverInstances");
      serverInstances.setAccessible(true);
      assertEquals(serverInstances.get(task).getClass(), LinkedBlockingDeque.class);
      importFile = task.getClass().getDeclaredField("importFile");
      importFile.setAccessible(true);
      assertEquals(((File) importFile.get(task)).getName(), "test.gml");
      stop = task.getClass().getDeclaredField("stop");
      stop.setAccessible(true);
      assertFalse((boolean) stop.get(task));
    } catch (NoSuchFieldException | IllegalAccessException e) {
      fail();
    }

  }

  public void testNewImporterTaskMethods() {
    ImporterTask task = new ImporterTask(new LinkedBlockingDeque<>(), new File("test.gml"));
    assertEquals(4, task.getClass().getDeclaredMethods().length);
  }

  public void testNewImporterTaskStopMethod() {
    ImporterTask task = new ImporterTask(new LinkedBlockingDeque<>(), new File("test.gml"));

    try {
      Field stopF = task.getClass().getDeclaredField("stop");
      stopF.setAccessible(true);
      assertFalse((Boolean) stopF.get(task));
      Method stopM = task.getClass().getDeclaredMethod("stop");
      stopM.setAccessible(true);
      stopM.invoke(task);
      assertTrue((Boolean) stopF.get(task));
    } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }

  }

  public void testNewImporterTaskIsRunningMethod() {
    ImporterTask task = new ImporterTask(new LinkedBlockingDeque<>(), new File("test.gml"));

    try {
      Method isRunning = task.getClass().getDeclaredMethod("isRunning");
      isRunning.setAccessible(true);
      Field stopF = task.getClass().getDeclaredField("stop");
      stopF.setAccessible(true);
      assertTrue((Boolean) isRunning.invoke(task));
      Method stopM = task.getClass().getDeclaredMethod("stop");
      stopM.setAccessible(true);
      stopM.invoke(task);
      assertFalse((Boolean) isRunning.invoke(task));
    } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }

  }

  public void testNewImporterTaskSetupFilesMethod() {
    File impFile = new File(
        System.getProperty("java.io.tmpdir") + System.getProperty("java.io.tmpdir") + "test.gml");
    File projFile = null;
    ImporterTask task = new ImporterTask(new LinkedBlockingDeque<>(), impFile);
    String localhostUriStr = "http://localhost:9999/";

    try {
      URI localhostUri = new URI(localhostUriStr);
      Method setupFiles = task.getClass().getDeclaredMethod("setupFiles", URI.class);
      setupFiles.setAccessible(true);
      try {
        setupFiles.invoke(task, localhostUri);
      } catch (InvocationTargetException e) {
        assertEquals(e.getTargetException().getClass().getSuperclass().getSuperclass().getName(),
            IOException.class.getName());
      }

      impFile = new File(System.getProperty("java.io.tmpdir") + "test.gml");
      projFile = new File(impFile.getAbsolutePath()
          .replace(ImporterTask.EXT_FILE_GML, ImporterTask.PROJECT_CONFIG));
      task = new ImporterTask(new LinkedBlockingDeque<>(), impFile);

      assertEquals(((File) setupFiles.invoke(task, localhostUri)).getName(), projFile.getName());

      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document cfg = builder.parse(projFile);
      assertEquals(
          cfg.getElementsByTagName(NquadsExporterTask.CFG_KEY_SERVER).item(0).getTextContent(),
          localhostUri.getHost());
      assertEquals(
          cfg.getElementsByTagName(NquadsExporterTask.CFG_KEY_PORT).item(0).getTextContent(),
          String.valueOf(localhostUri.getPort()));
      assertEquals(
          cfg.getElementsByTagName(NquadsExporterTask.CFG_KEY_SID).item(0).getTextContent(),
          ImporterTask.URL_BLZG_NS + BlazegraphServerTask.NAMESPACE + ImporterTask.URL_SPARQL);

    } catch (NoSuchMethodException | URISyntaxException | IllegalAccessException | InvocationTargetException |
        SAXException | IOException | ParserConfigurationException e) {
      fail();
    } finally {
      if (Objects.requireNonNull(impFile).isFile()) {
        if (!impFile.delete()) {
          fail();
        }
      }
      if (Objects.requireNonNull(projFile).isFile()) {
        if (!projFile.delete()) {
          fail();
        }
      }
    }

  }

  public void testNewImporterTaskRunMethod() {
    File impFile = new File(System.getProperty("java.io.tmpdir") + "test.gml");
    File testFile = new File(
        Objects.requireNonNull(this.getClass().getResource("/test.gml")).getFile());
    File nqFile = new File(
        impFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_GML, ImporterTask.EXT_FILE_NQUADS));
    File jnlFile = new File(
        impFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_GML, ImporterTask.EXT_FILE_JNL));
    File propFile = new File(impFile.getAbsolutePath()
        .replace(ImporterTask.EXT_FILE_GML, BlazegraphServerTask.PROPERTY_FILE));
    File projFile = new File(
        impFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_GML, ImporterTask.PROJECT_CONFIG));
    assertTrue(testFile.exists());
    try {
      Files.copy(testFile.toPath(), impFile.toPath());
    } catch (IOException e) {
      fail();
    }

    ExecutorService serverExecutor = Executors.newFixedThreadPool(1);
    BlockingQueue<Server> importQueue = new LinkedBlockingDeque<>();
    BlazegraphServerTask serverTask = new BlazegraphServerTask(importQueue,
        jnlFile.getAbsolutePath());
    serverExecutor.execute(serverTask);

    ImporterTask task = new ImporterTask(importQueue, new File(""));

    try {
      task.run();
      fail();
    } catch (JPSRuntimeException e) {
      assertEquals(e.getClass(), JPSRuntimeException.class);
    }

    try {
      Field server = serverTask.getClass().getDeclaredField("server");
      server.setAccessible(true);
      importQueue.put((Server) server.get(serverTask));
      task = new ImporterTask(importQueue, impFile);
      Field stop = task.getClass().getDeclaredField("stop");
      Field serverInstances = task.getClass().getDeclaredField("serverInstances");
      stop.setAccessible(true);
      serverInstances.setAccessible(true);
      new Thread(task).start();

      while (!(boolean) stop.get(task)) {
        if (((BlockingQueue<?>) serverInstances.get(task)).size() == 0) {
          if (Objects.requireNonNull(nqFile).isFile()) {
            assertTrue(nqFile.delete());
          }
        }
      }

    } catch (NoSuchFieldException | IllegalAccessException | InterruptedException e) {
      fail();
    } finally {
      if (Objects.requireNonNull(impFile).isFile()) {
        if (!impFile.delete()) {
          fail();
        }
      }
      if (Objects.requireNonNull(jnlFile).isFile()) {
        if (!jnlFile.delete()) {
          fail();
        }
      }
      if (Objects.requireNonNull(propFile).isFile()) {
        if (!propFile.delete()) {
          fail();
        }
      }
      if (Objects.requireNonNull(projFile).isFile()) {
        if (!projFile.delete()) {
          fail();
        }
      }
    }
  }

}
