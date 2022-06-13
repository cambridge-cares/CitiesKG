package uk.ac.cam.cares.twa.cities.tasks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import org.gdal.ogr.Geometry;
import uk.ac.cam.cares.twa.cities.model.geo.Transform;
import uk.ac.cam.cares.twa.cities.model.geo.Utilities;

/** KMLTilingTask intends to postprocess the exported KML files (unsorted) to spatially organised tiles
 *  It contains 3 steps: KMLParser, KMLTiler, KMLSorter
 *
 *  KMLParser: the output of running this class should be the generation of the summary file in csv
 *                 (gmlid, envelope[xmin, xmax, ymin, ymax], envelopeCentroid, corresponding file)
 *
 *  KMLTiler: This class will help to calculate the NumColRow (number of rows and columns) based on the initial tile size
 *
 *  KMLSorter:
 *
 *                 */
public class KMLTilingTask_backup implements Runnable{

  private int nRow;
  private int nCol;
  private String sourceSRID;
  private String targetSRID;
  private double extent_Xmin;
  private double extent_Xmax;
  private double extent_Ymin;
  private double extent_Ymax;
  private double Textent_Xmin;
  private double Textent_Xmax;
  private double Textent_Ymin;
  private double Textent_Ymax;
  private double initTileSize;  //125, 250
  private double tileLength;  // RowNumber
  private double tileWidth;  // ColNumber
  private String masterJSONName = "test_extruded_MasterJSON.json";
  public String sortedCSVname = "sorted_tiles_summary.csv";

  private double[] transformedExtent = new double[4]; // [Xmin, Xmax, Ymin, Ymax]  // 25833
  private String outCsvFile = "new_summary";
  private String typecsv = ".csv";
  private String[] CSVHeader = {"gmlid", "envelope", "envelopeCentroid", "tiles", "filename"};
  private String outputDir;
  public String[] filesList;
  public String sortedCSVFile;
  private String masterJSONFile;

  public KMLTilingTask_backup(String kmlCRS, String targetCRS, String outputDir, int initTileSize) {

    this.sourceSRID = kmlCRS;
    this.targetSRID = targetCRS;
    extent_Xmin = Double.POSITIVE_INFINITY;
    extent_Xmax = Double.NEGATIVE_INFINITY;
    extent_Ymin = Double.POSITIVE_INFINITY;
    extent_Ymax = Double.NEGATIVE_INFINITY;

    Textent_Xmin = Double.POSITIVE_INFINITY;
    Textent_Xmax = Double.NEGATIVE_INFINITY;
    Textent_Ymin = Double.POSITIVE_INFINITY;
    Textent_Ymax = Double.NEGATIVE_INFINITY;

    this.outputDir = outputDir;
    this.sortedCSVFile = this.outputDir + sortedCSVname;
    this.masterJSONFile = this.outputDir + masterJSONName;
    this.initTileSize = initTileSize;

  }

  @Override
  public void run() {

    // Prepare the parameter (number of columns and rows) and update tileWidth and tileLength that it needs for the following tiling
    int[] numRowCol = getNumRowCol();
    createMasterJson();

    // Start calculating tile position
    long start1 = System.currentTimeMillis();
    List<String[]> csvData = new ArrayList<>();
    CSVParser csvParser = new CSVParserBuilder().withEscapeChar('\0').build(); // with this '\0' can read backslash; with '\\' can not read backslash

    for (String inputfile : filesList) {
      try (CSVReader reader = new CSVReaderBuilder(new FileReader(inputfile)).withCSVParser(csvParser).build()) {
        String[] header = reader.readNext();
        csvData = reader.readAll();
      } catch (IOException e) {
        e.printStackTrace();
      } catch (CsvException e) {
        e.printStackTrace();
      }

      Integer[] tilePosition;
      List<String[]> outCSVData = new ArrayList<>();

      for (String[] inputRow : csvData) {  // skip the header
        tilePosition = assignTiles(inputRow[2]);
        String[] outRow = {inputRow[0], inputRow[1], inputRow[2],
            Utilities.arr2str(tilePosition), inputRow[3]};  //kmltiling.getFileName(csvData.get(i)[3], "test")
        outCSVData.add(outRow);
      }
      long finish1 = System.currentTimeMillis();
      System.out.println("Reading the files took " + (finish1 - start1) + " ms");
      Collections.sort(outCSVData, new Comparator<String[]>()
          {
            public int compare(String[] o1, String[] o2)
            {
              //compare two object and return an integer
              return o1[3].compareTo(o2[3]);}
          }
      );

      try (CSVWriter writer = new CSVWriter(new FileWriter(this.sortedCSVFile))) { //inputfile
        writer.writeNext(CSVHeader);
        writer.writeAll(outCSVData);
        outCSVData = new ArrayList<>(); // empty outputData
        finish1 = System.currentTimeMillis();
        System.out.println("The tiles information is stored in " + this.sortedCSVFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    long finish2 = System.currentTimeMillis();

    System.out.println("KMLTilingTask took total time: " + (finish2 - start1) + " ms");
  }

  public void setUp(String summaryCSV){
    this.filesList = Utilities.getInputFiles(summaryCSV);
  }

  public String getsortedCSVFile(){
    return this.sortedCSVFile;
  }
  public String getmasterJSONFile() {
    return this.masterJSONFile;
  }
  // Updated Extent: 13.09278683392157 13.758936971880468 52.339762874361156 52.662766032905616
  /*
   * Update the boundary of the extent */

  public void updateExtent(double[] geomEnvelope) {  //[xmin, xmax, ymin, ymax]

    // updateExtent in 4236
    if (geomEnvelope[0] <= this.extent_Xmin) { this.extent_Xmin = geomEnvelope[0]; }
    if (geomEnvelope[1] >= this.extent_Xmax) { this.extent_Xmax = geomEnvelope[1]; }
    if (geomEnvelope[2] <= this.extent_Ymin) { this.extent_Ymin = geomEnvelope[2]; }
    if (geomEnvelope[3] >= this.extent_Ymax) { this.extent_Ymax = geomEnvelope[3]; }

    // updateExtent in 25833
    //double[] testTenvelope = {1.3110057376503, 1.31143044782066, 103.818749218958, 103.819364392128};
    //geomEnvelope = new double[]{geomEnvelope[2], geomEnvelope[3], geomEnvelope[0], geomEnvelope[1]};  // switch the coordinates
    double[] Tenvelope = Transform.reprojectEnvelope(geomEnvelope, Integer.valueOf(sourceSRID), Integer.valueOf(targetSRID));

    if (Tenvelope[0] <= this.Textent_Xmin) { this.Textent_Xmin = Tenvelope[0]; }
    if (Tenvelope[1] >= this.Textent_Xmax) { this.Textent_Xmax = Tenvelope[1]; }
    if (Tenvelope[2] <= this.Textent_Ymin) { this.Textent_Ymin = Tenvelope[2]; }
    if (Tenvelope[3] >= this.Textent_Ymax) { this.Textent_Ymax = Tenvelope[3]; }

  }
  /*
  public void updateExtent(double[] geomEnvelope) {  //[xmin, xmax, ymin, ymax]

    // updateExtent in 4236
    if (geomEnvelope[0] <= this.extent_Xmin) { this.extent_Xmin = geomEnvelope[0]; }    // X : latitude
    if (geomEnvelope[1] >= this.extent_Xmax) { this.extent_Xmax = geomEnvelope[1]; }
    if (geomEnvelope[2] <= this.extent_Ymin) { this.extent_Ymin = geomEnvelope[2]; }
    if (geomEnvelope[3] >= this.extent_Ymax) { this.extent_Ymax = geomEnvelope[3]; }

    // updateExtent in 25833
    //double[] testTenvelope = {1.3110057376503, 1.31143044782066, 103.818749218958, 103.819364392128};
    //geomEnvelope = new double[]{geomEnvelope[2], geomEnvelope[3], geomEnvelope[0], geomEnvelope[1]};  // switch the coordinates
    double[] lowerCorner = {geomEnvelope[0], geomEnvelope[2]};
    double[] upperCorner = {geomEnvelope[1], geomEnvelope[3]};
    double[] TlowerCorner = Transform.reprojectPoint(lowerCorner, Integer.valueOf(kmlSRID), Integer.valueOf(transformSRID));
    double[] TupperCorner = Transform.reprojectPoint(upperCorner, Integer.valueOf(kmlSRID), Integer.valueOf(transformSRID));
    double[] Tenvelope = {TlowerCorner[0], TupperCorner[0], TlowerCorner[1], TupperCorner[1]};

    if (Tenvelope[0] <= this.Textent_Xmin) { this.Textent_Xmin = Tenvelope[0]; }
    if (Tenvelope[1] >= this.Textent_Xmax) { this.Textent_Xmax = Tenvelope[1]; }
    if (Tenvelope[2] <= this.Textent_Ymin) { this.Textent_Ymin = Tenvelope[2]; }
    if (Tenvelope[3] >= this.Textent_Ymax) { this.Textent_Ymax = Tenvelope[3]; }

  }
*/
  public void updateExtent(double[][] geometry){

    double[] origEnvelop = Transform.getEnvelop(geometry);

    // updateExtent in 4236
    if (origEnvelop[0] <= this.extent_Xmin) { this.extent_Xmin = origEnvelop[0]; }
    if (origEnvelop[1] >= this.extent_Xmax) { this.extent_Xmax = origEnvelop[1]; }
    if (origEnvelop[2] <= this.extent_Ymin) { this.extent_Ymin = origEnvelop[2]; }
    if (origEnvelop[3] >= this.extent_Ymax) { this.extent_Ymax = origEnvelop[3]; }

    double[][] Tgeometry = Transform.reprojectGeometry(geometry, Integer.valueOf(sourceSRID), Integer.valueOf(targetSRID));

    double[] Tenvelop = Transform.getEnvelop(Tgeometry);

    if (Tenvelop[0] <= this.Textent_Xmin) { this.Textent_Xmin = Tenvelop[0]; }
    if (Tenvelop[1] >= this.Textent_Xmax) { this.Textent_Xmax = Tenvelop[1]; }
    if (Tenvelop[2] <= this.Textent_Ymin) { this.Textent_Ymin = Tenvelop[2]; }
    if (Tenvelop[3] >= this.Textent_Ymax) { this.Textent_Ymax = Tenvelop[3]; }

  }

  // Return the latest status of the Extent
  public double[] getExtent() {
    return new double[]{this.extent_Xmin, this.extent_Xmax, this.extent_Ymin, this.extent_Ymax};
  }

  /* The input is extentDim with 4326 */
  private int[] getNumRowCol() {

    this.nCol = (int) (Math.ceil(
        (this.Textent_Ymax - this.Textent_Ymin) / this.initTileSize));
    this.nRow = (int) (Math.ceil(
        (this.Textent_Xmax - this.Textent_Xmin) / this.initTileSize));

    this.tileLength = (this.Textent_Xmax - this.Textent_Xmin) / this.nRow;
    this.tileWidth = (this.Textent_Ymax - this.Textent_Ymin) / this.nCol;

    return new int[]{nRow, nCol};

  }

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

  /* Compute the belonging tile of a cityobject based on the transformed Extent and centroid of the cityobject (based on meter) */
  private Integer[] assignTiles(String centroidStr) {
    double[] transformedCentroid = reproject(centroidStr, Integer.valueOf(sourceSRID), Integer.valueOf(targetSRID));
    Integer col = (int) (Math.floor(
        (transformedCentroid[0] - this.Textent_Ymin) / this.tileWidth));
    Integer row = (int) (Math.floor(
        (transformedCentroid[1] - this.Textent_Xmin) / this.tileLength));
    if (col < 0 || row < 0) {
      System.out.println(centroidStr + " is located " + row + ", " + col); }
    return new Integer[]{row, col};
  }

  /* To Note: Need to be careful with latitude and longitude , Check with epsg.io*/
  /* Creation of MasterJson file, which is used to name the tiles' filename in KMLSorterTask.java */
  private void createMasterJson(){
    LinkedHashMap masterjson = new LinkedHashMap();
    masterjson.put("version", "1.0.0");
    masterjson.put("layername", "test");
    masterjson.put("fileextension", ".kml");
    masterjson.put("displayform", "extruded");
    masterjson.put("minLodPixels", 10);
    masterjson.put("maxLodPixels", -1);
    masterjson.put("colnum", this.nCol );
    masterjson.put("rownum", this.nRow);

    LinkedHashMap bbox = new LinkedHashMap();
    bbox.put("xmin", this.extent_Ymin);  // 13.09278683392157
    bbox.put("xmax", this.extent_Ymax);  // 13.758936971880468
    bbox.put("ymin", this.extent_Xmin);  // 52.339762874361156
    bbox.put("ymax", this.extent_Xmax);  //52.662766032905616
    masterjson.put("bbox", bbox);

    // This GSON library can help to write pretty-printed json file
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String prettyJsonString = gson.toJson(masterjson, masterjson.getClass());
    System.out.println(prettyJsonString);

    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(this.masterJSONFile))) {
      writer.write(prettyJsonString);
    } catch (Exception ex) {
      System.err.println("Couldn't write masterjson\n" + ex.getMessage());
    }
  }



  /*
  public static void main(String[] args) {

    //String inputPath = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\summary_chunk5000\\";
    String inputPath = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\summary_532051.csv";
    String outputDir ="C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\";
    KMLTilingTask kmltiling = new KMLTilingTask("4326", "25833", outputDir);
    kmltiling.setUp(inputPath);

    kmltiling.updateExtent(new double[]{13.09278683392157, 13.758936971880468, 52.339762874361156, 52.662766032905616});  // whole berlin

    //kmltiling.updateExtent(new double[]{13.19134362161867, 13.342529716588182, 52.46676146536689, 52.54844920036194}); // charlottenberg self_estimate
    //kmltiling.updateExtent(new double[]{13.188898996667495, 13.34393752497554, 52.46509432020338, 52.549388879408454}); // postgis

    int[] numRowCol = kmltiling.getNumRowCol();
    kmltiling.createMasterJson();
    kmltiling.run();
  }
   */


}



