package uk.ac.cam.cares.twa.cities.tasks.test;

import com.bigdata.rdf.sail.webapp.NanoSparqlServer;
import junit.framework.TestCase;
import org.eclipse.jetty.server.Server;
import uk.ac.cam.cares.twa.cities.tasks.BlazegraphServerTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingDeque;

public class BlazegraphServerTaskTest  extends TestCase {

    public void testNewBlazegraphServerTask() {
        BlazegraphServerTask task;

        try {
            task = new BlazegraphServerTask(new LinkedBlockingDeque<>(), "test.jnl");
            assertNotNull(task);
        }  catch (Exception e) {
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
        assertEquals(6, task.getClass().getDeclaredMethods().length);
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

    public void testNewBlazegraphServerTaskSetupPathsMethod() {
        BlazegraphServerTask task = new BlazegraphServerTask(new LinkedBlockingDeque<>(), "/test/test.jnl");

        try {
            Field PROPERTY_FILE = task.getClass().getDeclaredField("PROPERTY_FILE");
            PROPERTY_FILE.setAccessible(true);
            Method setupPaths = task.getClass().getDeclaredMethod("setupPaths");
            setupPaths.setAccessible(true);
            assertEquals(setupPaths.invoke(task), "/test/test" + PROPERTY_FILE.get(task));

        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            fail();
        }
    }

    public void testNewBlazegraphServerTaskSetupFilesMethod() {
        BlazegraphServerTask task = new BlazegraphServerTask(new LinkedBlockingDeque<>(), "/test/test.jnl");
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
            prop.load(new FileInputStream(propFile));
            assertEquals(prop.getProperty("com.bigdata.journal.AbstractJournal.file"), journalPath.get(task));

            try {
                setupFiles.invoke(task, propFilePath + fs + propFile.getName());
                fail();
            } catch (InvocationTargetException e) {
                assertEquals(e.getTargetException().getClass().getSuperclass().getName(), IOException.class.getName());
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
            assertTrue(System.getProperty(NanoSparqlServer.SystemProperties.JETTY_XML).contains(".jar!/jetty.xml"));
            assertEquals(System.getProperty(NanoSparqlServer.SystemProperties.BIGDATA_PROPERTY_FILE), jnlPath);

        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            fail();
        }
    }

    public void testNewBlazegraphServerTaskSetupServerMethod() {
        String jnlPath = System.getProperty("java.io.tmpdir") + "test.jnl";
        BlazegraphServerTask task = new BlazegraphServerTask(new LinkedBlockingDeque<>(), jnlPath);
        String jettyCfg = "jetty.xml";

        try {
            Method setupServer = task.getClass().getDeclaredMethod("setupServer", String.class, String.class);
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
        }

    }

}
