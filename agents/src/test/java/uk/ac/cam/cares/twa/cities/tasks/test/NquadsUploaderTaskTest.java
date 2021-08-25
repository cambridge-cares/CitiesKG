package uk.ac.cam.cares.twa.cities.tasks.test;

import junit.framework.TestCase;
import uk.ac.cam.cares.twa.cities.tasks.NquadsUploaderTask;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class NquadsUploaderTaskTest  extends TestCase {

    public void testNewNquadsUploaderTask() {
        NquadsUploaderTask task;

        try {
            task = new NquadsUploaderTask(new LinkedBlockingDeque<>(), new URI("http://www.test.com/"));
            assertNotNull(task);
        }  catch (Exception e) {
            fail();
        }
    }

    public void testNewNquadsUploaderTaskFields() {
        BlockingQueue<File> queue = new LinkedBlockingDeque<>();
        URI uri = null;
        try {
            uri = new URI("http://www.test.com/");
        } catch (URISyntaxException e) {
            fail();
        }
        NquadsUploaderTask task = new NquadsUploaderTask(queue, uri);
        assertEquals(4, task.getClass().getDeclaredFields().length);

        Field CTYPE_NQ ;
        Field stop;
        Field nqQueue;
        Field endpointUri;

        try {
            CTYPE_NQ = task.getClass().getDeclaredField("CTYPE_NQ");
            assertEquals(CTYPE_NQ.get(task), "text/x-nquads");
            stop = task.getClass().getDeclaredField("stop");
            stop.setAccessible(true);
            assertFalse((Boolean) stop.get(task));
            nqQueue = task.getClass().getDeclaredField("nqQueue");
            nqQueue.setAccessible(true);
            assertEquals(nqQueue.get(task), queue);
            endpointUri = task.getClass().getDeclaredField("endpointUri");
            endpointUri.setAccessible(true);
            assertEquals(endpointUri.get(task), uri);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }

    }

    public void testNewNquadsUploaderTaskMethods() {
        try {
            NquadsUploaderTask task = new NquadsUploaderTask(new LinkedBlockingDeque<>(), new URI("http://www.test.com/"));
            assertEquals(2, task.getClass().getDeclaredMethods().length);
        } catch (URISyntaxException e) {
            fail();
        }
    }

    public void testNewNquadsUploaderStopMethod() {
        try {
            NquadsUploaderTask task = new NquadsUploaderTask(new LinkedBlockingDeque<>(), new URI("http://www.test.com/"));
            Field stopF = task.getClass().getDeclaredField("stop");
            stopF.setAccessible(true);
            assertFalse((Boolean) stopF.get(task));
            Method stopM = task.getClass().getDeclaredMethod("stop");
            stopM.setAccessible(true);
            stopM.invoke(task);
            assertTrue((Boolean) stopF.get(task));
        } catch (URISyntaxException | NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            fail();
        }

    }

    public void testNewNquadsUploaderRunMethod() {
        //@Todo: implementation
    }

}
