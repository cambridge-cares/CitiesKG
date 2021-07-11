package uk.ac.cam.cares.twa.cities.agents.geo.test;

import junit.framework.TestCase;
import org.apache.jena.query.Query;
import org.geotools.geometry.jts.GeometryBuilder;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;
import uk.ac.cam.cares.jps.base.query.KGRouter;
import uk.ac.cam.cares.jps.base.query.RemoteKnowledgeBaseClient;
import uk.ac.cam.cares.twa.cities.agents.geo.DistanceAgent;
import uk.ac.cam.cares.twa.cities.model.geo.Envelope;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DistanceAgentTest extends TestCase {

    @Mock
    KnowledgeBaseClientInterface kgClientMock = Mockito.mock(RemoteKnowledgeBaseClient.class);

    @Test
    public void testGetDistanceQuery() {
        DistanceAgent distanceAgent = new DistanceAgent();
        String uri1 = "http://localhost/berlin/cityobject/UUID_39742eff-29ec-4c04-a732-22ee2a7986c4/";
        String uri2 = "http://localhost/berlin/cityobject/UUID_39742eff-29ec-4c04-a732-22ee2a7986c5/";

        try {
            assertNotNull(distanceAgent.getClass().getDeclaredMethod("getDistanceQuery", String.class, String.class));
            Method getDistanceQuery = distanceAgent.getClass().getDeclaredMethod("getDistanceQuery", String.class, String.class);
            getDistanceQuery.setAccessible(true);

            Query q = (Query) getDistanceQuery.invoke(distanceAgent, uri1, uri2);
            assertTrue(q.toString().contains("<http://localhost/berlin/cityobject/UUID_39742eff-29ec-4c04-a732-22ee2a7986c4/>"));
            assertTrue(q.toString().contains("<http://localhost/berlin/cityobject/UUID_39742eff-29ec-4c04-a732-22ee2a7986c5/>"));

        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetDistance() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        DistanceAgent distanceAgent = new DistanceAgent();

        //test with mocked kgClient and kgRouter when it returns a string.
        String distance = "[{'Distance': 10.0}]";
        Mockito.when(kgClientMock.execute(ArgumentMatchers.anyString())).thenReturn(distance);

        try (MockedStatic<KGRouter> kgRouterMock = Mockito.mockStatic(KGRouter.class)) {
            kgRouterMock.when(() -> KGRouter.getKnowledgeBaseClient(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean()))
                    .thenReturn(kgClientMock);
            assertNotNull(distanceAgent.getClass().getDeclaredMethod("getDistance", String.class, String.class));
            Method getDistance = distanceAgent.getClass().getDeclaredMethod("getDistance", String.class, String.class);
            getDistance.setAccessible(true);

            assertEquals(10.0, getDistance.invoke(distanceAgent,"firstUri", "secondUri" ));
        }


        //test with mocked kgClient and kgRouter when there is no string to return.
        String distanceEmpty = "[]";
        Mockito.when(kgClientMock.execute(ArgumentMatchers.anyString())).thenReturn(distanceEmpty);

        try (MockedStatic<KGRouter> kgRouterMock = Mockito.mockStatic(KGRouter.class)) {
            kgRouterMock.when(() -> KGRouter.getKnowledgeBaseClient(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean()))
                    .thenReturn(kgClientMock);
            try {
                assertNotNull(distanceAgent.getClass().getDeclaredMethod("getDistance", String.class, String.class));
                Method getDistance = distanceAgent.getClass().getDeclaredMethod("getDistance", String.class, String.class);
                getDistance.setAccessible(true);

                getDistance.invoke(distanceAgent,"firstUri", "secondUri");
                Assert.fail(); }

            catch (InvocationTargetException e){
                assertEquals(JSONException.class, e.getCause().getClass());
                assertEquals("JSONArray[0] not found.", e.getCause().getMessage()); }
        }
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

        try {
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