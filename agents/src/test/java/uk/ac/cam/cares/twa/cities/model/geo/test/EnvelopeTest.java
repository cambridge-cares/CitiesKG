package uk.ac.cam.cares.twa.cities.model.geo.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import uk.ac.cam.cares.twa.cities.model.geo.Envelope;

public class EnvelopeTest extends TestCase {

    @Test
    public void testExtractEnvelopePoints(){

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
