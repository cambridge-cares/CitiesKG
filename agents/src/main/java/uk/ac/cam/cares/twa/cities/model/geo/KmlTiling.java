package uk.ac.cam.cares.twa.cities.model.geo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.opencsv.CSVReader;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.ogr;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osr;
import org.json.JSONObject;

public class KmlTiling {

  private int nRow;
  private int nCol;
  private String crs;
  private double extent_Xmin = Double.POSITIVE_INFINITY;
  private double extent_Xmax = Double.NEGATIVE_INFINITY;
  private double extent_Ymin = Double.POSITIVE_INFINITY;
  private double extent_Ymax = Double.NEGATIVE_INFINITY;
  private double tileLength = 125;
  private double[] transformedExtent = new double[4]; // [Xmin, Xmax, Ymin, Ymax]  // 25833
  public String outCsvFile = "new_summary";
  public String typecsv = ".csv";
  public String outputDir = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\";


  public KmlTiling(String crs) {
    this.crs = crs;
  }

  // Updated Extent: 13.09278683392157 13.758936971880468 52.339762874361156 52.662766032905616
  /*
   * Update the boundary of the extent */
  public void updateExtent(double[] geomEnvelope) {

    double xmin = geomEnvelope[0];
    double xmax = geomEnvelope[1];
    double ymin = geomEnvelope[2];
    double ymax = geomEnvelope[3];

    if (xmin <= this.extent_Xmin) {
      this.extent_Xmin = xmin;
    }
    if (xmax >= this.extent_Xmax) {
      this.extent_Xmax = xmax;
    }
    if (ymin <= this.extent_Ymin) {
      this.extent_Ymin = ymin;
    }
    if (ymax >= this.extent_Ymax) {
      this.extent_Ymax = ymax;
    }
  }

  // Return the latest status of the Extent
  public double[] getExtent() {
    return new double[]{this.extent_Xmin, this.extent_Xmax, this.extent_Ymin, this.extent_Ymax};
  }

  /* The input is extentDim with 4326 */
  public int[] getNumRowCol() {

    // translate the lowerCorner and upperCorner to 25833 for dividing 125
    Geometry lowerCorner = new Geometry(ogr.wkbPoint); // [xmin, ymin]
    lowerCorner.AddPoint(this.extent_Xmin, this.extent_Ymin);

    Geometry upperCorner = new Geometry(ogr.wkbPoint); // [xmax, ymax]
    upperCorner.AddPoint(this.extent_Xmax, this.extent_Ymax);

    double[] clowerCorner = reprojectPoint(new double[]{this.extent_Xmin, this.extent_Ymin}, 4326,
        25833);
    double[] cupperCorner = reprojectPoint(new double[]{this.extent_Xmax, this.extent_Ymax}, 4326,
        25833);

    transformedExtent = new double[]{clowerCorner[0], cupperCorner[0], clowerCorner[1],
        cupperCorner[1]};

    if (transformedExtent[1] - transformedExtent[0] < 125
        || transformedExtent[3] - transformedExtent[2] < 125) {
      System.out.println("the input parameter are invalid, smaller than default length");
    } else {
      this.nRow = (int) (Math.ceil(
          (transformedExtent[3] - transformedExtent[2]) / this.tileLength));
      this.nCol = (int) (Math.ceil(
          (transformedExtent[1] - transformedExtent[0]) / this.tileLength));
    }

    return new int[]{nRow, nCol};
  }

  public double[] reprojectPoint(double[] centroid, int from_epsg, int to_epsg) {
    Geometry point = new Geometry(ogr.wkbPoint);

    SpatialReference source = new SpatialReference();
    source.ImportFromEPSG(from_epsg);

    SpatialReference target = new SpatialReference();
    target.ImportFromEPSG(to_epsg);

    CoordinateTransformation transformMatrix = osr.CreateCoordinateTransformation(source, target);
    point.AddPoint(centroid[0], centroid[1]);
    double[] transformed = transformMatrix.TransformPoint(point.GetX(),
        point.GetY()); //  transform seems to be for more than a point

    return transformed;
  }

  public double[] reproject(String geomStr, int from_epsg, int to_epsg) {
    String[] splitStr = geomStr.split("#");
    double[] geomArr = new double[splitStr.length];
    for (int i = 0; i < splitStr.length; i++) {
      geomArr[i] = Double.valueOf(splitStr[i]);
    }
    Geometry geom = null;
    double[] transformedGeom = null;

    if (geomArr.length == 2) {
      // This is a centroid
      transformedGeom = reprojectPoint(geomArr, from_epsg, to_epsg);

    } else if (geomArr.length == 4) {
      // This is an envelope with [xmin, xmax, ymin, ymax]

    } else {
      // this is a polygon in general
      System.out.println("The input parameter is neither a point or envelop with 4 indicators");
    }

    return transformedGeom;
  }

  /* Compute the tile location based on the transformed extent (based on meter)*/
  public Integer[] assignTiles(String centroidStr) {
    double[] transformedCentroid = reproject(centroidStr, 4326, 25833);
    Integer col = (int) (Math.floor(
        (transformedCentroid[0] - this.transformedExtent[0]) / this.tileLength));
    Integer row = (int) (Math.floor(
        (transformedCentroid[1] - this.transformedExtent[2]) / this.tileLength));

    return new Integer[]{col, row};
  }

  private <T> String arr2str(T[] arr) {
    String output = "";
    String sep = "#";

    for (int j = 0; j < arr.length; j++) {
      output += String.valueOf(arr[j]);
      if (j != arr.length - 1) {
        output += sep;
      }
    }

    return output;
  }


  public void createMasterJson(){

    LinkedHashMap masterjson = new LinkedHashMap();
    masterjson.put("version", "1.0.0");
    masterjson.put("layername", "test");
    masterjson.put("fileextension", ".kml");
    masterjson.put("displayform", "extruded");
    masterjson.put("minLodPixels", 0);
    masterjson.put("maxLodPixels", -1);
    masterjson.put("colnum", 368 );
    masterjson.put("rownum", 280);

    LinkedHashMap bbox = new LinkedHashMap();
    bbox.put("xmin", 13.09278683392157);
    bbox.put("xmax", 13.758936971880468);
    bbox.put("ymin", 52.339762874361156);
    bbox.put("ymax", 52.662766032905616);
    masterjson.put("bbox", bbox);

    // This GSON library can help to write pretty-printed json file
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String prettyJsonString = gson.toJson(masterjson, masterjson.getClass());
    System.out.println(prettyJsonString);

    String fileName = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\test_extruded_MasterJSON.json";
    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName))) {
      writer.write(prettyJsonString);
    } catch (Exception ex) {
      System.err.println("Couldn't write masterjson\n" + ex.getMessage());
    }

  }

  public String getFileName(String inputStr, String keyword){
    int i = inputStr.indexOf(keyword);
    String filename = inputStr.substring(i, inputStr.length());
    return filename;
  }

  public static void main(String[] args) {

    String inputDir = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\summary_chunk5000\\";
    File directoryPath = new File(inputDir);
    String[] filelist = directoryPath.list();

    List<String[]> csvData = new ArrayList<>();
    KmlTiling kmltiling = new KmlTiling("4326");
    kmltiling.updateExtent(new double[]{13.09278683392157, 13.758936971880468, 52.339762874361156,
        52.662766032905616});
    int[] numRowCol = kmltiling.getNumRowCol();
    //kmltiling.createMasterJson();
    long start0 = System.currentTimeMillis();

    for (String inputfile : filelist) {
      try (CSVReader reader = new CSVReader(new FileReader(inputDir + inputfile))) {
        csvData = reader.readAll();
        //r.forEach(x -> System.out.println(Arrays.toString(x)));
      } catch (IOException e) {
        e.printStackTrace();
      } catch (CsvException e) {
        e.printStackTrace();
      }

      Integer[] tilePosition;

      List<String[]> outputData = new ArrayList<>();
      String[] header = {"gmlid", "envelope", "envelopeCentroid", "tiles", "filename"};
      outputData.add(header);

      long start1 = System.currentTimeMillis();
      for (int i = 0; i < csvData.size(); ++i) {  // skip the header
        tilePosition = kmltiling.assignTiles(csvData.get(i)[2]);
        String[] outRow = {csvData.get(i)[0], csvData.get(i)[1], csvData.get(i)[2],
            kmltiling.arr2str(tilePosition), kmltiling.getFileName(csvData.get(i)[3], "test")};
        outputData.add(outRow);

      }
      System.out.println("Finished processing " + inputfile);
      long finish1 = System.currentTimeMillis();
      try (CSVWriter writer = new CSVWriter(new FileWriter(
          kmltiling.outputDir + "new_" + inputfile))) {
        writer.writeAll(outputData);
        outputData = new ArrayList<>(); // empty outputData
        finish1 = System.currentTimeMillis();

      } catch (IOException e) {
        e.printStackTrace();
      }
      System.out.println("Process time: " + (finish1 - start1));
    }
    long finish0 = System.currentTimeMillis();
    System.out.println("Total time: " + (finish0 - start0));
  }
  }



