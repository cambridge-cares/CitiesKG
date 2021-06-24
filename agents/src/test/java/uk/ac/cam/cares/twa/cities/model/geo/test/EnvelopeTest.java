package uk.ac.cam.cares.twa.cities.model.geo.test;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import uk.ac.cam.cares.twa.cities.model.geo.Envelope;

public class EnvelopeTest extends TestCase {

    @Test
    public void testExtractEnvelopePoints(){
        Envelope envelope = new Envelope("EPSG:4326");

        // string would 5 points and 3 dimensions;
        String envelopeString1 = "1#1#1#1#2#1#2#2#1#2#1#1#1#1#1";
        envelope.extractEnvelopePoints(envelopeString1);
        assertEquals(6.0/4.0, envelope.getCentroid().getCoordinate().getX());
        assertEquals(6.0/4.0, envelope.getCentroid().getCoordinate().getY());
        assertEquals(1.0, envelope.getCentroid().getCoordinate().getZ());

        //String with 5 points and 2 dimensions;
        String envelopeString2 = "1#1#1#2#2#2#2#1#1#1";
        envelope.extractEnvelopePoints(envelopeString2);
        assertEquals(6.0/4.0, envelope.getCentroid().getCoordinate().getX());
        assertEquals(6.0/4.0, envelope.getCentroid().getCoordinate().getY());
        assertEquals(0.0, envelope.getCentroid().getCoordinate().getZ());

        // string would contain only 4,3,2 points; --> works, error is thrown when it is only 1 point.
        // Ask Arkadiusz how to treat envelope
        String envelopeString3 = "2#2#1#2#3#1#3#3#1#3#2#1";

        // Number of coordinates is not divisible by 3 or 2;
        String envelopeString4 = "1#1#1#1#2";
        try{
            envelope.extractEnvelopePoints(envelopeString4);
            Assert.fail();
        }
        catch (IllegalArgumentException error){
           assertEquals("Ordinate array length 5 is not a multiple of dimension 2", error.getMessage());
        }

        //empty string;
        String envelopeString5 = "";
        try{
            envelope.extractEnvelopePoints(envelopeString5);
            Assert.fail();
        }
        catch (NumberFormatException error){
            assertEquals("empty String", error.getMessage());
        }

        //string with no #;
        String envelopeString6 = "1 1 1 1 2 1 2 2 1 2 1 1 1 1 1";
        try{
            envelope.extractEnvelopePoints(envelopeString6);
            Assert.fail();
        }
        catch (NumberFormatException error){
        }

        //string with no numbers.
        String envelopeString7 = "skjdfhiehfslseofeslfh";
        try{
            envelope.extractEnvelopePoints(envelopeString7);
            Assert.fail();
        }
        catch (NumberFormatException error){
        }
    }

    @Test
    public void testGetCentroid(){
        Envelope envelope = new Envelope("EPSG:4326");
        assertNull(envelope.getCentroid());

        String envelopeString = "1.29227#103.83094#1#1.29262#103.83094#1#1.29262#103.83148#1#1.29227#103.83148#1#1.29227#103.83094#1";
        envelope.extractEnvelopePoints(envelopeString);
        Point centroid = envelope.getCentroid();
        assertFalse(Double.isNaN(centroid.getCoordinate().getZ()));

        String envelopeString2 = "1.29227#103.83094#1.29262#103.83094#1.29262#103.83148#1.29227#103.83148#1.29227#103.83094";
        envelope.extractEnvelopePoints(envelopeString);
        Point centroid2 = envelope.getCentroid();
        assertFalse(Double.isNaN(centroid.getCoordinate().getZ()));
    }

    @Test
    public void testGetCRS(){
        Envelope envelope = new Envelope("EPSG:4326");
        assertEquals(envelope.getCRS(),"EPSG:4326");
    }
}
