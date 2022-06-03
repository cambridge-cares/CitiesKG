package uk.ac.cam.cares.twa.cities.tasks.geo.test;

import org.citydb.ImpExp;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import uk.ac.cam.cares.twa.cities.tasks.geo.ExporterTask;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static org.junit.jupiter.api.Assertions.*;

public class ExporterTaskTest {

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

    @Test
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

    @Test
    public void testNewExporterTaskFields() {

        // Setup of input parameters for the method
        String[] gmlIds = testgmlIds;
        File outputFile = new File(System.getProperty(outTmpDir) + outFileName);
        String outputPath = outputFile.getAbsolutePath();

        ExporterTask task = new ExporterTask(gmlIds, outputPath, testServerInfo);
        assertEquals(13, task.getClass().getDeclaredFields().length);

        Field PROJECT_CONFIG;
        Field EXT_FILE_KML;
        Field PLACEHOLDER_HOST;
        Field PLACEHOLDER_PORT;
        Field PLACEHOLDER_NS;
        Field inputs;
        Field outputpath;
        Field serverinfo;
        Field stop;
        Field PLACEHOLDER_GMLID;
        Field ARG_SHELL;
        Field ARG_KMLEXPORT;
        Field ARG_CFG;

        try {
            PROJECT_CONFIG = task.getClass().getDeclaredField("PROJECT_CONFIG");
            assertEquals("project.xml", PROJECT_CONFIG.get(task));
            EXT_FILE_KML = task.getClass().getDeclaredField("EXT_FILE_KML");
            assertEquals(".kml", EXT_FILE_KML.get(task));
            PLACEHOLDER_GMLID = task.getClass().getDeclaredField("PLACEHOLDER_GMLID");
            assertEquals("{{gmlid}}", PLACEHOLDER_GMLID.get(task));
            PLACEHOLDER_HOST = task.getClass().getDeclaredField("PLACEHOLDER_HOST");
            assertEquals("{{host}}", PLACEHOLDER_HOST.get(task));
            PLACEHOLDER_PORT = task.getClass().getDeclaredField("PLACEHOLDER_PORT");
            assertEquals("{{port}}", PLACEHOLDER_PORT.get(task));
            PLACEHOLDER_NS = task.getClass().getDeclaredField("PLACEHOLDER_NS");
            assertEquals("{{namespace}}", PLACEHOLDER_NS.get(task));
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
            serverinfo = task.getClass().getDeclaredField("serverinfo");
            serverinfo.setAccessible(true);
            assertEquals(testServerInfo, serverinfo.get(task));
            stop = task.getClass().getDeclaredField("stop");
            stop.setAccessible(true);
            assertFalse((boolean) stop.get(task));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }

    }

    @Test
    public void testNewExporterTaskMethods() {

        String[] gmlIds = testgmlIds;
        File outputFile = new File(System.getProperty(outTmpDir) + outFileName);
        String outputPath= outputFile.getAbsolutePath();

        ExporterTask task = new ExporterTask(gmlIds, outputPath, testServerInfo);
        assertEquals(4, task.getClass().getDeclaredMethods().length);
    }

    @Test
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

    @Test
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
                fail();
                //assertEquals(IOException.class.getName(), e.getTargetException().getClass().getSuperclass().getSuperclass().getName());
            }

            assert modifiedConfigFile != null;
            assertEquals(configFile.getName(), modifiedConfigFile.getName());

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document cfg = builder.parse(modifiedConfigFile);
            assertEquals(gmlIds[0], cfg.getElementsByTagName("id").item(1).getTextContent());

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
        File testKml = new File(
                Objects.requireNonNull(this.getClass().getResource("/testoutput.kml")).getFile());

        // Test with fake gmlId
        String[] exampleGmlIds = {"BLDG_000300000001c242"};

        // Setup the CityExportAgent
        File outputFile = new File(System.getProperty(outTmpDir) + outFileName);
        String outputPath = outputFile.getAbsolutePath();

        ExporterTask task = new ExporterTask(exampleGmlIds, outputPath, testServerInfo);
        String actualFilePath = outputPath.replace(".kml", "_extruded.kml");
        File actualFile = new File(actualFilePath);

        try {
            Field stop = task.getClass().getDeclaredField("stop");
            stop.setAccessible(true);
            try (MockedStatic<ImpExp> mock = Mockito.mockStatic(ImpExp.class)) {
                mock.when(() -> ImpExp.main(ArgumentMatchers.any())).thenAnswer((Answer<Void>) invocation -> {
                    Files.copy(testKml.toPath(), actualFile.toPath());
                    return null;
                });
                task.run();
                assertTrue((Boolean) stop.get(task));
                assertTrue(actualFile.exists());  // if the task is successfully executed, test_extruded.kml should be generated
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        } finally {
            if (!actualFile.delete()) {
                fail();
            }
            NquadsExporterTaskTest.NquadsExporterTaskTestHelper.tearDown();
        }

    }

}