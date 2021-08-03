package uk.ac.cam.cares.twa.cities.tasks.test;

import junit.framework.TestCase;
import uk.ac.cam.cares.twa.cities.tasks.NquadsExporterTask;

import java.io.File;
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
}
