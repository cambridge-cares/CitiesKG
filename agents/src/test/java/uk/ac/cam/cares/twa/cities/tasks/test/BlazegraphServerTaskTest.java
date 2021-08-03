package uk.ac.cam.cares.twa.cities.tasks.test;

import junit.framework.TestCase;
import uk.ac.cam.cares.twa.cities.tasks.BlazegraphServerTask;

import java.lang.reflect.Field;
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
        assertEquals(12, task.getClass().getDeclaredFields().length);
        Field PROPERTY_FILE;
        Field PROPERTY_FILE_PATH;
        Field JETTY_CFG_PATH;
        Field WAR_PATH;
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
            PROPERTY_FILE_PATH = task.getClass().getDeclaredField("PROPERTY_FILE_PATH");
            PROPERTY_FILE_PATH.setAccessible(true);
            JETTY_CFG_PATH = task.getClass().getDeclaredField("JETTY_CFG_PATH");
            JETTY_CFG_PATH.setAccessible(true);
            WAR_PATH = task.getClass().getDeclaredField("WAR_PATH");
            WAR_PATH.setAccessible(true);
            SYS_PROP_JETTY = task.getClass().getDeclaredField("SYS_PROP_JETTY");
            SYS_PROP_JETTY.setAccessible(true);
            NAMESPACE = task.getClass().getDeclaredField("NAMESPACE");
            NAMESPACE.setAccessible(true);
            DEF_JOURNAL_NAME = task.getClass().getDeclaredField("DEF_JOURNAL_NAME");
            DEF_JOURNAL_NAME.setAccessible(true);
            FS = task.getClass().getDeclaredField("FS");
            FS.setAccessible(true);
            journalPath = task.getClass().getDeclaredField("journalPath");
            journalPath.setAccessible(true);
            queue = task.getClass().getDeclaredField("queue");
            queue.setAccessible(true);
            stop = task.getClass().getDeclaredField("stop");
            stop.setAccessible(true);
            server = task.getClass().getDeclaredField("server");
            server.setAccessible(true);
        } catch (NoSuchFieldException e) {
            fail();
        }
    }

}
