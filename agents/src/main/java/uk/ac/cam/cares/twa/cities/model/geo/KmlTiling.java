package uk.ac.cam.cares.twa.cities.model.geo;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.ogr;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osr;

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
  public void updateExtent(double[] geomEnvelope){

    double xmin = geomEnvelope[0];
    double xmax = geomEnvelope[1];
    double ymin = geomEnvelope[2];
    double ymax = geomEnvelope[3];

    if (xmin <= this.extent_Xmin) { this.extent_Xmin = xmin; }
    if (xmax >= this.extent_Xmax) { this.extent_Xmax = xmax; }
    if (ymin <= this.extent_Ymin) { this.extent_Ymin = ymin; }
    if (ymax >= this.extent_Ymax) { this.extent_Ymax = ymax; }
  }

  // Return the latest status of the Extent
  public double[] getExtent() {
    return new double[]{this.extent_Xmin, this.extent_Xmax, this.extent_Ymin, this.extent_Ymax};
  }

  /* The input is extentDim with 4326 */
  public double[] getNumRowCol() {

    // translate the lowerCorner and upperCorner to 25833 for dividing 125
    Geometry lowerCorner = new Geometry(ogr.wkbPoint); // [xmin, ymin]
    lowerCorner.AddPoint(this.extent_Xmin, this.extent_Ymin);

    Geometry upperCorner = new Geometry(ogr.wkbPoint); // [xmax, ymax]
    upperCorner.AddPoint(this.extent_Xmax, this.extent_Ymax);

    double[] clowerCorner = reprojectPoint(new double[]{this.extent_Xmin, this.extent_Ymin}, 4326, 25833);
    double[] cupperCorner = reprojectPoint(new double[]{this.extent_Xmax, this.extent_Ymax}, 4326, 25833);

    transformedExtent = new double[]{clowerCorner[0], cupperCorner[0], clowerCorner[1], cupperCorner[1]};

    if (transformedExtent[1] - transformedExtent[0] < 125 || transformedExtent[3] - transformedExtent[2] < 125) {
      System.out.println("the input parameter are invalid, smaller than default length");
    } else {
      this.nRow = (int)(Math.ceil((transformedExtent[3] - transformedExtent[2]) / this.tileLength ));
      this.nCol = (int)(Math.ceil((transformedExtent[1] - transformedExtent[0]) / this.tileLength ));
    }

    return new double[]{nRow, nCol};
  }

  public double[] reprojectPoint(double[] centroid, int from_epsg, int to_epsg){
    Geometry point = new Geometry(ogr.wkbPoint);

    SpatialReference source = new SpatialReference();
    source.ImportFromEPSG(from_epsg);

    SpatialReference target = new SpatialReference();
    target.ImportFromEPSG(to_epsg);

    CoordinateTransformation transformMatrix = osr.CreateCoordinateTransformation(source, target);
    point.AddPoint(centroid[0], centroid[1]);
    double[] transformed = transformMatrix.TransformPoint( point.GetX(), point.GetY()); //  transform seems to be for more than a point

    return transformed;
  }

  public double[] reproject(String geomStr, int from_epsg, int to_epsg) {
    String[] splitStr = geomStr.split("#");
    double[] geomArr = new double[splitStr.length];
    for (int i = 0; i < splitStr.length; i++){
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
    Integer col = (int)(Math.floor((transformedCentroid[0] - this.transformedExtent[0]) / this.tileLength));
    Integer row = (int)(Math.floor((transformedCentroid[1] - this.transformedExtent[2]) / this.tileLength));

    return new Integer[]{col, row};
  }

  private <T> String arr2str (T[] arr){
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

  public static void main(String[] args) {

    String inputfile = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\summary.csv";

    List<String[]> csvData = new ArrayList<>();
    KmlTiling kmltiling = new KmlTiling("4326");
    kmltiling.updateExtent(new double[]{13.09278683392157, 13.758936971880468, 52.339762874361156, 52.662766032905616});
    double[] numColRow = kmltiling.getNumRowCol();

    try (CSVReader reader = new CSVReader(new FileReader(inputfile))) {
      csvData = reader.readAll();
      //r.forEach(x -> System.out.println(Arrays.toString(x)));
    } catch (IOException e) {
      e.printStackTrace();
    } catch (CsvException e) {
      e.printStackTrace();
    }
    Integer[] tilePosition = new Integer[2];
    List<String[]> outputData = new ArrayList<>();
    String[] header = {"gmlid", "envelope", "envelopeCentroid", "tiles" ,"filename"};
    outputData.add(header);

    for (int i = 195000; i < csvData.size()-1; ++i) {  // skip the header
      tilePosition = kmltiling.assignTiles(csvData.get(i)[2]);
      String[] outRow = {csvData.get(i)[0], csvData.get(i)[1], csvData.get(i)[2],
          kmltiling.arr2str(tilePosition), csvData.get(i)[3]};
      outputData.add(outRow);
      if ( i % 5000 == 0){
        System.out.println("Finished processing " + i + " items at "+ csvData.get(i)[0]);
        try (CSVWriter writer = new CSVWriter(new FileWriter(kmltiling.outputDir + kmltiling.outCsvFile + "_" + i + kmltiling.typecsv))) {
          writer.writeAll(outputData);
          outputData = new ArrayList<>();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }



  }
}
