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
  public EnvelopeCentroid() {}

  public static double[] getEnvelope(KMLAbstractGeometry kmlGeom, String buildingId) {

    double[] envelope = new double[4];

    if (kmlGeom instanceof KMLPoint) {
      System.out.println("Point placemark has no Envelope");
    }
    else if (kmlGeom instanceof KMLMultiGeometry) // most common for buildings
    {
      KMLMultiGeometry multiGeometry = ((KMLMultiGeometry) kmlGeom);

      ArrayList<KMLPolygon> geometries = (ArrayList) multiGeometry.getGeometries();

      // Initialize a OGR MultiPolygon
      Geometry multiPolygon = new Geometry(ogr.wkbMultiPolygon);
      // Create the polygon and add to the multipolygon
      for (KMLPolygon kmlPolygon : geometries) {
        Geometry ring = new Geometry(ogr.wkbLinearRing);
        ArrayList coorinates = (ArrayList) kmlPolygon.getOuterBoundary()
            .getCoordinates().list;  // only care the outerboundary for envelope, but can also add the innerboundary
        for (Object pos : coorinates) {
          Position point = (Position) pos;
          ring.AddPoint(point.getLongitude().degrees, point.getLatitude().degrees); // long, lat
        }
        Geometry polygon = new Geometry(ogr.wkbPolygon);
        polygon.AddGeometry(ring);
        multiPolygon.AddGeometry(polygon);
      }

      multiPolygon.GetEnvelope(envelope);
      //KMLPolygon polygon = (KMLPolygon) multiGeometry.getGeometries().toArray()[0];
      //ArrayList positionL = (ArrayList) polygon.getOuterBoundary().getCoordinates().list;
      SpatialReference source = new SpatialReference();
      source.ImportFromEPSG(4326);

      SpatialReference target = new SpatialReference();
      target.ImportFromEPSG(25833);

      CoordinateTransformation transformMatrix = osr.CreateCoordinateTransformation(source, target);

      multiPolygon.Transform(transformMatrix);
      double[] testenvelope = new double[4];
      multiPolygon.GetEnvelope(testenvelope);
      System.out.println("TestEnvelop: "  + testenvelope[0] + " " + testenvelope[1] + " " + testenvelope[2] + " " + testenvelope[3]);
    } else if (kmlGeom instanceof KMLPolygon) {

    } else if (kmlGeom instanceof KMLLinearRing) {
      // Handle line, polygon, etc placemarks
    } else if (kmlGeom instanceof KMLLineString) {
      // Handle LineString
      PositionList coordinates = ((KMLLineString) kmlGeom).getCoordinates();
      ArrayList positionL = (ArrayList) coordinates.list;
      System.out.println("LineString placemark at: " + ((KMLLineString) kmlGeom).getCoordinates());
    }
    return envelope;
  }
}
