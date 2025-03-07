package uk.ac.cam.cares.twa.cities.model.geo;

import gov.nasa.worldwind.ogc.kml.KMLPolygon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.gdal.ogr.Geometry;
import org.gdal.ogr.ogr;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.gdal.osr.osr;

/**
 * A library to handle geometry from Blazegraph
 *
 * @author <a href="mailto:shiying.li@sec.ethz.ch">Shiying Li</a>
 */
public class Transform {

  public Transform(){

  }

  /**
   * Create a GDAL polygon from an array of 3 dimensional points
   *
   * @param geometry - geometry represented by an array of 3 dimensional points
   * @return Geometry - GDAL geometry
   */
  public static Geometry buildPolygon(double[][] geometry){

    int dimension = geometry[0].length;

    Geometry ring = new Geometry(ogr.wkbLinearRing);
    for (double[] doubles : geometry) {
      if (dimension == 2) {
        ring.AddPoint(doubles[0], doubles[1]);
      } else if (dimension == 3) {
        ring.AddPoint(doubles[0], doubles[1], doubles[2]);
      }
    }
    Geometry polygon = new Geometry(ogr.wkbPolygon);
    polygon.AddGeometry(ring);

    return polygon;
  }

  /**
   * Get the envelope from a geometry
   *
   * @param geometry - double[][] represents an array of 3 dimensional points
   * @return double[] - the envelope of the geometry
   */
  public static double[] getEnvelop(double[][] geometry) {
    Geometry polygon = buildPolygon(geometry);

    double[] envelop = new double[4];
    polygon.GetEnvelope(envelop);

    return envelop;
  }

  /**
   * Reproject geometry points from source EPSG to destination EPSG
   *
   * @param geometry - To be transformed geometry
   * @param from_epsg - source EPSG
   * @param to_epsg - destination EPSG
   * @return double[][] - transformed points array
   */
  public static double[][] reprojectGeometry (double[][] geometry, int from_epsg, int to_epsg) {
    SpatialReference source = new SpatialReference();
    source.ImportFromEPSG(from_epsg);

    SpatialReference target = new SpatialReference();
    target.ImportFromEPSG(to_epsg);

    CoordinateTransformation transformMatrix = osr.CreateCoordinateTransformation(source, target);

    Geometry polygon = buildPolygon(geometry);
    polygon.Transform(transformMatrix);

    return polygon.GetBoundary().GetPoints();
  }

  /**
   * Reproject the point like [x,y] from source EPSG to destination EPSG using the method "TransformPoint (latitude, longtitude)"
   *
   * @param centroid - centroid point to be transformed
   * @param from_epsg - source EPSG
   * @param to_epsg - destination EPSG
   * @return double[] - Transformed point
   */
  public static double[] reprojectPoint(double[] centroid, int from_epsg, int to_epsg) {
    Geometry point = new Geometry(ogr.wkbPoint);

    SpatialReference source = new SpatialReference();
    source.ImportFromEPSG(from_epsg);

    SpatialReference target = new SpatialReference();
    target.ImportFromEPSG(to_epsg);

    CoordinateTransformation transformMatrix = osr.CreateCoordinateTransformation(source, target);
    point.AddPoint(centroid[0], centroid[1]);
    return transformMatrix.TransformPoint(point.GetX(), point.GetY()); //  transform seems to be for more than a point
  }

  /**
   * Reproject the envelope from source EPSG to destination EPSG : [xmin, xmax, ymin, ymax] --> [ymin, ymax, xmin, xmax]
   * Create the polygon from the envelope and apply the transformation
   *
   * @param envelope - the envelope to be transformed
   * @param from_epsg - source EPSG
   * @param to_epsg - destination EPSG
   * @return double[] - Transformed Envelope points [ymin, ymax, xmin, xmax] in destination EPSG
   */
  public static double[] reprojectEnvelope(double[] envelope, int from_epsg, int to_epsg) {

    SpatialReference source = new SpatialReference();
    source.ImportFromEPSG(from_epsg);

    SpatialReference target = new SpatialReference();
    target.ImportFromEPSG(to_epsg);

    CoordinateTransformation transformMatrix = osr.CreateCoordinateTransformation(source, target);

    double xmin = envelope[0];
    double xmax = envelope[1];
    double ymin = envelope[2];
    double ymax = envelope[3];

    Geometry ring = new Geometry(ogr.wkbLinearRing);
    ring.AddPoint(xmin, ymax);
    ring.AddPoint(xmin, ymin);
    ring.AddPoint(xmax, ymin);
    ring.AddPoint(xmax, ymax);
    ring.AddPoint(xmin, ymax);
    Geometry polygon = new Geometry(ogr.wkbPolygon);
    polygon.AddGeometry(ring);

    polygon.Transform(transformMatrix);

    double[][] points = polygon.GetBoundary().GetPoints();

    return getEnvelopeFromPoints(points);
  }

  /**
   * Extract envolope corner points from a multidimensional array which represents an array of 3-dimensional points
   *
   * @param pointsArray - An array of 3 dimensional points
   * @return double[] - Envelope points [ymin, ymax, xmin, xmax]
   */
  public static double[] getEnvelopeFromPoints(double[][] pointsArray){
    List<Double> xCoords = new ArrayList<>();
    List<Double> yCoords = new ArrayList<>();
    List<Double> zCoords = new ArrayList<>();

    for (double[] coords : pointsArray){
      xCoords.add(coords[0]);
      yCoords.add(coords[1]);
      zCoords.add(coords[2]);
    }

    double new_xmin = Collections.min(xCoords);
    double new_xmax = Collections.max(xCoords);
    double new_ymin = Collections.min(yCoords);
    double new_ymax = Collections.max(yCoords);

    return new double[]{new_ymin, new_ymax, new_xmin, new_xmax};
  }
}

