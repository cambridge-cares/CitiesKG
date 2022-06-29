package uk.ac.cam.cares.twa.cities.tasks;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class KMLTilingTaskTest {
    // set up new KMLTilingTask
    public String path2unsortedKML = System.getProperty("java.io.tmpdir") + "unsorted";
    public String path2sortedKML = System.getProperty("java.io.tmpdir") + "sorted";
    public int databaseCRS = 4326;
    public String displayForm = "extruded";
    public String namespaceIri = "http://127.0.0.1:9999/blazegraph/namespace/test/sparql";

    // for tear down of directories and files
    public String FS = System.getProperty("file.separator");
    public File unsortedDir = new File(this.path2unsortedKML);
    public File sortedDir = new File(this.path2sortedKML);
    public File unsortedKmlFile = new File(unsortedDir.getAbsolutePath() + this.FS + "testoutput.kml");
    public File summcsv = new File(this.sortedDir + this.FS + "summary_1.csv");

    @Before
    public void before() {
        tearDown();
    }

    @AfterEach
    public void tearDown() {
        if (Objects.requireNonNull(this.summcsv.exists())) {
            if (!this.summcsv.delete()) {
                fail();
            }
        }
        if (Objects.requireNonNull(this.unsortedKmlFile.exists())) {
            if (!this.unsortedKmlFile.delete()) {
                fail();
            }
        }
        if (Objects.requireNonNull(this.unsortedDir.exists())) {
            if (!this.unsortedDir.delete()) {
                fail();
            }
        }
        if (Objects.requireNonNull(this.sortedDir.exists())) {
            if (!this.sortedDir.delete()) {
                fail();
            }
        }
    }

    @Test
    public void testNewKMLTilingTask() {
        // test case where incorrect display form, should throw IllegalArgumentException
        try {
            new KMLTilingTask(this.path2unsortedKML, this.path2sortedKML, this.databaseCRS, "display", this.namespaceIri);
        } catch (Exception e) {
            assert e instanceof IllegalArgumentException;
        }

        // test case where constructor successfully created
        try {
            new KMLTilingTask(this.path2unsortedKML, this.path2sortedKML, this.databaseCRS, this.displayForm, this.namespaceIri);
            assertTrue(this.sortedDir.exists());
        } catch (Exception e) {
            fail();
        }
        // assignment of values to fields tested in testNewKMLTilingTaskFields
    }

    @Test
    public void testNewKMLTilingTaskFields() {
        try {
            KMLTilingTask task = new KMLTilingTask(this.path2unsortedKML, this.path2sortedKML, this.databaseCRS, this.displayForm, this.namespaceIri);
            assertEquals(36, task.getClass().getDeclaredFields().length);

            Field crsWGS84 = task.getClass().getDeclaredField("crsWGS84");
            crsWGS84.setAccessible(true);
            assertEquals(4326, crsWGS84.get(task));
            Field initTileSize = task.getClass().getDeclaredField("initTileSize");
            initTileSize.setAccessible(true);
            assertEquals(250, initTileSize.get(task));

            Field CRSinMeter = task.getClass().getDeclaredField("CRSinMeter");
            CRSinMeter.setAccessible(true);
            assertEquals(4326, CRSinMeter.get(task));
            Field unsortedKMLdir = task.getClass().getDeclaredField("unsortedKMLdir");
            unsortedKMLdir.setAccessible(true);
            assertEquals(this.path2unsortedKML, unsortedKMLdir.get(task));
            Field sortedKMLdir = task.getClass().getDeclaredField("sortedKMLdir");
            sortedKMLdir.setAccessible(true);
            assertEquals(this.path2sortedKML, sortedKMLdir.get(task));
            Field displayForm = task.getClass().getDeclaredField("displayForm");
            displayForm.setAccessible(true);
            assertEquals(this.displayForm, displayForm.get(task));

            Field outCsvName = task.getClass().getDeclaredField("outCsvName");
            outCsvName.setAccessible(true);
            assertEquals("summary", outCsvName.get(task));
            Field underScore = task.getClass().getDeclaredField("underScore");
            underScore.setAccessible(true);
            assertEquals("_", underScore.get(task));
            Field outFileExt = task.getClass().getDeclaredField("outFileExt");
            outFileExt.setAccessible(true);
            assertEquals(".csv", outFileExt.get(task));
            Field outputDir = task.getClass().getDeclaredField("outputDir");
            outputDir.setAccessible(true);
            assertEquals(this.path2sortedKML, outputDir.get(task));
            assertEquals(0, ((ArrayList<String[]>) task.getClass().getDeclaredField("dataContent").get(task)).size());
            Field gmlidList = task.getClass().getDeclaredField("gmlidList");
            gmlidList.setAccessible(true);
            assertEquals(0, ((ArrayList<String>) gmlidList.get(task)).size());
            Field parserCSV = task.getClass().getDeclaredField("parserCSV");
            parserCSV.setAccessible(true);
            assertNull(parserCSV.get(task));

            Field extent_Xmin = task.getClass().getDeclaredField("extent_Xmin");
            extent_Xmin.setAccessible(true);
            assertEquals(Double.POSITIVE_INFINITY, extent_Xmin.get(task));
            Field extent_Xmax = task.getClass().getDeclaredField("extent_Xmax");
            extent_Xmax.setAccessible(true);
            assertEquals(Double.NEGATIVE_INFINITY, extent_Xmax.get(task));
            Field extent_Ymin = task.getClass().getDeclaredField("extent_Ymin");
            extent_Ymin.setAccessible(true);
            assertEquals(Double.POSITIVE_INFINITY, extent_Ymin.get(task));
            Field extent_Ymax = task.getClass().getDeclaredField("extent_Ymax");
            extent_Ymax.setAccessible(true);
            assertEquals(Double.NEGATIVE_INFINITY, extent_Ymax.get(task));
            Field Textent_Xmin = task.getClass().getDeclaredField("Textent_Xmin");
            Textent_Xmin.setAccessible(true);
            assertEquals(Double.POSITIVE_INFINITY, Textent_Xmin.get(task));
            Field Textent_Xmax = task.getClass().getDeclaredField("Textent_Xmax");
            Textent_Xmax.setAccessible(true);
            assertEquals(Double.NEGATIVE_INFINITY, Textent_Xmax.get(task));
            Field Textent_Ymin = task.getClass().getDeclaredField("Textent_Ymin");
            Textent_Ymin.setAccessible(true);
            assertEquals(Double.POSITIVE_INFINITY, Textent_Ymin.get(task));
            Field Textent_Ymax = task.getClass().getDeclaredField("Textent_Ymax");
            Textent_Ymax.setAccessible(true);
            assertEquals(Double.NEGATIVE_INFINITY, Textent_Ymax.get(task));
            Field nRow = task.getClass().getDeclaredField("nRow");
            nRow.setAccessible(true);
            assertEquals(0, nRow.get(task));
            Field nCol = task.getClass().getDeclaredField("nCol");
            nCol.setAccessible(true);
            assertEquals(0, nCol.get(task));
            Field layerName = task.getClass().getDeclaredField("layerName");
            layerName.setAccessible(true);
            assertEquals("test", layerName.get(task));
            Field displayOptions = task.getClass().getDeclaredField("displayOptions");
            displayOptions.setAccessible(true);
            String[] displayOptionsArr = (String[]) displayOptions.get(task);
            assertEquals("FOOTPRINT", displayOptionsArr[0]);
            assertEquals("EXTRUDED", displayOptionsArr[1]);
            assertEquals("GEOMETRY", displayOptionsArr[2]);
            assertEquals("COLLADA", displayOptionsArr[3]);
            Field tileLength = task.getClass().getDeclaredField("tileLength");
            tileLength.setAccessible(true);
            assertEquals(0.0, tileLength.get(task));
            Field tileWidth = task.getClass().getDeclaredField("tileWidth");
            tileWidth.setAccessible(true);
            assertEquals(0.0, tileWidth.get(task));
            Field tilingCSV = task.getClass().getDeclaredField("tilingCSV");
            tilingCSV.setAccessible(true);
            assertNull(tilingCSV.get(task));

            Field tileFeatureMap = task.getClass().getDeclaredField("tileFeatureMap");
            tileFeatureMap.setAccessible(true);
            assertEquals(0, ((HashMap<String, List<Feature>>) tileFeatureMap.get(task)).size());
            Field csvData = task.getClass().getDeclaredField("csvData");
            csvData.setAccessible(true);
            assertNull(csvData.get(task));
            Field featuresMap = task.getClass().getDeclaredField("featuresMap");
            featuresMap.setAccessible(true);
            assertEquals(0, ((HashMap<String, Feature>) featuresMap.get(task)).size());
            Field fileStatus = task.getClass().getDeclaredField("fileStatus");
            fileStatus.setAccessible(true);
            assertEquals(0, ((HashMap<String, Boolean>) fileStatus.get(task)).size());
            Field count = task.getClass().getDeclaredField("count");
            count.setAccessible(true);
            assertEquals(0, count.get(task));
            Field buildingInTiles = task.getClass().getDeclaredField("buildingInTiles");
            buildingInTiles.setAccessible(true);
            assertEquals(0, buildingInTiles.get(task));
            Field iriprefix = task.getClass().getDeclaredField("iriprefix");
            iriprefix.setAccessible(true);
            assertEquals(this.namespaceIri, iriprefix.get(task));

            Field stop = task.getClass().getDeclaredField("stop");
            stop.setAccessible(true);
            assertFalse((Boolean) stop.get(task));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testNewKMLTilingTaskMethods() {
        KMLTilingTask task = new KMLTilingTask(this.path2unsortedKML, this.path2sortedKML, this.databaseCRS, this.displayForm, this.namespaceIri);
        assertEquals(26, task.getClass().getDeclaredMethods().length);
    }

    @Test
    public void testStop() {
        try {
            KMLTilingTask task = new KMLTilingTask(this.path2unsortedKML, this.path2sortedKML, this.databaseCRS, this.displayForm, this.namespaceIri);
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
            KMLTilingTask task = new KMLTilingTask(this.path2unsortedKML, this.path2sortedKML, this.databaseCRS, this.displayForm, this.namespaceIri);
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
        // this test is intentionally left blank
    }

    @Test
    public void testParseKml() {
        try {
            KMLTilingTask task = new KMLTilingTask(this.path2unsortedKML, this.path2sortedKML, this.databaseCRS, this.displayForm, this.namespaceIri);
            Method parseKml = task.getClass().getDeclaredMethod("parseKML");
            parseKml.setAccessible(true);

            // create unsorted dir and copy unsorted kml file from test resources to unsorted dir
            this.unsortedDir.mkdirs();
            File kml = new File(Objects.requireNonNull(this.getClass().getResource("/testoutput.kml")).getFile());
            Files.copy(kml.toPath(), this.unsortedKmlFile.toPath());
            System.out.println("test");

            parseKml.invoke(task);

            // if getPlacemarks method is successfully called, dataContent and gmlidList should be updated
            Field dataContent = task.getClass().getDeclaredField("dataContent");
            Field gmlidList = task.getClass().getDeclaredField("gmlidList");
            gmlidList.setAccessible(true);
            assertEquals(1, ((ArrayList<String[]>) dataContent.get(task)).size());
            assertEquals("BLDG_123a", ((ArrayList<String>) gmlidList.get(task)).get(0));

            // if createCSVFile is called, csv summary file should exist in output dir
            assertTrue(this.summcsv.exists());
        } catch (IOException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            fail();
        }
    }

}