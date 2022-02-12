package uk.ac.cam.cares.twa.cities.tasks;


import gov.nasa.worldwind.geom.Position.PositionList;
import gov.nasa.worldwind.ogc.kml.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.xml.stream.XMLStreamException;
import uk.ac.cam.cares.twa.cities.model.geo.EnvelopeCentroid;

public class KMLParserTask {

  public static void printPlacemarks(KMLAbstractFeature feature)
  {
    if (feature instanceof KMLAbstractContainer)
    {
      KMLAbstractContainer container = (KMLAbstractContainer) feature;
      for (KMLAbstractFeature f : container.getFeatures())
      {
        printPlacemarks(f); // recursive
      }
    }
    else if (feature instanceof KMLPlacemark)
    {
      KMLAbstractGeometry geom = ((KMLPlacemark) feature).getGeometry();
      String name = ((KMLPlacemark) feature).getName();
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
        System.out.println("MulitiGeometry placemark at: " + positionL);
        double[] envelope = EnvelopeCentroid.getEnvelope(geom, name);
        System.out.println("Envelop: "  + envelope[0] + " " + envelope[1] + " " + envelope[2] + " " + envelope[3]);
      }
      else if (geom instanceof KMLPolygon){

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
      String inputfile = "C:\\Users\\Shiying\\Documents\\CKG\\CitiesKG\\3dcitydb-web-map-1.9.0\\3dwebclient\\test_tiles2\\Tiles\\0\\1\\test_Tile_0_1_extruded.kml";
      KMLRoot kmlRoot = null;
      KMLDocument document = null;
      KMLStyle style = null;
      try {
        File myObj = new File(inputfile);
        kmlRoot = KMLRoot.create(myObj);
        kmlRoot.parse();

      } catch (IOException | XMLStreamException e) {
        e.printStackTrace();
      }
      document = ((KMLDocument) kmlRoot.getFields().getValues().toArray()[0]);
      String name = kmlRoot.getFeature().getName();
      style = (KMLStyle)document.getStyleSelectors().toArray()[0];


      KMLParserTask.printPlacemarks(kmlRoot.getFeature());
      System.out.println(kmlRoot.getFeature());
    }


  }
