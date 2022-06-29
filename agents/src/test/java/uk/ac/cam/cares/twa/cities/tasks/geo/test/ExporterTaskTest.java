package uk.ac.cam.cares.twa.cities.tasks.geo.test;

import org.citydb.ImpExp;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import uk.ac.cam.cares.twa.cities.agents.geo.CityExportAgent;
import uk.ac.cam.cares.twa.cities.tasks.geo.ExporterTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class ExporterTaskTest {
    // set up task params
    public String FS = System.getProperty("file.separator");
    public String namespaceIri = "http://127.0.0.1:9999/blazegraph/namespace/test/sparql";
    public JSONObject serverInfo = new JSONObject().put("host", "127.0.0.1").put("port", "9999").put("namespace", "/blazegraph/namespace/test/sparql");
    public String srsname = "EPSG:25833";
    public String outputDir = System.getProperty("java.io.tmpdir") + "export";
    public String outputPath = outputDir + this.FS + "test.kml";
    public String[] displayMode = {"false", "true", "false", "false"};
    public int lod = 2;
    public String[] gmlIds = {"BLDG_123a"};
    public CityExportAgent.Params taskParams = new CityExportAgent.Params(namespaceIri, serverInfo, srsname, outputDir, outputPath, displayMode, lod, gmlIds);

    // for tear down of directories and files
    public File kmlDir = new File(this.outputDir + this.FS + "kmlFiles");
    public File exportFile = new File(kmlDir.getAbsolutePath() + this.FS + "test.kml");
    public File outputDirFile = new File(this.outputDir);
    public File cfgDir = new File(this.outputDir + this.FS + "cfgFiles");
    public File cfgFile = new File(this.cfgDir + this.FS + "test_project.xml");

    @Test
    public void testNewExporterTask() {
        try {
            ExporterTask task = new ExporterTask(this.taskParams);

            Field taskParams = task.getClass().getDeclaredField("taskParams");
            taskParams.setAccessible(true);
            Field lod = task.getClass().getDeclaredField("lod");
            lod.setAccessible(true);

            assertNotNull(task);
            assertEquals(this.taskParams, taskParams.get(task));
            assertEquals(this.lod, lod.get(task));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testNewExporterTaskFields() {
        try {
            ExporterTask task = new ExporterTask(this.taskParams);
            assertEquals(17, task.getClass().getDeclaredFields().length);

            assertEquals("project.xml", task.getClass().getDeclaredField("PROJECT_CONFIG").get(task));
            assertEquals(".kml", task.getClass().getDeclaredField("EXT_FILE_KML").get(task));
            assertEquals("{{gmlid}}", task.getClass().getDeclaredField("PLACEHOLDER_GMLID").get(task));
            assertEquals("{{host}}", task.getClass().getDeclaredField("PLACEHOLDER_HOST").get(task));
            assertEquals("{{port}}", task.getClass().getDeclaredField("PLACEHOLDER_PORT").get(task));
            assertEquals("{{lod}}", task.getClass().getDeclaredField("PLACEHOLDER_LOD").get(task));
            assertEquals("{{display1}}", task.getClass().getDeclaredField("PLACEHOLDER_FOOTPRINT").get(task));
            assertEquals("{{display2}}", task.getClass().getDeclaredField("PLACEHOLDER_EXTRUDED").get(task));
            assertEquals("{{display3}}", task.getClass().getDeclaredField("PLACEHOLDER_GEOMETRY").get(task));
            assertEquals("{{display4}}", task.getClass().getDeclaredField("PLACEHOLDER_COLLADA").get(task));
            assertEquals("{{namespace}}", task.getClass().getDeclaredField("PLACEHOLDER_NS").get(task));
            assertEquals("-shell", task.getClass().getDeclaredField("ARG_SHELL").get(task));
            assertEquals("-kmlExport", task.getClass().getDeclaredField("ARG_KMLEXPORT").get(task));
            assertEquals("-config", task.getClass().getDeclaredField("ARG_CFG").get(task));

            Field lod = task.getClass().getDeclaredField("lod");
            lod.setAccessible(true);
            assertEquals(this.lod, lod.get(task));
            Field stop = task.getClass().getDeclaredField("stop");
            stop.setAccessible(true);
            assertFalse((Boolean) stop.get(task));
            Field taskParams = task.getClass().getDeclaredField("taskParams");
            taskParams.setAccessible(true);
            assertEquals(this.taskParams, taskParams.get(task));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testNewExporterTaskMethods() {
        ExporterTask task = new ExporterTask(this.taskParams);
        assertEquals(7, task.getClass().getDeclaredMethods().length);
    }

    @Test
    public void testGetStopStatus() {
        try {
            ExporterTask task = new ExporterTask(this.taskParams);
            Field stop = task.getClass().getDeclaredField("stop");
            stop.setAccessible(true);

            assertFalse(task.getStopStatus());
            stop.set(task, true);
            assertTrue(task.getStopStatus());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testStop() {
        try {
            ExporterTask task = new ExporterTask(this.taskParams);
            Field stop = task.getClass().getDeclaredField("stop");
            stop.setAccessible(true);

            assertFalse((Boolean) stop.get(task));
            task.stop();
            assertTrue((Boolean) stop.get(task));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testIsRunning() {
        try {
            ExporterTask task = new ExporterTask(this.taskParams);
            Field stop = task.getClass().getDeclaredField("stop");
            stop.setAccessible(true);

            assertTrue(task.isRunning());
            stop.set(task, true);
            assertFalse(task.isRunning());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testRun() {
        tearDown();
        ExporterTask task = new ExporterTask(this.taskParams);
        assertTrue(task.isRunning());

        try (MockedStatic<ImpExp> mock = Mockito.mockStatic(ImpExp.class)) {
            task.run();
            assertTrue(this.cfgFile.exists());
            assertTrue(this.kmlDir.exists());
            mock.verify(() -> ImpExp.main(ArgumentMatchers.any()), Mockito.times(1));
            assertFalse(task.isRunning());
        } catch (Exception e) {
            fail();
        } finally {
            tearDown();
        }
    }

    @Test
    public void testSetupKmlPath() {
        tearDown();
        try {
            ExporterTask task = new ExporterTask(this.taskParams);
            Method setupKmlPath = task.getClass().getDeclaredMethod("setupKmlPath", CityExportAgent.Params.class);
            setupKmlPath.setAccessible(true);

            assertEquals(this.exportFile, setupKmlPath.invoke(task, this.taskParams));
            assertTrue(this.kmlDir.exists());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            fail();
        } finally {
            tearDown();
        }
    }

    @Test
    public void testSetupConfig() {
        tearDown();
        try {
            ExporterTask task = new ExporterTask(this.taskParams);
            Method setupConfig = task.getClass().getDeclaredMethod("setupConfig", CityExportAgent.Params.class);
            setupConfig.setAccessible(true);

            setupConfig.invoke(task, this.taskParams);
            assertTrue(this.cfgFile.exists());

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            XPath xpath = XPathFactory.newInstance().newXPath();
            Document cfg = builder.parse(this.cfgFile);

            assertEquals("BLDG_123a", ((Node) xpath.compile("/project/kmlExport/query/gmlIds/id").evaluate(cfg, XPathConstants.NODE)).getTextContent());
            assertEquals("127.0.0.1", ((Node) xpath.compile("/project/database/connections/connection/server").evaluate(cfg, XPathConstants.NODE)).getTextContent());
            assertEquals("9999", ((Node) xpath.compile("/project/database/connections/connection/port").evaluate(cfg, XPathConstants.NODE)).getTextContent());
            assertEquals("/blazegraph/namespace/test/sparql", ((Node) xpath.compile("/project/database/connections/connection/sid").evaluate(cfg, XPathConstants.NODE)).getTextContent());
            assertEquals("2", ((Node) xpath.compile("/project/kmlExport/lodToExportFrom").evaluate(cfg, XPathConstants.NODE)).getTextContent());
            assertEquals("false", ((Node) xpath.compile("/project/kmlExport/buildingDisplayForms/displayForm[form=4]/active").evaluate(cfg, XPathConstants.NODE)).getTextContent());
            assertEquals("false", ((Node) xpath.compile("/project/kmlExport/buildingDisplayForms/displayForm[form=3]/active").evaluate(cfg, XPathConstants.NODE)).getTextContent());
            assertEquals("true", ((Node) xpath.compile("/project/kmlExport/buildingDisplayForms/displayForm[form=2]/active").evaluate(cfg, XPathConstants.NODE)).getTextContent());
            assertEquals("false", ((Node) xpath.compile("/project/kmlExport/buildingDisplayForms/displayForm[form=1]/active").evaluate(cfg, XPathConstants.NODE)).getTextContent());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
            fail();
        } finally {
            tearDown();
        }
    }

    @Test
    public void testCreateGmlIds() {
        try {
            ExporterTask task = new ExporterTask(this.taskParams);
            Method createGmlids = task.getClass().getDeclaredMethod("createGmlids", String[].class);
            createGmlids.setAccessible(true);

            assertEquals("<id>BLDG_123a</id>\n", createGmlids.invoke(task, (Object) this.gmlIds));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            fail();
        }
    }

    public void tearDown() {
        if (Objects.requireNonNull(this.cfgFile).exists()) {
            if (!this.cfgFile.delete()) {
                fail();
            }
        }
        if (Objects.requireNonNull(this.cfgDir).exists()) {
            if (!this.cfgDir.delete()) {
                fail();
            }
        }
        if (Objects.requireNonNull(this.exportFile).exists()) {
            if (!this.exportFile.delete()) {
                fail();
            }
        }
        if (Objects.requireNonNull(this.kmlDir).exists()) {
            if (!this.kmlDir.delete()) {
                fail();
            }
        }
        if (Objects.requireNonNull(this.outputDirFile).exists()) {
            if (!this.outputDirFile.delete()) {
                fail();
            }
        }
    }
}