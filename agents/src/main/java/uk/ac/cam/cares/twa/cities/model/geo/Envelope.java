package uk.ac.cam.cares.twa.cities.model.geo;

/**
 * This comment describes the Envelope class.
 * Unique URIs are used as inputs for Envelope class.
 * The class transforms Envelope string into a list of points that defines envelope boundary and from it computes envelope centroid.
 */
public class Envelope {

   /**
    * extractEnvelopePoints method is used by getEnvelope method from DistanceAgent class.
    * It transforms envelopeString into 5 points representing envelope boundary attribute.
    */
   private float[] extractEnvelopePoints(String[] envelopeString){
      float[] boundary = new float[15];
      return boundary;

   }

   /**
    * computeCentroid method uses boundary attribute of an envelope class and computes a centroid for each envelope.
    */
   private float[] computeCentroid(float[] boundary){
      float[] centroid= new float[3];
      return centroid;

   }
}
