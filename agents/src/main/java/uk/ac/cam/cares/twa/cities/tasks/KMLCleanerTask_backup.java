package uk.ac.cam.cares.twa.cities.tasks;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.MultiGeometry;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import de.micromata.opengis.kml.v_2_2_0.StyleSelector;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import uk.ac.cam.cares.twa.cities.model.geo.Utilities;


/** The KMLCleanerTask intends to remove duplicated entries of geometries due to the database
 *  Not necessary to be used */
public class KMLCleanerTask_backup implements Runnable{
  private String inputFolder;   // contains the split kml files
  private String outputFolder;  // contains cleaned kml files
  private String projectFolder; // contains the merged kml file and the original file
  private String[] filesList;
  public KMLCleanerTask_backup(String input, String output){
    inputFolder = input;
    outputFolder = output;
  }

  @Override
  public void run() {
    this.filesList = Utilities.getInputFiles(inputFolder);

    for (String file : this.filesList){
      Kml kml = Kml.unmarshal(new File(file));
      Document doc = (Document) kml.getFeature();
      List<Feature> features = doc.getFeature();
      List<StyleSelector> styles = doc.getStyleSelector();
      List<Feature> cleanFeatures = new ArrayList<>();
      for (Feature feat: features){
          Feature cleaned = cleanFeature(feat);
          cleanFeatures.add(cleaned);
      }
      String fileNameWithExt = new File(file).getName().toString();
      String outputFile = Paths.get(outputFolder, fileNameWithExt).toString();
      String fileNameWithOutExt = FilenameUtils.removeExtension(fileNameWithExt);
      writeKml(new File(outputFile), cleanFeatures, styles, fileNameWithOutExt);
    }
  }

  public Feature cleanFeature(Feature unclean){
    Placemark placemark = (Placemark) unclean;
    MultiGeometry geoms = (MultiGeometry) placemark.getGeometry();

    MultiGeometry cleaned = CleanGeometry(geoms.getGeometry());
    Placemark cleanedFeature = new Placemark();
    cleanedFeature.setGeometry(cleaned);
    return cleanedFeature;
  }

  public MultiGeometry CleanGeometry(List<Geometry> uncleanGeometries){
    MultiGeometry cleanedGeometries = new MultiGeometry();
    Geometry firstGeometry = uncleanGeometries.get(0);
    cleanedGeometries.addToGeometry(firstGeometry);
    for (int i = 1; i < uncleanGeometries.size(); ++i){
      if (((Polygon) firstGeometry).equals((Polygon) uncleanGeometries.get(i))){
        continue;
      }else{
        Polygon polygon = (Polygon) uncleanGeometries.get(i);
        polygon.setExtrude(true);
        polygon.setTessellate(true);
        cleanedGeometries.addToGeometry(polygon);
      }
    }
    return cleanedGeometries;
  }

  // write new kml files
  private void writeKml(File file, List<Feature> list, List<StyleSelector> styles, String name) {
    Kml kml = new Kml();
    kml.createAndSetDocument().withName(name).withOpen(false).withFeature(list).withStyleSelector(styles);
    try {
      kml.marshal(file);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args){
    String input = "C:\\Users\\Shiying\\PycharmProjects\\citygml_splitter\\ura_split_200";
    String output = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder";
    KMLCleanerTask_backup cleaner = new KMLCleanerTask_backup(input, output);
    cleaner.run();
  }
}
