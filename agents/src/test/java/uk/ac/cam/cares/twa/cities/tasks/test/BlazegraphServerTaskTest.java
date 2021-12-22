package uk.ac.cam.cares.twa.cities.tasks.test;

import com.bigdata.rdf.sail.webapp.NanoSparqlServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import junit.framework.TestCase;
import org.eclipse.jetty.server.Server;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.tasks.BlazegraphServerTask;
import uk.ac.cam.cares.twa.cities.tasks.ImporterTask;

public class BlazegraphServerTaskTest extends TestCase {

  public void testNewBlazegraphServerTask() {
    BlazegraphServerTask task;

    try {
      task = new BlazegraphServerTask(new LinkedBlockingDeque<>(), "test.jnl");
      assertNotNull(task);
    } catch (Exception e) {
      fail();
    }
  }

  public void testNewBlazegraphServerTaskFields() {
    BlazegraphServerTask task = new BlazegraphServerTask(new LinkedBlockingDeque<>(), "test.jnl");
    assertEquals(13, task.getClass().getDeclaredFields().length);
    Field PROPERTY_FILE;
    Field PROPERTY_FILE_PATH;
    Field JETTY_CFG_PATH;
    Field WAR_PATH;
    Field NSS_PATH;
    Field SYS_PROP_JETTY;
    Field NAMESPACE;
    Field DEF_JOURNAL_NAME;
    Field FS;
    Field journalPath;
    Field queue;
    Field stop;
    Field server;

    try {
      PROPERTY_FILE = task.getClass().getDeclaredField("PROPERTY_FILE");
      PROPERTY_FILE.setAccessible(true);
      assertEquals(PROPERTY_FILE.get(task), "RWStore.properties");
      PROPERTY_FILE_PATH = task.getClass().getDeclaredField("PROPERTY_FILE_PATH");
      PROPERTY_FILE_PATH.setAccessible(true);
      assertEquals(PROPERTY_FILE_PATH.get(task), "../../../../../../../");
      JETTY_CFG_PATH = task.getClass().getDeclaredField("JETTY_CFG_PATH");
      JETTY_CFG_PATH.setAccessible(true);
      assertEquals(JETTY_CFG_PATH.get(task), "jetty.xml");
      WAR_PATH = task.getClass().getDeclaredField("WAR_PATH");
      WAR_PATH.setAccessible(true);
      assertEquals(WAR_PATH.get(task), "war");
      NSS_PATH = task.getClass().getDeclaredField("NSS_PATH");
      NSS_PATH.setAccessible(true);
      assertEquals(NSS_PATH.get(task), "com/bigdata/rdf/sail/webapp/");
      SYS_PROP_JETTY = task.getClass().getDeclaredField("SYS_PROP_JETTY");
      SYS_PROP_JETTY.setAccessible(true);
      assertEquals(SYS_PROP_JETTY.get(task), "jetty.home");
      NAMESPACE = task.getClass().getDeclaredField("NAMESPACE");
      NAMESPACE.setAccessible(true);
      assertEquals(NAMESPACE.get(task), "tmpkb");
      DEF_JOURNAL_NAME = task.getClass().getDeclaredField("DEF_JOURNAL_NAME");
      DEF_JOURNAL_NAME.setAccessible(true);
      assertEquals(DEF_JOURNAL_NAME.get(task), "citiesKG.jnl");
      FS = task.getClass().getDeclaredField("FS");
      FS.setAccessible(true);
      assertEquals(FS.get(task), System.getProperty("file.separator"));
      journalPath = task.getClass().getDeclaredField("journalPath");
      journalPath.setAccessible(true);
      assertEquals(journalPath.get(task), "test.jnl");
      queue = task.getClass().getDeclaredField("queue");
      queue.setAccessible(true);
      assertEquals(queue.get(task).getClass(), LinkedBlockingDeque.class);
      stop = task.getClass().getDeclaredField("stop");
      stop.setAccessible(true);
      assertFalse((Boolean) stop.get(task));
      server = task.getClass().getDeclaredField("server");
      server.setAccessible(true);
      assertNull(server.get(task));
    } catch (NoSuchFieldException | IllegalAccessException e) {
      fail();
    }
  }

  public void testNewBlazegraphServerTaskMethods() {
    BlazegraphServerTask task = new BlazegraphServerTask(new LinkedBlockingDeque<>(), "test.jnl");
    assertEquals(7, task.getClass().getDeclaredMethods().length);
  }

  public void testNewBlazegraphServerTaskStopMethod() {
    BlazegraphServerTask task = new BlazegraphServerTask(new LinkedBlockingDeque<>(), "test.jnl");

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

  public void testNewBlazegraphServerTaskIsRunningMethod() {
    BlazegraphServerTask task = new BlazegraphServerTask(new LinkedBlockingDeque<>(), "test.jnl");

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

  public void testNewBlazegraphServerTaskSetupPathsMethod() {
    BlazegraphServerTask task = new BlazegraphServerTask(new LinkedBlockingDeque<>(),
        "/test/test.jnl");
    String fs = System.getProperty("file.separator");

    try {
      Field PROPERTY_FILE = task.getClass().getDeclaredField("PROPERTY_FILE");
      PROPERTY_FILE.setAccessible(true);
      Method setupPaths = task.getClass().getDeclaredMethod("setupPaths");
      setupPaths.setAccessible(true);
      assertEquals(setupPaths.invoke(task), fs + "test" + fs + "test" + PROPERTY_FILE.get(task));

    } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }
  }

  public void testNewBlazegraphServerTaskSetupFilesMethod() {
    BlazegraphServerTask task = new BlazegraphServerTask(new LinkedBlockingDeque<>(),
        "/test/test.jnl");
    String fs = System.getProperty("file.separator");
    File sysTmp = new File(System.getProperty("java.io.tmpdir"));
    File propFile = null;

    try {
      Field PROPERTY_FILE = task.getClass().getDeclaredField("PROPERTY_FILE");
      PROPERTY_FILE.setAccessible(true);
      Field DEF_JOURNAL_NAME = task.getClass().getDeclaredField("DEF_JOURNAL_NAME");
      DEF_JOURNAL_NAME.setAccessible(true);
      Field journalPath = task.getClass().getDeclaredField("journalPath");
      journalPath.setAccessible(true);
      Method setupFiles = task.getClass().getDeclaredMethod("setupFiles", String.class);
      setupFiles.setAccessible(true);
      String propFilePath = sysTmp.getAbsolutePath() + fs + PROPERTY_FILE.get(task);
      propFile = (File) setupFiles.invoke(task, propFilePath);
      assertEquals(propFile.getAbsolutePath(), propFilePath);
      Properties prop = new Properties();
      FileInputStream fin = new FileInputStream(propFile);
      prop.load(fin);
      fin.close();

      String expected = new File(journalPath.get(task).toString()).getAbsolutePath();
      assertEquals(prop.getProperty("com.bigdata.journal.AbstractJournal.file"),
          expected);

      try {
        setupFiles.invoke(task, propFilePath + fs + propFile.getName());
        fail();
      } catch (InvocationTargetException e) {
        assertEquals(e.getTargetException().getClass().getSuperclass().getSuperclass(), IOException.class);
      }

    } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException |
        IOException e) {
      fail();
    } finally {
      if (Objects.requireNonNull(propFile).isFile()) {
        if (!propFile.delete()) {
          fail();
        }
      }
    }
  }

  public void testNewBlazegraphServerTaskSetupSystemMethod() {
    String jnlPath = System.getProperty("java.io.tmpdir") + "test.jnl";
    BlazegraphServerTask task = new BlazegraphServerTask(new LinkedBlockingDeque<>(), jnlPath);
    String jettyCfg = "jetty.xml";

    try {
      Field SYS_PROP_JETTY = task.getClass().getDeclaredField("SYS_PROP_JETTY");
      SYS_PROP_JETTY.setAccessible(true);
      Method setupSystem = task.getClass().getDeclaredMethod("setupSystem", String.class);
      setupSystem.setAccessible(true);
      assertEquals(new File((String) setupSystem.invoke(task, jnlPath)).getName(), jettyCfg);
      assertTrue(System.getProperty(BlazegraphServerTask.SYS_PROP_JETTY).contains(".jar!/war"));
      assertTrue(System.getProperty(NanoSparqlServer.SystemProperties.JETTY_XML)
          .contains(".jar!/jetty.xml"));
      assertEquals(System.getProperty(NanoSparqlServer.SystemProperties.BIGDATA_PROPERTY_FILE),
          jnlPath);

    } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }
  }

  public void testNewBlazegraphServerTaskSetupServerMethod() {
    String jnlPath = System.getProperty("java.io.tmpdir") + "test.jnl";
    File jnlFile = new File(jnlPath);
    File propFile = new File(
        jnlPath.replace(ImporterTask.EXT_FILE_JNL, BlazegraphServerTask.PROPERTY_FILE));
    BlazegraphServerTask task = new BlazegraphServerTask(new LinkedBlockingDeque<>(), jnlPath);
    String jettyCfg = "jetty.xml";

    try {
      Method setupServer = task.getClass()
          .getDeclaredMethod("setupServer", String.class, String.class);
      setupServer.setAccessible(true);

      try {
        setupServer.invoke(task, jnlPath, "");
      } catch (InvocationTargetException e) {
        assertEquals(e.getTargetException().getStackTrace()[1].getClassName(),
            "com.bigdata.rdf.sail.webapp.NanoSparqlServer");
      }

      assertEquals(setupServer.invoke(task, jnlPath, jettyCfg).getClass(), Server.class);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail();
    } finally {
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
    }

  }

  public void testNewBlazegraphServerTaskRunMethod() {
    String jnlPath = System.getProperty("java.io.tmpdir") + "test.jnl";
    File jnlFile = new File(jnlPath);
    File propFile = new File(
        jnlPath.replace(ImporterTask.EXT_FILE_JNL, BlazegraphServerTask.PROPERTY_FILE));

    BlazegraphServerTask task = new BlazegraphServerTask(null, jnlPath);

    try {
      task.run();
      fail();
    } catch (JPSRuntimeException e) {
      assertEquals(e.getClass(), JPSRuntimeException.class);
    } finally {
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
    }

    task = new BlazegraphServerTask(new LinkedBlockingDeque<>(), "");

    try {
      task.run();
      fail();
    } catch (JPSRuntimeException e) {
      assertEquals(e.getClass(), JPSRuntimeException.class);
    }

    task = new BlazegraphServerTask(new LinkedBlockingDeque<>(), null);

    try {
      task.run();
      fail();
    } catch (JPSRuntimeException e) {
      assertEquals(e.getClass(), JPSRuntimeException.class);
    }

    task = new BlazegraphServerTask(new LinkedBlockingDeque<>(), jnlPath);

    try {
      Field server = task.getClass().getDeclaredField("server");
      Field stop = task.getClass().getDeclaredField("stop");
      Field queue = task.getClass().getDeclaredField("queue");
      server.setAccessible(true);
      stop.setAccessible(true);
      queue.setAccessible(true);
      new Thread(task).start();

      while (!(boolean) stop.get(task)) {
        if (server.get(task) != null) {
          assertEquals(Server.class, server.get(task).getClass());
          if (((Server) server.get(task)).isRunning()) {
            if (((BlockingQueue<?>) queue.get(task)).size() > 0) {
              assertEquals(Server.class, ((BlockingQueue<?>) queue.get(task)).take().getClass());
              //Expected behaviour of stopping server just after it started to result in printing runtime
              //error log to the console. The test of the run() method will pass.
              //ERROR: CreateKBTask.java:121: java.lang.RuntimeException: java.lang.RuntimeException: java.lang.InterruptedException
              ((Server) server.get(task)).stop();
              assertFalse(((Server) server.get(task)).isRunning());
            }
          }
        }
      }
    } catch (Exception e) {
      fail();
    } finally {
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
    }


  }

}
