package uk.ac.cam.cares.twa.cities.tasks;

import com.github.jsonldjava.utils.Obj;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.MultiGeometry;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import de.micromata.opengis.kml.v_2_2_0.StyleSelector;
import gov.nasa.worldwind.ogc.kml.KMLAbstractContainer;
import gov.nasa.worldwind.ogc.kml.KMLAbstractFeature;
import gov.nasa.worldwind.ogc.kml.KMLAbstractGeometry;
import gov.nasa.worldwind.ogc.kml.KMLMultiGeometry;
import gov.nasa.worldwind.ogc.kml.KMLPlacemark;
import gov.nasa.worldwind.ogc.kml.KMLPoint;
import gov.nasa.worldwind.ogc.kml.KMLPolygon;
import gov.nasa.worldwind.ogc.kml.KMLRoot;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import org.gdal.ogr.Geometry;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.model.geo.EnvelopeCentroid;
import uk.ac.cam.cares.twa.cities.model.geo.Transform;
import uk.ac.cam.cares.twa.cities.model.geo.Utilities;

/** KMLTilingTask intends to postprocess the exported KML files (unsorted) to spatially organised tiles
 *  It contains 3 steps: KMLParser, KMLTiler, KMLSorter
 *  Unsorted KML file : Output from exporter in WGS48
 *
 *  KMLParser: the output of running this class should be the generation of the summary file in csv
 *                 (gmlid, envelope[xmin, xmax, ymin, ymax], envelopeCentroid, corresponding file)
 *
 *  KMLTiler: This class will help to calculate the NumColRow (number of rows and columns) based on the initial tile size
 *
 *  KMLSorter: Sort the unsorted KML files based on the csv_summary
 *
 **/
public class KMLTilingTask implements Runnable{

  // KMLTilingTask class variables
  private static final int crsWGS84 = 4326; // global WGS 84 on cesium
  private static final int initTileSize = 250; // in meter. The tilting of the extent should be done in meter

  private final int CRSinMeter;  // 23855 for berlin in meter in blazegraph
  private String unsortedKMLdir; // directory that contains the unsorted KML files
  private String sortedKMLdir; // directory that contains the sorted KML files
  private String displayForm;

  // KML Parsing
  private static final String outCsvName = "summary";
  private static final String underScore = "_";
  private static final String outFileExt = ".csv";
  private String outputDir;
  public List<String[]> dataContent = new ArrayList<>();
  private List<String> gmlidList = new ArrayList<>();
  private Path parserCSV;  // created by KML parsing step, without tiles position

  // KML Tiling
  private double extent_Xmin;  // initial extent from kml file in 4326 (in degree)
  private double extent_Xmax;
  private double extent_Ymin;
  private double extent_Ymax;
  private double Textent_Xmin;  // transformed extent in meter
  private double Textent_Xmax;
  private double Textent_Ymin;
  private double Textent_Ymax;
  private int nRow;
  private int nCol;
  private static final String layerName = "test";
  private static final String[] displayOptions = {"FOOTPRINT", "EXTRUDED", "GEOMETRY", "COLLADA"};
  private double tileLength;  // RowNumber
  private double tileWidth;  // ColNumber
  private Path tilingCSV;  // created by KML tiling step, with tiles position
  // KML Sorting
  private HashMap<String, List<Feature>> tileFeatureMap = new HashMap<>(); // hashmap containing tile name as key,
  private HashMap<String, ArrayList<String[]>> csvData;
  private HashMap<String, Feature> featuresMap = new HashMap<>();// <gmlid, features>
  private HashMap<String, Boolean> fileStatus = new HashMap<>();
  private int count = 0; // count the number of buildings
  private int buildingInTiles = 0;
  private String iriprefix;
  //private final BlockingQueue<Params> taskParamsQueue;
  private Boolean stop = false;

  public KMLTilingTask(String path2unsortedKML, String path2sortedKML, int databaseCRS, String displayForm, String namespaceIri) {

    // KMLTilingTask: Initialize the variables
    CRSinMeter = databaseCRS; //"32648"; "25833"
    if (Arrays.asList(displayOptions).indexOf(displayForm.toUpperCase()) != -1){
      this.displayForm = displayForm; // "extruded"
    } else{
      throw new IllegalArgumentException("Invalid displayform is giving to KMLTilingTask.");
    }
    unsortedKMLdir = path2unsortedKML;
    sortedKMLdir = path2sortedKML;
    iriprefix = namespaceIri;
    this.outputDir = Utilities.createDir(sortedKMLdir);

    // KML Tiling: Initialize the extent for the tiling step; extent in 4326 same as in kml; Textent -- transformed extent (in meter for berlin)
    extent_Xmin = Double.POSITIVE_INFINITY;
    extent_Xmax = Double.NEGATIVE_INFINITY;
    extent_Ymin = Double.POSITIVE_INFINITY;
    extent_Ymax = Double.NEGATIVE_INFINITY;

    Textent_Xmin = Double.POSITIVE_INFINITY;
    Textent_Xmax = Double.NEGATIVE_INFINITY;
    Textent_Ymin = Double.POSITIVE_INFINITY;
    Textent_Ymax = Double.NEGATIVE_INFINITY;
    System.out.println("KMLTilingTask Initialzation Thread Name: " + Thread.currentThread().getName());

  }

  public void stop() {
    stop = true;
  }

  public boolean isRunning() {
    return !stop;
  }

  @Override
  public void run() {
    while(isRunning()){
        System.out.println("KMLTilingTask RUN Thread Name: " + Thread.currentThread().getName());
        parseKML();
        tilingKML();
        sortingKML();
        stop();
      }
  }

  /** KML parsing : parse the kml to java object and calculate the extent of the whole region */
  private void parseKML(){

    String[] inputFiles = Utilities.getInputFiles(unsortedKMLdir);
    File currFile;

    long start = System.currentTimeMillis();
    for (String file : inputFiles){
      currFile = new File(file);
      int before = dataContent.size();
      System.out.println("Reading the file: " + currFile + " with the size of " + currFile.length());
      KMLRoot kmlRoot = null;
      try {
        kmlRoot = KMLRoot.create(currFile);
        kmlRoot.parse();
      } catch (IOException | XMLStreamException e) {
        e.printStackTrace();
      }
      this.getPlacemarks(kmlRoot.getFeature(), currFile);
      System.out.println("After reading " + currFile.getName() + " file has the length = " + String.valueOf(dataContent.size() - before));
    }

    long end1 = System.currentTimeMillis();

    if (this.dataContent.size() != 0) {
      this.parserCSV = this.createCSVFile(this.dataContent);

      System.out.println("Checking if the gmlidList is unique: " + Utilities.isUnique(this.gmlidList));
      System.out.println("Saving the file in : " + parserCSV);
      System.out.println("Updated Extent: " + extent_Xmin + " " + extent_Xmax + " " + extent_Ymin + " " + extent_Ymax);
    }
    long end2 = System.currentTimeMillis();
    System.out.println("Reading " + inputFiles.length + " files takes " + (end1 - start) + " ms");
    System.out.println("The KMLParserTask took " + (end2 - start) + " ms");
  }

  /** KML parsing : calculate the envelop and center from KMLAbstractFeature and add to datacontent */
  private void getPlacemarks(KMLAbstractFeature feature, File currFile)
  {
    if (feature instanceof KMLAbstractContainer)
    {
      KMLAbstractContainer container = (KMLAbstractContainer) feature;
      for (KMLAbstractFeature f : container.getFeatures())
      {
        getPlacemarks(f, currFile); // recursive
      }
    }
    else if (feature instanceof KMLPlacemark)
    {
      KMLAbstractGeometry geom = ((KMLPlacemark) feature).getGeometry();
      String buildingId = feature.getName();
      if (geom instanceof KMLPoint)
      {
        System.out.println("Point placemark at: " + ((KMLPoint) geom).getCoordinates());
      }
      else if (geom instanceof KMLMultiGeometry) // most common for buildings
      {
        KMLMultiGeometry multiGeometry = ((KMLMultiGeometry) geom);
        KMLPolygon polygon = (KMLPolygon) multiGeometry.getGeometries().toArray()[0];
        double[] envelope = EnvelopeCentroid.calcEnvelope(geom);
        double[] centroid = EnvelopeCentroid.calcCentroid(envelope);
        this.gmlidList.add(buildingId);
        String[] row = {buildingId, arr2str(envelope), arr2str(centroid), currFile.getName()};
        this.dataContent.add(row);
        this.updateExtent(envelope);
      }
    }
  }

  /** KML parsing : for the record in CSV, this method convert double[] to a string with x0#y0#z0#x1#y1#z1#x2#y2#z2 */
  private String arr2str (double[] arr){
    String output = "";
    String sep = "#";
    for (int j = 0; j < arr.length; j++) {
      output += String.valueOf(arr[j]);
      if (j != arr.length -1) {
        output += sep;
      }
    }
    return output;
  }

  /** KML parsing : Save java object "dataContent" to CSV file */
  private Path createCSVFile(List<String[]> dataContent) {

    String[] header = {"gmlid", "envelope", "envelopeCentroid", "filename"};

    List<String[]> csvContent = new ArrayList<>();
    csvContent.add(header);
    csvContent.addAll(dataContent);

    Path outputCSV = Paths.get(this.outputDir, this.outCsvName + "_" +
        String.valueOf(dataContent.size()) + this.outFileExt);

    try (CSVWriter writer = new CSVWriter(new FileWriter(outputCSV.toString()))) {
      writer.writeAll(csvContent);
      System.out.println("The summary has " + dataContent.size() + " rows.");
    } catch (IOException e) {
      e.printStackTrace();
    }
    return outputCSV;
  }

  /** KML Tiling: Update the boundary of the extent
   *  This method will be called every time when a city object is read and its envelop is calculated
   *  @param : geomEnvelope [xmin, xmax, ymin, ymax] */
  private void updateExtent(double[] geomEnvelope) {

    if (geomEnvelope[0] <= this.extent_Xmin) { this.extent_Xmin = geomEnvelope[0]; }
    if (geomEnvelope[1] >= this.extent_Xmax) { this.extent_Xmax = geomEnvelope[1]; }
    if (geomEnvelope[2] <= this.extent_Ymin) { this.extent_Ymin = geomEnvelope[2]; }
    if (geomEnvelope[3] >= this.extent_Ymax) { this.extent_Ymax = geomEnvelope[3]; }

    double[] Tenvelope = Transform.reprojectEnvelope(geomEnvelope, crsWGS84, CRSinMeter);

    if (Tenvelope[0] <= this.Textent_Xmin) { this.Textent_Xmin = Tenvelope[0]; }
    if (Tenvelope[1] >= this.Textent_Xmax) { this.Textent_Xmax = Tenvelope[1]; }
    if (Tenvelope[2] <= this.Textent_Ymin) { this.Textent_Ymin = Tenvelope[2]; }
    if (Tenvelope[3] >= this.Textent_Ymax) { this.Textent_Ymax = Tenvelope[3]; }

  }

  /** Test purpose: reproject geometry instead of the envelope , to see if it makes any difference */
  public void updateExtent(double[][] geometry){

    double[] origEnvelop = Transform.getEnvelop(geometry);

    // updateExtent in 4236
    if (origEnvelop[0] <= this.extent_Xmin) { this.extent_Xmin = origEnvelop[0]; }
    if (origEnvelop[1] >= this.extent_Xmax) { this.extent_Xmax = origEnvelop[1]; }
    if (origEnvelop[2] <= this.extent_Ymin) { this.extent_Ymin = origEnvelop[2]; }
    if (origEnvelop[3] >= this.extent_Ymax) { this.extent_Ymax = origEnvelop[3]; }

    double[][] Tgeometry = Transform.reprojectGeometry(geometry, crsWGS84, CRSinMeter);

    double[] Tenvelop = Transform.getEnvelop(Tgeometry);

    if (Tenvelop[0] <= this.Textent_Xmin) { this.Textent_Xmin = Tenvelop[0]; }
    if (Tenvelop[1] >= this.Textent_Xmax) { this.Textent_Xmax = Tenvelop[1]; }
    if (Tenvelop[2] <= this.Textent_Ymin) { this.Textent_Ymin = Tenvelop[2]; }
    if (Tenvelop[3] >= this.Textent_Ymax) { this.Textent_Ymax = Tenvelop[3]; }


  }
  /*******************************************************************************************************************/

  private void tilingKML(){

    int[] numRowCol = calcNumRowCol(); // update the nCol, nRow, tileLength, tileWidth
    Path path2masterJson = createMasterJson();

    // Start calculating tile position
    //long start1 = System.currentTimeMillis();
    List<String[]> csvData = new ArrayList<>();

    // Read the csv_summary from KML parsing step (parserCSV)
    CSVParser csvParser = new CSVParserBuilder().withEscapeChar('\0').build(); // with this '\0' can read backslash; with '\\' can not read backslash
    try (CSVReader reader = new CSVReaderBuilder(new FileReader(parserCSV.toString())).withCSVParser(csvParser).build()) {
        String[] header = reader.readNext();
        csvData = reader.readAll();
    } catch (IOException | CsvException e) {
        e.printStackTrace();
    }

    // Add the tilePosition to the csv_summary file
    Integer[] tilePosition;
    List<String[]> outCSVData = new ArrayList<>();

    for (String[] inputRow : csvData) {  // skip the header
      tilePosition = assignTiles(inputRow[2]);
      String[] outRow = {inputRow[0], inputRow[1], inputRow[2],
          Utilities.arr2str(tilePosition), inputRow[3]};  //kmltiling.getFileName(csvData.get(i)[3], "test")
      outCSVData.add(outRow);
    }
    long finish1 = System.currentTimeMillis();
    //System.out.println("Reading the files took " + (finish1 - start1) + " ms");

    // Sorting outCSVData according to tilePosition
    Collections.sort(outCSVData, new Comparator<String[]>()
        {
          public int compare(String[] o1, String[] o2)
          {
            //compare two object and return an integer
            return o1[3].compareTo(o2[3]);
          }
        });

    String[] CSVHeader = {"gmlid", "envelope", "envelopeCentroid", "tiles", "filename"};

    this.tilingCSV = Paths.get(this.outputDir, "sorted"+ "_" + outCsvName + "_" +
        String.valueOf(dataContent.size()) + outFileExt);

    try (CSVWriter writer = new CSVWriter(new FileWriter(this.tilingCSV.toString()))) {
        writer.writeNext(CSVHeader);
        writer.writeAll(outCSVData);
        System.out.println("The tiles information is stored in " + this.tilingCSV.toString());
      } catch (IOException e) {
        e.printStackTrace();
      }

    //long finish2 = System.currentTimeMillis();
    //System.out.println("KMLTilingTask took total time: " + (finish2 - start1) + " ms");
  }

  /** KML Tiling: Calucate the nCol and nRow based on meter
   *  update tileWidth and tileLength that it needs for the following tiling */
  private int[] calcNumRowCol() {

    this.nCol = (int) (Math.ceil(
        (this.Textent_Ymax - this.Textent_Ymin) / initTileSize));
    this.nRow = (int) (Math.ceil(
        (this.Textent_Xmax - this.Textent_Xmin) / initTileSize));

    this.tileLength = (this.Textent_Xmax - this.Textent_Xmin) / this.nRow;
    this.tileWidth = (this.Textent_Ymax - this.Textent_Ymin) / this.nCol;

    return new int[]{nRow, nCol};
  }

  /** KML Tiling: Reproject the geometry from from_epsg to to_epsg */
  private double[] reproject(String geomStr, int from_epsg, int to_epsg) {
    String[] splitStr = geomStr.split("#");
    double[] geomArr = new double[splitStr.length];
    for (int i = 0; i < splitStr.length; i++) {
      geomArr[i] = Double.valueOf(splitStr[i]);
    }
    Geometry geom = null;
    double[] transformedGeom = null;

    if (geomArr.length == 2) {
      // This is a centroid
      transformedGeom = Transform.reprojectPoint(geomArr, from_epsg, to_epsg);

    } else if (geomArr.length == 4) {
      // This is an envelope with [xmin, xmax, ymin, ymax]
      transformedGeom = Transform.reprojectEnvelope(geomArr, from_epsg, to_epsg);
    } else {
      // this is a polygon in general
      System.err.println("The input parameter is neither a point or envelop with 4 indicators");
    }

    return transformedGeom;
  }

  /** KML Tiling: Compute the belonging tile of a cityobject based on the transformed Extent and centroid of the cityobject (based on meter) */
  private Integer[] assignTiles(String centroidStr) {
    double[] transformedCentroid = reproject(centroidStr, Integer.valueOf(crsWGS84), Integer.valueOf(CRSinMeter));
    Integer col = (int) (Math.floor(
        (transformedCentroid[0] - this.Textent_Ymin) / this.tileWidth));
    Integer row = (int) (Math.floor(
        (transformedCentroid[1] - this.Textent_Xmin) / this.tileLength));
    if (col < 0 || row < 0) {
      System.out.println(centroidStr + " is located " + row + ", " + col); }
    return new Integer[]{row, col};
  }

  /** KML Tiling: To Note: Need to be careful with latitude and longitude , Check with epsg.io
  * Creation of MasterJson file, which is used to name the tiles' filename in KMLSorterTask.java */
  private Path createMasterJson(){
    LinkedHashMap<String, Object> masterjson = new LinkedHashMap<>();
    masterjson.put("version", "1.0.0");
    masterjson.put("layername", layerName);  // should not be hardcoded, usually "test"
    masterjson.put("fileextension", ".kml");
    masterjson.put("displayform", displayForm); // should not be hardcoded, e.g., "extruded"
    masterjson.put("minLodPixels", 10);
    masterjson.put("maxLodPixels", -1);
    masterjson.put("colnum", this.nCol );
    masterjson.put("rownum", this.nRow);

    LinkedHashMap<String, Double> bbox = new LinkedHashMap<>();
    bbox.put("xmin", this.extent_Ymin);
    bbox.put("xmax", this.extent_Ymax);
    bbox.put("ymin", this.extent_Xmin);
    bbox.put("ymax", this.extent_Xmax);
    masterjson.put("bbox", bbox);

    masterjson.put("iriprefix", this.iriprefix + "cityobject/");

    // This GSON library can help to write pretty-printed json file
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String prettyJsonString = gson.toJson(masterjson, masterjson.getClass());

    System.out.println(prettyJsonString);

    // Construct masterjson file name and path
    Path masterJson = Paths.get(this.outputDir, layerName + '_' + displayForm + '_'+ "MasterJSON.json");

    try (BufferedWriter writer = Files.newBufferedWriter(masterJson)) {
      writer.write(prettyJsonString);
    } catch (Exception ex) {
      System.err.println("Couldn't write masterjson\n" + ex.getMessage());
    }
    return masterJson;
  }

  /****** KML Sorting ***************************************************************************************************/

  private void sortingKML(){

    /**
     this.inputPath = inputPath;
     this.unsortedDir = Utilities.getInputDir(inputPath);
     this.unsortedFiles = Utilities.getInputFiles(inputPath);
     this.projectFolder = projectFolder; // the folder where the Tiles folder will be put and diverse summary file
     this.masterJSONFile = masterJSONFile;
     this.summaryCSV = sortedSummary;
     this.outputDir = Utilities.createDir(projectFolder + tilesFolder);

     JSONObject masterJSON;
     try {
     masterJSON = new JSONObject(new String(Files.readAllBytes(Paths.get(masterJSONFile))));
     } catch (IOException e) {
     throw new JPSRuntimeException(e);
     }*/
    long start = System.currentTimeMillis();
    createTilesDirs(this.outputDir);

    this.csvData = readCSV2Map(this.tilingCSV.toString());
    initFilesStatus(unsortedKMLdir);  // initial all files are not visited

    List<StyleSelector> styles;

    // Read the one kml file to allFeatures, else we will use the hashmap to store the features
    String[] filesList = Utilities.getInputFiles(unsortedKMLdir);


    if (filesList.length == 1){
      // only 1 file is contained; as the file may be very large, the design should avoid reading it multiple times
      Kml kml = Kml.unmarshal(new File(filesList[0]));  // if single file: unsortedDir == fileList[0]
      Document doc = (Document) kml.getFeature();
      styles = doc.getStyleSelector();
      List<Feature> features = ((Document) kml.getFeature()).getFeature();
      for (Feature feat: features){
        String gmlid = feat.getName();
        featuresMap.put(gmlid, feat);
      }
      // avoid the re-visit of this file in the future
      updateFileStatus(filesList[0], true);
    }else{
      // if there is more than 1 file, this step will only retrieve the stylesSelector of the first file
      styles = getStylesFromKml(new File(filesList[0]));
    }

    for (int i = 0; i < this.nRow; i++) {
      for (int k = 0; k < this.nCol; k++) {
        String name = this.layerName + "_Tile_" + i + "_" + k + "_" + this.displayForm + ".kml";
        File out = new File(Paths.get(this.outputDir,"Tiles", String.valueOf(i), String.valueOf(k), name).toString());
        if (out.exists()){
          continue;
        }
        HashMap<String, ArrayList<String>> map = getBuildingsInTile(i, k);
        List<Feature> features = getFeaturesInTile(map);  // read the features from the files
        writeKml(out, features, styles, name);
        System.out.println("finished writing " + out.getAbsolutePath());
      }
    }

    long end = System.currentTimeMillis();
    long duration = (end - start);
    System.out.println("KMLSorterTask takes: " + duration + " ms");
    System.out.println("start: " + start + " ms\nend: " + end + " ms"); // @todo: need to genrate the run journal file
    System.out.println("buildings: " + this.count);
    System.out.println("Features have written: " + this.buildingInTiles);
    System.out.println("The tiling process is done!!");

  }

  /** KML Sorting: Only execute one time in the beginning */
  private void initFilesStatus(String inputDir){

    String[] filesList = Utilities.getInputFiles(inputDir);

    for (String filepath : filesList){
      this.fileStatus.put(filepath, false);
    }
  }

  /** KML Sorting: Check the import status of the files */
  private boolean isImported(String fileName){
    return this.fileStatus.get(fileName);
  }

  /** KML Sorting: */
  private void updateFileStatus(String filepath, boolean status){
    this.fileStatus.replace(filepath, status);
  }

  /** KML Sorting: Could not use tiles as Hashmap key, as there is repeatness.
   * if used, add all String[] as list */
  private HashMap<String, ArrayList<String[]>> readCSV2Map(String csvfile) {
    HashMap<String, ArrayList<String[]>> CSVRows = new HashMap<>();
    List<String[]> csvData = new ArrayList<>();

    try (CSVReader reader = new CSVReader(new FileReader(csvfile))) {
      String[] csvHeader = reader.readNext();
      csvData = reader.readAll();
    } catch (IOException | CsvException e) {
      e.printStackTrace();
    }

    String currKey = "";
    ArrayList<String[]> currValue = new ArrayList<>();
    ArrayList<String> gmlidList = new ArrayList<>();
    for (String[] row : csvData) {
      gmlidList.add(row[0]);

      if (currKey.isEmpty() && currValue.isEmpty()) {
        currKey = row[3];
        currValue.add(row);
      }
      else if (currKey.equals(row[3])) {
        currValue.add(row);
      }else if (!currKey.equals(row[3]) && !currValue.isEmpty()){
        // clean the currKey and currValues by putting them into map
        CSVRows.put(currKey, currValue);
        currKey = "";
        currValue = new ArrayList<>();
        // update the currKey and currValue
        currKey = row[3];
        currValue.add(row);
      }else{
        System.out.println("Not Processing " + row[0]);
      }
    }
    // need to check if the last row is added
    if (!CSVRows.containsKey(currKey)) {
      CSVRows.put(currKey, currValue);
    }

    int csvrowSize = 0;
    for ( String key : CSVRows.keySet() ) {
      csvrowSize += CSVRows.get(key).size();
    }
    System.out.println("gmlidList has the size of " + gmlidList.size());
    System.out.println("gmlidList is unique : " + Utilities.isUnique(gmlidList));
    System.out.println("CSVRows has the size of " + csvrowSize);

    return CSVRows;
  }

  /**  KML Sorting:
   * Given a hashmap with filename (Key) and Array of gmlIds (Values),
   * return a list of features belonging to the tile
   *
   * @param map a hashmap with filename (Key) and Array of gmlIds (Values)
   * @return List of features
   */
  private List<Feature> getFeaturesInTile(HashMap<String, ArrayList<String>> map) {
    List<Feature> featuresInTile = new ArrayList<>();
    // loop through for each kml file that has buildings in this tile
    for (String fileName : map.keySet()) {   // filename should be a full path
      HashMap<String, Feature> featuresInKml = new HashMap<>();

      if (!isImported(fileName)){
        // get all the buildings in the current kml file
        featuresInKml = getFeaturesFromKml(new File(fileName));
        this.featuresMap.putAll(featuresInKml);
        updateFileStatus(fileName, true);
      }
      // Find the corresponding features from global featuresMap
      for (String buildingId : map.get(fileName)) {
        if (this.featuresMap.containsKey(buildingId)){
          Feature feature = this.featuresMap.get(buildingId);
          Placemark placemark = (Placemark) feature;
          MultiGeometry geoms = (MultiGeometry) placemark.getGeometry();
          for (de.micromata.opengis.kml.v_2_2_0.Geometry geom : geoms.getGeometry()) {
            Polygon polygon = (Polygon) geom;

            if (displayForm == "extruded"){
              polygon.setExtrude(true);
              polygon.setTessellate(true);
            }
          }
          // put feature info into list to be returned
          featuresInTile.add(feature);
          this.featuresMap.remove(buildingId);
        } else{
          System.err.println(buildingId + "does not exist in the featuresMap");
          break;
        }
      }
    }
    return featuresInTile;
  }

  /**
   *  KML Sorting: Given tile position, create a hashmap with filename (Key) and Array of gmlIds (Values)
   *
   * @param rowNum the row number of the tile
   * @param colNum the column number of the tile
   * @return hashmap with <filename (Key) , Array of gmlIds (Values) >
   */
  private HashMap<String, ArrayList<String>> getBuildingsInTile(int rowNum, int colNum) {
    // key is the file location, value is an array of gmlIds in this file location that belongs to this tile
    HashMap<String, ArrayList<String>> buildings = new HashMap<>();   // building hashmap contains filename (key) and gmlids (value)

    if (this.csvData.containsKey(rowNum + "#" + colNum)) {
      // add buildings
      for (String[] rowInfo : csvData.get(rowNum + "#" + colNum)){
        String filename = rowInfo[4];
        String filepath = Paths.get(unsortedKMLdir, filename).toString();
        if (buildings.containsKey(filepath)){
          // if this file location has already been added to map, add the gmlId to existing array
          buildings.get(filepath).add(rowInfo[0]);
          this.count++;
        }else{
          // if the file location is not in the map, add it to map
          ArrayList<String> mapValue = new ArrayList<>();
          mapValue.add(rowInfo[0]);
          buildings.put(filepath, mapValue);
          this.count++;
        }

      }
    }
    return buildings;
  }


  // create one List<Feature> for each tile
  private void createFeatureLists() {
    for (int i = 0; i < this.nRow; i++) {
      for (int k = 0; k < this.nCol; k++) {
        String key = this.layerName + "_Tile_" + i + "_" + k + "_" + this.displayForm;
        this.tileFeatureMap.put(key, new ArrayList<>());
      }
    }
  }


  // Shiying: get all features from one kml file
  private HashMap<String, Feature> getFeaturesFromKml(File file) {
    HashMap<String, Feature> featuresMap = new HashMap<>();
    Kml kml = Kml.unmarshal(file);
    List<Feature> features = ((Document) kml.getFeature()).getFeature();
    for (Feature feat: features){
      String gmlid = feat.getName();
      featuresMap.put(gmlid, feat);
    }
    return featuresMap;
  }

  // get style list from one kml file
  private List<StyleSelector> getStylesFromKml(File file) {
    Kml kml = Kml.unmarshal(file);
    Document doc = (Document) kml.getFeature();
    List<StyleSelector> styles = doc.getStyleSelector();
    return styles;
  }

  // create folders for tiles
  private void createTilesDirs (String dir) {
    File parentFolder = new File(dir + "\\Tiles");
    if (parentFolder.mkdir()) {
      for (int i = 0; i < this.nRow; i++) {
        File rowFolder = new File(parentFolder.getAbsolutePath() + "\\" + i);
        if (rowFolder.mkdir()) {
          for (int k = 0; k < this.nCol; k++) {
            File colFolder = new File(rowFolder.getAbsolutePath() + "\\" + k);
            if (!colFolder.mkdir()) {
              throw new JPSRuntimeException("Failed to make column folder " + colFolder.getAbsolutePath());
            }
          }
        } else {
          throw new JPSRuntimeException("failed to make row folder " + rowFolder.getAbsolutePath());
        }
      }
    }

  }

  /** KML Sorting: write new kml files */
  private void writeKml(File file, List<Feature> list, List<StyleSelector> styles, String name) {
    this.buildingInTiles += list.size();
    Kml kml = new Kml();
    kml.createAndSetDocument().withName(name).withOpen(false).withFeature(list).withStyleSelector(styles);
    try (OutputStream out = new FileOutputStream(file)) {
      kml.marshal(out);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}



