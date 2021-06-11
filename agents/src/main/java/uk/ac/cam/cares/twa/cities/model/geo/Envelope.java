package uk.ac.cam.cares.twa.cities.model.geo;
import java.util.Arrays;

import org.geotools.geometry.jts.GeometryBuilder;
import org.locationtech.jts.geom.*;

/**
 * Unique URIs are used as inputs for Envelope class.
 * The class transforms Envelope string into a list of points that defines envelope boundary and from it computes envelope centroid.
 */
public class Envelope {

    private int numberOfPoints = 5;
    private int numberOfDimensions = 3;
    private Polygon boundary;
    private Point centroid;
    private GeometryBuilder factory = new GeometryBuilder();
    private String crs;

    //Constructor so that user is forced by creating instance to add a CRS.
    public Envelope(String crs) {
        this.crs = crs;
    }

    public static void main (String[] args){
      Envelope envelope = new Envelope("EPSG:4326");
      String envelopeString = "1.29227#103.83094#496#1.29262#103.83094#333#1.29262#103.83148#12#1.29227#103.83148#567#1.29227#103.83094#111";

      envelope.extractEnvelopePoints(envelopeString);

      System.out.println(envelope.boundary);
      System.out.println(envelope.boundary.getDimension());
      System.out.println(envelope.getCentroid());
   }

   /** It transforms envelopeString into 5 points representing envelope boundary attribute.
    */
   public void extractEnvelopePoints(String envelopeString){

      String[] pointsAsString = (envelopeString.split("#"));

      if (pointsAsString.length % 3 == 0){
           numberOfDimensions = 3;
      }
      else {
           numberOfDimensions = 2;
       }
      numberOfPoints = pointsAsString.length/numberOfDimensions;
      double[] points = new double[pointsAsString.length];

      for (int index = 0; index < pointsAsString.length; index++){
          points[index]= Double.parseDouble(pointsAsString[index]);
      }

      if (numberOfDimensions == 3){
          boundary = factory.polygonZ(points);
      }
      else{
          boundary = factory.polygon(points);
      }
      centroid = boundary.getCentroid();
   }

    /** Method gets centroid as Point.
     */
  public Point getCentroid() {
      return centroid;
  }
    /** Method gets envelope CRS.
     */
  public String getCRS(){
      return crs;
  }
}