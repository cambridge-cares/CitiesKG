package uk.ac.cam.cares.twa.cities.model.geo;

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

  // Transform point like [x, y]
  public static double[] reprojectPoint(double[] centroid, int from_epsg, int to_epsg) {
    Geometry point = new Geometry(ogr.wkbPoint);

    SpatialReference source = new SpatialReference();
    source.ImportFromEPSG(from_epsg);

    SpatialReference target = new SpatialReference();
    target.ImportFromEPSG(to_epsg);

    CoordinateTransformation transformMatrix = osr.CreateCoordinateTransformation(source, target);
    point.AddPoint(centroid[0], centroid[1]);
    double[] transformed = transformMatrix.TransformPoint(point.GetX(),
        point.GetY()); //  transform seems to be for more than a point

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

    double[] transformed = new double[]{new_xmin, new_xmax, new_ymin, new_ymax};

    return transformed;
  }



}
