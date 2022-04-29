package uk.ac.cam.cares.twa.cities.agents.geo.test;

import org.apache.jena.query.Query;
import org.apache.jena.update.UpdateRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.stubbing.Answer;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.agents.geo.DistanceAgent;
import uk.ac.cam.cares.twa.cities.models.ModelContext;
import uk.ac.cam.cares.twa.cities.models.geo.EnvelopeType;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DistanceAgentTest {

  @Test
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

    @Test
    public void testGetNamespace()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        DistanceAgent distanceAgent = new DistanceAgent();
        Method getNamespace = distanceAgent.getClass().getDeclaredMethod("getNamespace", String.class);
        getNamespace.setAccessible(true);

        //test whether Uri is split  and assembled into a namespace correctly.
        String uri = "http://localhost:9999/blazegraph/namespace/berlin/sparql/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3/";
        assertEquals("http://localhost:9999/blazegraph/namespace/berlin/sparql", getNamespace.invoke(distanceAgent, uri));

        //test whether Uri is split  and assembled into a namespace correctly.
        String uri2 = "http://localhost:9999/blazegraph/namespace/berlin/sparql/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3";
        assertEquals("http://localhost:9999/blazegraph/namespace/berlin/sparql", getNamespace.invoke(distanceAgent, uri2));
    }

    @Test
    public void testGetObjectSRSQuery()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        DistanceAgent distanceAgent = new DistanceAgent();
        String uri1 = "http://localhost:9999/blazegraph/namespace/berlin/sparql/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3/";

        assertNotNull(distanceAgent.getClass().getDeclaredMethod("getObjectSRSQuery", String.class, boolean.class));
        Method getObjectSRSQuery = distanceAgent.getClass().getDeclaredMethod("getObjectSRSQuery", String.class, boolean.class);
        getObjectSRSQuery.setAccessible(true);

        //test return source srs.
        Query q = (Query) getObjectSRSQuery.invoke(distanceAgent, uri1, true);
        assertTrue(q.toString().contains("<http://localhost:9999/blazegraph/namespace/berlin/sparql/>"));
        assertTrue(q.toString().contains("ocgml:srsname"));

        //test return a target srs.
        Query q2 = (Query) getObjectSRSQuery.invoke(distanceAgent, uri1, false);
        assertTrue(q2.toString().contains("<http://localhost:9999/blazegraph/namespace/berlin/sparql/>"));
        assertTrue(q2.toString().contains("ocgml:metricSrsName"));
    }

    @Test
    public void testGetObjectSrs() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {

        DistanceAgent distanceAgent = new DistanceAgent();

        // Get getObjectSrs method
        assertNotNull(distanceAgent.getClass().getDeclaredMethod("getObjectSrs", String.class, boolean.class));
        Method getObjectSrs = distanceAgent.getClass().getDeclaredMethod("getObjectSrs", String.class, boolean.class);
        getObjectSrs.setAccessible(true);

        String uri = "http://localhost:9999/blazegraph/namespace/berlin/sparql/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3/";
        JSONArray expected3414 = new JSONArray().put(new JSONObject().put("srsName", "EPSG:3414"));
        JSONArray expected4326 = new JSONArray().put(new JSONObject().put("srsName", "EPSG:4326"));

        ModelContext context = new ModelContext("route", "namespace");
        Field contextField = distanceAgent.getClass().getDeclaredField("context");
        contextField.setAccessible(true);
        contextField.set(distanceAgent, context);

        //test with mocked AccessAgentCaller
        try (MockedStatic<AccessAgentCaller> accessAgentCallerMock = Mockito.mockStatic(AccessAgentCaller.class)) {
            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                    .thenReturn(expected3414);
            assertEquals("EPSG:3414", getObjectSrs.invoke(distanceAgent, uri, true));

            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                    .thenReturn(expected4326);
            assertEquals("EPSG:4326", getObjectSrs.invoke(distanceAgent, uri, true));
        }
    }

    @Test
    public void testGetDistance() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {

        DistanceAgent distanceAgent = new DistanceAgent();

        // Get getDistance method
        assertNotNull(distanceAgent.getClass().getDeclaredMethod("getDistance", String.class, String.class));
        Method getDistance = distanceAgent.getClass().getDeclaredMethod("getDistance", String.class, String.class);
        getDistance.setAccessible(true);

        JSONArray expected = new JSONArray().put(new JSONObject().put("distance", "10.0"));
        JSONArray expectedBlank = new JSONArray();

        ModelContext context = new ModelContext("route", "namespace");
        Field contextField = distanceAgent.getClass().getDeclaredField("context");
        contextField.setAccessible(true);
        contextField.set(distanceAgent, context);

        try (MockedStatic<AccessAgentCaller> accessAgentCallerMock = Mockito.mockStatic(AccessAgentCaller.class)) {

            //test with mocked AccessAgentCaller when it returns a string.
            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                    .thenReturn(expected);
            assertEquals(10.0, getDistance.invoke(distanceAgent, "http://localhost/berlin/cityobject/UUID_1/", "http://localhost/berlin/cityobject/UUID_2/"));

            //test with mocked AccessAgentCaller when there is no string to return.
            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                    .thenReturn((expectedBlank));
            assertEquals(-1.0, getDistance.invoke(distanceAgent, "http://localhost/berlin/cityobject/UUID_1/", "http://localhost/berlin/cityobject/UUID_2/"));
        }
    }

    @Test
    public void testComputeDistance() {

        DistanceAgent distanceAgent = new DistanceAgent();

        EnvelopeType.setSourceCrsName("EPSG:24500");

        // test distance calculation without CRS conversion.
        String envelopeString1 = "1#1#0#1#2#0#2#2#0#2#1#0#1#1#0";
        EnvelopeType envelope1 = new EnvelopeType(envelopeString1, "POLYGON-3-15");

        String envelopeString2 = "1#2#1#1#3#1#2#3#1#2#2#1#1#2#1";
        EnvelopeType envelope2 = new EnvelopeType(envelopeString2, "POLYGON-3-15");

        assertEquals(0.999897643510321, distanceAgent.computeDistance(envelope1, envelope2));

        // test distance calculation with CRS conversion.
        EnvelopeType.setSourceCrsName("EPSG:3414");
        String envelopeString3 = "2.85#-1.85#0#2.85#0.15#0#4.85#0.15#0#4.85#-1.85#0#2.85#-1.85#0";
        EnvelopeType envelope3 = new EnvelopeType(envelopeString3, "POLYGON-3-15");

        assertEquals(1.0033228374688898, distanceAgent.computeDistance(envelope1, envelope3));
    }

    @Test
    public void testGetSetDistanceQuery() {

        DistanceAgent distanceAgent = new DistanceAgent();

        String uri1 = "http:cityobjectxample/cityobject2";
        String uri2 = "http:cityobjectxample/cityobject3";
        String distanceUriMock = "123e4567-e89b-12d3-a456-556642440000";
        String valueUriMock = "123e4567-e89b-12d3-a456-556642441111";
        double distance = 10.0;

        try (MockedStatic<UUID> randomUUID = Mockito.mockStatic(UUID.class, RETURNS_DEEP_STUBS)) {

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
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testSetDistance() throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException {

        DistanceAgent distanceAgent = new DistanceAgent();

        ModelContext context = new ModelContext("route", "namespace");
        Field contextField = distanceAgent.getClass().getDeclaredField("context");
        contextField.setAccessible(true);
        contextField.set(distanceAgent, context);

        try (MockedStatic<AccessAgentCaller> accessAgentCallerMock = Mockito.mockStatic(AccessAgentCaller.class)) {
            assertNotNull(distanceAgent.getClass().getDeclaredMethod("setDistance", String.class, String.class, double.class));
            Method setDistance = distanceAgent.getClass().getDeclaredMethod("setDistance", String.class, String.class, double.class);
            setDistance.setAccessible(true);

            accessAgentCallerMock.when(() -> AccessAgentCaller.updateStore(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                    .thenAnswer((Answer<Void>) invocation -> null);
            try {
                setDistance.invoke(distanceAgent, "http://localhost/berlin/cityobject/UUID_1/", "http://localhost/berlin/cityobject/UUID_2", 10.0);
            } catch (Exception e) {
                fail();
            }
        }
    }
}