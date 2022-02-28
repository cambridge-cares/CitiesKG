package uk.ac.cam.cares.twa.cities.tasks;

import com.opencsv.CSVWriter;
import gov.nasa.worldwind.geom.Position.PositionList;
import gov.nasa.worldwind.ogc.kml.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import uk.ac.cam.cares.twa.cities.model.geo.EnvelopeCentroid;
import uk.ac.cam.cares.twa.cities.model.geo.Utilities;

// the output of running this class should be the generation of the summary file in csv (gmlid, envelope[xmin, xmax, ymin, ymax], envelopeCentroid, corresponding file)
public class KMLParserTask implements Runnable{

  private String[] filelist;
  private File currFile;
  private final String outCsvFile = "summary_";
  private final String outFileExt = ".csv";
  private final String outputDir = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\";
  private String inputDir;
  public List<String[]> dataContent = new ArrayList<>();
  private List<String> gmlidList = new ArrayList<>();
  private KMLTilingTask kmltiling = new KMLTilingTask("4326", "25833");

  public KMLParserTask(String path){

    this.filelist = Utilities.getInputFiles(path);
    this.inputDir = Utilities.getInputDir(path);

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
      String outputFile = this.createCSVFile(this.dataContent);

      System.out.println("Checking if the gmlidList is unique: " + Utilities.isUnique(this.gmlidList));
      System.out.println("Saving the file in : " + outputFile);
      System.out.println("Updated Extent: " + kmltiling.getExtent()[0] + " " + kmltiling.getExtent()[1] + " " + kmltiling.getExtent()[2] + " " + kmltiling.getExtent()[3]);
    }

    long end2 = System.currentTimeMillis();
    System.out.println("Reading " + this.filelist.length + " files takes " + (end1 - start) + " ms");
    System.out.println("The KMLParserTask took " + (end2 - start) + " ms");
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
      else if (geom instanceof KMLPolygon) {}
      else if (geom instanceof KMLLinearRing) {} // Handle line, polygon, etc placemarks
      else if (geom instanceof KMLLineString)
      {
        // Handle LineString
        PositionList coordinates = ((KMLLineString) geom).getCoordinates();
        ArrayList positionL = (ArrayList) coordinates.list;
        System.out.println("LineString placemark at: " + ((KMLLineString) geom).getCoordinates());
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
    String outputPath = this.outputDir + this.outCsvFile + dataContent.size() + this.outFileExt;
    try (CSVWriter writer = new CSVWriter(new FileWriter(outputPath))) {
      writer.writeAll(csvContent);
      System.out.println("The summary has " + dataContent.size() + " rows.");
    } catch (IOException e) {
      e.printStackTrace();
    }
    return outputPath;
  }

    public static void main(String[] args) {

      String inputfile = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder_1\\charlottenberg_extruded_blaze.kml";
      String inputDir = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\exported_data_whole\\";

      KMLParserTask parserTask = new KMLParserTask(inputDir);
      parserTask.run();



/*
      String inputfile = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\summary.csv";
      List<String[]> csvData = new ArrayList<>();
      try (CSVReader reader = new CSVReader(new FileReader(inputfile))) {
        String[] header = reader.readNext();
        csvData = reader.readAll();
        //r.forEach(x -> System.out.println(Arrays.toString(x)));
      } catch (IOException e) {
        e.printStackTrace();
      } catch (CsvException e) {
        e.printStackTrace();
      }
      int index = 0;
      String outputDir = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\summary_folder\\";

      while (index < csvData.size() && index + 5000 < csvData.size()) {
        List<String[]> newList = csvData.subList(index, index + 5000);
        try (CSVWriter writer = new CSVWriter(
            new FileWriter(outputDir + "summary_" + index + ".csv"))) {
          writer.writeAll(newList);
          index += 5000;
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      List<String[]> lastList = csvData.subList(index, csvData.size());
      try (CSVWriter writer = new CSVWriter(
          new FileWriter(outputDir + "summary_" + index + ".csv"))) {
        writer.writeAll(lastList);
      } catch (IOException e) {
        e.printStackTrace();
      }

 */
    }

}
/*// re-arrange the generated file and split it into chunk with a size of 50000
      String inputfile = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\summary.csv";
      List<String[]> csvData = new ArrayList<>();
      try (CSVReader reader = new CSVReader(new FileReader(inputfile))) {
        csvData = reader.readAll();
        //r.forEach(x -> System.out.println(Arrays.toString(x)));
      } catch (IOException e) {
        e.printStackTrace();
      } catch (CsvException e) {
        e.printStackTrace();
      }
      int index = 0;
      String outputDir = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\summary_folder\\";

      while (index < csvData.size() && index + 5000 < csvData.size()) {
        List<String[]> newList = csvData.subList(index, index + 5000);
        try (CSVWriter writer = new CSVWriter(
            new FileWriter(outputDir + "summary_" + index + ".csv"))) {
          writer.writeAll(newList);
          index += 5000;
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      List<String[]> lastList = csvData.subList(index, csvData.size() - 1);
      try (CSVWriter writer = new CSVWriter(
          new FileWriter(outputDir + "summary_" + index + ".csv"))) {
        writer.writeAll(lastList);
      } catch (IOException e) {
        e.printStackTrace();
      }*/