package uk.ac.cam.cares.twa.cities.tasks;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import de.micromata.opengis.kml.v_2_2_0.*;


import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import uk.ac.cam.cares.twa.cities.model.geo.Utilities;

public class KMLSorterTask implements Runnable {

    //TODO replace all / with system file separator

    private String name; // project name, need to match the layer name in master json
    private int rows; // the number of tile rows
    private int cols; // the number of tile columns
    private File[] dirList; // list of unsorted kml files
    private JSONObject summaryJSON; // json file that describes which building is in which tile

    private HashMap<String, List<Feature>> tileFeatureMap = new HashMap<>(); // hashmap containing tile name as key,
    // and a list of features for that tile as the value
    private int count = 0; // count the number of buildings
    private int files = 0; // count the number of kml files sorted

    private String[] csvHeader;
    private HashMap<String, ArrayList<String[]>> csvData;

    private List<Feature> allFeatures;
    private HashMap<String, Feature> featuresMap = new HashMap<>();// <gmlid, features>
    private int buildingInTiles = 0;
    private String tilesFolder = "sorted\\";

    private String unsortedDir;  // "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\exported_data_whole\\"
    private String projectFolder; // "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\"
    private String outputDir; // "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\sorted_berlin\\"
    private String summaryCSV; // "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\sorted_tiles_summary.csv";
    private String masterJSON; // "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\test_extruded_MasterJSON.json";

    private HashMap<String, Boolean> fileStatus = new HashMap<>();

    public KMLSorterTask(String unsortedDir, String projectFolder, String masterJSON, String sortedSummary){
        this.unsortedDir = unsortedDir;
        this.projectFolder = projectFolder;
        this.masterJSON = masterJSON;
        this.summaryCSV = sortedSummary;
        this.outputDir = Utilities.createDir(projectFolder + tilesFolder);

    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        /* directory of unsorted kml files */
        //unsortedDir = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\charlottenberg_extruded_blaze.kml";
        this.dirList = new File(unsortedDir).listFiles();

        List<StyleSelector> styles = getStylesFromKml(dirList[0]);

        setFields(new File(unsortedDir), masterJSON, new File(summaryCSV), "test");
        //rows = 148;  //280  // [rows, cols] = [75, 84]
        //cols = 184;   // 368
        name = "test";
        createFolders(this.outputDir);
        this.csvData = readCSV2Map(this.summaryCSV);
        initFilesStatus(unsortedDir);

        // Read the one kml file to allFeatures, else we will use the hashmap to store the features
        if (new File(unsortedDir).isFile()){
            Kml kml = Kml.unmarshal(new File(unsortedDir));
            allFeatures = ((Document) kml.getFeature()).getFeature();
        }

        for (int i = 0; i < rows; i++) {
            for (int k = 0; k < cols; k++) {
                String name = this.name + "_Tile_" + i + "_" + k + "_extruded";
                File out = new File(this.outputDir + "\\Tiles\\" + i + "\\" + k + "\\" + name + ".kml");
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
        //System.out.println("files sorted: " + this.files);

        // Copy MasterJson to the outputDir
        Path src = Paths.get(this.masterJSON);
        String masterFile = new File(this.masterJSON).getName();
        Path dest = Paths.get(this.outputDir + "\\" + this.tilesFolder + "\\" + masterFile);
        try {
            Files.copy(src, dest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Only execute one time in the beginning
    private void initFilesStatus(String inputDir){
        String[] filesList = Utilities.getInputFiles(inputDir);

        for (String filepath : filesList){
            this.fileStatus.put(filepath, false);
        }
    }

    // Check the import status of the files
    private boolean isImported(String filepath){
        return this.fileStatus.get(filepath);
    }
    private void updateFileStatus(String filepath, boolean status){
        this.fileStatus.replace(filepath, status);
    }

    /* Could not use tiles as Hashmap key, as there is repeatness.
    * if used, add all String[] as list */
    private HashMap<String, ArrayList<String[]>> readCSV2Map(String csvfile) {
        HashMap<String, ArrayList<String[]>> CSVRows = new HashMap<>();
        List<String[]> csvData = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(csvfile))) {
            csvHeader = reader.readNext();
            csvData = reader.readAll();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CsvException e) {
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

    /**
     * Given a hashmap with filename (Key) and Array of gmlIds (Values),
     * return a list of features belonging to the tile
     *
     * @param map a hashmap with filename (Key) and Array of gmlIds (Values)
     * @return List of features
     */

    private List<Feature> getFeaturesInTile(HashMap<String, ArrayList<String>> map) {
        List<Feature> featuresInTile = new ArrayList<>();
        // loop through for each kml file that has buildings in this tile
        for (String filepath : map.keySet()) {
            HashMap<String, Feature> featuresInKml = new HashMap<>();
            if (!isImported(filepath)){
                // get all the buildings in the current kml file
                featuresInKml = getFeaturesFromKml(new File(filepath));
                this.featuresMap.putAll(featuresInKml);
                updateFileStatus(filepath, true);
            }
            // Find the corresponding features from global featuresMap
            for (String buildingId : map.get(filepath)) {
                if (this.featuresMap.containsKey(buildingId)){
                    Feature feature = this.featuresMap.get(buildingId);
                    Placemark placemark = (Placemark) feature;
                    MultiGeometry geoms = (MultiGeometry) placemark.getGeometry();
                    for (Geometry geom : geoms.getGeometry()) {
                        Polygon polygon = (Polygon) geom;
                        polygon.setExtrude(true);
                        polygon.setTessellate(true);
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
     * Given tile position, create a hashmap with filename (Key) and Array of gmlIds (Values)
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
                String filepath = unsortedDir + filename;
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

    // set inputs as fields
    private void setFields(File dir, String masterJSON, File summaryJSON, String projName) {
        this.name = projName;
        this.dirList = dir.listFiles();
        JSONObject test;
        try {
            test = new JSONObject(new String(Files.readAllBytes(new File(masterJSON).toPath())));
            //this.summaryJSON = new JSONObject(new String(Files.readAllBytes(summaryJSON.toPath())));
        } catch (IOException e) {
            throw new JPSRuntimeException(e);
        }
        this.rows = test.getInt("rownum");
        this.cols = test.getInt("colnum");
        System.out.println("Reading from MasterJSON: " + masterJSON);
        System.out.println("colnum:" + cols + ", rownum: " + rows);
        if (rows == 0 || cols == 0) {
            throw new RuntimeException("Colnum and Rownum are invalid!");
        }
    }


    // create one List<Feature> for each tile
    private void createFeatureLists() {
        for (int i = 0; i < this.rows; i++) {
            for (int k = 0; k < this.cols; k++) {
                String key = this.name + "_Tile_" + i + "_" + k + "_extruded";
                this.tileFeatureMap.put(key, new ArrayList<>());
            }
        }
    }


    // get all features from one kml file
    /*
    private List<Feature> getFeaturesFromKml(File file) {
        Kml kml = Kml.unmarshal(file);
        List<Feature> features = ((Document) kml.getFeature()).getFeature();
        return features;
    }*/

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
    private void createFolders(String dir) {
        File parentFolder = new File(dir + "\\Tiles");
        if (parentFolder.mkdir()) {
            for (int i = 0; i < this.rows; i++) {
                File rowFolder = new File(parentFolder.getAbsolutePath() + "\\" + i);
                if (rowFolder.mkdir()) {
                    for (int k = 0; k < this.cols; k++) {
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

    // write new kml files
    private void writeKml(File file, List<Feature> list, List<StyleSelector> styles, String name) {
        this.buildingInTiles += list.size();
        Kml kml = new Kml();
        kml.createAndSetDocument().withName(name).withOpen(false).withFeature(list).withStyleSelector(styles);
        try {
            kml.marshal(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        String unsortedDir = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\exported_data_whole\\";
        String projectFolder = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\";
        String summaryCSV = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\sorted_tiles_summary.csv";
        String masterJSON = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\test_extruded_MasterJSON.json";
        KMLSorterTask kmlSorter=new KMLSorterTask(unsortedDir, projectFolder, masterJSON, summaryCSV);
        kmlSorter.run();

    }
}

