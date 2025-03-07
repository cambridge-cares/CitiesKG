package uk.ac.cam.cares.twa.cities.model.geo;

import gov.nasa.worldwind.ogc.kml.*;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class EnvelopeCentroidTest {

    @Test
    public void testNewEnvelopeCentroid() {
        EnvelopeCentroid envelopeCentroid = new EnvelopeCentroid();
        assertNotNull(envelopeCentroid);
    }

    @Test
    public void testNewEnvelopeCentroidFields() {
        try {
            EnvelopeCentroid envelopeCentroid = new EnvelopeCentroid();

            assertEquals(3, envelopeCentroid.getClass().getDeclaredFields().length);

            Field envelope = envelopeCentroid.getClass().getDeclaredField("envelope");
            envelope.setAccessible(true);
            assertNotNull(envelope.get(envelopeCentroid));

            Field centroid = envelopeCentroid.getClass().getDeclaredField("centroid");
            centroid.setAccessible(true);
            assertNotNull(centroid.get(envelopeCentroid));

            Field building = envelopeCentroid.getClass().getDeclaredField("building");
            building.setAccessible(true);
            assertNotNull(building);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testNewEnvelopeCentroidMethods() {
        EnvelopeCentroid envelopeCentroid = new EnvelopeCentroid();
        assertEquals(2, envelopeCentroid.getClass().getDeclaredMethods().length);
    }

    @Test
    public void testCalcEnvelope() throws IOException, XMLStreamException {
        // test case for multigeom
        KMLRoot kmlRoot = KMLRoot.create(Objects.requireNonNull(this.getClass().getResource(
            "/testoutput.kml")).getFile());
        kmlRoot.parse();
        KMLAbstractContainer abCon = (KMLAbstractContainer) kmlRoot.getFeature();

        List<KMLAbstractFeature> abFeats = abCon.getFeatures();
        KMLPlacemark placemark = (KMLPlacemark) abFeats.get(0);

        double[] multigeomEnvelope = EnvelopeCentroid.calcEnvelope(placemark.getGeometry());

        assertEquals(1.0, multigeomEnvelope[0]);
        assertEquals(4.0, multigeomEnvelope[1]);
        assertEquals(1.0, multigeomEnvelope[2]);
        assertEquals(4.0, multigeomEnvelope[3]);
    }

    @Test
    public void testCalcCentroid() {
        double[] envelope = {0.0, 1.0, 0.0, 1.0};
        double[] centroid = EnvelopeCentroid.calcCentroid(envelope);
        assertEquals(0.5, centroid[0]);
        assertEquals(0.5, centroid[1]);
    }
}
