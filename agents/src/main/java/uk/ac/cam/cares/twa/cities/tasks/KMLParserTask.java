package uk.ac.cam.cares.twa.cities.tasks;

import com.opencsv.CSVWriter;
import gov.nasa.worldwind.ogc.kml.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import uk.ac.cam.cares.twa.cities.model.geo.EnvelopeCentroid;
import uk.ac.cam.cares.twa.cities.model.geo.Utilities;

// the output of running this class should be the generation of the summary file in csv
// (gmlid, envelope[xmin, xmax, ymin, ymax], envelopeCentroid, corresponding file)
public class KMLParserTask implements Runnable{

  private String[] filelist;
  private File currFile;
  private final String outCsvName = "summary_";
  private final String outFileExt = ".csv";
  private final String outputDir;
  private String inputDir;
  public List<String[]> dataContent = new ArrayList<>();
  private List<String> gmlidList = new ArrayList<>();
  private KMLTilingTask kmltiling;
  private String outputFile;

  public KMLParserTask(String intputPath, String outputDir, String CRSinDegree, String CRSinMeter, int initTileSize){

    this.filelist = Utilities.getInputFiles(intputPath);
    this.inputDir = Utilities.getInputDir(intputPath);

    this.outputDir = Utilities.createDir(outputDir);
    this.kmltiling = new KMLTilingTask(CRSinDegree, CRSinMeter, outputDir, initTileSize); // 4326 --> 25833 or 4326 --> 3414
  }

  @Override
  public void run() {
    long start = System.currentTimeMillis();
    for (String file : this.filelist){
      this.currFile = new File(file);
      System.out.println("Reading the file: " + this.currFile);
      KMLRoot kmlRoot = null;
      try {
        kmlRoot = KMLRoot.create(this.currFile);
        kmlRoot.parse();
      } catch (IOException | XMLStreamException e) {
        e.printStackTrace();
      }
      this.getPlacemarks(kmlRoot.getFeature());
    }
    long end1 = System.currentTimeMillis();

    if (this.dataContent.size() != 0) {
      this.outputFile = this.createCSVFile(this.dataContent);

      System.out.println("Checking if the gmlidList is unique: " + Utilities.isUnique(this.gmlidList));
      System.out.println("Saving the file in : " + outputFile);
      System.out.println("Updated Extent: " + kmltiling.getExtent()[0] + " " + kmltiling.getExtent()[1] + " " + kmltiling.getExtent()[2] + " " + kmltiling.getExtent()[3]);
    }

    long end2 = System.currentTimeMillis();
    System.out.println("Reading " + this.filelist.length + " files takes " + (end1 - start) + " ms");
    System.out.println("The KMLParserTask took " + (end2 - start) + " ms");
  }

  public double[] getUpdatedExtent(){
    return kmltiling.getExtent();
  }

  public String getOutFile(){
    return this.outputFile;
  }

  public void getPlacemarks(KMLAbstractFeature feature)
  {
    if (feature instanceof KMLAbstractContainer)
    {
      KMLAbstractContainer container = (KMLAbstractContainer) feature;
      for (KMLAbstractFeature f : container.getFeatures())
      {
        getPlacemarks(f); // recursive
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
        double[] envelope = EnvelopeCentroid.getEnvelope(geom);
        double[] centroid = EnvelopeCentroid.getCentroid(envelope);
        this.gmlidList.add(buildingId);
        // {"gmlid", "envelope", "envelopeCentroid", "filename"};
        String[] row = {buildingId, arr2str(envelope), arr2str(centroid), this.currFile.getName()};
        this.dataContent.add(row);
        kmltiling.updateExtent(envelope);
        //System.out.println("Envelop: "  + envelope[0] + " " + envelope[1] + " " + envelope[2] + " " + envelope[3]);
        //ArrayList geometries = (ArrayList) multiGeometry.getGeometries();
        //ArrayList positionL = (ArrayList) polygon.getOuterBoundary().getCoordinates().list;
        //System.out.println("MulitiGeometry placemark at: " + positionL);
      }

    }
  }

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

  private String createCSVFile(List<String[]> dataContent) {
    String[] header = {"gmlid", "envelope", "envelopeCentroid", "filename"};

    List<String[]> csvContent = new ArrayList<>();
    csvContent.add(header);
    csvContent.addAll(dataContent);
    String outputPath = this.outputDir + "\\" +this.outCsvName + dataContent.size() + this.outFileExt;
    try (CSVWriter writer = new CSVWriter(new FileWriter(outputPath))) {
      writer.writeAll(csvContent);
      System.out.println("The summary has " + dataContent.size() + " rows.");
    } catch (IOException e) {
      e.printStackTrace();
    }
    return outputPath;
  }

/*
    public static void main(String[] args) {

      String inputfile = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder_1\\charlottenberg_extruded_blaze.kml";
      String inputDir = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\exported_data_whole\\";
      String outputDir = null;

      KMLParserTask parserTask = new KMLParserTask(inputDir, outputDir);
      parserTask.run();

    }
 */

}
