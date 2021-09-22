package uk.ac.cam.cares.twa.cities.agents.geo.test;

import junit.framework.TestCase;
import org.apache.jena.query.Query;
import org.apache.jena.update.UpdateRequest;
import org.geotools.geometry.jts.GeometryBuilder;
import org.locationtech.jts.geom.Point;
import org.mockito.*;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;
import uk.ac.cam.cares.jps.base.query.KGRouter;
import uk.ac.cam.cares.jps.base.query.RemoteKnowledgeBaseClient;
import uk.ac.cam.cares.twa.cities.agents.geo.DistanceAgent;
import uk.ac.cam.cares.twa.cities.model.geo.Envelope;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class DistanceAgentTest extends TestCase {

    @Mock
    KnowledgeBaseClientInterface kgClientMock = Mockito.mock(RemoteKnowledgeBaseClient.class);

    public void testGetDistanceQuery()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        DistanceAgent distanceAgent = new DistanceAgent();
        String uri1 = "http://localhost/berlin/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3/";
        String uri2 = "http://localhost/berlin/cityobject/UUID_6cbfb096-5116-4962-9162-48b736768cd4/";


        assertNotNull(distanceAgent.getClass().getDeclaredMethod("getDistanceQuery", String.class, String.class));
        Method getDistanceQuery = distanceAgent.getClass().getDeclaredMethod("getDistanceQuery", String.class, String.class);
        getDistanceQuery.setAccessible(true);

        Query q = (Query) getDistanceQuery.invoke(distanceAgent, uri1, uri2);
        assertTrue(q.toString().contains("<http://localhost/berlin/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3/>"));
        assertTrue(q.toString().contains("<http://localhost/berlin/cityobject/UUID_6cbfb096-5116-4962-9162-48b736768cd4/>"));

    }

    public void testGetNamespace()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        DistanceAgent distanceAgent = new DistanceAgent();
        Method getNamespace = distanceAgent.getClass().getDeclaredMethod("getNamespace", String.class);
        getNamespace.setAccessible(true);

        //test whether Uri is split  and assembled into a namespace correctly.
        String uri = "http://localhost/berlin/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3/";
        assertEquals("http://localhost/berlin", getNamespace.invoke(distanceAgent, uri));

        //test whether Uri is split  and assembled into a namespace correctly.
        String uri2 = "http://localhost/berlin/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3";
        assertEquals("http://localhost/berlin", getNamespace.invoke(distanceAgent, uri2));
    }

    public void testGetObjectSRSQuery()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        DistanceAgent distanceAgent = new DistanceAgent();
        String uri1 = "http://localhost/berlin/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3/";

        assertNotNull(distanceAgent.getClass().getDeclaredMethod("getObjectSRSQuery", String.class, boolean.class));
        Method getObjectSRSQuery = distanceAgent.getClass().getDeclaredMethod("getObjectSRSQuery", String.class, boolean.class);
        getObjectSRSQuery.setAccessible(true);

        //test with mocked kgClient and kgRouter when it returns source srs.
        Query q = (Query) getObjectSRSQuery.invoke(distanceAgent, uri1, true);
        assertTrue(q.toString().contains("<http://localhost/berlin/sparql>"));
        assertTrue(q.toString().contains("ocgml:srsname"));

        //test with mocked kgClient and kgRouter when it returns a target srs.
        Query q2 = (Query) getObjectSRSQuery.invoke(distanceAgent, uri1, false);
        assertTrue(q2.toString().contains("<http://localhost/berlin/sparql>"));
        assertTrue(q2.toString().contains("ocgml:metricSrsName"));
    }

    public void testGetObjectSrs() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        DistanceAgent distanceAgent = new DistanceAgent();
        String uri = "http://localhost/berlin/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3/";

        //test with mocked kgClient and kgRouter when it returns a string.
        String srs = "[{'srsName': 'EPSG:3414'}]";
        when(kgClientMock.execute(ArgumentMatchers.anyString())).thenReturn(srs);
        try (MockedStatic<KGRouter> kgRouterMock = Mockito.mockStatic(KGRouter.class)) {
            kgRouterMock.when(() -> KGRouter.getKnowledgeBaseClient(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean()))
                .thenReturn(kgClientMock);

            assertNotNull(distanceAgent.getClass().getDeclaredMethod("getObjectSrs", String.class, boolean.class));
            Method getObjectSrs = distanceAgent.getClass().getDeclaredMethod("getObjectSrs", String.class, boolean.class);
            getObjectSrs.setAccessible(true);
            assertEquals("EPSG:3414", getObjectSrs.invoke(distanceAgent,uri, true));
        }

        //test with mocked kgClient and kgRouter when method is called to return appropriate metric srs.
        String targetSrs = "[{'srsName': 'EPSG:3414'}]";
        when(kgClientMock.execute(ArgumentMatchers.anyString())).thenReturn(targetSrs);
        try (MockedStatic<KGRouter> kgRouterMock = Mockito.mockStatic(KGRouter.class)) {
            kgRouterMock.when(() -> KGRouter.getKnowledgeBaseClient(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean()))
                .thenReturn(kgClientMock);

            assertNotNull(distanceAgent.getClass().getDeclaredMethod("getObjectSrs", String.class, boolean.class));
            Method getObjectSrs = distanceAgent.getClass().getDeclaredMethod("getObjectSrs", String.class, boolean.class);
            getObjectSrs.setAccessible(true);
            assertEquals("EPSG:3414", getObjectSrs.invoke(distanceAgent,uri, false));
        }

        //test with mocked kgClient and kgRouter when there is no string to return.
        String srsEmpty = "[]";
        when(kgClientMock.execute(ArgumentMatchers.anyString())).thenReturn(srsEmpty);
        try (MockedStatic<KGRouter> kgRouterMock = Mockito.mockStatic(KGRouter.class)) {
            kgRouterMock.when(() -> KGRouter.getKnowledgeBaseClient(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean()))
                .thenReturn(kgClientMock);
            Method getObjectSrs = distanceAgent.getClass().getDeclaredMethod("getObjectSrs", String.class, boolean.class);
            getObjectSrs.setAccessible(true);

            assertEquals("EPSG:4236",  getObjectSrs.invoke(distanceAgent,uri, true));
        }
    }

    public void testGetDistance() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        DistanceAgent distanceAgent = new DistanceAgent();

        //test with mocked kgClient and kgRouter when it returns a string.
        String distance = "[{'distance': 10.0}]";
        when(kgClientMock.execute(ArgumentMatchers.anyString())).thenReturn(distance);
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
        when(kgClientMock.execute(ArgumentMatchers.anyString())).thenReturn(distanceEmpty);
        try (MockedStatic<KGRouter> kgRouterMock = Mockito.mockStatic(KGRouter.class)) {
            kgRouterMock.when(() -> KGRouter.getKnowledgeBaseClient(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean()))
                    .thenReturn(kgClientMock);
            assertNotNull(distanceAgent.getClass().getDeclaredMethod("getDistance", String.class, String.class));
            Method getDistance = distanceAgent.getClass().getDeclaredMethod("getDistance", String.class, String.class);
            getDistance.setAccessible(true);

            assertEquals(-1.0,  getDistance.invoke(distanceAgent,"firstUri", "secondUri"));
        }
    }

    public void testGetEnvelope(){

        DistanceAgent distanceAgent = new DistanceAgent();
        String uri = "http://localhost/berlin/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3/";
        String mockedEnvelopeString = "0#0#0#0#1#0#1#1#0#1#0#0";

        try (MockedConstruction<Envelope> ignored = mockConstruction(Envelope.class,
                (mock, context)-> when(mock.getEnvelopeString(uri)).thenReturn(mockedEnvelopeString))) {
            Envelope envelope = distanceAgent.getEnvelope(uri, "EPSG:25833");
            Mockito.verify(envelope).getEnvelopeString(uri);
            Mockito.verify(envelope).extractEnvelopePoints(mockedEnvelopeString);
        }
    }

    public void testComputeDistance() {

        DistanceAgent distanceAgent = new DistanceAgent();

        // test distance calculation without CRS conversion.
        String envelopeString1 = "1#1#0#1#2#0#2#2#0#2#1#0#1#1#0";
        Envelope envelope1 =  new Envelope("EPSG:24500");
        envelope1.extractEnvelopePoints(envelopeString1);

        String envelopeString2 = "1#2#1#1#3#1#2#3#1#2#2#1#1#2#1";
        Envelope envelope2 =  new Envelope("EPSG:24500");
        envelope2.extractEnvelopePoints(envelopeString2);

        assertEquals(1.4142135623730951, distanceAgent.computeDistance(envelope1, envelope2, "EPSG:24500"));

        // test distance calculation with CRS conversion.
        String envelopeString3 = "2.85#-1.85#0#2.85#0.15#0#4.85#0.15#0#4.85#-1.85#0#2.85#-1.85#0";
        Envelope envelope3 = new Envelope("EPSG:3414");
        envelope3.extractEnvelopePoints(envelopeString3);

        assertEquals(1.0034225353460755, distanceAgent.computeDistance(envelope1, envelope3, "EPSG:24500"));
    }

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
            fail();
        }
    }

    public void testGetSetDistanceQuery(){

        DistanceAgent distanceAgent = new DistanceAgent();

        String uri1 = "http:cityobjectxample/cityobject2";
        String uri2 = "http:cityobjectxample/cityobject3";
        String distanceUriMock = "123e4567-e89b-12d3-a456-556642440000";
        String valueUriMock = "123e4567-e89b-12d3-a456-556642441111";
        double distance = 10.0;

        try (MockedStatic<UUID> randomUUID = Mockito.mockStatic(UUID.class, RETURNS_DEEP_STUBS)){

            randomUUID.when(() -> UUID.randomUUID().toString()).thenReturn(distanceUriMock, valueUriMock);

            assertNotNull(distanceAgent.getClass().getDeclaredMethod("getSetDistanceQuery", String.class, String.class, double.class));
            Method getSetDistanceQuery = distanceAgent.getClass().getDeclaredMethod("getSetDistanceQuery", String.class, String.class, double.class);
            getSetDistanceQuery.setAccessible(true);

            UpdateRequest ur = (UpdateRequest) getSetDistanceQuery.invoke(distanceAgent, uri1, uri2, distance);

            assertTrue(ur.toString().contains("<http:cityobjectxample/cityobject2>"));
            assertTrue(ur.toString().contains("<http:cityobjectxample/cityobject3>"));
            assertTrue(ur.toString().contains("10.0"));
            assertTrue(ur.toString().contains("123e4567-e89b-12d3-a456-556642440000"));
            assertTrue(ur.toString().contains("123e4567-e89b-12d3-a456-556642441111"));
        }
        catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            fail();
        }
    }

    public void testSetDistance() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        DistanceAgent distanceAgent = new DistanceAgent();

        when(kgClientMock.executeUpdate(ArgumentMatchers.any(UpdateRequest.class))).thenReturn(0);
        try (MockedStatic<KGRouter> kgRouterMock = Mockito.mockStatic(KGRouter.class)) {
            kgRouterMock.when(() -> KGRouter.getKnowledgeBaseClient(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean()))
                    .thenReturn(kgClientMock);
            assertNotNull(distanceAgent.getClass().getDeclaredMethod("setDistance", String.class, String.class, double.class));
            Method setDistance = distanceAgent.getClass().getDeclaredMethod("setDistance", String.class, String.class, double.class);
            setDistance.setAccessible(true);
            assertEquals(0, setDistance.invoke(distanceAgent,"firstUri", "secondUri", 10.0 ));
        }
    }
}