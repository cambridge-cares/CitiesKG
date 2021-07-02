package uk.ac.cam.cares.twa.cities.model.geo.test;

import junit.framework.TestCase;
import org.apache.jena.query.Query;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import uk.ac.cam.cares.twa.cities.model.geo.Envelope;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EnvelopeTest extends TestCase {

    @Test
    public void testGetEnvelopeQuery(){

        String expectedQuery = "PREFIX  ocgml: <http://locahost/ontocitygml/>\n\nSELECT  ?Envelope\nWHERE\n  { GRAPH <http://localhost/berlin/cityobject/>\n      { <http://localhost/berlin/cityobject/UUID_39742eff-29ec-4c04-a732-22ee2a7986c4/>\n                  ocgml:EnvelopeType  ?Envelope}}\n";

        Envelope envelope = new Envelope("EPSG:4326");
        String uriString = "http://localhost/berlin/cityobject/UUID_39742eff-29ec-4c04-a732-22ee2a7986c4/";

        try{
            assertNotNull(envelope.getClass().getDeclaredMethod("getEnvelopeQuery", String.class));
            Method getEnvelopeQuery = envelope.getClass().getDeclaredMethod("getEnvelopeQuery", String.class);
            getEnvelopeQuery.setAccessible(true);

            Query q = (Query) getEnvelopeQuery.invoke(envelope, uriString);
            assertEquals(expectedQuery, q.toString());

        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testGetEnvelopeString(){
        //tbd
        Envelope envelope =  new Envelope("EPSG:4236");
        String uriString = "http://www.theworldavatar.com/citieskg/singapore/cityobject/UUID_00790be5-11a4-4da8-9be8-7c0c9dc7cfc2/";
        String expectedEnvelopeString = "29021.61944#28581.3432#0.0#29108.64926#28581.3432#0.0#29108.64926#28762.99547#68.5#29021.61944#28762.99547#68.5#29021.61944#28581.3432#0.0";

        assertEquals(expectedEnvelopeString, envelope.getEnvelopeString(uriString));
    }


    @Test
    public void testGetKGClientForEnvelopeQuery(){
    //tbd
    }


    @Test
    public void testExtractEnvelopePoints(){
        Envelope envelope = new Envelope("EPSG:4326");

        //empty string;
        String envelopeString5 = "";
        try{
            envelope.extractEnvelopePoints(envelopeString5);
            Assert.fail();
        }
        catch (IllegalArgumentException error){
            assertEquals("empty String", error.getMessage());
        }

        //string with no #;
        String envelopeString6 = "1 1 1 1 2 1 2 2 1 2 1 1 1 1 1";
        try{
            envelope.extractEnvelopePoints(envelopeString6);
            Assert.fail();
        }
        catch (IllegalArgumentException error){
            assertEquals("Does not contain #", error.getMessage());
        }

        // Number of coordinates is not divisible by 3 or 2 or if there is less points than 4?;
        String envelopeString4 = "1#1#1#1#2";
        try{
            envelope.extractEnvelopePoints(envelopeString4);
            Assert.fail();
        }
        catch (IllegalArgumentException error){
            assertEquals("Number of points is not divisible by 3 or 2", error.getMessage());
        }

        // string would have less than 4 points;
        String envelopeString8 = "1#1#1#1#2#1#2#2#1";
        try{
            envelope.extractEnvelopePoints(envelopeString8);
            Assert.fail();
        }
        catch  (IllegalArgumentException error){
            assertEquals("Polygon has less than 4 points", error.getMessage());
        }

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
        envelope.extractEnvelopePoints(envelopeString2);
        Point centroid2 = envelope.getCentroid();
        assertFalse(Double.isNaN(centroid2.getCoordinate().getZ()));
    }


    @Test
    public void testGetCRS(){
        Envelope envelope = new Envelope("EPSG:4326");
        assertEquals(envelope.getCRS(),"EPSG:4326");
    }
}
