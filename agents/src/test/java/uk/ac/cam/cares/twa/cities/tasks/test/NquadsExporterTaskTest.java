package uk.ac.cam.cares.twa.cities.tasks.test;

import junit.framework.TestCase;
import uk.ac.cam.cares.twa.cities.tasks.NquadsExporterTask;

import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.BlockingQueue;
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

}
