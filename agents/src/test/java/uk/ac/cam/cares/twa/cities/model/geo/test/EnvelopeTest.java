package uk.ac.cam.cares.twa.cities.model.geo.test;

import java.lang.reflect.Field;
import junit.framework.TestCase;
import org.apache.jena.query.Query;
import org.json.JSONException;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.ac.cam.cares.jps.base.interfaces.KnowledgeBaseClientInterface;
import uk.ac.cam.cares.jps.base.query.KGRouter;
import uk.ac.cam.cares.jps.base.query.RemoteKnowledgeBaseClient;
import uk.ac.cam.cares.twa.cities.model.geo.Envelope;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EnvelopeTest extends TestCase {

    @Mock
    KnowledgeBaseClientInterface kgClientMock = Mockito.mock(RemoteKnowledgeBaseClient.class);

    public void testGetEnvelopeQuery(){

        Envelope envelope = new Envelope("EPSG:4326");
        String uriString = "http://localhost/berlin/cityobject/UUID_89f9a49d-e53b-4f59-beb5-748371d58c25/";

        try {
            assertNotNull(envelope.getClass().getDeclaredMethod("getEnvelopeQuery", String.class));
            Method getEnvelopeQuery = envelope.getClass().getDeclaredMethod("getEnvelopeQuery", String.class);
            getEnvelopeQuery.setAccessible(true);

            Query q = (Query) getEnvelopeQuery.invoke(envelope, uriString);
            assertTrue(q.toString().contains("<http://localhost/berlin/cityobject/UUID_89f9a49d-e53b-4f59-beb5-748371d58c25/>"));
        }
        catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            fail();
        }
    }

    public void testGetEnvelopeGraphUri()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {

        Envelope envelope = new Envelope("EPSG:4236");

        Method getEnvelopeGraphUri = envelope.getClass().getDeclaredMethod("getEnvelopeGraphUri", String.class);
        getEnvelopeGraphUri.setAccessible(true);

        Field cityObject = envelope.getClass().getDeclaredField("cityobjectURI");
        cityObject.setAccessible(true);
        cityObject.set(envelope, "/sparql/cityobject/");
        //test whether Uri is split  and assembled into a namespace correctly.
        String uri = "http://localhost/berlin/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3/";
        assertEquals("http://localhost/berlin/sparql/cityobject/", getEnvelopeGraphUri.invoke(envelope, uri));
    }

    public void testGetEnvelopeString(){

        Envelope envelope = new Envelope("EPSG:4326");
        String uri = "http://localhost/berlin/cityobject/UUID_89f9a49d-e53b-4f59-beb5-748371d58c25/";
        //test with mocked kgClient and kgRouter when it returns a string.
        String json = "[{'Envelope': '1#2#0'}]";
        Mockito.when(kgClientMock.execute(ArgumentMatchers.anyString())).thenReturn(json);

        try (MockedStatic<KGRouter> kgRouterMock = Mockito.mockStatic(KGRouter.class)) {
            kgRouterMock.when(() -> KGRouter.getKnowledgeBaseClient(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean()))
                    .thenReturn(kgClientMock);
            assertEquals("1#2#0", envelope.getEnvelopeString(uri));
        }

        //test with mocked kgClient and kgRouter when there is no string to return.
        String jsonEmpty = "[]";
        Mockito.when(kgClientMock.execute(ArgumentMatchers.anyString())).thenReturn(jsonEmpty);

        try (MockedStatic<KGRouter> kgRouterMock = Mockito.mockStatic(KGRouter.class)) {
            kgRouterMock.when(() -> KGRouter.getKnowledgeBaseClient(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean()))
                    .thenReturn(kgClientMock);
            try {
                envelope.getEnvelopeString(uri);
                fail();
            }
            catch (JSONException error){
                assertEquals("JSONArray[0] not found.", error.getMessage());
            }
        }
    }

    public void testExtractEnvelopePoints(){

        Envelope envelope = new Envelope("EPSG:4326");

        //when empty string;
        String envelopeString5 = "";
        try {
            envelope.extractEnvelopePoints(envelopeString5);
            fail();
        }
        catch (IllegalArgumentException error){
            assertEquals("empty String", error.getMessage());
        }

        //when string with no #;
        String envelopeString6 = "1 1 1 1 2 1 2 2 1 2 1 1 1 1 1";
        try {
            envelope.extractEnvelopePoints(envelopeString6);
            fail();
        }
        catch (IllegalArgumentException error){
            assertEquals("Does not contain #", error.getMessage());
        }

        //when the number of coordinates is not divisible by 3 or 2.
        String envelopeString4 = "1#1#1#1#2";
        try {
            envelope.extractEnvelopePoints(envelopeString4);
            fail();
        }
        catch (IllegalArgumentException error){
            assertEquals("Number of points is not divisible by 3 or 2", error.getMessage());
        }

        //when string would have less than 4 points;
        String envelopeString8 = "1#1#1#1#2#1#2#2#1";
        try {
            envelope.extractEnvelopePoints(envelopeString8);
            fail();
        }
        catch  (IllegalArgumentException error){
            assertEquals("Polygon has less than 4 points", error.getMessage());
        }

        //when string would 5 points and 3 dimensions;
        String envelopeString1 = "1#1#1#1#2#1#2#2#1#2#1#1#1#1#1";
        envelope.extractEnvelopePoints(envelopeString1);
        assertEquals(6.0/4.0, envelope.getCentroid().getCoordinate().getX());
        assertEquals(6.0/4.0, envelope.getCentroid().getCoordinate().getY());
        assertEquals(1.0, envelope.getCentroid().getCoordinate().getZ());

        //when string with 5 points and 2 dimensions;
        String envelopeString2 = "1#1#1#2#2#2#2#1#1#1";
        envelope.extractEnvelopePoints(envelopeString2);
        assertEquals(6.0/4.0, envelope.getCentroid().getCoordinate().getX());
        assertEquals(6.0/4.0, envelope.getCentroid().getCoordinate().getY());
        assertEquals(0.0, envelope.getCentroid().getCoordinate().getZ());
    }

    public void testGetCentroid(){

        Envelope envelope = new Envelope("EPSG:4326");
        assertNull(envelope.getCentroid());

        //when centroid is with 3 dimensions;
        String envelopeString = "1.29227#103.83094#1#1.29262#103.83094#1#1.29262#103.83148#1#1.29227#103.83148#1#1.29227#103.83094#1";
        envelope.extractEnvelopePoints(envelopeString);
        Point centroid = envelope.getCentroid();
        assertFalse(Double.isNaN(centroid.getCoordinate().getZ()));

        //when centroid is with 2 dimensions;
        String envelopeString2 = "1.29227#103.83094#1.29262#103.83094#1.29262#103.83148#1.29227#103.83148#1.29227#103.83094";
        envelope.extractEnvelopePoints(envelopeString2);
        Point centroid2 = envelope.getCentroid();
        assertFalse(Double.isNaN(centroid2.getCoordinate().getZ()));
    }

    public void testGetCRS(){
        Envelope envelope = new Envelope("EPSG:4326");
        assertEquals(envelope.getCRS(),"EPSG:4326");
    }
}
