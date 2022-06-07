package uk.ac.cam.cares.twa.cities.tasks.test;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.cam.cares.twa.cities.tasks.BlazegraphServerTask.DEF_JOURNAL_NAME;

import com.bigdata.rdf.store.DataLoader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.agents.geo.CityImportAgent;
import uk.ac.cam.cares.twa.cities.tasks.BlazegraphServerTask;
import uk.ac.cam.cares.twa.cities.tasks.ImporterTask;
import uk.ac.cam.cares.twa.cities.tasks.NquadsExporterTask;

public class NquadsExporterTaskTest {

  @Test
  public void testNewNquadsExporterTask() {
    NquadsExporterTask task;

    try {
      task = new NquadsExporterTask(new LinkedBlockingDeque<>(), new File("test.gml"),
          "http://www.test.com/");
      assertNotNull(task);
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void testNewNquadsExporterTaskFields() {
    BlockingQueue<File> queue = new LinkedBlockingDeque<>();
    File impFile = new File("test.gml");
    String url = "http://www.test.com/";
    NquadsExporterTask task = new NquadsExporterTask(queue, impFile, url);
    assertEquals(16, task.getClass().getDeclaredFields().length);

    Field ARG_OUTDIR;
    Field ARG_FORMAT;
    Field NQ_OUTDIR;
    Field NQ_FORMAT;
    Field NQ_FILENAME;
    Field EX_PROP_FILENAME;
    Field EXT_GZ;
    Field EXT_FILE_ZIP;
    Field CFG_KEY_SERVER;
    Field CFG_KEY_PORT;
    Field CFG_KEY_SID;
    Field FS;
    Field stop;
    Field nqQueue;
    Field importFile;
    Field targetUrl;

    try {
      ARG_OUTDIR = task.getClass().getDeclaredField("ARG_OUTDIR");
      assertEquals(ARG_OUTDIR.get(task), "-outdir");
      ARG_FORMAT = task.getClass().getDeclaredField("ARG_FORMAT");
      assertEquals(ARG_FORMAT.get(task), "-format");
      NQ_OUTDIR = task.getClass().getDeclaredField("NQ_OUTDIR");
      assertEquals(NQ_OUTDIR.get(task), "quads");
      NQ_FORMAT = task.getClass().getDeclaredField("NQ_FORMAT");
      assertEquals(NQ_FORMAT.get(task), "N-Quads");
      NQ_FILENAME = task.getClass().getDeclaredField("NQ_FILENAME");
      assertEquals(NQ_FILENAME.get(task), "data.nq.gz");
      EX_PROP_FILENAME = task.getClass().getDeclaredField("EX_PROP_FILENAME");
      assertEquals(EX_PROP_FILENAME.get(task), "kb.properties");
      EXT_GZ = task.getClass().getDeclaredField("EXT_GZ");
      assertEquals(EXT_GZ.get(task), ".gz");
      EXT_FILE_ZIP = task.getClass().getDeclaredField("EXT_FILE_ZIP");
      assertEquals(EXT_FILE_ZIP.get(task), ".zip");
      CFG_KEY_SERVER = task.getClass().getDeclaredField("CFG_KEY_SERVER");
      assertEquals(CFG_KEY_SERVER.get(task), "server");
      CFG_KEY_PORT = task.getClass().getDeclaredField("CFG_KEY_PORT");
      assertEquals(CFG_KEY_PORT.get(task), "port");
      CFG_KEY_SID = task.getClass().getDeclaredField("CFG_KEY_SID");
      assertEquals(CFG_KEY_SID.get(task), "sid");
      FS = task.getClass().getDeclaredField("FS");
      FS.setAccessible(true);
      assertEquals(FS.get(task), System.getProperty("file.separator"));
      stop = task.getClass().getDeclaredField("stop");
      stop.setAccessible(true);
      assertFalse((boolean) stop.get(task));
      nqQueue = task.getClass().getDeclaredField("nqQueue");
      nqQueue.setAccessible(true);
      assertEquals(nqQueue.get(task), queue);
      importFile = task.getClass().getDeclaredField("importFile");
      importFile.setAccessible(true);
      assertEquals(importFile.get(task), impFile);
      targetUrl = task.getClass().getDeclaredField("targetUrl");
      targetUrl.setAccessible(true);
      assertEquals(targetUrl.get(task), url);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      fail();
    }

  }

  @Test
  public void testNewNquadsExporterTaskMethods() {
    NquadsExporterTask task = new NquadsExporterTask(new LinkedBlockingDeque<>(),
        new File("test.gml"), "http://www.test.com/");
    assertEquals(6, task.getClass().getDeclaredMethods().length);
  }

  @Test
  public void testNewNquadsExporterTaskStopMethod() {
    NquadsExporterTask task = new NquadsExporterTask(new LinkedBlockingDeque<>(),
        new File("test.gml"), "http://www.test.com/");

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

  @Test
  public void testNewNquadsExporterTaskIsRunningMethod() {
    NquadsExporterTask task = new NquadsExporterTask(new LinkedBlockingDeque<>(),
        new File("test.gml"), "http://www.test.com/");

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

  @Test
  public void testNewNquadsExporterTaskExportToNquadsFileFromJnlFileMethod() {
    File impFile = NquadsExporterTaskTestHelper.impFile;
    File nqFile = NquadsExporterTaskTestHelper.nqFile;
    NquadsExporterTask task = new NquadsExporterTask(new LinkedBlockingDeque<>(), impFile,
        "http://www.test.com/");

    try {
      Method exportToNquadsFileFromJnlFile = task.getClass()
          .getDeclaredMethod("exportToNquadsFileFromJnlFile", File.class);
      exportToNquadsFileFromJnlFile.setAccessible(true);

      try {
        exportToNquadsFileFromJnlFile.invoke(task, new File(""));
      } catch (InvocationTargetException e) {
        assertEquals(e.getTargetException().getClass(), JPSRuntimeException.class);
      }

      NquadsExporterTaskTestHelper.setUp();
      assertEquals(((File) exportToNquadsFileFromJnlFile.invoke(task, nqFile)).getAbsolutePath(),
          NquadsExporterTaskTestHelper.nqGzFile.getAbsolutePath());
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail();
    } finally {
      NquadsExporterTaskTestHelper.tearDown();
    }

  }

  @Test
  public void testNewNquadsExporterTaskGetLocalSourceUrlFromProjectCfgMethod() {
    NquadsExporterTask task = new NquadsExporterTask(new LinkedBlockingDeque<>(),
        NquadsExporterTaskTestHelper.impFile, "http://www.test.com/");

    try {
      Method getLocalSourceUrlFromProjectCfg = task.getClass()
          .getDeclaredMethod("getLocalSourceUrlFromProjectCfg", File.class);
      getLocalSourceUrlFromProjectCfg.setAccessible(true);

      try {
        getLocalSourceUrlFromProjectCfg.invoke(task, new File(""));
      } catch (InvocationTargetException e) {
        assertEquals(e.getTargetException().getClass(), JPSRuntimeException.class);
      }

      Files.copy(NquadsExporterTaskTestHelper.testNqFile.toPath(),
          NquadsExporterTaskTestHelper.nqGzFile.toPath());
      Files.copy(NquadsExporterTaskTestHelper.testProjFile.toPath(),
          NquadsExporterTaskTestHelper.projFile.toPath());

      NquadsExporterTaskTestHelper.setUp();

      assertEquals(
          getLocalSourceUrlFromProjectCfg.invoke(task, NquadsExporterTaskTestHelper.nqGzFile),
          "http://127.0.0.1:52066/blazegraph/namespace/tmpkb/sparql/");

    } catch (NoSuchMethodException | IllegalAccessException | IOException | InvocationTargetException e) {
      fail();
    } finally {
      NquadsExporterTaskTestHelper.tearDown();
    }

  }

  @Test
  public void testNewNquadsExporterTaskChangeUrlsInNQuadsFileMethod() {
    NquadsExporterTaskTestHelper.tearDown();
    String from = "127.0.0.1:52066";
    String to = "www.test.com";
    File nqGzFile = NquadsExporterTaskTestHelper.nqGzFile;
    NquadsExporterTask task = new NquadsExporterTask(new LinkedBlockingDeque<>(),
        NquadsExporterTaskTestHelper.impFile, "http://" + to + "/");

    try {
      Method changeUrlsInNQuadsFile = task.getClass()
          .getDeclaredMethod("changeUrlsInNQuadsFile", File.class, String.class, String.class);
      changeUrlsInNQuadsFile.setAccessible(true);

      try {
        changeUrlsInNQuadsFile.invoke(task, new File(""), from, to);
      } catch (InvocationTargetException e) {
        assertEquals(e.getTargetException().getClass().getSuperclass(), IOException.class);
      }

      try {
        Files.copy(NquadsExporterTaskTestHelper.testNqFile.toPath(), nqGzFile.toPath());
        changeUrlsInNQuadsFile.invoke(task, nqGzFile, from, to);
      } catch (InvocationTargetException e) {
        assertEquals(e.getTargetException().getClass().getSuperclass(), IOException.class);
      }

      String nQuads = FileUtils.readFileToString(nqGzFile, String.valueOf(StandardCharsets.UTF_8));
      assertEquals(nQuads.indexOf(to), -1);

      FileOutputStream fos = new FileOutputStream(nqGzFile.getAbsolutePath());

      GZIPOutputStream gzos = new GZIPOutputStream(fos);

      try {
        gzos.write(nQuads.getBytes());
      } finally {
        try {
          gzos.finish();
          gzos.close();
          fos.close();
        } catch (IOException e) {
          fail();
        }
      }

      File targetNqFile = (File) changeUrlsInNQuadsFile.invoke(task, nqGzFile, from, to);

      assertEquals(targetNqFile.getAbsolutePath(),
          NquadsExporterTaskTestHelper.nqFile.getAbsolutePath());

      nQuads = FileUtils.readFileToString(targetNqFile, String.valueOf(StandardCharsets.UTF_8));
      assertEquals(nQuads.indexOf(to), 8);

    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IOException e) {
      fail();
    } finally {
      NquadsExporterTaskTestHelper.tearDown();
    }
  }

  @Test
  public void testNewNquadsExporterTaskRunMethod() {
    String to = "www.test.com";
    NquadsExporterTask task = new NquadsExporterTask(new LinkedBlockingDeque<>(),
        NquadsExporterTaskTestHelper.impFile, "http://" + to + "/");

    try {
      Files.copy(NquadsExporterTaskTestHelper.testNqFile.toPath(),
          NquadsExporterTaskTestHelper.nqFile.toPath(), REPLACE_EXISTING);
      Files.copy(NquadsExporterTaskTestHelper.testPropFile.toPath(),
          NquadsExporterTaskTestHelper.propFile.toPath(), REPLACE_EXISTING);
      try {
        task.run();
      } catch (Exception e) {
        assertEquals(e.getClass(), JPSRuntimeException.class);
      } finally {
        NquadsExporterTaskTestHelper.tearDown();
      }
    } catch (IOException e) {
      fail();
    }

    try {
      Files.copy(NquadsExporterTaskTestHelper.testNqFile.toPath(),
          NquadsExporterTaskTestHelper.nqFile.toPath(), REPLACE_EXISTING);
      Files.copy(NquadsExporterTaskTestHelper.testPropFile.toPath(),
          NquadsExporterTaskTestHelper.propFile.toPath(), REPLACE_EXISTING);
      Files.copy(NquadsExporterTaskTestHelper.testProjFile.toPath(),
          NquadsExporterTaskTestHelper.projFile.toPath(), REPLACE_EXISTING);
      try {
        task.run();
      } catch (Exception e) {
        assertEquals(e.getClass(), JPSRuntimeException.class);
      } finally {
        NquadsExporterTaskTestHelper.tearDown();
      }
    } catch (IOException e) {
      fail();
    }

    NquadsExporterTaskTestHelper.setUp();

    try {
      Field stop = task.getClass().getDeclaredField("stop");
      Field nqQueue = task.getClass().getDeclaredField("nqQueue");
      stop.setAccessible(true);
      nqQueue.setAccessible(true);

      new Thread(task).start();

      while (!(boolean) stop.get(task)) {
        if (((BlockingQueue<?>) nqQueue.get(task)).size() > 0) {
          File targetNqFile = (File) ((BlockingQueue<?>) nqQueue.get(task)).take();
          assertEquals(targetNqFile.getAbsolutePath(),
              NquadsExporterTaskTestHelper.nqFile.getAbsolutePath());
          assertEquals(
              FileUtils.readFileToString(targetNqFile, String.valueOf(StandardCharsets.UTF_8))
                  .indexOf(to), 8);
          Method stopM = task.getClass().getDeclaredMethod("stop");
          stopM.setAccessible(true);
          stopM.invoke(task);
        }
      }

    } catch (NoSuchFieldException | IllegalAccessException | InterruptedException | IOException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    } finally {
      NquadsExporterTaskTestHelper.tearDown();
    }

  }

  public static class NquadsExporterTaskTestHelper {
    public static final String FS = System.getProperty("file.separator");
    public static final File impFile = new File(System.getProperty("java.io.tmpdir") + "test.gml");
    public static final File testFile = new File(Objects.requireNonNull(
        NquadsExporterTaskTestHelper.class.getClassLoader()
            .getResource("test.gml")).getFile());
    public static final File testNqFile = new File(Objects.requireNonNull(
        NquadsExporterTaskTestHelper.class.getClassLoader()
            .getResource("test.nq")).getFile());
    public static final File testProjFile = new File(Objects.requireNonNull(
        NquadsExporterTaskTestHelper.class.getClassLoader()
            .getResource("testproject.xml")).getFile());
    public static final File testPropFile = new File(Objects.requireNonNull(
        NquadsExporterTaskTestHelper.class.getClassLoader()
            .getResource("RWStore.properties")).getFile());
    public static final File nqFile = new File(
        impFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_GML, ImporterTask.EXT_FILE_NQUADS));
    public static final File nqGzFile = new File(
        nqFile.getAbsolutePath() + NquadsExporterTask.EXT_GZ);
    public static final File jnlFile = new File(
        impFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_GML, ImporterTask.EXT_FILE_JNL));
    public static final File propFile = new File(impFile.getAbsolutePath()
        .replace(ImporterTask.EXT_FILE_GML, BlazegraphServerTask.PROPERTY_FILE));
    public static final File projFile = new File(
        impFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_GML, ImporterTask.PROJECT_CONFIG));
    public static final File defaultJnlFile = new File(
            System.getProperty("user.dir").concat(FS + DEF_JOURNAL_NAME));
    public static final File quadsDir = new File(
        System.getProperty("java.io.tmpdir") + NquadsExporterTask.NQ_OUTDIR);
    public static final File splitDir = new File(
        System.getProperty("java.io.tmpdir") + CityImportAgent.KEY_SPLIT);

    public static void setUp() {
      assertTrue(testNqFile.exists());
      assertTrue(testPropFile.exists());
      assertTrue(testProjFile.exists());
      try {
        Files.copy(testNqFile.toPath(), nqFile.toPath(), REPLACE_EXISTING);
        Files.copy(testProjFile.toPath(), projFile.toPath(), REPLACE_EXISTING);

        Method setupFiles = BlazegraphServerTask.class.getDeclaredMethod("setupFiles", String.class);
        setupFiles.setAccessible(true);
        setupFiles.invoke(new BlazegraphServerTask(new LinkedBlockingDeque<>(), jnlFile.getAbsolutePath()), propFile.getAbsolutePath());

        String data = FileUtils.readFileToString(projFile, String.valueOf(Charset.defaultCharset()));
        data = data.replace("www.theworldavatar.com", "127.0.0.1");
        data = data.replace("/citieskg/namespace/berlin/sparql/", "/blazegraph/namespace/tmpkb/sparql/");
        data = data.replace("<port>83</port>", "<port>52066</port>");
        FileUtils.writeStringToFile(projFile, data);
      } catch (IOException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        fail();
      }

      String[] args = {"-namespace", "tmpkb",
          propFile.getAbsolutePath(), nqFile.getAbsolutePath()};

      try {
        DataLoader.main(args);
      } catch (IOException e) {
        fail();
      }

    }

    public static void tearDown() {
      System.gc();
      if (Objects.requireNonNull(impFile).isFile()) {
        if (!impFile.delete()) {
          fail();
        }
      }
      if (Objects.requireNonNull(nqFile).isFile()) {
        if (!nqFile.delete()) {
          fail();
        }
      }
      if (Objects.requireNonNull(nqGzFile).isFile()) {
        if (!nqGzFile.delete()) {
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
      if (Objects.requireNonNull(defaultJnlFile).isFile()) {
        if(!defaultJnlFile.delete()) {
          fail();
        }
      }
      if (Objects.requireNonNull(quadsDir).isDirectory()) {
        try {
          FileUtils.deleteDirectory(quadsDir);
        } catch (IOException e) {
          fail();
        }
      }
      if (Objects.requireNonNull(splitDir).isDirectory()) {
        try {
          FileUtils.deleteDirectory(splitDir);
        } catch (IOException e) {
          fail();
        }
      }
    }
  }

}
