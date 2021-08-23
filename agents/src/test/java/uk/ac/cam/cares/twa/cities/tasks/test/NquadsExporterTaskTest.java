package uk.ac.cam.cares.twa.cities.tasks.test;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Server;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.tasks.BlazegraphServerTask;
import uk.ac.cam.cares.twa.cities.tasks.ImporterTask;
import uk.ac.cam.cares.twa.cities.tasks.NquadsExporterTask;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

public class NquadsExporterTaskTest  extends TestCase {

    public void testNewNquadsExporterTask() {
        NquadsExporterTask task;

        try {
            task = new NquadsExporterTask(new LinkedBlockingDeque<>(), new File("test.gml"), "http://www.test.com/");
            assertNotNull(task);
        }  catch (Exception e) {
            fail();
        }
    }

    public void testNewNquadsExporterTaskFields() {
        BlockingQueue<File> queue = new LinkedBlockingDeque<>();
        File impFile = new File("test.gml");
        String url = "http://www.test.com/";
        NquadsExporterTask task = new NquadsExporterTask(queue, impFile, url);
        assertEquals(15, task.getClass().getDeclaredFields().length);

        Field ARG_OUTDIR;
        Field ARG_FORMAT;
        Field NQ_OUTDIR;
        Field NQ_FORMAT;
        Field NQ_FILENAME;
        Field EX_PROP_FILENAME;
        Field EXT_GZ;
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

    public void testNewNquadsExporterTaskMethods() {
        NquadsExporterTask task = new NquadsExporterTask(new LinkedBlockingDeque<>(), new File("test.gml"), "http://www.test.com/");
        assertEquals(5, task.getClass().getDeclaredMethods().length);
    }

    public void testNewNquadsExporterTaskStopMethod() {
        NquadsExporterTask task = new NquadsExporterTask(new LinkedBlockingDeque<>(), new File("test.gml"), "http://www.test.com/");

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

    public void testNewNquadsExporterTaskExportToNquadsFileFromJnlFileMethod() {
        File impFile = new File(System.getProperty("java.io.tmpdir") + "test.gml");
        File nqFile = new File(impFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_GML, ImporterTask.EXT_FILE_NQUADS));
        NquadsExporterTask task = new NquadsExporterTask(new LinkedBlockingDeque<>(), impFile, "http://www.test.com/");

        try {
            Method exportToNquadsFileFromJnlFile = task.getClass().getDeclaredMethod("exportToNquadsFileFromJnlFile", File.class);
            exportToNquadsFileFromJnlFile.setAccessible(true);

            try {
                exportToNquadsFileFromJnlFile.invoke(task, new File(""));
            } catch (InvocationTargetException e) {
                assertEquals(e.getTargetException().getClass(), JPSRuntimeException.class);
            }

            NquadsExporterTaskTestHelper.setUp();
            assertEquals(((File) exportToNquadsFileFromJnlFile.invoke(task, nqFile)).getAbsolutePath(), nqFile.getAbsolutePath() + NquadsExporterTask.EXT_GZ);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            fail();
        } finally {
            NquadsExporterTaskTestHelper.tearDown();
        }

    }

    public void testNewNquadsExporterTaskGetLocalSourceUrlFromProjectCfgMethod() {

    }

    public void testNewNquadsExporterTaskChangeUrlsInNQuadsFileMethod() {

    }

    public void testNewNquadsExporterTaskRunMethod() {

    }


    public static class NquadsExporterTaskTestHelper {
        public static final File impFile = new File(System.getProperty("java.io.tmpdir") + "test.gml");
        public static final File testFile = new File(Objects.requireNonNull(NquadsExporterTaskTest.NquadsExporterTaskTestHelper.class.getClassLoader().getResource("test.gml")).getFile());
        public static final File nqFile = new File(impFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_GML, ImporterTask.EXT_FILE_NQUADS));
        public static final File jnlFile = new File(impFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_GML, ImporterTask.EXT_FILE_JNL));
        public static final File propFile = new File(impFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_GML, BlazegraphServerTask.PROPERTY_FILE));
        public static final File projFile = new File(impFile.getAbsolutePath().replace(ImporterTask.EXT_FILE_GML, ImporterTask.PROJECT_CONFIG));
        public static final File quadsDir = new File(System.getProperty("java.io.tmpdir") + NquadsExporterTask.NQ_OUTDIR);


            public static void setUp() {
                assertTrue(testFile.exists());
                try {
                    Files.copy(testFile.toPath(), impFile.toPath());
                } catch (IOException e) {
                    fail();
                }
                ExecutorService serverExecutor = Executors.newFixedThreadPool(1);
                BlockingQueue<Server> importQueue = new LinkedBlockingDeque<>();
                BlazegraphServerTask serverTask = new BlazegraphServerTask(importQueue, jnlFile.getAbsolutePath());
                serverExecutor.execute(serverTask);

                ImporterTask task = new ImporterTask(importQueue, impFile);

                try {
                    Field stop = task.getClass().getDeclaredField("stop");
                    Field serverInstances = task.getClass().getDeclaredField("serverInstances");
                    stop.setAccessible(true);
                    serverInstances.setAccessible(true);
                    new Thread(task).start();

                    while (!(boolean) stop.get(task)) {
                        if (((BlockingQueue<?>) serverInstances.get(task)).size() == 0) {
                            if (Objects.requireNonNull(nqFile).isFile()) {
                                assertTrue(nqFile.exists());
                            }
                        }
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    fail();
                }
            }

            public static void tearDown() {
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
                if (Objects.requireNonNull(quadsDir).isDirectory()) {
                    try {
                        FileUtils.deleteDirectory(quadsDir);
                    } catch (IOException e) {
                        fail();
                    }
                }
            }
        }



}
