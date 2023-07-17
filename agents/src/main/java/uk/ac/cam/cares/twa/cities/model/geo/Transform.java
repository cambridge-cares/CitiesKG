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

public class Transform {

  public Transform(){

  }

  public static Geometry buildPolygon(double[][] geometry){

    int dimension = geometry[0].length;

    Geometry ring = new Geometry(ogr.wkbLinearRing);
    for (int i = 0; i < geometry.length; ++i){
      if (dimension == 2){
        ring.AddPoint(geometry[i][0], geometry[i][1]);
      } else if (dimension == 3){
        ring.AddPoint(geometry[i][0], geometry[i][1], geometry[i][2]);
      }
    }
    Geometry polygon = new Geometry(ogr.wkbPolygon);
    polygon.AddGeometry(ring);

    return polygon;
  }

  public static double[] getEnvelop(double[][] geometry) {
    Geometry polygon = buildPolygon(geometry);

    double[] envelop = new double[4];
    polygon.GetEnvelope(envelop);

    return envelop;
  }


  public static double[][] reprojectGeometry (double[][] geometry, int from_epsg, int to_epsg) {
    SpatialReference source = new SpatialReference();
    source.ImportFromEPSG(from_epsg);

    SpatialReference target = new SpatialReference();
    target.ImportFromEPSG(to_epsg);

    CoordinateTransformation transformMatrix = osr.CreateCoordinateTransformation(source, target);

    Geometry polygon = buildPolygon(geometry);

    polygon.Transform(transformMatrix);

    double[][] points = polygon.GetBoundary().GetPoints();

    return points;
  }

  // Transform point like [x, y]
  public static double[] reprojectPoint(double[] centroid, int from_epsg, int to_epsg) {
    Geometry point = new Geometry(ogr.wkbPoint);

    SpatialReference source = new SpatialReference();
    source.ImportFromEPSG(from_epsg);

    SpatialReference target = new SpatialReference();
    target.ImportFromEPSG(to_epsg);

    CoordinateTransformation transformMatrix = osr.CreateCoordinateTransformation(source, target);
    point.AddPoint(centroid[0], centroid[1]);
    double[] transformed = transformMatrix.TransformPoint(point.GetX(), point.GetY()); //  transform seems to be for more than a point
    // TransformPoint (latitude, longtitude)
    return transformed;
  }

  // Transform envelope like [xmin, xmax, ymin, ymax] -->
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

    List<Double> xCoords = new ArrayList<>();
    List<Double> yCoords = new ArrayList<>();
    List<Double> zCoords = new ArrayList<>();

    for (double[] coords : points){
      xCoords.add(coords[0]);
      yCoords.add(coords[1]);
      zCoords.add(coords[2]);
    }

    double new_xmin = Collections.min(xCoords);
    double new_xmax = Collections.max(xCoords);
    double new_ymin = Collections.min(yCoords);
    double new_ymax = Collections.max(yCoords);

    double[] transformed = new double[]{new_ymin, new_ymax, new_xmin, new_xmax};

    return transformed;
  }

}
