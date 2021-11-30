package uk.ac.cam.cares.twa.cities.tasks.geo.test;

import junit.framework.TestCase;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.tasks.geo.ExporterTask;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class ExporterTaskTest extends TestCase {

    public String[] testgmlIds = {"abc"};
    public String outFileName = "/test.kml";
    public String outTmpDir = "java.io.tmpdir"; // Note: this path is dependent on the PC, e.g., C:\Users\Shiying\AppData\Local\Temp\test_extruded.kml
    public JSONObject testServerInfo = initialServerInfo();

    public JSONObject initialServerInfo() {
        JSONObject serverInfo = new JSONObject();
        serverInfo.put("host", "127.0.0.1");
        serverInfo.put("port", "9999");
        serverInfo.put("namespace", "/blazegraph/namespace/testdata/sparql");
        return serverInfo;
    }


    public void testNewExporterTask() {
        ExporterTask task;

        String[] gmlIds = testgmlIds;
        File outputFile = new File(System.getProperty(outTmpDir) + outFileName);
        String outputPath= outputFile.getAbsolutePath();

        try {
            task = new ExporterTask(gmlIds, outputPath, testServerInfo);
            assertNotNull(task);
        }  catch (Exception e) {
            fail();
        }
    }

    public void testNewExporterTaskFields() {

        // Setup of input parameters for the method
        String[] gmlIds = testgmlIds;
        File outputFile = new File(System.getProperty(outTmpDir) + outFileName);
        String outputPath = outputFile.getAbsolutePath();

        ExporterTask task = new ExporterTask(gmlIds, outputPath, testServerInfo);
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
            assertEquals("project.xml", PROJECT_CONFIG.get(task));
            PLACEHOLDER_GMLID = task.getClass().getDeclaredField("PLACEHOLDER_GMLID");
            assertEquals("{{gmlid}}", PLACEHOLDER_GMLID.get(task));
            ARG_SHELL = task.getClass().getDeclaredField("ARG_SHELL");
            assertEquals("-shell", ARG_SHELL.get(task));
            ARG_KMLEXPORT = task.getClass().getDeclaredField("ARG_KMLEXPORT");
            assertEquals("-kmlExport=", ARG_KMLEXPORT.get(task));
            ARG_CFG = task.getClass().getDeclaredField("ARG_CFG");
            assertEquals("-config=", ARG_CFG.get(task));
            inputs = task.getClass().getDeclaredField("inputs");
            inputs.setAccessible(true);
            assertEquals(gmlIds, inputs.get(task));
            outputpath = task.getClass().getDeclaredField("outputpath");
            outputpath.setAccessible(true);
            assertEquals(outputPath, outputpath.get(task));
            stop = task.getClass().getDeclaredField("stop");
            stop.setAccessible(true);
            assertFalse((boolean) stop.get(task));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }

    }


    public void testNewExporterTaskMethods() {

        String[] gmlIds = testgmlIds;
        File outputFile = new File(System.getProperty(outTmpDir) + outFileName);
        String outputPath= outputFile.getAbsolutePath();

        ExporterTask task = new ExporterTask(gmlIds, outputPath, testServerInfo);
        assertEquals(3, task.getClass().getDeclaredMethods().length);
    }


    public void testNewExporterTaskStopMethod() {

        String[] gmlIds = testgmlIds;
        File outputFile = new File(System.getProperty(outTmpDir) + outFileName);
        String outputPath= outputFile.getAbsolutePath();

        ExporterTask task = new ExporterTask(gmlIds, outputPath, testServerInfo);

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


    public void testNewExporterTaskSetupConfigMethod() {

        // Setup the CityExportAgent
        String[] gmlIds = testgmlIds;
        File outputFile = new File(System.getProperty(outTmpDir) + outFileName);
        String outputPath= outputFile.getAbsolutePath();
        ExporterTask task = new ExporterTask(gmlIds, outputPath, testServerInfo);

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
                assertEquals(IOException.class.getName(), e.getTargetException().getClass().getSuperclass().getSuperclass().getName());
            }

            assert modifiedConfigFile != null;
            assertEquals(configFile.getName(), modifiedConfigFile.getName());

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document cfg = builder.parse(modifiedConfigFile);
            assertEquals(gmlIds, cfg.getElementsByTagName("id").item(1).getTextContent());

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


    public void testNewExporterTaskRunMethod() {

        // Test with fake gmlId
        String[] exampleGmlIds = {"BLDG_000300000001c242"};

        // Setup the CityExportAgent
        File outputFile = new File(System.getProperty(outTmpDir) + outFileName);
        String outputPath = outputFile.getAbsolutePath();

        ExporterTask task = new ExporterTask(exampleGmlIds, outputPath, testServerInfo);
        String actualFilePath = outputPath.replace(".kml", "_extruded.kml");
        File actualFile = new File(actualFilePath);

        try {
            task.run();
        } catch (JPSRuntimeException e) {
            assertEquals(JPSRuntimeException.class, e.getClass());
        } finally {
            assertTrue(actualFile.exists());  // if the task is successfully executed, test_extruded.kml should be generated
        }

        // After the runnable, the
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        Document cfg;

        try {
            builder = factory.newDocumentBuilder();
            cfg = builder.parse(actualFile);
            assertEquals(1, cfg.getElementsByTagName("coordinates").getLength());   // if the request gmlId exists in the database, the exported kml should contain "coordinates" information. Otherwise not
        } catch (ParserConfigurationException | IOException | SAXException e) {
            fail();
        } finally {
            if (!actualFile.delete()) { // If the actualFile is not generated, it means Runnable fails
                fail();
            }
        }

    }

}