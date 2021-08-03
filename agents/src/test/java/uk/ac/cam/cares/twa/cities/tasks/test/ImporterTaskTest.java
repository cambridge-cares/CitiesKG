package uk.ac.cam.cares.twa.cities.tasks.test;

import junit.framework.TestCase;
import uk.ac.cam.cares.twa.cities.tasks.ImporterTask;

import java.io.File;
import java.util.concurrent.LinkedBlockingDeque;

public class ImporterTaskTest extends TestCase {

    public void testNewImporterTask() {
        ImporterTask task;

        try {
            task = new ImporterTask(new LinkedBlockingDeque<>(), new File("test.gml"));
            assertNotNull(task);
        }  catch (Exception e) {
            fail();
        }
    }
}
