package uk.ac.cam.cares.twa.cities.ceaagent.test;

import junit.framework.TestCase;
import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.timeseries.TimeSeriesClient;
import uk.ac.cam.cares.twa.cities.ceaagent.CEAAgent;
import org.apache.jena.query.Query;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

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

        assertEquals(47, agent.getClass().getDeclaredFields().length);

        Field URI_ACTION;
        Field URI_UPDATE;
        Field URI_QUERY;
        Field KEY_REQ_METHOD;
        Field KEY_REQ_URL;
        Field KEY_TARGET_URL;
        Field KEY_IRI;
        Field CITY_OBJECT;
        Field CITY_OBJECT_GEN_ATT;
        Field BUILDING;
        Field ENERGY_PROFILE;
        Field SURFACE_GEOMETRY;
        Field KEY_GRID_DEMAND;
        Field KEY_ELECTRICITY_DEMAND;
        Field KEY_HEATING_DEMAND;
        Field KEY_COOLING_DEMAND;
        Field KEY_PV_ROOF_AREA;
        Field KEY_PV_WALL_NORTH_AREA;
        Field KEY_PV_WALL_SOUTH_AREA;
        Field KEY_PV_WALL_EAST_AREA;
        Field KEY_PV_WALL_WEST_AREA;
        Field KEY_PV_ROOF_SUPPLY;
        Field KEY_PV_WALL_NORTH_SUPPLY;
        Field KEY_PV_WALL_SOUTH_SUPPLY;
        Field KEY_PV_WALL_EAST_SUPPLY;
        Field KEY_PV_WALL_WEST_SUPPLY;
        Field KEY_TIMES;
        Field NUM_CEA_THREADS;
        Field CEAExecutor;
        Field TIME_SERIES_CLIENT_PROPS;
        Field tsClient;
        Field timeUnit;
        Field FS;
        Field ocgmlUri;
        Field ontoUBEMMPUri;
        Field rdfUri;
        Field owlUri;
        Field purlEnaeqUri;
        Field purlInfrastructureUri;
        Field timeSeriesUri;
        Field thinkhomeUri;
        Field unitOntologyUri;
        Field QUERY_ROUTE;
        Field UPDATE_ROUTE;
        Field requestUrl;
        Field targetUrl;

        try {
            URI_ACTION = agent.getClass().getDeclaredField("URI_ACTION");
            assertEquals(URI_ACTION.get(agent), "/cea/run");
            URI_UPDATE = agent.getClass().getDeclaredField("URI_UPDATE");
            assertEquals(URI_UPDATE.get(agent), "/cea/update");
            URI_QUERY = agent.getClass().getDeclaredField("URI_QUERY");
            assertEquals(URI_QUERY.get(agent), "/cea/query");
            KEY_REQ_METHOD = agent.getClass().getDeclaredField("KEY_REQ_METHOD");
            assertEquals(KEY_REQ_METHOD.get(agent), "method");
            KEY_REQ_URL = agent.getClass().getDeclaredField("KEY_REQ_URL");
            assertEquals(KEY_REQ_URL.get(agent), "requestUrl");
            KEY_TARGET_URL = agent.getClass().getDeclaredField("KEY_TARGET_URL");
            assertEquals(KEY_TARGET_URL.get(agent), "targetUrl");
            KEY_IRI = agent.getClass().getDeclaredField("KEY_IRI");
            assertEquals(KEY_IRI.get(agent), "iris");
            CITY_OBJECT = agent.getClass().getDeclaredField("CITY_OBJECT");
            assertEquals(CITY_OBJECT.get(agent), "cityobject");
            CITY_OBJECT_GEN_ATT = agent.getClass().getDeclaredField("CITY_OBJECT_GEN_ATT");
            assertEquals(CITY_OBJECT_GEN_ATT.get(agent), "cityobjectgenericattrib");
            BUILDING = agent.getClass().getDeclaredField("BUILDING");
            assertEquals(BUILDING.get(agent), "building");
            ENERGY_PROFILE = agent.getClass().getDeclaredField("ENERGY_PROFILE");
            assertEquals(ENERGY_PROFILE.get(agent), "energyprofile");
            SURFACE_GEOMETRY = agent.getClass().getDeclaredField("SURFACE_GEOMETRY");
            assertEquals(SURFACE_GEOMETRY.get(agent), "surfacegeometry");
            KEY_GRID_DEMAND = agent.getClass().getDeclaredField("KEY_GRID_DEMAND");
            assertEquals(KEY_GRID_DEMAND.get(agent), "grid_demand");
            KEY_ELECTRICITY_DEMAND = agent.getClass().getDeclaredField("KEY_ELECTRICITY_DEMAND");
            assertEquals(KEY_ELECTRICITY_DEMAND.get(agent), "electricity_demand");
            KEY_HEATING_DEMAND = agent.getClass().getDeclaredField("KEY_HEATING_DEMAND");
            assertEquals(KEY_HEATING_DEMAND.get(agent), "heating_demand");
            KEY_COOLING_DEMAND = agent.getClass().getDeclaredField("KEY_COOLING_DEMAND");
            assertEquals(KEY_COOLING_DEMAND.get(agent), "cooling_demand");
            KEY_PV_ROOF_AREA = agent.getClass().getDeclaredField("KEY_PV_ROOF_AREA");
            assertEquals(KEY_PV_ROOF_AREA.get(agent), "PV_area_roof");
            KEY_PV_WALL_NORTH_AREA = agent.getClass().getDeclaredField("KEY_PV_WALL_NORTH_AREA");
            assertEquals(KEY_PV_WALL_NORTH_AREA.get(agent), "PV_area_wall_north");
            KEY_PV_WALL_SOUTH_AREA = agent.getClass().getDeclaredField("KEY_PV_WALL_SOUTH_AREA");
            assertEquals(KEY_PV_WALL_SOUTH_AREA.get(agent), "PV_area_wall_south");
            KEY_PV_WALL_EAST_AREA = agent.getClass().getDeclaredField("KEY_PV_WALL_EAST_AREA");
            assertEquals(KEY_PV_WALL_EAST_AREA.get(agent), "PV_area_wall_east");
            KEY_PV_WALL_WEST_AREA = agent.getClass().getDeclaredField("KEY_PV_WALL_WEST_AREA");
            assertEquals(KEY_PV_WALL_WEST_AREA.get(agent), "PV_area_wall_west");
            KEY_PV_ROOF_SUPPLY = agent.getClass().getDeclaredField("KEY_PV_ROOF_SUPPLY");
            assertEquals(KEY_PV_ROOF_SUPPLY.get(agent), "PV_supply_roof");
            KEY_PV_WALL_NORTH_SUPPLY = agent.getClass().getDeclaredField("KEY_PV_WALL_NORTH_SUPPLY");
            assertEquals(KEY_PV_WALL_NORTH_SUPPLY.get(agent), "PV_supply_wall_north");
            KEY_PV_WALL_SOUTH_SUPPLY = agent.getClass().getDeclaredField("KEY_PV_WALL_SOUTH_SUPPLY");
            assertEquals(KEY_PV_WALL_SOUTH_SUPPLY.get(agent), "PV_supply_wall_south");
            KEY_PV_WALL_EAST_SUPPLY = agent.getClass().getDeclaredField("KEY_PV_WALL_EAST_SUPPLY");
            assertEquals(KEY_PV_WALL_EAST_SUPPLY.get(agent), "PV_supply_wall_east");
            KEY_PV_WALL_WEST_SUPPLY = agent.getClass().getDeclaredField("KEY_PV_WALL_WEST_SUPPLY");
            assertEquals(KEY_PV_WALL_WEST_SUPPLY.get(agent), "PV_supply_wall_west");
            KEY_TIMES = agent.getClass().getDeclaredField("KEY_TIMES");
            assertEquals(KEY_TIMES.get(agent), "times");
            NUM_CEA_THREADS = agent.getClass().getDeclaredField("NUM_CEA_THREADS");
            assertEquals(NUM_CEA_THREADS.get(agent), 1);
            CEAExecutor = agent.getClass().getDeclaredField("CEAExecutor");
            CEAExecutor.setAccessible(true);
            assertFalse(((ExecutorService) CEAExecutor.get(agent)).isTerminated());
            TIME_SERIES_CLIENT_PROPS = agent.getClass().getDeclaredField("TIME_SERIES_CLIENT_PROPS");
            TIME_SERIES_CLIENT_PROPS.setAccessible(true);
            assertEquals(TIME_SERIES_CLIENT_PROPS.get(agent), "timeseriesclient.properties");
            tsClient = agent.getClass().getDeclaredField("tsClient");
            tsClient.setAccessible(true);
            assertNull(tsClient.get(agent));
            timeUnit = agent.getClass().getDeclaredField("timeUnit");
            assertEquals(timeUnit.get(agent), "OffsetDateTime");
            FS = agent.getClass().getDeclaredField("FS");
            FS.setAccessible(true);
            assertEquals(FS.get(agent), System.getProperty("file.separator"));
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
            purlEnaeqUri = agent.getClass().getDeclaredField("purlEnaeqUri");
            purlEnaeqUri.setAccessible(true);
            assertEquals(purlEnaeqUri.get(agent), config.getString("uri.ontology.purl.enaeq"));
            purlInfrastructureUri = agent.getClass().getDeclaredField("purlInfrastructureUri");
            purlInfrastructureUri.setAccessible(true);
            assertEquals(purlInfrastructureUri.get(agent), config.getString("uri.ontology.purl.infrastructure"));
            timeSeriesUri = agent.getClass().getDeclaredField("timeSeriesUri");
            timeSeriesUri.setAccessible(true);
            assertEquals(timeSeriesUri.get(agent), config.getString("uri.ts"));
            thinkhomeUri = agent.getClass().getDeclaredField("thinkhomeUri");
            thinkhomeUri.setAccessible(true);
            assertEquals(thinkhomeUri.get(agent), config.getString("uri.ontology.thinkhome"));
            unitOntologyUri = agent.getClass().getDeclaredField("unitOntologyUri");
            unitOntologyUri.setAccessible(true);
            assertEquals(unitOntologyUri.get(agent), config.getString("uri.ontology.om"));
            requestUrl = agent.getClass().getDeclaredField("requestUrl");
            requestUrl.setAccessible(true);
            assertNull(requestUrl.get(agent));
            targetUrl = agent.getClass().getDeclaredField("targetUrl");
            targetUrl.setAccessible(true);
            assertNull(targetUrl.get(agent));
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
        assertEquals(29, agent.getClass().getDeclaredMethods().length);
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

        // check failure with no IRI or request url or target url
        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
                    BadRequestException.class);
        }
        requestParams.put(CEAAgent.KEY_REQ_URL, "http://localhost:8086/agents/cea/run");

        // check failure with no IRI or target url
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

        // check failure with no target URL
        try {
            validateInput.invoke(agent, requestParams);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
                    BadRequestException.class);
        }
 
        requestParams.put(CEAAgent.KEY_TARGET_URL, "http://localhost:8086/agents/cea/update");

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
        assertTrue(q1.toString().contains("ocgml:GeometryType"));
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
        assertTrue(q.toString().contains("ocgml:GeometryType"));

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
