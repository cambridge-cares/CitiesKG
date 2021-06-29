package uk.ac.cam.cares.twa.cities.agents.geo.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import uk.ac.cam.cares.twa.cities.agents.geo.DistanceAgent;
import uk.ac.cam.cares.twa.cities.model.geo.Envelope;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class DistanceAgentTest extends TestCase {

    @Test
    public void testGetDistance(){
    }

    @Test
    public void testGetEnvelope() {
        DistanceAgent distanceAgent = new DistanceAgent();

        String uri1 = "http://localhost/berlin/cityobject/UUID_3eda2ae1-5621-47f6-91f9-cfb2ca7ff69a/";
        String uri2 = "http://localhost/berlin/cityobject/UUID_d75160e2-57b5-441d-b2d4-3b92f698515c/";

        ArrayList<String> uris = new ArrayList<>();
        uris.add(uri1);
        uris.add(uri2);

        distanceAgent.getEnvelope(uris);
    }

    @Test
    public void testComputeDistance(){
        DistanceAgent distanceAgent = new DistanceAgent();

        String envelopeString1 = "1#1#0#1#2#0#2#2#0#2#1#0#1#1#0";
        Envelope envelope1 =  new Envelope("EPSG:24500");
        envelope1.extractEnvelopePoints(envelopeString1);
        String envelopeString2 = "1#2#1#1#3#1#2#3#1#2#2#1#1#2#1";
        Envelope envelope2 =  new Envelope("EPSG:24500");
        envelope2.extractEnvelopePoints(envelopeString2);

        assertEquals(1.4142135623730951, distanceAgent.computeDistance(envelope1, envelope2));

        // test distance calculation with CRS conversion.

        String envelopeString3 = "2.85#-1.85#0#2.85#0.15#0#4.85#0.15#0#4.85#-1.85#0#2.85#-1.85#0";
        Envelope envelope3 =  new Envelope("EPSG:3414");
        envelope3.extractEnvelopePoints(envelopeString3);

        assertEquals(1.0034225353460755, distanceAgent.computeDistance(envelope1, envelope3));
    }

    @Test
    public void testSetDistance(){
    }
}
