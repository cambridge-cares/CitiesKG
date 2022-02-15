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

// the output of running this class should be the generation of the summary file in csv (gmlid, envelope[xmin, xmax, ymin, ymax], envelopeCentroid, corresponding file)
public class KMLParserTask implements Runnable{

  // filename
  // Some essential section of KML for the creation
  // <Placemark> --> id
  //

  private String[] filelist;
  private String currFile;
  private String outCsvFile = "summary.csv";
  private String outputDir = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\";
  private String inputPath;
  public List<String[]> dataContent = new ArrayList<>();
  private List<String> gmlidList = new ArrayList<>();

  public KMLParserTask(String inputPath){
    this.inputPath = inputPath;
    File path = new File(inputPath);

    if (path.isFile()) {
      this.filelist = new String[1];
      this.filelist[0] = inputPath;
    }
    else if (path.isDirectory()) {
      this.filelist = path.list();
    }
  }

  @Override
  public void run() {
    for (String file : this.filelist){
      this.currFile = this.inputPath + file;
      System.out.println("Reading the file: " + this.currFile);
      KMLRoot kmlRoot = null;
      try {
        File myObj = new File(this.currFile);
        kmlRoot = KMLRoot.create(myObj);
        kmlRoot.parse();
      } catch (IOException | XMLStreamException e) {
        e.printStackTrace();
      }
      this.getPlacemarks(kmlRoot.getFeature());
    }

    if (this.dataContent.size() != 0) {
      System.out.println("Checking if the gmlidList is unique: " + isUnique(this.gmlidList));
      System.out.println("Saving the file in : " + this.outputDir + this.outCsvFile);
      this.createCSVFile(this.dataContent);
    }
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
      String buildingId = ((KMLPlacemark) feature).getName();
      if (geom instanceof KMLPoint)
      {
        System.out.println("Point placemark at: " + ((KMLPoint) geom).getCoordinates());
      }
      else if (geom instanceof KMLMultiGeometry) // most common for buildings
      {
        KMLMultiGeometry multiGeometry = ((KMLMultiGeometry) geom);
        ArrayList geometries = (ArrayList) multiGeometry.getGeometries();
        KMLPolygon polygon = (KMLPolygon) multiGeometry.getGeometries().toArray()[0];
        ArrayList positionL = (ArrayList) polygon.getOuterBoundary().getCoordinates().list;
        //System.out.println("MulitiGeometry placemark at: " + positionL);
        double[] envelope = EnvelopeCentroid.getEnvelope(geom);
        double[] centroid = EnvelopeCentroid.getCentroid(envelope);
        this.gmlidList.add(buildingId);
        String[] row = {buildingId, convert2Str(envelope), convert2Str(centroid), this.currFile}; //{"gmlid", "envelope", "envelopeCentroid", "filename"};
        this.dataContent.add(row);
        //System.out.println("Envelop: "  + envelope[0] + " " + envelope[1] + " " + envelope[2] + " " + envelope[3]);
      }
      else if (geom instanceof KMLPolygon)
      {

      }
      else if (geom instanceof KMLLinearRing)
      {
        // Handle line, polygon, etc placemarks
      }
      else if (geom instanceof KMLLineString)
      {
        // Handle LineString
        PositionList coordinates = ((KMLLineString) geom).getCoordinates();
        ArrayList positionL = (ArrayList) coordinates.list;
        System.out.println("LineString placemark at: " + ((KMLLineString) geom).getCoordinates());
      }
    }
  }

    public static void main(String[] args) {

      //String inputfile = "C:\\Users\\Shiying\\Documents\\CKG\\CitiesKG\\3dcitydb-web-map-1.9.0\\3dwebclient\\test_tiles2\\Tiles\\0\\1\\test_Tile_0_1_extruded.kml";
      String inputfile = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\exported_data_whole\\test_0_extruded.kml";
      String inputDir = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\exported_data_whole\\";
      File directoryPath = new File(inputDir);
      String[] filelist = directoryPath.list();
      KMLParserTask parserTask = new KMLParserTask(inputDir);
      parserTask.run();

      //System.out.println(kmlRoot.getFeature());
    }

    private String convert2Str (double[] arr){
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

    private void createCSVFile(List<String[]> dataContent) {
      String[] header = {"gmlid", "envelope", "envelopeCentroid", "filename"};

      List<String[]> csvContent = new ArrayList<>();
      csvContent.add(header);
      csvContent.addAll(dataContent);

      try (CSVWriter writer = new CSVWriter(new FileWriter(this.outputDir + this.outCsvFile))) {
        writer.writeAll(csvContent);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    private boolean isUnique (List<String> gmlidList) {
      Set<String> gmlidSet = new HashSet<String>(gmlidList);
      boolean unique = false;
      if (gmlidSet.size() < gmlidList.size()) {
        unique = false;
      } else if (gmlidSet.size() == gmlidList.size()) {
        unique = true;
      }
      return unique;
    }

}
