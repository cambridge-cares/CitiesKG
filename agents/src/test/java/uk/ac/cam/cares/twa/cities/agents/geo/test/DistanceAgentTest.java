package uk.ac.cam.cares.twa.cities.agents.geo.test;

import junit.framework.TestCase;
import org.apache.jena.query.Query;
import org.geotools.geometry.jts.GeometryBuilder;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import uk.ac.cam.cares.twa.cities.agents.geo.DistanceAgent;
import uk.ac.cam.cares.twa.cities.model.geo.Envelope;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DistanceAgentTest extends TestCase {

    @Test
    public void testGetDistanceQuery() {
        DistanceAgent distanceAgent = new DistanceAgent();
        String expectedQuery = "PREFIX  ocgml: <http://locahost/ontocitygml/>\n" + "\n" + "SELECT  ?distance\n" + "WHERE\n" + "  { GRAPH <SomeGraph>\n" + "      { <http://localhost/berlin/cityobject/UUID_39742eff-29ec-4c04-a732-22ee2a7986c4/>\n" + "                  ocgml:hasDistance  ?distanceUri}\n" + "    <http://localhost/berlin/cityobject/UUID_39742eff-29ec-4c04-a732-22ee2a7986c4/>\n" + "              ocgml:hasDistance  ?distanceUri .\n" + "    ?distanceUri  ocgml:hasValue  ?distance\n" + "  }"+ "\n";
        String uri1 = "http://localhost/berlin/cityobject/UUID_39742eff-29ec-4c04-a732-22ee2a7986c4/";
        String uri2 = "http://localhost/berlin/cityobject/UUID_39742eff-29ec-4c04-a732-22ee2a7986c4/";

        try {
            assertNotNull(distanceAgent.getClass().getDeclaredMethod("getDistanceQuery", String.class, String.class));
            Method getDistanceQuery = distanceAgent.getClass().getDeclaredMethod("getDistanceQuery", String.class, String.class);
            getDistanceQuery.setAccessible(true);

            Query q = (Query) getDistanceQuery.invoke(distanceAgent, uri1, uri2);
            assertEquals(expectedQuery, q.toString());

        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetDistance(){
        //tbd
    }


    @Test
    public void testGetKGClientForDistanceQuery(){
        //tbd
    }


    @Test
    public void testGetEnvelope() {
        //tbd
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
    public void testSetUniformCRS(){
        DistanceAgent distanceAgent = new DistanceAgent();
        GeometryBuilder builder = new GeometryBuilder();
        Point point = builder.pointZ(1,1,0);

        try{
            assertNotNull(distanceAgent.getClass().getDeclaredMethod("setUniformCRS", Point.class, String.class, String.class));
            Method setUniformCRS = distanceAgent.getClass().getDeclaredMethod("setUniformCRS", Point.class, String.class, String.class);
            setUniformCRS.setAccessible(true);

            Point pointTransformed = (Point) setUniformCRS.invoke(distanceAgent,point, "EPSG:3414", "EPSG:24500");

            assertEquals(3.3531995128068957, pointTransformed.getCoordinate().x);
            assertEquals(-0.34659662783087697, pointTransformed.getCoordinate().y);
            assertEquals(0.0, pointTransformed.getCoordinate().z);
        }
        catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testSetDistance(){
        //tbd
    }
}