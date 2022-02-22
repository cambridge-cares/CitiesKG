package uk.ac.cam.cares.twa.cities.tasks;

import de.micromata.opengis.kml.v_2_2_0.*;
import org.json.JSONArray;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class KMLSorterTask_backup implements Runnable {

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

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        // directory of unsorted kml files
        File directory = new File("C:\\Users\\HTAI01\\Documents\\KMLpostprocessing\\src\\main\\resources\\exported_data_whole");
        // file location of master json file
        File masterJson = new File("C:\\Users\\HTAI01\\Documents\\KMLpostprocessing\\src\\main\\resources" +
                "\\metadata_export_charlottenburg\\test_extruded_MasterJSON.json");
        // file location of summary json file describing which building is in which tile
        File summaryJson = new File("C:\\Users\\HTAI01\\Documents\\KMLpostprocessing\\src\\main\\resources" +
                "\\metadata_export_charlottenburg\\test.json");
        // file location of one unsorted kml file, can be replaced later by getting one file from dirList
        File test0 = new File("C:\\Users\\HTAI01\\Documents\\KMLpostprocessing\\src\\main\\resources" +
                "\\exported_data_some\\test_0_extruded.kml");
        // file location of where the tiles should be created, for now need to manually make the parent Tiles folder
        String outDir = "C:\\Users\\HTAI01\\Documents\\GitHub\\CitiesKG\\3dcitydb-web-map-1.9.0" +
                "\\3dwebclient\\metadata_export_charlottenburg\\Tiles\\";
        // needs to match the layer name defined in master json
        String projName = "test";

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
        long end = System.currentTimeMillis();
        long duration = (end - start);
        System.out.println(duration + " ms");
        System.out.println("start: " + start + " ms\nend: " + end + " ms");
        System.out.println("buildings: " + this.count);
        System.out.println("files sorted: " + this.files);
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
                this.tileFeatureMap.put(key, new LinkedList<>());
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
                this.count ++;
                // manually set extruded and tessellate to true
                Placemark placemark = (Placemark) feature;
                MultiGeometry geoms = (MultiGeometry) placemark.getGeometry();
                for (Geometry geom: geoms.getGeometry()) {
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
    private List<Feature> getFeaturesFromKml(File file) {
        Kml kml = Kml.unmarshal(file);
        List<Feature> features = ((Document) kml.getFeature()).getFeature();
        return features;
    }

    /*
    private ArrayList<Point> getEnvelopeFromFeature(Feature feature) {
        ArrayList<Point> envelope = new ArrayList<>();
        Placemark placemark = (Placemark) feature;
        MultiGeometry geoms = (MultiGeometry) placemark.getGeometry();
        for (Geometry geom: geoms.getGeometry()) {
            Boundary boundary = ((Polygon) geom).getOuterBoundaryIs();
            List<Coordinate> coordinates = boundary.getLinearRing().getCoordinates();
            for (Coordinate coordinate : coordinates) {
                coordinate.getLatitude();
                coordinate.getLongitude();
            }
        }
        return envelope;
    }

     */

    // get style list from one kml file
    private List<StyleSelector> getStylesFromKml(File file) {
        Kml kml = Kml.unmarshal(file);
        Document doc = (Document) kml.getFeature();
        List<StyleSelector> styles = doc.getStyleSelector();
        return styles;
    }

    // create folders for tiles
    private void createFolders(String dir) {
        for (int i = 0; i < this.rows; i++) {
            File rowFolder = new File(dir + i);
            if (rowFolder.mkdir()) {
                for (int k = 0; k < this.cols; k++) {
                    File colFolder = new File(rowFolder.getAbsolutePath() + "\\" + k);
                    if (!colFolder.mkdir()) {
                        throw new JPSRuntimeException("Failed to make column folder");
                    }
                }
            } else {
                throw new JPSRuntimeException("failed to make row folder");
            }
        }
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
}

