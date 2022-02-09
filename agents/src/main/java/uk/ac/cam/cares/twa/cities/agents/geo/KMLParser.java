package uk.ac.cam.cares.twa.cities.agents.geo;


import gov.nasa.worldwind.ogc.kml.*;
import java.io.File;
import java.io.IOException;

public class KMLParser {

    public static void main(String[] args) {
      String inputfile = "C:\\Users\\Shiying\\Documents\\CKG\\CitiesKG\\3dcitydb-web-map-1.9.0\\3dwebclient\\test_tiles2\\Tiles\\0\\1\\test_Tile_0_1_extruded.kml";
      KMLRoot kmlRoot;
      try {
        File myObj = new File(inputfile);
        kmlRoot = new KMLRoot(myObj);

      } catch (IOException e) {
        e.printStackTrace();
      }


    }
}
