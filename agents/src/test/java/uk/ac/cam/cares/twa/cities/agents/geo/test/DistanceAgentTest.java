package uk.ac.cam.cares.twa.cities.agents.geo.test;

import junit.framework.TestCase;
import org.junit.Test;
import uk.ac.cam.cares.twa.cities.agents.geo.DistanceAgent;
import uk.ac.cam.cares.twa.cities.model.geo.Envelope;

public class DistanceAgentTest extends TestCase {

    @Test
    public void testGetDistance(){
    }

    @Test
    public void testGetEnvelope(){
    }

    @Test
    public void testComputeDistance(){
        DistanceAgent distanceAgent = new DistanceAgent();

        String envelopeString1 = "1#1#0#1#2#0#2#2#0#2#1#0#1#1#0";
        Envelope envelope1 =  new Envelope("EPSG:24500");
        envelope1.extractEnvelopePoints(envelopeString1);
        // centroid coordinates are 1.5,1.5,0.

        String envelopeString2 = "1#2#1#1#3#1#2#3#1#2#2#1#1#2#1";
        Envelope envelope2 =  new Envelope("EPSG:24500");
        envelope2.extractEnvelopePoints(envelopeString2);
        // centroid coordinates are 1.5,2.5,1.

        //computes distance between point without crs conversion.
        assertEquals(Math.sqrt(2.0),distanceAgent.computeDistance(envelope1, envelope2));

        //1.5,2.5 centroid values in epsg:3414 is 3.85,-0.85.
        String envelopeString3 = "2.85#-1.85#0#2.85#0.15#0#4.85#0.15#0#4.85#-1.85#0#2.85#-1.85#0";
        Envelope envelope3 =  new Envelope("EPSG:3414");
        envelope3.extractEnvelopePoints(envelopeString3);

        //computes distance between point with crs conversion.
        assertTrue(Math.abs(1.0 - distanceAgent.computeDistance(envelope1, envelope3)) < 0.01);
    }

    @Test
    public void testSetDistance(){
    }
}
