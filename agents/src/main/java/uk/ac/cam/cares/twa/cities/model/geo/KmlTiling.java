package uk.ac.cam.cares.twa.cities.model.geo;

import org.geotools.geometry.jts.GeometryBuilder;
import org.locationtech.jts.geom.Point;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;

public class KmlTiling {

  private int nRow;
  private int nCol;
  private String crs;
  private double extent_Xmin = Double.POSITIVE_INFINITY;
  private double extent_Xmax = Double.NEGATIVE_INFINITY;
  private double extent_Ymin = Double.POSITIVE_INFINITY;
  private double extent_Ymax = Double.NEGATIVE_INFINITY;


  public KmlTiling(String crs) {
    this.crs = crs;
  }

  /*
  * Update the boundary of the extent */
  public static void updateExtent(double[] geomEnvelope){

  }





}
