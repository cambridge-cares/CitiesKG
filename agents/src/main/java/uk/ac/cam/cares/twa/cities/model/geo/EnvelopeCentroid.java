package uk.ac.cam.cares.twa.cities.model.geo;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Position.PositionList;
import gov.nasa.worldwind.ogc.kml.KMLAbstractGeometry;
import gov.nasa.worldwind.ogc.kml.KMLLineString;
import gov.nasa.worldwind.ogc.kml.KMLLinearRing;
import gov.nasa.worldwind.ogc.kml.KMLMultiGeometry;
import gov.nasa.worldwind.ogc.kml.KMLPlacemark;
import gov.nasa.worldwind.ogc.kml.KMLPoint;
import gov.nasa.worldwind.ogc.kml.KMLPolygon;
import java.util.ArrayList;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.ogr;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osr;


public class EnvelopeCentroid {

  private double[] envelope = new double[4];
  private Geometry centroid = new Geometry(ogr.wkbPoint);
  private Geometry building = new Geometry(ogr.wkbMultiPolygon);

  public EnvelopeCentroid() {}

  public static double[] getEnvelope(KMLAbstractGeometry kmlGeom) {

    double[] envelope = new double[4];

    if (kmlGeom instanceof KMLPolygon) {
    }
    else if (kmlGeom instanceof KMLMultiGeometry) // most common for buildings with 1 polygon or multipolygon
    {
      KMLMultiGeometry multiGeometry = ((KMLMultiGeometry) kmlGeom);
      ArrayList<KMLPolygon> geometries = (ArrayList) multiGeometry.getGeometries();

      // Initialize a OGR MultiPolygon
      Geometry multiPolygon = new Geometry(ogr.wkbMultiPolygon);
      // Create the polygon and add to the multipolygon
      for (KMLPolygon kmlPolygon : geometries) {
        Geometry ring = new Geometry(ogr.wkbLinearRing);
        ArrayList coorinates = (ArrayList) kmlPolygon.getOuterBoundary().getCoordinates().list;
        // @TODO: only care the outerboundary for envelope, but can also add the innerboundary
        for (Object pos : coorinates) {
          Position point = (Position) pos;
          ring.AddPoint(point.getLongitude().degrees, point.getLatitude().degrees); // long, lat
        }
        Geometry polygon = new Geometry(ogr.wkbPolygon);
        polygon.AddGeometry(ring);
        multiPolygon.AddGeometry(polygon);
      }

      multiPolygon.GetEnvelope(envelope);

    } else if (kmlGeom instanceof KMLLinearRing) {
      // Handle line, polygon, etc placemarks
    } else if (kmlGeom instanceof KMLLineString) {
      // Handle LineString
      PositionList coordinates = ((KMLLineString) kmlGeom).getCoordinates();
      ArrayList positionL = (ArrayList) coordinates.list;
      System.out.println("LineString placemark at: " + ((KMLLineString) kmlGeom).getCoordinates());
    } else {
      System.out.println("Point placemark has no Envelope");
    }
    return envelope;
  }

  /* This class calculate the centroid of building's envelope and not the building */
  /* the centroid or geometric center of a plane figure is the arithmetic mean position of all the points in the figure. */
  // TODO: two different methods to getCentroid
  public static double[] getCentroid(double[] envelope) { // return point // envelope = [Xmin, Xmax, Ymin, Ymax]
    //Geometry centroid = new Geometry(ogr.wkbPoint);
    double[] centroid = new double[2]; // {centroidX, centroidY}
    centroid[0] = (envelope[1] - envelope[0]) / 2 ;
    centroid[1] = (envelope[3] - envelope[2]) / 2 ;
    //centroid.AddPoint(centroidX, centroidY);
    return centroid;
  }
}
