package uk.ac.cam.cares.twa.cities.agents.geo.test;

import junit.framework.TestCase;
import org.json.JSONObject;
import uk.ac.cam.cares.twa.cities.agents.geo.CEAAgent;
import org.apache.jena.query.Query;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

public class CEAAgentTest extends TestCase {
    public void testCEAAgent() {
       CEAAgent agent;

        try {
            agent = new CEAAgent();
            assertNotNull(agent);
        } catch (Exception e) {
            fail();
        }

    }

    public void testCEAAgentFields() {
        CEAAgent agent = new CEAAgent();
        ResourceBundle config = ResourceBundle.getBundle("CEAAgentConfig");

        assertEquals(17, agent.getClass().getDeclaredFields().length);

        Field URI_ACTION;
        Field KEY_REQ_METHOD;
        Field KEY_IRI;
        Field CITY_OBJECT;
        Field CITY_OBJECT_GEN_ATT;
        Field BUILDING;
        Field ENERGY_PROFILE;
        Field NUM_CEA_THREADS;
        Field CEAExecutor;
        Field kgClient;
        Field ocgmlUri;
        Field ontoUBEMMPUri;
        Field rdfUri;
        Field owlUri;
        Field unitOntology;
        Field QUERY_ROUTE;
        Field UPDATE_ROUTE;

        try {
            URI_ACTION = agent.getClass().getDeclaredField("URI_ACTION");
            assertEquals(URI_ACTION.get(agent), "/cea");
            KEY_REQ_METHOD = agent.getClass().getDeclaredField("KEY_REQ_METHOD");
            assertEquals(KEY_REQ_METHOD.get(agent), "method");
            KEY_IRI = agent.getClass().getDeclaredField("KEY_IRI");
            assertEquals(KEY_IRI.get(agent), "iri");
            CITY_OBJECT = agent.getClass().getDeclaredField("CITY_OBJECT");
            assertEquals(CITY_OBJECT.get(agent), "cityobject");
            CITY_OBJECT_GEN_ATT = agent.getClass().getDeclaredField("CITY_OBJECT_GEN_ATT");
            assertEquals(CITY_OBJECT_GEN_ATT.get(agent), "cityobjectgenericattrib");
            BUILDING = agent.getClass().getDeclaredField("BUILDING");
            assertEquals(BUILDING.get(agent), "building");
            ENERGY_PROFILE = agent.getClass().getDeclaredField("ENERGY_PROFILE");
            assertEquals(ENERGY_PROFILE.get(agent), "energyprofile");
            NUM_CEA_THREADS = agent.getClass().getDeclaredField("NUM_CEA_THREADS");
            assertEquals(NUM_CEA_THREADS.get(agent), 1);
            CEAExecutor = agent.getClass().getDeclaredField("CEAExecutor");
            CEAExecutor.setAccessible(true);
            assertFalse(((ExecutorService) CEAExecutor.get(agent)).isTerminated());
            kgClient = agent.getClass().getDeclaredField("kgClient");
            kgClient.setAccessible(true);
            assertNull(kgClient.get(agent));
            ocgmlUri = agent.getClass().getDeclaredField("ocgmlUri");
            ocgmlUri.setAccessible(true);
            assertEquals(ocgmlUri.get(agent), config.getString("uri.ontology.ontocitygml"));
            ontoUBEMMPUri = agent.getClass().getDeclaredField("ontoUBEMMPUri");
            ontoUBEMMPUri.setAccessible(true);
            assertEquals(ontoUBEMMPUri.get(agent), config.getString("uri.ontology.ontoubemmp"));
            rdfUri = agent.getClass().getDeclaredField("rdfUri");
            rdfUri.setAccessible(true);
            assertEquals(rdfUri.get(agent), config.getString("uri.ontology.rdf"));
            owlUri = agent.getClass().getDeclaredField("owlUri");
            owlUri.setAccessible(true);
            assertEquals(owlUri.get(agent), config.getString("uri.ontology.owl"));
            unitOntology = agent.getClass().getDeclaredField("unitOntology");
            unitOntology.setAccessible(true);
            assertEquals(unitOntology.get(agent), config.getString("uri.ontology.om"));
            QUERY_ROUTE = agent.getClass().getDeclaredField("QUERY_ROUTE");
            QUERY_ROUTE.setAccessible(true);
            assertEquals(QUERY_ROUTE.get(agent), config.getString("uri.route.local"));
            UPDATE_ROUTE = agent.getClass().getDeclaredField("UPDATE_ROUTE");
            UPDATE_ROUTE.setAccessible(true);
            assertEquals(UPDATE_ROUTE.get(agent), config.getString("uri.route.local"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    public void testCEAAgentMethods() {
        CEAAgent agent = new CEAAgent();
        assertEquals(13, agent.getClass().getDeclaredMethods().length);
    }

    public void testValidateInput() {
        CEAAgent agent = new CEAAgent();
        Method validateInput = null;

        try {
            validateInput = agent.getClass().getDeclaredMethod("validateInput", JSONObject.class);
        } catch (Exception e) {
            fail();
        }

        JSONObject requestParams = new JSONObject();

        // check failure with empty request params
        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
                    BadRequestException.class);
        }

        requestParams.put(CEAAgent.KEY_REQ_METHOD, HttpMethod.GET);

        // check failure with no IRI
        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
                    BadRequestException.class);
        }

        requestParams.put(CEAAgent.KEY_IRI, "test");

        // check failure with GET http method
        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
                    BadRequestException.class);
        }

        requestParams.put(CEAAgent.KEY_REQ_METHOD, HttpMethod.POST);

        // should pass now
        try {
            assertTrue((Boolean) validateInput.invoke(agent, requestParams));
        } catch (Exception e) {
            fail();
        }

    }

    public void testGetNamespace()
         throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        CEAAgent agent = new CEAAgent();
        String uri = "http://localhost/berlin/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";

        Method getNamespace = agent.getClass().getDeclaredMethod("getNamespace", String.class);
        assertNotNull(getNamespace);
        getNamespace.setAccessible(true);

        String result = (String) getNamespace.invoke(agent, uri);
        assertEquals("http://localhost/berlin", result);

    }

    public void testGetGraph()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        CEAAgent agent = new CEAAgent();
        String uri = "http://localhost/berlin/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";
        String graph = "building";

        Method getGraph = agent.getClass().getDeclaredMethod("getGraph", String.class, String.class);
        assertNotNull(getGraph);
        getGraph.setAccessible(true);

        String result = (String) getGraph.invoke(agent, uri, graph);
        assertEquals("http://localhost/berlin/building/", result);

    }

    public void testGetUUID()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        CEAAgent agent = new CEAAgent();
        String uri = "http://localhost/berlin/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";

        Method getUUID = agent.getClass().getDeclaredMethod("getUUID", String.class);
        assertNotNull(getUUID);
        getUUID.setAccessible(true);

        String result = (String) getUUID.invoke(agent, uri);
        assertEquals("UUID_583747b0-1655-4761-8050-4036436a1052", result);

    }

    public void testGetQuery()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        CEAAgent agent = new CEAAgent();
        String case1 = "Envelope";
        String case2 = "Height";
        String uri1 = "http://localhost/berlin/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";
        String uri2 = "http://localhost/kings-lynn-open-data/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";

        Method getQuery = agent.getClass().getDeclaredMethod("getQuery", String.class, String.class);
        assertNotNull(getQuery);
        getQuery.setAccessible(true);

        Query q1 = (Query) getQuery.invoke(agent, uri1, case1);
        assertTrue(q1.toString().contains("ocgml:EnvelopeType"));
        Query q2 = (Query) getQuery.invoke(agent, uri1, case2);
        assertTrue(q2.toString().contains("ocgml:attrName"));
        assertTrue(q2.toString().contains("ocgml:realVal"));
        assertTrue(q2.toString().contains("height"));
        Query q3 = (Query) getQuery.invoke(agent, uri2, case2);
        assertTrue(q3.toString().contains("ocgml:measuredHeight"));

    }

    public void testGetGeometryQuery()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        String uri = "http://localhost/berlin/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";

        Method getGeometryQuery = agent.getClass().getDeclaredMethod("getGeometryQuery", String.class);
        assertNotNull(getGeometryQuery);
        getGeometryQuery.setAccessible(true);

        Query q = (Query) getGeometryQuery.invoke(agent, uri);
        assertTrue(q.toString().contains("ocgml:EnvelopeType"));

    }

    public void testGetHeightQuery()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        String uri1 = "http://localhost/berlin/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";
        String uri2 = "http://localhost/kings-lynn-open-data/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";

        Method getHeightQuery = agent.getClass().getDeclaredMethod("getHeightQuery", String.class);
        assertNotNull(getHeightQuery);
        getHeightQuery.setAccessible(true);

        Query q1 = (Query) getHeightQuery.invoke(agent, uri1);
        assertTrue(q1.toString().contains("ocgml:attrName"));
        assertTrue(q1.toString().contains("ocgml:realVal"));
        assertTrue(q1.toString().contains("height"));
        Query q2 = (Query) getHeightQuery.invoke(agent, uri2);
        assertTrue(q2.toString().contains("ocgml:measuredHeight"));

    }
}
