package uk.ac.cam.cares.twa.cities.tasks.test;

import junit.framework.TestCase;
import uk.ac.cam.cares.twa.cities.tasks.NquadsUploaderTask;

import java.net.URI;
import java.util.concurrent.LinkedBlockingDeque;

public class NquadsUploaderTaskTest  extends TestCase {

    public void testNewNquadsExporterTask() {
        NquadsUploaderTask task;

        try {
            task = new NquadsUploaderTask(new LinkedBlockingDeque<>(), new URI("http://www.test.com/"));
            assertNotNull(task);
        }  catch (Exception e) {
            fail();
        }
    }
}
