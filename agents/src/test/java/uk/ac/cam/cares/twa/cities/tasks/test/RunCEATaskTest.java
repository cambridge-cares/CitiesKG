package uk.ac.cam.cares.twa.cities.tasks.test;

import junit.framework.TestCase;
import uk.ac.cam.cares.twa.cities.tasks.CEAInputData;
import uk.ac.cam.cares.twa.cities.tasks.RunCEATask;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;

public class RunCEATaskTest extends TestCase {
    public void testRunCEATask() {
        RunCEATask task;

        try {
            URI testURI = new URI("http://localhost/test");
            task = new RunCEATask(new CEAInputData("test","test"), testURI, 0);
            assertNotNull(task);
        } catch (Exception e) {
            fail();
        }
    }

    public void testRunCEATaskFields() {
        try {
        CEAInputData testData = new CEAInputData("test","test");
        URI testURI = new URI("http://localhost/test");
        RunCEATask task = new RunCEATask(testData, testURI, 0);

        assertEquals(6, task.getClass().getDeclaredFields().length);

        Field inputs;
        Field stop;
        Field SHAPEFILE_SCRIPT;
        Field WORKFLOW_SCRIPT;
        Field CREATE_WORKFLOW_SCRIPT;
        Field FS;


            inputs = task.getClass().getDeclaredField("inputs");
            inputs.setAccessible(true);
            assertEquals(inputs.get(task), testData);
            stop = task.getClass().getDeclaredField("stop");
            stop.setAccessible(true);
            assertFalse((boolean) stop.get(task));
            SHAPEFILE_SCRIPT = task.getClass().getDeclaredField("SHAPEFILE_SCRIPT");
            SHAPEFILE_SCRIPT.setAccessible(true);
            assertEquals(SHAPEFILE_SCRIPT.get(task), "create_shapefile.py");
            WORKFLOW_SCRIPT = task.getClass().getDeclaredField("WORKFLOW_SCRIPT");
            WORKFLOW_SCRIPT.setAccessible(true);
            assertEquals(WORKFLOW_SCRIPT.get(task), "workflow.yml");
            CREATE_WORKFLOW_SCRIPT = task.getClass().getDeclaredField("CREATE_WORKFLOW_SCRIPT");
            CREATE_WORKFLOW_SCRIPT.setAccessible(true);
            assertEquals(CREATE_WORKFLOW_SCRIPT.get(task), "create_cea_workflow.py");
            FS = task.getClass().getDeclaredField("FS");
            FS.setAccessible(true);
            assertEquals(FS.get(task), System.getProperty("file.separator"));

        } catch (NoSuchFieldException | IllegalAccessException | URISyntaxException e) {
            fail();
        }
    }

    public void testRunCEATaskMethods() {
        try{
        URI testURI = new URI("http://localhost/test");
        RunCEATask task = new RunCEATask(new CEAInputData("test","test"), testURI, 0);
        assertEquals(6, task.getClass().getDeclaredMethods().length);
        } catch (URISyntaxException e) {
            fail();
        }

    }

    public void testRunCEATaskStopMethod() {
        try {
            URI testURI = new URI("http://localhost/test");
            RunCEATask task = new RunCEATask(new CEAInputData("test","test"), testURI, 0);

            Field stopField = task.getClass().getDeclaredField("stop");
            stopField.setAccessible(true);
            assertFalse((Boolean) stopField.get(task));
            Method stopMethod = task.getClass().getDeclaredMethod("stop");
            stopMethod.setAccessible(true);
            stopMethod.invoke(task);
            assertTrue((Boolean) stopField.get(task));
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException | URISyntaxException e) {
            fail();
        }

    }
    public void testRunCEATaskDeleteDirectoryContentsMethod() {
        try {
            URI testURI = new URI("http://localhost/test");
            RunCEATask task = new RunCEATask(new CEAInputData("test","test"), testURI, 0);

            File myTempDir = new File(System.getProperty("java.io.tmpdir"));
            File newDirectory = new File(myTempDir, "new_directory");
            assertTrue(newDirectory.mkdir() || newDirectory.isDirectory());

            File tempFile = File.createTempFile("text", ".temp", newDirectory);

            Method deleteDirectoryContents = task.getClass().getDeclaredMethod("deleteDirectoryContents", File.class);
            deleteDirectoryContents.setAccessible(true);
            deleteDirectoryContents.invoke(task, newDirectory);

            assertFalse(tempFile.isFile());

        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | IOException | URISyntaxException e) {
            System.out.println(e.getMessage());
            fail();
        }


    }
}
