package uk.ac.cam.cares.twa.cities.tasks;

import com.google.gson.Gson;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import gov.nasa.worldwind.ogc.kml.KMLAbstractFeature;
import gov.nasa.worldwind.ogc.kml.KMLRoot;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.ac.cam.cares.twa.cities.model.geo.Transform;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class KMLTilingTaskTest {
    // set up new KMLTilingTask
    public String path2unsortedKML = System.getProperty("java.io.tmpdir") + "unsorted";
    public String path2sortedKML = System.getProperty("java.io.tmpdir") + "sorted";
    public int databaseCRS = 4326;
    public String displayForm = "extruded";
    public String namespaceIri = "http://127.0.0.1:9999/blazegraph/namespace/test/sparql/";

    // for tear down of directories and files
    public String FS = System.getProperty("file.separator");
    public File unsortedDir = new File(this.path2unsortedKML);
    public File sortedDir = new File(this.path2sortedKML);
    public File unsortedKmlFile = new File(unsortedDir.getAbsolutePath() + this.FS + "testoutput.kml");
    public File summcsv = new File(this.sortedDir + this.FS + "summary_1.csv");
    public File masterJson = new File(this.sortedDir + this.FS + "test_" + this.displayForm + "_MasterJSON.json");
    public File sortedcsv = new File(this.sortedDir + this.FS + "sorted_summary_1.csv");
    public File tilesDir = new File(this.sortedDir + this.FS + "Tiles");
    public File tilekml = new File(this.tilesDir + this.FS + "0" + this.FS + "0" + this.FS + "test_Tile_0_0_extruded.kml");

    @Before
    public void before() {
        tearDown();
    }

    public void setUp(Boolean unsortedKmlFile, Boolean summcsv, Boolean sortedcsv) {
        // create unsorted dir and copy unsorted kml file from test resources
        if (unsortedKmlFile) {
            try {
                if (!this.unsortedDir.exists()) this.unsortedDir.mkdirs();
                File kml = new File(Objects.requireNonNull(this.getClass().getResource("/testoutput.kml")).getFile());
                Files.copy(kml.toPath(), this.unsortedKmlFile.toPath());
            } catch (IOException e) {
                fail();
            }
        }
        // create sorted dir and copy summary csv file from test resources
        if (summcsv) {
            try {
                if (!this.sortedDir.exists()) this.sortedDir.mkdirs();
                File csv = new File(Objects.requireNonNull(this.getClass().getResource("/summary_1.csv")).getFile());
                Files.copy(csv.toPath(), this.summcsv.toPath());
            } catch (IOException e) {
                fail();
            }
        }
        // create sorted dir and copy sorted summary csv from test resources
        if (sortedcsv) {
            try {
                if (!this.sortedDir.exists()) this.sortedDir.mkdirs();
                File csv = new File(Objects.requireNonNull(this.getClass().getResource("/sorted_summary_1.csv")).getFile());
                Files.copy(csv.toPath(), this.sortedcsv.toPath());
            } catch (IOException e) {
                fail();
            }
        }
    }

    @AfterEach
    public void tearDown() {
        if (Objects.requireNonNull(this.tilekml.exists())) {
            // not deleting for sortingKml, check this
            if (!this.tilekml.delete()) {
                fail();
            }
        }
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
        if (Objects.requireNonNull(this.masterJson.exists())) {
            if (!this.masterJson.delete()) {
                fail();
            }
        }
        if (Objects.requireNonNull(this.sortedcsv.exists())) {
            if (!this.sortedcsv.delete()) {
                fail();
            }
        }
        if (Objects.requireNonNull(this.tilesDir.exists())) {
            try {
                FileUtils.deleteDirectory(this.tilesDir);
            } catch (IOException e) {
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

            setUp(true, false, false);

            parseKml.invoke(task);

            // if getPlacemarks method is successfully called, dataContent and gmlidList should be updated, extent and Textent should be updated
            Field gmlidList = task.getClass().getDeclaredField("gmlidList");
            gmlidList.setAccessible(true);
            Field dataContent = task.getClass().getDeclaredField("dataContent");
            Field extent_Xmin = task.getClass().getDeclaredField("extent_Xmin");
            extent_Xmin.setAccessible(true);
            Field extent_Xmax = task.getClass().getDeclaredField("extent_Xmax");
            extent_Xmax.setAccessible(true);
            Field extent_Ymin = task.getClass().getDeclaredField("extent_Ymin");
            extent_Ymin.setAccessible(true);
            Field extent_Ymax = task.getClass().getDeclaredField("extent_Ymax");
            extent_Ymax.setAccessible(true);
            Field Textent_Xmin = task.getClass().getDeclaredField("Textent_Xmin");
            Textent_Xmin.setAccessible(true);
            Field Textent_Xmax = task.getClass().getDeclaredField("Textent_Xmax");
            Textent_Xmax.setAccessible(true);
            Field Textent_Ymin = task.getClass().getDeclaredField("Textent_Ymin");
            Textent_Ymin.setAccessible(true);
            Field Textent_Ymax = task.getClass().getDeclaredField("Textent_Ymax");
            Textent_Ymax.setAccessible(true);

            assertTrue(((ArrayList<String>) gmlidList.get(task)).size() > 0);
            assertTrue(((ArrayList<String[]>) dataContent.get(task)).size() > 0);
            assertTrue((Double) extent_Xmin.get(task) != Double.POSITIVE_INFINITY);
            assertTrue((Double) extent_Xmax.get(task) != Double.NEGATIVE_INFINITY);
            assertTrue((Double) extent_Ymin.get(task) != Double.POSITIVE_INFINITY);
            assertTrue((Double) extent_Ymax.get(task) != Double.NEGATIVE_INFINITY);
            assertTrue((Double) Textent_Xmin.get(task) != Double.POSITIVE_INFINITY);
            assertTrue((Double) Textent_Xmax.get(task) != Double.NEGATIVE_INFINITY);
            assertTrue((Double) Textent_Ymin.get(task) != Double.POSITIVE_INFINITY);
            assertTrue((Double) Textent_Ymax.get(task) != Double.NEGATIVE_INFINITY);

            // if createCSVFile is called, csv summary file should exist in output dir
            assertTrue(this.summcsv.exists());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            fail();
        }
    }

    @Test
    public void testGetPlacemarks() {
        try {
            KMLTilingTask task = new KMLTilingTask(this.path2unsortedKML, this.path2sortedKML, this.databaseCRS, this.displayForm, this.namespaceIri);
            Method getPlacemarks = task.getClass().getDeclaredMethod("getPlacemarks", KMLAbstractFeature.class, File.class);
            getPlacemarks.setAccessible(true);

            setUp(true, false, false);
            KMLRoot root = KMLRoot.create(this.unsortedKmlFile);
            root.parse();

            getPlacemarks.invoke(task, root.getFeature(), this.unsortedKmlFile);

            // if getPlacemarks method is successfully called, dataContent and gmlidList should be updated, extent and Textent should be updated
            Field gmlidList = task.getClass().getDeclaredField("gmlidList");
            gmlidList.setAccessible(true);
            Field dataContent = task.getClass().getDeclaredField("dataContent");
            Field extent_Xmin = task.getClass().getDeclaredField("extent_Xmin");
            extent_Xmin.setAccessible(true);
            Field extent_Xmax = task.getClass().getDeclaredField("extent_Xmax");
            extent_Xmax.setAccessible(true);
            Field extent_Ymin = task.getClass().getDeclaredField("extent_Ymin");
            extent_Ymin.setAccessible(true);
            Field extent_Ymax = task.getClass().getDeclaredField("extent_Ymax");
            extent_Ymax.setAccessible(true);
            Field Textent_Xmin = task.getClass().getDeclaredField("Textent_Xmin");
            Textent_Xmin.setAccessible(true);
            Field Textent_Xmax = task.getClass().getDeclaredField("Textent_Xmax");
            Textent_Xmax.setAccessible(true);
            Field Textent_Ymin = task.getClass().getDeclaredField("Textent_Ymin");
            Textent_Ymin.setAccessible(true);
            Field Textent_Ymax = task.getClass().getDeclaredField("Textent_Ymax");
            Textent_Ymax.setAccessible(true);

            ArrayList<String> gmlidListArrList = (ArrayList<String>) gmlidList.get(task);
            ArrayList<String[]> dataContentArrList = (ArrayList<String[]>) dataContent.get(task);
            String[] dataContentArr = dataContentArrList.get(0);
            assertEquals(1, gmlidListArrList.size());
            assertEquals("BLDG_123a", gmlidListArrList.get(0));
            assertEquals(1, dataContentArrList.size());
            assertEquals("BLDG_123a", dataContentArr[0]);
            assertEquals("1.0#4.0#1.0#4.0", dataContentArr[1]);
            assertEquals("2.5#2.5", dataContentArr[2]);
            assertEquals(this.unsortedKmlFile.getName(), dataContentArr[3]);
            assertEquals(1.0, extent_Xmin.get(task));
            assertEquals(4.0, extent_Xmax.get(task));
            assertEquals(1.0, extent_Ymin.get(task));
            assertEquals(4.0, extent_Ymax.get(task));
        } catch (IOException | XMLStreamException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            fail();
        }
    }

    @Test
    public void testArr2Str() {
        try {
            KMLTilingTask task = new KMLTilingTask(this.path2unsortedKML, this.path2sortedKML, this.databaseCRS, this.displayForm, this.namespaceIri);
            Method arr2str = task.getClass().getDeclaredMethod("arr2str", double[].class);
            arr2str.setAccessible(true);

            double[] arr = {0.0, 1.0, 2.0, 3.0};

            assertEquals("0.0#1.0#2.0#3.0", arr2str.invoke(task, arr));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            fail();
        }
    }

    @Test
    public void testCreateCSVFile() {
        try {
            KMLTilingTask task = new KMLTilingTask(this.path2unsortedKML, this.path2sortedKML, this.databaseCRS, this.displayForm, this.namespaceIri);
            Method createCSVFile = task.getClass().getDeclaredMethod("createCSVFile", List.class);
            createCSVFile.setAccessible(true);

            String[] dataArr = {"BLDG_123a", "0.0#1.0#2.0#3.0", "1.0#1.0", "testoutput.kml"};
            List<String[]> dataContent = new ArrayList<>();
            dataContent.add(dataArr);

            createCSVFile.invoke(task, dataContent);

            assertTrue(this.summcsv.exists());

            CSVParser parser = new CSVParserBuilder().withEscapeChar('\0').build();
            try (CSVReader reader = new CSVReaderBuilder(new FileReader(this.summcsv.getAbsolutePath())).withCSVParser(parser).build()) {
                String[] header = reader.readNext();
                List<String[]> rows = reader.readAll();
                String[] row = rows.get(0);

                assertEquals("gmlid", header[0]);
                assertEquals("envelope", header[1]);
                assertEquals("envelopeCentroid", header[2]);
                assertEquals("filename", header[3]);
                assertEquals(1, rows.size());
                assertEquals(dataArr[0], row[0]);
                assertEquals(dataArr[1], row[1]);
                assertEquals(dataArr[2], row[2]);
                assertEquals(dataArr[3], row[3]);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IOException | CsvException e) {
            fail();
        }
    }

    @Test
    public void testUpdateExtent1DArray() {
        try {
            KMLTilingTask task = new KMLTilingTask(this.path2unsortedKML, this.path2sortedKML, this.databaseCRS, this.displayForm, this.namespaceIri);
            Method updateExtent = task.getClass().getDeclaredMethod("updateExtent", double[].class);
            updateExtent.setAccessible(true);
            Field extent_Xmin = task.getClass().getDeclaredField("extent_Xmin");
            extent_Xmin.setAccessible(true);
            Field extent_Xmax = task.getClass().getDeclaredField("extent_Xmax");
            extent_Xmax.setAccessible(true);
            Field extent_Ymin = task.getClass().getDeclaredField("extent_Ymin");
            extent_Ymin.setAccessible(true);
            Field extent_Ymax = task.getClass().getDeclaredField("extent_Ymax");
            extent_Ymax.setAccessible(true);
            Field Textent_Xmin = task.getClass().getDeclaredField("Textent_Xmin");
            Textent_Xmin.setAccessible(true);
            Field Textent_Xmax = task.getClass().getDeclaredField("Textent_Xmax");
            Textent_Xmax.setAccessible(true);
            Field Textent_Ymin = task.getClass().getDeclaredField("Textent_Ymin");
            Textent_Ymin.setAccessible(true);
            Field Textent_Ymax = task.getClass().getDeclaredField("Textent_Ymax");
            Textent_Ymax.setAccessible(true);

            // test case where geom envelope is not within current envelope, extent should be updated
            double[] geomEnvelope1 = {0.0, 1.0, 0.0, 1.0};
            updateExtent.invoke(task, geomEnvelope1);
            assertEquals(0.0, extent_Xmin.get(task));
            assertEquals(1.0, extent_Xmax.get(task));
            assertEquals(0.0, extent_Ymin.get(task));
            assertEquals(1.0, extent_Ymax.get(task));
            assertEquals(0.0, Textent_Xmin.get(task));
            assertEquals(1.0, Textent_Xmax.get(task));
            assertEquals(0.0, Textent_Ymin.get(task));
            assertEquals(1.0, Textent_Ymax.get(task));

            // test case where geom envelope is within current envelope, and extent should not be updated
            double[] geomEnvelope2 = {0.2, 0.8, 0.2, 0.8};
            updateExtent.invoke(task, geomEnvelope2);
            assertEquals(0.0, extent_Xmin.get(task));
            assertEquals(1.0, extent_Xmax.get(task));
            assertEquals(0.0, extent_Ymin.get(task));
            assertEquals(1.0, extent_Ymax.get(task));
            assertEquals(0.0, Textent_Xmin.get(task));
            assertEquals(1.0, Textent_Xmax.get(task));
            assertEquals(0.0, Textent_Ymin.get(task));
            assertEquals(1.0, Textent_Ymax.get(task));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            fail();
        }
    }

    @Test
    public void testUpdateExtent2DArray() {
        try {
            KMLTilingTask task = new KMLTilingTask(this.path2unsortedKML, this.path2sortedKML, this.databaseCRS, this.displayForm, this.namespaceIri);
            Method updateExtent = task.getClass().getDeclaredMethod("updateExtent", double[][].class);
            Field extent_Xmin = task.getClass().getDeclaredField("extent_Xmin");
            extent_Xmin.setAccessible(true);
            Field extent_Xmax = task.getClass().getDeclaredField("extent_Xmax");
            extent_Xmax.setAccessible(true);
            Field extent_Ymin = task.getClass().getDeclaredField("extent_Ymin");
            extent_Ymin.setAccessible(true);
            Field extent_Ymax = task.getClass().getDeclaredField("extent_Ymax");
            extent_Ymax.setAccessible(true);
            Field Textent_Xmin = task.getClass().getDeclaredField("Textent_Xmin");
            Textent_Xmin.setAccessible(true);
            Field Textent_Xmax = task.getClass().getDeclaredField("Textent_Xmax");
            Textent_Xmax.setAccessible(true);
            Field Textent_Ymin = task.getClass().getDeclaredField("Textent_Ymin");
            Textent_Ymin.setAccessible(true);
            Field Textent_Ymax = task.getClass().getDeclaredField("Textent_Ymax");
            Textent_Ymax.setAccessible(true);

            // test case where geom envelope is not within current envelope, extent should be updated
            double[][] geomEnvelope1 = {{0.0, 0.0}, {0.0, 1.0}, {1.0, 1.0}, {1.0, 0.0}, {0.0, 0.0}};
            updateExtent.invoke(task, (Object) geomEnvelope1);
            assertEquals(0.0, extent_Xmin.get(task));
            assertEquals(1.0, extent_Xmax.get(task));
            assertEquals(0.0, extent_Ymin.get(task));
            assertEquals(1.0, extent_Ymax.get(task));
            assertEquals(0.0, Textent_Xmin.get(task));
            assertEquals(1.0, Textent_Xmax.get(task));
            assertEquals(0.0, Textent_Ymin.get(task));
            assertEquals(1.0, Textent_Ymax.get(task));

            // test case where geom envelope is within current envelope, and extent should not be updated
            double[][] geomEnvelope2 = {{0.2, 0.2}, {0.2, 0.8}, {0.8, 0.8}, {0.8, 0.2}, {0.2, 0.2}};
            updateExtent.invoke(task, (Object) geomEnvelope2);
            assertEquals(0.0, extent_Xmin.get(task));
            assertEquals(1.0, extent_Xmax.get(task));
            assertEquals(0.0, extent_Ymin.get(task));
            assertEquals(1.0, extent_Ymax.get(task));
            assertEquals(0.0, Textent_Xmin.get(task));
            assertEquals(1.0, Textent_Xmax.get(task));
            assertEquals(0.0, Textent_Ymin.get(task));
            assertEquals(1.0, Textent_Ymax.get(task));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            fail();
        }
    }

    @Test
    public void testTilingKml() {
        try {
            KMLTilingTask task = new KMLTilingTask(this.path2unsortedKML, this.path2sortedKML, this.databaseCRS, this.displayForm, this.namespaceIri);
            Method tilingKML = task.getClass().getDeclaredMethod("tilingKML");
            tilingKML.setAccessible(true);
            Field extent_Xmin = task.getClass().getDeclaredField("extent_Xmin");
            extent_Xmin.setAccessible(true);
            Field extent_Xmax = task.getClass().getDeclaredField("extent_Xmax");
            extent_Xmax.setAccessible(true);
            Field extent_Ymin = task.getClass().getDeclaredField("extent_Ymin");
            extent_Ymin.setAccessible(true);
            Field extent_Ymax = task.getClass().getDeclaredField("extent_Ymax");
            extent_Ymax.setAccessible(true);
            Field parserCSV = task.getClass().getDeclaredField("parserCSV");
            parserCSV.setAccessible(true);
            Field Textent_Ymax = task.getClass().getDeclaredField("Textent_Ymax");
            Textent_Ymax.setAccessible(true);
            Field Textent_Ymin = task.getClass().getDeclaredField("Textent_Ymin");
            Textent_Ymin.setAccessible(true);
            Field Textent_Xmax = task.getClass().getDeclaredField("Textent_Xmax");
            Textent_Xmax.setAccessible(true);
            Field Textent_Xmin = task.getClass().getDeclaredField("Textent_Xmin");
            Textent_Xmin.setAccessible(true);
            Field dataContent = task.getClass().getDeclaredField("dataContent");

            setUp(false, true, false);
            extent_Xmin.set(task, 0.0);
            extent_Xmax.set(task, 0.0);
            extent_Ymin.set(task, 0.0);
            extent_Ymax.set(task, 0.0);
            parserCSV.set(task, this.summcsv.toPath());
            Textent_Ymax.set(task, 250.0);
            Textent_Ymin.set(task, 0.0);
            Textent_Xmax.set(task, 250.0);
            Textent_Xmin.set(task, 0.0);
            ArrayList<String[]> dataContentArrList = (ArrayList<String[]>) dataContent.get(task);
            dataContentArrList.add(new String[1]);

            tilingKML.invoke(task);

            assertTrue(this.masterJson.exists());
            assertTrue(this.sortedcsv.exists());

            CSVParser parser = new CSVParserBuilder().withEscapeChar('\0').build();
            try (CSVReader reader = new CSVReaderBuilder(new FileReader(this.sortedcsv.getAbsolutePath())).withCSVParser(parser).build()) {
                String[] header = reader.readNext();
                List<String[]> rows = reader.readAll();
                String[] row = rows.get(0);

                assertEquals("gmlid", header[0]);
                assertEquals("envelope", header[1]);
                assertEquals("envelopeCentroid", header[2]);
                assertEquals("tiles", header[3]);
                assertEquals("filename", header[4]);
                assertEquals(1, rows.size());
                assertEquals("BLDG_123a", row[0]);
                assertEquals("0.0#1.0#2.0#3.0", row[1]);
                assertEquals("1.0#1.0", row[2]);
                assertEquals("0#0", row[3]);
                assertEquals("testoutput.kml", row[4]);
            }
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException | InvocationTargetException | IOException | CsvException e) {
            fail();
        }
    }

    @Test
    public void testCalcNumRowCol() {
        try {
            KMLTilingTask task = new KMLTilingTask(this.path2unsortedKML, this.path2sortedKML, this.databaseCRS, this.displayForm, this.namespaceIri);
            Method calcNumRowCol = task.getClass().getDeclaredMethod("calcNumRowCol");
            calcNumRowCol.setAccessible(true);
            Field Textent_Ymax = task.getClass().getDeclaredField("Textent_Ymax");
            Textent_Ymax.setAccessible(true);
            Field Textent_Ymin = task.getClass().getDeclaredField("Textent_Ymin");
            Textent_Ymin.setAccessible(true);
            Field Textent_Xmax = task.getClass().getDeclaredField("Textent_Xmax");
            Textent_Xmax.setAccessible(true);
            Field Textent_Xmin = task.getClass().getDeclaredField("Textent_Xmin");
            Textent_Xmin.setAccessible(true);
            Field nCol = task.getClass().getDeclaredField("nCol");
            nCol.setAccessible(true);
            Field nRow = task.getClass().getDeclaredField("nRow");
            nRow.setAccessible(true);
            Field tileLength = task.getClass().getDeclaredField("tileLength");
            tileLength.setAccessible(true);
            Field tileWidth = task.getClass().getDeclaredField("tileWidth");
            tileWidth.setAccessible(true);

            Textent_Ymax.set(task, 250.0);
            Textent_Ymin.set(task, 0.0);
            Textent_Xmax.set(task, 250.0);
            Textent_Xmin.set(task, 0.0);

            int[] rowCol = (int[]) calcNumRowCol.invoke(task);

            assertEquals(1, rowCol[0]);
            assertEquals(1, rowCol[1]);
            assertEquals(1, nRow.get(task));
            assertEquals(1, nCol.get(task));
            assertEquals(250.0, tileLength.get(task));
            assertEquals(250.0, tileWidth.get(task));
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException | InvocationTargetException e) {
            fail();
        }
    }

    @Test
    public void testReproject() {
        try {
            KMLTilingTask task = new KMLTilingTask(this.path2unsortedKML, this.path2sortedKML, this.databaseCRS, this.displayForm, this.namespaceIri);
            Method reproject = task.getClass().getDeclaredMethod("reproject", String.class, int.class, int.class);
            reproject.setAccessible(true);

            // test case when input is a centroid, should call reprojectPoint
            try (MockedStatic<Transform> transform = Mockito.mockStatic(Transform.class)) {
                transform.when(() -> Transform.reprojectPoint(ArgumentMatchers.any(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()))
                        .thenReturn(new double[2]);
                reproject.invoke(task, "1.0#1.0", 4326, 4326);
                transform.verify(() -> Transform.reprojectPoint(ArgumentMatchers.any(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()),
                        Mockito.times(1));
            }

            // test case when input is an envelope, should call reprojectEnvelope
            try (MockedStatic<Transform> transform = Mockito.mockStatic(Transform.class)) {
                transform.when(() -> Transform.reprojectEnvelope(ArgumentMatchers.any(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()))
                        .thenReturn(new double[4]);
                reproject.invoke(task, "1.0#1.0#1.0#1.0", 4326, 4326);
                transform.verify(() -> Transform.reprojectEnvelope(ArgumentMatchers.any(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()),
                        Mockito.times(1));
            }

            // test case when neither point nor envelope, should return null
            assertNull(reproject.invoke(task, "1.0#1.0#1.0", 4326, 4326));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            fail();
        }
    }

    @Test
    public void testAssignTiles() {
        try {
            KMLTilingTask task = new KMLTilingTask(this.path2unsortedKML, this.path2sortedKML, this.databaseCRS, this.displayForm, this.namespaceIri);
            Method assignTiles = task.getClass().getDeclaredMethod("assignTiles", String.class);
            assignTiles.setAccessible(true);
            Field Textent_Ymin = task.getClass().getDeclaredField("Textent_Ymin");
            Textent_Ymin.setAccessible(true);
            Field Textent_Xmin = task.getClass().getDeclaredField("Textent_Xmin");
            Textent_Xmin.setAccessible(true);
            Field tileLength = task.getClass().getDeclaredField("tileLength");
            tileLength.setAccessible(true);
            Field tileWidth = task.getClass().getDeclaredField("tileWidth");
            tileWidth.setAccessible(true);

            Textent_Ymin.set(task, 0.0);
            Textent_Xmin.set(task, 0.0);
            tileLength.set(task, 1.0);
            tileWidth.set(task, 1.0);

            Integer[] rowCol = (Integer[]) assignTiles.invoke(task, "5.0#5.0");

            assertEquals(5, rowCol[0]);
            assertEquals(5, rowCol[1]);
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException | InvocationTargetException e) {
            fail();
        }
    }

    @Test
    public void testCreateMasterJson() {
        try {
            KMLTilingTask task = new KMLTilingTask(this.path2unsortedKML, this.path2sortedKML, this.databaseCRS, this.displayForm, this.namespaceIri);
            Method createMasterJson = task.getClass().getDeclaredMethod("createMasterJson");
            createMasterJson.setAccessible(true);
            Field nRow = task.getClass().getDeclaredField("nRow");
            nRow.setAccessible(true);
            Field nCol = task.getClass().getDeclaredField("nCol");
            nCol.setAccessible(true);
            Field extent_Xmin = task.getClass().getDeclaredField("extent_Xmin");
            extent_Xmin.setAccessible(true);
            Field extent_Xmax = task.getClass().getDeclaredField("extent_Xmax");
            extent_Xmax.setAccessible(true);
            Field extent_Ymin = task.getClass().getDeclaredField("extent_Ymin");
            extent_Ymin.setAccessible(true);
            Field extent_Ymax = task.getClass().getDeclaredField("extent_Ymax");
            extent_Ymax.setAccessible(true);

            nRow.set(task, 1);
            nCol.set(task, 1);
            extent_Ymin.set(task, 0.0);
            extent_Ymax.set(task, 0.0);
            extent_Xmin.set(task, 0.0);
            extent_Xmax.set(task, 0.0);

            assertEquals(this.masterJson.getAbsolutePath(), createMasterJson.invoke(task).toString());
            assertTrue(this.masterJson.exists());

            Gson gson = new Gson();
            Reader reader = Files.newBufferedReader(this.masterJson.toPath());
            Map<String, ?> map = gson.fromJson(reader, Map.class);
            reader.close();

            assertEquals("1.0.0", map.get("version"));
            assertEquals("test", map.get("layername"));
            assertEquals(".kml", map.get("fileextension"));
            assertEquals(this.displayForm, map.get("displayform"));
            // gson reader passes int values as double by default, so assert equivalent double value
            assertEquals(10.0, map.get("minLodPixels"));
            assertEquals(-1.0, map.get("maxLodPixels"));
            assertEquals(1.0, map.get("colnum"));
            assertEquals(1.0, map.get("rownum"));
            Map<String, Double> bbox = (Map<String, Double>) map.get("bbox");
            assertEquals(0.0, bbox.get("xmin"));
            assertEquals(0.0, bbox.get("xmax"));
            assertEquals(0.0, bbox.get("ymin"));
            assertEquals(0.0, bbox.get("ymax"));
            assertEquals(this.namespaceIri + "cityobject/", map.get("iriprefix"));
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException | InvocationTargetException | IOException e) {
            fail();
        }
    }

    @Test
    public void testSortingKml() {
        try {
            KMLTilingTask task = new KMLTilingTask(this.path2unsortedKML, this.path2sortedKML, this.databaseCRS, this.displayForm, this.namespaceIri);
            Method sortingKml = task.getClass().getDeclaredMethod("sortingKML");
            sortingKml.setAccessible(true);
            Field tilingCSV = task.getClass().getDeclaredField("tilingCSV");
            tilingCSV.setAccessible(true);
            Field csvData = task.getClass().getDeclaredField("csvData");
            csvData.setAccessible(true);
            Field featuresMap = task.getClass().getDeclaredField("featuresMap");
            featuresMap.setAccessible(true);
            Field fileStatus = task.getClass().getDeclaredField("fileStatus");
            fileStatus.setAccessible(true);
            Field nRow = task.getClass().getDeclaredField("nRow");
            nRow.setAccessible(true);
            Field nCol = task.getClass().getDeclaredField("nCol");
            nCol.setAccessible(true);

            // test case when there is one file
            // check setup and teardown
            setUp(true, false, true);
            tilingCSV.set(task, this.sortedcsv.toPath());
            nRow.set(task, 1);
            nCol.set(task, 1);

            sortingKml.invoke(task);

            assertTrue(this.tilesDir.exists());
            assertEquals(1, ((Map<?, ?>) csvData.get(task)).size());
            //assertTrue((Boolean) ((Map<?, ?>) fileStatus.get(task)).get(this.unsortedKmlFile));
            assertTrue(this.tilekml.exists());


        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
            fail();
        }
        // put sorted csv
        // put unsorted kml

        // test case when theres 1 file

        // csvdata field should not be empty
        // features map should have something
        // file status should be read
        // 0,0 dir should be created/ tile 0 0  file exists



    }

}