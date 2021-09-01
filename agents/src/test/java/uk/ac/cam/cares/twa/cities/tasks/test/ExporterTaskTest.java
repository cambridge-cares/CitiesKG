package uk.ac.cam.cares.twa.cities.tasks.test;

import junit.framework.TestCase;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import uk.ac.cam.cares.twa.cities.tasks.ExporterTask;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Objects;

import org.junit.jupiter.api.Assertions.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

class ExporterTaskTest extends TestCase {

    public String testgmlIds = "abc";

    @Test
    public void testNewExporterTask() {
        ExporterTask task;

        String gmlIds = testgmlIds;
        File outputFile = new File(System.getProperty("java.io.tmpdir") + "/test.kml");
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
        File outputFile = new File(System.getProperty("java.io.tmpdir") + "/test.kml");
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
        File outputFile = new File(System.getProperty("java.io.tmpdir") + "/test.kml");
        String outputPath= outputFile.getAbsolutePath();

        ExporterTask task = new ExporterTask(gmlIds, outputPath);
        assertEquals(3, task.getClass().getDeclaredMethods().length);
    }

    @Test
    public void testNewImporterTaskStopMethod() {

        String gmlIds = testgmlIds;
        File outputFile = new File(System.getProperty("java.io.tmpdir") + "/test.kml");
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
    public void testNewExporterTaskSetupFilesMethod() {

        // Setup the CityExportAgent
        String gmlIds = testgmlIds;
        File outputFile = new File(System.getProperty("java.io.tmpdir") + "/test.kml");
        String outputPath= outputFile.getAbsolutePath();
        ExporterTask task = new ExporterTask(gmlIds, outputPath);

        // Working with tempFolder and tempFile
        File configFile = new File(System.getProperty("java.io.tmpdir") + "/testproject.xml");  //initialize File object and passing path as argument but the file is not created yet
        File modifiedConfigFile = null;
        String localhostUriStr = "http://localhost:8081/";

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
    public void testNewImporterTaskRunMethod() {
        // Setup the CityExportAgent
        String gmlIds = testgmlIds;
        File outputFile = new File(System.getProperty("java.io.tmpdir") + "/test.kml");
        String outputPath= outputFile.getAbsolutePath();
        ExporterTask task = new ExporterTask(gmlIds, outputPath);


    }

}