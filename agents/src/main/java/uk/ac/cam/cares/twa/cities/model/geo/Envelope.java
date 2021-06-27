package uk.ac.cam.cares.twa.cities.model.geo;
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

    public Envelope(String crs) {
        this.crs = crs;
    }

    public static void main (String[] args){
      Envelope envelope = new Envelope("EPSG:4326");
      String envelopeString = "1#1#1#1#2#1#2#2#1#2#1#1#1#1#1";
      envelope.extractEnvelopePoints(envelopeString);
      System.out.println(envelope.boundary);
      System.out.println(envelope.boundary.getDimension());
      System.out.println(envelope.getCentroid().getCoordinate());
      System.out.println(envelope.getCentroid());
   }

   /** It transforms envelopeString into 5 points representing envelope boundary attribute.
    */
   public void extractEnvelopePoints(String envelopeString) {
       if (envelopeString.equals("")) {
           throw new IllegalArgumentException("empty String");
       }
       else if (!envelopeString.contains("#")){
           throw new IllegalArgumentException("Does not contain #");
       }

      String[] pointsAsString = (envelopeString.split("#"));

      if (pointsAsString.length % 3 == 0){
           numberOfDimensions = 3;
      }
      else if (pointsAsString.length % 2 == 0){
           numberOfDimensions = 2;
       }
      else {
          throw new IllegalArgumentException("Number of points is not divisible by 3 or 2");
       }

      numberOfPoints = pointsAsString.length/numberOfDimensions;
       if (numberOfPoints < 4) {
          throw new IllegalArgumentException("Polygon has less than 4 points");
       }
      double[] points = new double[pointsAsString.length];
      for (int index = 0; index < pointsAsString.length; index++){
          points[index]= Double.parseDouble(pointsAsString[index]);
      }
      double centroidZ = 0;
      if (numberOfDimensions == 3){
          boundary = factory.polygonZ(points);
          for (int z = 2; z < points.length-3; z +=3 ){
              centroidZ += points[z];
          }
          centroidZ = centroidZ/(numberOfPoints-1);
      }
      else{
          boundary = factory.polygon(points);
      }
      centroid = boundary.getCentroid();

      // Updates the centroid Z value. If it's 3D, it overrides the existing Z, of it's 2D: it's replaces default NaN with 0.
      centroid.getCoordinateSequence().setOrdinate(0, 2, centroidZ);
      centroid.geometryChanged();
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