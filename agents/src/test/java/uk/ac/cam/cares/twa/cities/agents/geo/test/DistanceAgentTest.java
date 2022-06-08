package uk.ac.cam.cares.twa.cities.agents.geo.test;

import org.apache.jena.query.Query;
import org.apache.jena.update.UpdateRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.stubbing.Answer;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.agents.geo.DistanceAgent;
import uk.ac.cam.cares.twa.cities.models.ModelContext;
import uk.ac.cam.cares.twa.cities.models.geo.CityObject;
import uk.ac.cam.cares.twa.cities.models.geo.EnvelopeType;
import uk.ac.cam.cares.twa.cities.models.geo.GeometryType;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DistanceAgentTest {

    @AfterEach
    public void tearDown() {
        DistanceAgent distanceAgent = new DistanceAgent();

        try {
            Field context = distanceAgent.getClass().getDeclaredField("context");
            context.setAccessible(true);
            context.set(distanceAgent, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testNewDistanceAgent() {
        DistanceAgent distanceAgent;

        try {
            distanceAgent = new DistanceAgent();
            assertNotNull(distanceAgent);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewDistanceAgentFields() {
        DistanceAgent distanceAgent = new DistanceAgent();

        assertEquals(32, distanceAgent.getClass().getDeclaredFields().length);

        try {
            assertEquals("/distance", distanceAgent.getClass().getDeclaredField("URI_DISTANCE").get(distanceAgent));
            assertEquals("method", distanceAgent.getClass().getDeclaredField("KEY_REQ_METHOD").get(distanceAgent));
            assertEquals("iris", distanceAgent.getClass().getDeclaredField("KEY_IRIS").get(distanceAgent));
            assertEquals("distances", distanceAgent.getClass().getDeclaredField("KEY_DISTANCES").get(distanceAgent));

            Field RDF_SCHEMA = distanceAgent.getClass().getDeclaredField("RDF_SCHEMA");
            RDF_SCHEMA.setAccessible(true);
            assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#", RDF_SCHEMA.get(distanceAgent));
            Field XML_SCHEMA = distanceAgent.getClass().getDeclaredField("XML_SCHEMA");
            XML_SCHEMA.setAccessible(true);
            assertEquals("http://www.w3.org/2001/XMLSchema#", XML_SCHEMA.get(distanceAgent));
            Field OWL_SCHEMA = distanceAgent.getClass().getDeclaredField("OWL_SCHEMA");
            OWL_SCHEMA.setAccessible(true);
            assertEquals("http://www.w3.org/2002/07/owl#", OWL_SCHEMA.get(distanceAgent));
            Field DISTANCE_GRAPH = distanceAgent.getClass().getDeclaredField("DISTANCE_GRAPH");
            DISTANCE_GRAPH.setAccessible(true);
            assertEquals("/distance/", DISTANCE_GRAPH.get(distanceAgent));
            assertEquals("EPSG:4326", distanceAgent.getClass().getDeclaredField("DEFAULT_SRS").get(distanceAgent));
            assertEquals("EPSG:24500", distanceAgent.getClass().getDeclaredField("DEFAULT_TARGET_SRS").get(distanceAgent));

            Field DISTANCE_URI = distanceAgent.getClass().getDeclaredField("DISTANCE_URI");
            DISTANCE_URI.setAccessible(true);
            assertEquals("distanceUri", DISTANCE_URI.get(distanceAgent));
            Field OM_PREFIX = distanceAgent.getClass().getDeclaredField("OM_PREFIX");
            OM_PREFIX.setAccessible(true);
            assertEquals("om", OM_PREFIX.get(distanceAgent));
            Field PHENOMENON_PREDICATE = distanceAgent.getClass().getDeclaredField("PHENOMENON_PREDICATE");
            PHENOMENON_PREDICATE.setAccessible(true);
            assertEquals("hasPhenomenon", PHENOMENON_PREDICATE.get(distanceAgent));
            Field DISTANCE_OBJECT = distanceAgent.getClass().getDeclaredField("DISTANCE_OBJECT");
            DISTANCE_OBJECT.setAccessible(true);
            assertEquals("distance", DISTANCE_OBJECT.get(distanceAgent));
            Field DISTANCE_VALUE_URI = distanceAgent.getClass().getDeclaredField("DISTANCE_VALUE_URI");
            DISTANCE_VALUE_URI.setAccessible(true);
            assertEquals("valueUri", DISTANCE_VALUE_URI.get(distanceAgent));
            Field OCGML_PREFIX = distanceAgent.getClass().getDeclaredField("OCGML_PREFIX");
            OCGML_PREFIX.setAccessible(true);
            assertEquals("ocgml", OCGML_PREFIX.get(distanceAgent));
            Field SRS_PREDICATE = distanceAgent.getClass().getDeclaredField("SRS_PREDICATE");
            SRS_PREDICATE.setAccessible(true);
            assertEquals("srsname", SRS_PREDICATE.get(distanceAgent));
            Field METRIC_SRS_PREDICATE = distanceAgent.getClass().getDeclaredField("METRIC_SRS_PREDICATE");
            METRIC_SRS_PREDICATE.setAccessible(true);
            assertEquals("metricSrsName", METRIC_SRS_PREDICATE.get(distanceAgent));
            Field SRS_NAME_OBJECT = distanceAgent.getClass().getDeclaredField("SRS_NAME_OBJECT");
            SRS_NAME_OBJECT.setAccessible(true);
            assertEquals("srsName", SRS_NAME_OBJECT.get(distanceAgent));
            Field GRAPH_NAME = distanceAgent.getClass().getDeclaredField("GRAPH_NAME");
            GRAPH_NAME.setAccessible(true);
            assertEquals("graph", GRAPH_NAME.get(distanceAgent));
            Field RDF_PREFIX = distanceAgent.getClass().getDeclaredField("RDF_PREFIX");
            RDF_PREFIX.setAccessible(true);
            assertEquals("rdf", RDF_PREFIX.get(distanceAgent));
            Field RDF_PREDICATE = distanceAgent.getClass().getDeclaredField("RDF_PREDICATE");
            RDF_PREDICATE.setAccessible(true);
            assertEquals("type", RDF_PREDICATE.get(distanceAgent));
            Field OWL_PREFIX = distanceAgent.getClass().getDeclaredField("OWL_PREFIX");
            OWL_PREFIX.setAccessible(true);
            assertEquals("owl", OWL_PREFIX.get(distanceAgent));
            Field OWL_PREDICATE = distanceAgent.getClass().getDeclaredField("OWL_PREDICATE");
            OWL_PREDICATE.setAccessible(true);
            assertEquals("NamedIndividual", OWL_PREDICATE.get(distanceAgent));
            Field NUMERIC_PREDICATE = distanceAgent.getClass().getDeclaredField("NUMERIC_PREDICATE");
            NUMERIC_PREDICATE.setAccessible(true);
            assertEquals("hasNumericValue", NUMERIC_PREDICATE.get(distanceAgent));
            Field VALUE_PREDICATE = distanceAgent.getClass().getDeclaredField("VALUE_PREDICATE");
            VALUE_PREDICATE.setAccessible(true);
            assertEquals("hasValue", VALUE_PREDICATE.get(distanceAgent));
            Field QST_MARK = distanceAgent.getClass().getDeclaredField("QST_MARK");
            QST_MARK.setAccessible(true);
            assertEquals("?", QST_MARK.get(distanceAgent));
            Field COLON = distanceAgent.getClass().getDeclaredField("COLON");
            COLON.setAccessible(true);
            assertEquals(":", COLON.get(distanceAgent));

            // test readConfig
            Field ocgmlUri = distanceAgent.getClass().getDeclaredField("ocgmlUri");
            ocgmlUri.setAccessible(true);
            assertEquals(ResourceBundle.getBundle("config").getString("uri.ontology.ontocitygml"), ocgmlUri.get(distanceAgent));
            Field unitOntology = distanceAgent.getClass().getDeclaredField("unitOntology");
            unitOntology.setAccessible(true);
            assertEquals(ResourceBundle.getBundle("config").getString("uri.ontology.om"), unitOntology.get(distanceAgent));
            Field route = distanceAgent.getClass().getDeclaredField("route");
            route.setAccessible(true);
            assertEquals(ResourceBundle.getBundle("config").getString("uri.route"), route.get(distanceAgent));

            Field context = distanceAgent.getClass().getDeclaredField("context");
            context.setAccessible(true);
            assertNull(context.get(distanceAgent));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testNewDistanceAgentMethods() {
        DistanceAgent distanceAgent = new DistanceAgent();
        assertEquals(13, distanceAgent.getClass().getDeclaredMethods().length);
    }

    @Test
    public void testProcessRequestParameters() {
        DistanceAgent distanceAgent = new DistanceAgent();
        JSONObject requestParams = new JSONObject();

        // test case when request parameters are empty, validateInput should throw BadRequestException
        try {
            distanceAgent.processRequestParameters(requestParams);
        } catch (Exception e) {
            assertTrue(e instanceof BadRequestException);
        }

        // test case when iris are empty, context field should be null and should return empty distances array
        requestParams.put(DistanceAgent.KEY_REQ_METHOD, HttpMethod.POST);
        JSONArray iris = new JSONArray();
        requestParams.put(DistanceAgent.KEY_IRIS, iris);
        try {
            ArrayList<Double> distances = (ArrayList<Double>) distanceAgent.processRequestParameters(requestParams).getJSONArray("distances").get(0);
            assertEquals(0, distances.size());
            Field context = distanceAgent.getClass().getDeclaredField("context");
            context.setAccessible(true);
            assertNull(context.get(distanceAgent));
        } catch (Exception e) {
            fail();
        } finally {
            requestParams.remove("distances");
        }

        // test case when iris contains two values, request params should have a distance array with size 1
        iris.put("http://localhost/berlin/cityobject/UUID_1/");
        iris.put("http://localhost/berlin/cityobject/UUID_2/");
        requestParams.put(DistanceAgent.KEY_IRIS, iris);
        JSONArray result = new JSONArray().put(new JSONObject().put("distance", "10.0"));
        try (MockedStatic<AccessAgentCaller> accessAgentCallerMock = Mockito.mockStatic(AccessAgentCaller.class)) {
            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                    .thenReturn(result);
            ArrayList<Double> distances = (ArrayList<Double>) distanceAgent.processRequestParameters(requestParams).getJSONArray("distances").get(0);
            assertEquals(1, distances.size());
        } catch (Exception e) {
            fail();
        } finally {
            requestParams.remove("distances");
        }

        // test case when iris contains 3 values, request params should have a distance array with size 3
        iris.put("http://localhost/berlin/cityobject/UUID_3/");
        requestParams.put(DistanceAgent.KEY_IRIS, iris);
        try (MockedStatic<AccessAgentCaller> accessAgentCallerMock = Mockito.mockStatic(AccessAgentCaller.class)) {
            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                    .thenReturn(result);
            ArrayList<Double> distances = (ArrayList<Double>) distanceAgent.processRequestParameters(requestParams).getJSONArray("distances").get(0);
            assertEquals(3, distances.size());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testValidateInput() {
        DistanceAgent distanceAgent = new DistanceAgent();
        JSONObject requestParams = new JSONObject();

        // test case when request parameters are empty, should throw BadRequestException
        try {
            distanceAgent.validateInput(requestParams);
        } catch (Exception e) {
            assertTrue(e instanceof BadRequestException);
        }

        // test case when request parameters has method but no iris, should throw BadRequestException
        requestParams.put(DistanceAgent.KEY_REQ_METHOD, HttpMethod.GET);
        try {
            distanceAgent.validateInput(requestParams);
        } catch (Exception e) {
            assertTrue(e instanceof BadRequestException);
        }

        // test case when method is not POST, should throw BadRequestException
        requestParams.put(DistanceAgent.KEY_IRIS, new JSONArray());
        try {
            distanceAgent.validateInput(requestParams);
        } catch (Exception e) {
            assertTrue(e instanceof BadRequestException);
        }

        // test case with malformed iri, should throw BadRequestException
        requestParams.put(DistanceAgent.KEY_REQ_METHOD, HttpMethod.POST);
        requestParams.put(DistanceAgent.KEY_IRIS, new JSONArray().put("test"));
        try {
            distanceAgent.validateInput(requestParams);
        } catch (Exception e) {
            assertTrue(e instanceof BadRequestException);
        }

        // test case when validateInput returns true
        requestParams.put(DistanceAgent.KEY_IRIS, new JSONArray().put("http://localhost/berlin/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3/"));
        try {
            assertTrue(distanceAgent.validateInput(requestParams));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testReadConfig() {
        // this test is deliberately left blank
        // method is already tested in testNewDistanceAgentFields
    }

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
    public void testGetDistanceGraphUri() {
        DistanceAgent distanceAgent = new DistanceAgent();
        String uri = "http://localhost:9999/blazegraph/namespace/berlin/sparql/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3/";
        try {
            Method getDistanceGraphUri = distanceAgent.getClass().getDeclaredMethod("getDistanceGraphUri", String.class);
            getDistanceGraphUri.setAccessible(true);
            assertEquals("http://localhost:9999/blazegraph/namespace/berlin/sparql/distance/", getDistanceGraphUri.invoke(distanceAgent, uri));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            fail();
        }
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
    public void testGetEnvelope() {
        DistanceAgent distanceAgent = new DistanceAgent();
        String uriString = "http://localhost/berlin/cityobject/UUID_62130277-0dca-4c61-939d-c3c390d1efb3/";
        String coordinateSystem = "EPSG:25833";

        Field contextField;
        ModelContext context = Mockito.spy(new ModelContext("route", "namespace"));
        CityObject cityObject = new CityObject();
        cityObject.setEnvelopeType(new EnvelopeType("1#1#0#1#2#0#2#2#0#2#1#0#1#1#0", "POLYGON-3-15"));
        try {
            contextField = distanceAgent.getClass().getDeclaredField("context");
            contextField.setAccessible(true);
            contextField.set(distanceAgent, context);

            Mockito.doReturn(cityObject).when(context).loadAll(ArgumentMatchers.any(), ArgumentMatchers.anyString());
            assertEquals(cityObject.getEnvelopeType(), distanceAgent.getEnvelope(uriString, coordinateSystem));
            assertEquals("EPSG:25833", GeometryType.getSourceCrsName());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
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