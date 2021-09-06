package uk.ac.cam.cares.twa.cities.tasks.test;

import junit.framework.TestCase;
import org.citydb.event.EventDispatcher;
import org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.tasks.ExporterTask;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

class ExporterTaskTest extends TestCase {

    public String testgmlIds = "abc";
    public String outFileName = "/test.kml";
    public String outTmpDir = "java.io.tmpdir"; // Note: this path is dependent on the PC, e.g., C:\Users\Shiying\AppData\Local\Temp\test_extruded.kml
    //public final ExecutorService exporterExecutor = Executors.newFixedThreadPool(1);
    //private final ThreadPoolExecutor exporterExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);


    @Test
    public void testNewExporterTask() {
        ExporterTask task;

        String gmlIds = testgmlIds;
        File outputFile = new File(System.getProperty(outTmpDir) + outFileName);
        String outputPath= outputFile.getAbsolutePath();

        try {
            task = new ExporterTask(gmlIds, outputPath);
            assertNotNull(task);
        }  catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewExporterTaskFields() {

        // Setup of input parameters for the method
        String gmlIds = testgmlIds;
        File outputFile = new File(System.getProperty(outTmpDir) + outFileName);
        String outputPath = outputFile.getAbsolutePath();

        ExporterTask task = new ExporterTask(gmlIds, outputPath);
        assertEquals(8, task.getClass().getDeclaredFields().length);

        Field PROJECT_CONFIG;
        Field inputs;
        Field outputpath;
        Field stop;
        Field PLACEHOLDER_GMLID;
        Field ARG_SHELL;
        Field ARG_KMLEXPORT;
        Field ARG_CFG;

        try {
            PROJECT_CONFIG = task.getClass().getDeclaredField("PROJECT_CONFIG");
            assertEquals(PROJECT_CONFIG.get(task), "project.xml");
            PLACEHOLDER_GMLID = task.getClass().getDeclaredField("PLACEHOLDER_GMLID");
            assertEquals(PLACEHOLDER_GMLID.get(task), "{{gmlid}}");
            ARG_SHELL = task.getClass().getDeclaredField("ARG_SHELL");
            assertEquals(ARG_SHELL.get(task), "-shell");
            ARG_KMLEXPORT = task.getClass().getDeclaredField("ARG_KMLEXPORT");
            assertEquals(ARG_KMLEXPORT.get(task), "-kmlExport=");
            ARG_CFG = task.getClass().getDeclaredField("ARG_CFG");
            assertEquals(ARG_CFG.get(task), "-config=");
            inputs = task.getClass().getDeclaredField("inputs");
            inputs.setAccessible(true);
            assertEquals(inputs.get(task), gmlIds);
            outputpath = task.getClass().getDeclaredField("outputpath");
            outputpath.setAccessible(true);
            assertEquals(outputpath.get(task), outputPath);
            stop = task.getClass().getDeclaredField("stop");
            stop.setAccessible(true);
            assertFalse((boolean) stop.get(task));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }

    }

    @Test
    public void testNewExporterTaskMethods() {

        String gmlIds = testgmlIds;
        File outputFile = new File(System.getProperty(outTmpDir) + outFileName);
        String outputPath= outputFile.getAbsolutePath();

        ExporterTask task = new ExporterTask(gmlIds, outputPath);
        assertEquals(3, task.getClass().getDeclaredMethods().length);
    }

    @Test
    public void testNewExporterTaskStopMethod() {

        String gmlIds = testgmlIds;
        File outputFile = new File(System.getProperty(outTmpDir) + outFileName);
        String outputPath= outputFile.getAbsolutePath();

        ExporterTask task = new ExporterTask(gmlIds, outputPath);

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
    public void testNewExporterTaskSetupConfigMethod() {

        // Setup the CityExportAgent
        String gmlIds = testgmlIds;
        File outputFile = new File(System.getProperty(outTmpDir) + outFileName);
        String outputPath= outputFile.getAbsolutePath();
        ExporterTask task = new ExporterTask(gmlIds, outputPath);

        // Working with tempFolder and tempFile
        File configFile = new File(System.getProperty(outTmpDir) + "/testproject.xml");  //initialize File object and passing path as argument but the file is not created yet
        File modifiedConfigFile = null;

        try {
            // test the modified gmlids with the expected gmlids
            Method setupConfig = task.getClass().getDeclaredMethod("setupConfig");
            setupConfig.setAccessible(true);
            try {
                modifiedConfigFile = (File) setupConfig.invoke(task);
            } catch (InvocationTargetException e) {
                assertEquals(e.getTargetException().getClass().getSuperclass().getSuperclass().getName(), IOException.class.getName());
            }

            assertEquals(modifiedConfigFile.getName(), configFile.getName());

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document cfg = builder.parse(modifiedConfigFile);
            assertEquals(cfg.getElementsByTagName("id").item(1).getTextContent(), gmlIds);

        } catch (NoSuchMethodException | IllegalAccessException | SAXException | IOException | ParserConfigurationException e) {
            fail();
        } finally {
            if (Objects.requireNonNull(configFile).isFile()) {
                if (!configFile.delete()) {
                    fail();
                }
            }
        }
    }

    @Test
    public void testNewExporterTaskRunMethod() {

        // Test with fake gmlId
        String exampleGmlIds = "BLDG_000300000001c242";

        // Setup the CityExportAgent
        File outputFile = new File(System.getProperty(outTmpDir) + outFileName);
        String outputPath = outputFile.getAbsolutePath();

        ExporterTask task = new ExporterTask(exampleGmlIds, outputPath);
        String actualFilePath = outputPath.replace(".kml", "_extruded.kml");
        File actualFile = new File(actualFilePath);

        try {
            task.run();
        } catch (JPSRuntimeException e) {
            assertEquals(e.getClass(), JPSRuntimeException.class);
        } finally {
            assertTrue(actualFile.exists());  // if the task is successfully executed, test_extruded.kml should be generated
        }

        // After the runnable, the
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        Document cfg = null;

        try {
            builder = factory.newDocumentBuilder();
            cfg = builder.parse(actualFile);
            assertEquals(cfg.getElementsByTagName("coordinates").getLength(), 1);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            fail();
        } finally {
            // delete the temporally generated file
            if (Files.exists(Paths.get(actualFilePath))) {
                actualFile.delete();
            }
        }

    }

}