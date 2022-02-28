package uk.ac.cam.cares.twa.cities.tasks;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import de.micromata.opengis.kml.v_2_2_0.*;
import org.json.JSONArray;
import org.json.JSONObject;
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
    private JSONObject masterJSON; // master json file
    private HashMap<String, List<Feature>> tileFeatureMap = new HashMap<>(); // hashmap containing tile name as key,
    // and a list of features for that tile as the value
    private int count = 0; // count the number of buildings
    private int files = 0; // count the number of kml files sorted

    private String[] csvHeader;
    private HashMap<String, ArrayList<String[]>> csvData;
    private String unsortedDir;
    private List<Feature> allFeatures;
    private HashMap<String, Feature> featuresMap = new HashMap<>();// <gmlid, features>
    private int buildingInTiles = 0;

    private String outDir = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\sorted_berlin\\";
    private String summaryCSV = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\sorted_tiles_summary.csv";


    @Override
    public void run() {
        long start = System.currentTimeMillis();
        /* directory of unsorted kml files */
        unsortedDir = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\exported_data_whole\\";
        //unsortedDir = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\charlottenberg_extruded_blaze.kml";
        this.dirList = new File(unsortedDir).listFiles();

        /* file location of one unsorted kml file */
        //TODO: replace by getting one file from dirList
        File test0 = new File("C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\exported_data_whole\\test_0_extruded.kml");

        /* file location of where the tiles should be created */
        String outDir = this.outDir;

        List<StyleSelector> styles = getStylesFromKml(test0);

        //String masterJSON = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder\\test_extruded_MasterJSON.json";

        // @todo: read this from a masterjson
        rows = 148;  //280  // [rows, cols] = [75, 84]
        cols = 184;   // 368
        name = "test";
        createFolders(outDir);
        this.csvData = readCSV2Map(this.summaryCSV);

        // Read the one kml file to allFeatures, else we will use the hashmap to store the features
        if (new File(unsortedDir).isFile()){
            Kml kml = Kml.unmarshal(new File(unsortedDir));
            allFeatures = ((Document) kml.getFeature()).getFeature();
        }

        for (int i = 0; i < rows; i++) {
            for (int k = 0; k < cols; k++) {

                String name = this.name + "_Tile_" + i + "_" + k + "_extruded";
                File out = new File(outDir + "\\Tiles\\" + i + "\\" + k + "\\" + name + ".kml");
                if (out.exists()){
                    continue;
                }
                HashMap<String, ArrayList<String>> map = getBuildingsInTile(i, k);
                List<Feature> features = getFeaturesInTile(map);  // read the features from the files
                writeKml(out, features, styles, name);
                System.out.println("finished writing " + out.getAbsolutePath());
            }
        }



        /*
        // Hashmap method
        setFields(directory, masterJson, summaryJson, projName);
        createFeatureLists();
        createFolders(outDir);
        List<StyleSelector> styles = getStylesFromKml(test0);

        // for each unsorted kml files in the dirList, sort the buildings
        for (File file : this.dirList) {
            sortBuildings(file);
            this.files++;
        }

        for (int i = 0; i < this.rows; i++) {
            for (int k = 0; k < this.cols; k++) {
                String name = this.name + "_Tile_" + i + "_" + k + "_extruded";
                File file = new File(outDir + i + "\\" + k + "\\" + name + ".kml");
                List<Feature> features = this.tileFeatureMap.get(name);
                writeKml(file, features, styles, name);
                System.out.println(name);
            }
        }
        */
        long end = System.currentTimeMillis();
        long duration = (end - start);
        System.out.println(duration + " ms");
        System.out.println("start: " + start + " ms\nend: " + end + " ms"); // @todo: need to genrate the run journal file
        System.out.println("buildings: " + this.count);
        System.out.println("Features have written: " + this.buildingInTiles);
        //System.out.println("files sorted: " + this.files);

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
        /* loop through for each kml file that has buildings in this tile */
        for (String filepath : map.keySet()) {
            /* get all the buildings in the kml file */
            List<Feature> allFeaturesInKml = getFeaturesFromKml(new File(filepath));
            //List<Feature> allFeaturesInKml = allFeatures;
            // loop through all the buildings in the kml file
            for (Feature feature : allFeaturesInKml) {
                // if the building belongs to this tile, get feature info
                if (map.get(filepath).contains(feature.getName())) {
                    Placemark placemark = (Placemark) feature;
                    MultiGeometry geoms = (MultiGeometry) placemark.getGeometry();
                    for (Geometry geom : geoms.getGeometry()) {
                        Polygon polygon = (Polygon) geom;
                        polygon.setExtrude(true);
                        polygon.setTessellate(true);
                    }
                    // put feature info into list to be returned
                    featuresInTile.add(feature);
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
    private void setFields(File dir, File masterJSON, File summaryJSON, String projName) {
        this.name = projName;
        this.dirList = dir.listFiles();
        try {
            this.masterJSON = new JSONObject(new String(Files.readAllBytes(masterJSON.toPath())));
            this.summaryJSON = new JSONObject(new String(Files.readAllBytes(summaryJSON.toPath())));
        } catch (IOException e) {
            throw new JPSRuntimeException(e);
        }
        this.rows = this.masterJSON.getInt("rownum");
        this.cols = this.masterJSON.getInt("colnum");
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

    // sort buildings for one kml file
    private void sortBuildings(File file) {
        List<Feature> features = getFeaturesFromKml(file);
        // get all buildings from summary json file
        ArrayList<String> buildings = new ArrayList<>(this.summaryJSON.keySet());
        // loop through all features in the kml file
        for (Feature feature : features) {
            if (buildings.contains(feature.getName())) {
                this.count++;
                // manually set extruded and tessellate to true
                Placemark placemark = (Placemark) feature;
                MultiGeometry geoms = (MultiGeometry) placemark.getGeometry();
                for (Geometry geom : geoms.getGeometry()) {
                    Polygon polygon = (Polygon) geom;
                    polygon.setExtrude(true);
                    polygon.setTessellate(true);
                }
                // get name of tile that feature belongs to
                JSONObject building = summaryJSON.getJSONObject(feature.getName());
                JSONArray tile = (JSONArray) building.get("tile");
                String listName = this.name + "_Tile_" + tile.getInt(0) + "_" + tile.getInt(1) + "_extruded";
                // put feature in correct list
                (this.tileFeatureMap.get(listName)).add(feature);
            }
        }
        System.out.println(file.getAbsolutePath());
    }

    // get all features from one kml file
    /*
    private List<Feature> getFeaturesFromKml(File file) {
        Kml kml = Kml.unmarshal(file);
        List<Feature> features = ((Document) kml.getFeature()).getFeature();
        return features;
    }*/

    // Shiying: get all features from one kml file
    private List<Feature> getFeaturesFromKml(File file) {
        HashMap<String, Feature> featuresMap = new HashMap<>();
        Kml kml = Kml.unmarshal(file);
        List<Feature> features = ((Document) kml.getFeature()).getFeature();
        for (Feature feat: features){
            String gmlid = feat.getName();
            featuresMap.put(gmlid, feat);
        }
        return features;
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
    KMLSorterTask kmlSorter=new KMLSorterTask();
    kmlSorter.run();

    }
    }

