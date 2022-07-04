package uk.ac.cam.cares.twa.cities.ceaagent;

import org.jooq.exception.DataAccessException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.jps.base.timeseries.TimeSeries;
import uk.ac.cam.cares.jps.base.timeseries.TimeSeriesClient;
import org.apache.jena.query.Query;
import uk.ac.cam.cares.twa.cities.tasks.CEAInputData;
import uk.ac.cam.cares.twa.cities.tasks.RunCEATask;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class CEAAgentTest {
    @Test
    public void testCEAAgent() {
       CEAAgent agent;

        try {
            agent = new CEAAgent();
            assertNotNull(agent);
        } catch (Exception e) {
            fail();
        }

    }

    @Test
    public void testCEAAgentFields() {
        CEAAgent agent = new CEAAgent();
        ResourceBundle config = ResourceBundle.getBundle("CEAAgentConfig");

        assertEquals(49, agent.getClass().getDeclaredFields().length);

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
        Field DATABASE_SRS;
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
        Field accessAgentRoutes;
        Field requestUrl;
        Field targetUrl;
        Field localRoute;

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
            DATABASE_SRS = agent.getClass().getDeclaredField("DATABASE_SRS");
            assertEquals(DATABASE_SRS.get(agent), "databasesrs");
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

            // Test readConfig()
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
            accessAgentRoutes = agent.getClass().getDeclaredField("accessAgentRoutes");
            accessAgentRoutes.setAccessible(true);
            Map<String,String> accessAgentMap = (HashMap<String,String>) accessAgentRoutes.get(agent);
            assertEquals(accessAgentMap.get("http://www.theworldavatar.com:83/citieskg/namespace/berlin/sparql/"), config.getString("berlin.targetresourceid"));
            assertEquals(accessAgentMap.get("http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG24500/sparql/"), config.getString("singaporeEPSG24500.targetresourceid"));
            assertEquals(accessAgentMap.get("http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/"), config.getString("singaporeEPSG4326.targetresourceid"));
            assertEquals(accessAgentMap.get("http://www.theworldavatar.com:83/citieskg/namespace/kingslynnEPSG3857/sparql/"), config.getString("kingslynnEPSG3857.targetresourceid"));
            assertEquals(accessAgentMap.get("http://www.theworldavatar.com:83/citieskg/namespace/kingslynnEPSG27700/sparql/"), config.getString("kingslynnEPSG27700.targetresourceid"));
            localRoute = agent.getClass().getDeclaredField("localRoute");
            localRoute.setAccessible(true);
            assertEquals(localRoute.get(agent), config.getString("uri.route.local"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testCEAAgentMethods() {
        CEAAgent agent = new CEAAgent();
        assertEquals(31, agent.getClass().getDeclaredMethods().length);
    }

    @Test
    public void testProcessRequestParameters()
            throws NoSuchMethodException, InvocationTargetException , IllegalAccessException, NoSuchFieldException {

        CEAAgent agent = spy(new CEAAgent());
        Method processRequestParameters = agent.getClass().getDeclaredMethod("processRequestParameters", JSONObject.class);
        JSONObject requestParams = new JSONObject();

        // set route
        Field localRoute = agent.getClass().getDeclaredField("localRoute");
        localRoute.setAccessible(true);
        localRoute.set(agent, "test_route");

        // Test empty request params
        try {
            processRequestParameters.invoke(agent, requestParams);
        } catch(Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
                    BadRequestException.class);
        }

        // test data
        String measure_grid = "grid_demand";
        String measure_elec = "electricity_demand";
        String measure_heating = "heating_demand";
        String measure_cooling = "cooling_demand";
        String measure_PV_roof = "PV_supply_roof";
        String measure_PV_south = "PV_supply_wall_south";
        String measure_PV_north = "PV_supply_wall_north";
        String measure_PV_east = "PV_supply_wall_east";
        String measure_PV_west = "PV_supply_wall_west";
        String measure_building = "energyProfileBuilding";
        String test_measure = "test_uri1";
        String building = "test_building_uri";
        String measure_height = "Height";
        String test_height = "5.0";
        String measure_footprint = "Footprint";
        String test_footprint1 = "559267.200000246#313892.7999989044#0.0#559280.5400002463#313892.7999989044#0.0#559280.5400002463#313908.7499989033#0.0#559267.200000246#313908.7499989033#0.0#559267.200000246#313892.7999989044#0.0";
        String measure_crs = "CRS";
        String test_crs = "test_crs";

        JSONArray expected_ts = new JSONArray().put(new JSONObject().put(measure_grid, test_measure).put(measure_elec, test_measure).put(measure_heating, test_measure).put(measure_cooling, test_measure)
                .put(measure_PV_roof, test_measure).put(measure_PV_south, test_measure).put(measure_PV_north, test_measure).put(measure_PV_east, test_measure).put(measure_PV_west, test_measure));
        JSONArray expected_building = new JSONArray().put(new JSONObject().put(measure_building, building));
        JSONArray expected_height = new JSONArray().put(new JSONObject().put(measure_height, test_height));
        JSONArray expected_footprint = new JSONArray().put(new JSONObject().put(measure_footprint, test_footprint1));
        JSONArray expected_crs = new JSONArray().put(new JSONObject().put(measure_crs, test_crs));

        // Test the update endpoint
        requestParams.put(CEAAgent.KEY_REQ_URL, "http://localhost:8086/agents/cea/update");
        requestParams.put(CEAAgent.KEY_IRI, "['http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/cityobject/UUID_test/']");
        requestParams.put(CEAAgent.KEY_TARGET_URL, "http://localhost:8086/agents/cea/update");
        requestParams.put(CEAAgent.KEY_REQ_METHOD, HttpMethod.POST);

        JSONArray arrayMock = mock(JSONArray.class);
        when(arrayMock.length()).thenReturn(1);
        when(arrayMock.getString(anyInt())).thenReturn(OffsetDateTime.now().toString()).thenReturn("4.2");
        when(arrayMock.get(anyInt())).thenReturn(arrayMock);

        requestParams.put(CEAAgent.KEY_GRID_DEMAND, arrayMock);
        requestParams.put(CEAAgent.KEY_ELECTRICITY_DEMAND, arrayMock);
        requestParams.put(CEAAgent.KEY_HEATING_DEMAND, arrayMock);
        requestParams.put(CEAAgent.KEY_COOLING_DEMAND, arrayMock);
        requestParams.put(CEAAgent.KEY_PV_ROOF_AREA, arrayMock);
        requestParams.put(CEAAgent.KEY_PV_ROOF_SUPPLY, arrayMock);
        requestParams.put(CEAAgent.KEY_PV_WALL_SOUTH_AREA, arrayMock);
        requestParams.put(CEAAgent.KEY_PV_WALL_SOUTH_SUPPLY,arrayMock);
        requestParams.put(CEAAgent.KEY_PV_WALL_NORTH_AREA, arrayMock);
        requestParams.put(CEAAgent.KEY_PV_WALL_NORTH_SUPPLY, arrayMock);
        requestParams.put(CEAAgent.KEY_PV_WALL_EAST_AREA, arrayMock);
        requestParams.put(CEAAgent.KEY_PV_WALL_EAST_SUPPLY, arrayMock);
        requestParams.put(CEAAgent.KEY_PV_WALL_WEST_AREA, arrayMock);
        requestParams.put(CEAAgent.KEY_PV_WALL_WEST_SUPPLY, arrayMock);
        requestParams.put(CEAAgent.KEY_TIMES, arrayMock);

        doNothing().when(agent).updateStore(anyString(), anyString());

        JSONObject returnParams;

        try (MockedStatic<AccessAgentCaller> accessAgentCallerMock = mockStatic(AccessAgentCaller.class)) {

            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(anyString(), anyString()))
                    .thenReturn(expected_building).thenReturn(expected_ts);

            try (MockedConstruction<TimeSeriesClient> mockTs = mockConstruction(TimeSeriesClient.class)) {

                returnParams = (JSONObject) processRequestParameters.invoke(agent, requestParams);
                verify(mockTs.constructed().get(0), times(1)).addTimeSeriesData(any());
                assertEquals(requestParams, returnParams);

            }

            //Test the run endpoint
            requestParams.remove(CEAAgent.KEY_REQ_URL);
            requestParams.put(CEAAgent.KEY_REQ_URL, "http://localhost:8086/agents/cea/run");

            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(anyString(), anyString()))
                    .thenReturn(expected_height).thenReturn(expected_footprint).thenReturn(expected_crs);

            try (MockedConstruction<RunCEATask> mockTask = mockConstruction(RunCEATask.class)) {
                ThreadPoolExecutor executor = mock(ThreadPoolExecutor.class);
                Field CEAExecutor = agent.getClass().getDeclaredField("CEAExecutor");
                CEAExecutor.setAccessible(true);
                CEAExecutor.set(agent, executor);

                returnParams = (JSONObject) processRequestParameters.invoke(agent, requestParams);
                verify(executor, times(1)).execute(mockTask.constructed().get(0));
                assertEquals(requestParams, returnParams);
            }
        }

        //Test the query endpoint
        requestParams.remove(CEAAgent.KEY_REQ_URL);
        requestParams.put(CEAAgent.KEY_REQ_URL, "http://localhost:8086/agents/cea/query");

        //Test time series data
        String testUnit = "testUnit";
        String testScalar = "testScalar";
        ArrayList<String> testList = mock(ArrayList.class);
        when(testList.get(0)).thenReturn(testScalar);
        when(testList.get(1)).thenReturn(testUnit);
        String testReturnValue = "testAnnual";
        TimeSeries<OffsetDateTime> timeSeries = mock(TimeSeries.class);

        doReturn(testList).when(agent).getDataIRI(anyString(), anyString(), anyString());
        doReturn(testReturnValue).when(agent).calculateAnnual(any(), anyString());
        doReturn(timeSeries).when(agent).retrieveData(anyString());
        doReturn(testUnit).when(agent).getUnit(anyString());

        Field TIME_SERIES = agent.getClass().getDeclaredField("TIME_SERIES");
        List<String> time_series_strings = (List<String>) TIME_SERIES.get(agent);
        Field SCALARS = agent.getClass().getDeclaredField("SCALARS");
        List<String> scalar_strings = (List<String>) SCALARS.get(agent);

        returnParams = (JSONObject) processRequestParameters.invoke(agent, requestParams);
        String result = returnParams.get(CEAAgent.ENERGY_PROFILE).toString();
        for (String scalar : scalar_strings) {
            String expected = "\"" + scalar + "\"" + ":\"testScalar testUnit\"";
            assertTrue(result.contains(expected));
        }
        for (String ts : time_series_strings) {
            String expected = "\"Annual " + ts + "\"" + ":\"testAnnual testUnit\"";
            assertTrue(result.contains(expected));
        }
    }

    @Test
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

    @Test
    public void testValidateUpdateInput()  throws NoSuchMethodException, InvocationTargetException, IllegalAccessException{
        CEAAgent agent = new CEAAgent();
        Method validateUpdateInput  = agent.getClass().getDeclaredMethod("validateUpdateInput", JSONObject.class);
        assertNotNull(validateUpdateInput);
        validateUpdateInput.setAccessible(true);

        JSONObject requestParams = new JSONObject();
        requestParams.put(CEAAgent.KEY_IRI, "");
        requestParams.put(CEAAgent.KEY_TARGET_URL, "");
        requestParams.put(CEAAgent.KEY_GRID_DEMAND, "");
        requestParams.put(CEAAgent.KEY_ELECTRICITY_DEMAND, "");
        requestParams.put(CEAAgent.KEY_HEATING_DEMAND, "");
        requestParams.put(CEAAgent.KEY_COOLING_DEMAND, "");
        requestParams.put(CEAAgent.KEY_PV_ROOF_AREA, "");
        requestParams.put(CEAAgent.KEY_PV_ROOF_SUPPLY, "");
        requestParams.put(CEAAgent.KEY_PV_WALL_SOUTH_AREA, "");
        requestParams.put(CEAAgent.KEY_PV_WALL_SOUTH_SUPPLY, "");
        requestParams.put(CEAAgent.KEY_PV_WALL_NORTH_AREA, "");
        requestParams.put(CEAAgent.KEY_PV_WALL_NORTH_SUPPLY, "");
        requestParams.put(CEAAgent.KEY_PV_WALL_EAST_AREA, "");
        requestParams.put(CEAAgent.KEY_PV_WALL_EAST_SUPPLY, "");
        requestParams.put(CEAAgent.KEY_PV_WALL_WEST_AREA, "");
        requestParams.put(CEAAgent.KEY_PV_WALL_WEST_SUPPLY, "");
        requestParams.put(CEAAgent.KEY_TIMES, "");

        // check failure with empty request params
        assertTrue((Boolean) validateUpdateInput.invoke(agent, requestParams));

        requestParams.put(CEAAgent.KEY_IRI, "test");

        // check failure with only IRI
        assertTrue((Boolean) validateUpdateInput.invoke(agent, requestParams));

        requestParams.put(CEAAgent.KEY_TARGET_URL, "http://localhost:8086/agents/cea/update");

        // check failure with only IRI and target url
        assertTrue((Boolean) validateUpdateInput.invoke(agent, requestParams));

        requestParams.put(CEAAgent.KEY_GRID_DEMAND, "test");
        requestParams.put(CEAAgent.KEY_ELECTRICITY_DEMAND, "test");
        requestParams.put(CEAAgent.KEY_HEATING_DEMAND, "test");
        requestParams.put(CEAAgent.KEY_COOLING_DEMAND, "test");
        requestParams.put(CEAAgent.KEY_PV_ROOF_AREA, "test");
        requestParams.put(CEAAgent.KEY_PV_ROOF_SUPPLY, "test");
        requestParams.put(CEAAgent.KEY_PV_WALL_SOUTH_AREA, "test");
        requestParams.put(CEAAgent.KEY_PV_WALL_SOUTH_SUPPLY, "test");
        requestParams.put(CEAAgent.KEY_PV_WALL_NORTH_AREA, "test");
        requestParams.put(CEAAgent.KEY_PV_WALL_NORTH_SUPPLY, "test");
        requestParams.put(CEAAgent.KEY_PV_WALL_EAST_AREA, "test");
        requestParams.put(CEAAgent.KEY_PV_WALL_EAST_SUPPLY, "test");
        requestParams.put(CEAAgent.KEY_PV_WALL_WEST_AREA, "test");
        requestParams.put(CEAAgent.KEY_PV_WALL_WEST_SUPPLY, "test");
        requestParams.put(CEAAgent.KEY_TIMES, "test");

        // should pass now
        assertFalse((Boolean) validateUpdateInput.invoke(agent, requestParams));
    }

    @Test
    public void testValidateActionInput() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        Method validateActionInput = agent.getClass().getDeclaredMethod("validateActionInput", JSONObject.class);
        assertNotNull(validateActionInput);
        validateActionInput.setAccessible(true);

        JSONObject requestParams = new JSONObject();
        requestParams.put(CEAAgent.KEY_IRI, "");
        requestParams.put(CEAAgent.KEY_TARGET_URL, "");

        // check failure with empty request params
        assertTrue((Boolean) validateActionInput.invoke(agent, requestParams));

        requestParams.put(CEAAgent.KEY_IRI, "test");

        // check failure with only IRI
        assertTrue((Boolean) validateActionInput.invoke(agent, requestParams));

        requestParams.put(CEAAgent.KEY_TARGET_URL, "http://localhost:8086/agents/cea/update");

        // should pass now
        assertFalse((Boolean) validateActionInput.invoke(agent, requestParams));

    }

    @Test
    public void testValidateQueryInput() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        Method validateQueryInput = agent.getClass().getDeclaredMethod("validateQueryInput", JSONObject.class);
        assertNotNull(validateQueryInput);
        validateQueryInput.setAccessible(true);

        JSONObject requestParams = new JSONObject();
        requestParams.put(CEAAgent.KEY_IRI, "");

        // check failure with empty request params
        assertTrue((Boolean) validateQueryInput.invoke(agent, requestParams));

        requestParams.put(CEAAgent.KEY_IRI, "test");

        // should pass now
        assertFalse((Boolean) validateQueryInput.invoke(agent, requestParams));
    }

    @Test
    public void testGetList() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException{
        CEAAgent agent = new CEAAgent();
        Method getList = agent.getClass().getDeclaredMethod("getList", JSONObject.class, String.class);
        assertNotNull(getList);
        getList.setAccessible(true);

        JSONObject requestParams = new JSONObject();
        List<String> test_list_1 = new ArrayList<>();
        test_list_1.add("test_value_1");
        test_list_1.add("test_value_2");
        test_list_1.add("test_value_3");
        List<String> test_list_2 = new ArrayList<>();
        test_list_2.add("test_value_4");
        test_list_2.add("test_value_5");

        requestParams.put("test_key_1", test_list_1);
        requestParams.put("test_key_2", test_list_2);

        // test value list retrieved correctly
        List<String> result = (List<String>) getList.invoke(agent, requestParams, "test_key_2" );
        assertEquals(test_list_2, result);

    }

    @Test
    public void testGetTimeSeriesList() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException{
        CEAAgent agent = new CEAAgent();
        Method getTimeSeriesList = agent.getClass().getDeclaredMethod("getTimeSeriesList", JSONObject.class, String.class, Integer.class);
        assertNotNull(getTimeSeriesList);
        getTimeSeriesList.setAccessible(true);

        JSONObject requestParams = new JSONObject();
        List<String> test_list_1 = new ArrayList<>();
        test_list_1.add("1.5");
        test_list_1.add("2.5");
        test_list_1.add("3.5");
        List<String> test_list_2 = new ArrayList<>();
        test_list_2.add("4.5");
        test_list_2.add("5.5");
        List<List<String>> list_of_lists = new ArrayList<>();
        list_of_lists.add(test_list_1);
        list_of_lists.add(test_list_2);

        requestParams.put("test_key_1", list_of_lists);

        List<Double> expected_list = new ArrayList<>();
        expected_list.add(4.5);
        expected_list.add(5.5);

        // test time series retrieved correctly
        List<Double> result = (List<Double>) getTimeSeriesList.invoke(agent, requestParams, "test_key_1" , 1);
        assertEquals(expected_list, result);

    }

    @Test
    public void testGetTimesList() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException{
        CEAAgent agent = new CEAAgent();
        Method getTimesList = agent.getClass().getDeclaredMethod("getTimesList", JSONObject.class, String.class);
        assertNotNull(getTimesList);
        getTimesList.setAccessible(true);

        JSONObject requestParams = new JSONObject();
        List<OffsetDateTime> test_list_1 = new ArrayList<>();
        test_list_1.add(OffsetDateTime.now());
        test_list_1.add(OffsetDateTime.now());
        test_list_1.add(OffsetDateTime.now());
        List<OffsetDateTime> test_list_2 = new ArrayList<>();
        test_list_2.add(OffsetDateTime.now());
        test_list_2.add(OffsetDateTime.now());

        requestParams.put("test_key_1", test_list_1);
        requestParams.put("test_key_2", test_list_2);

        // test times retrieved correctly
        List<Double> result = (List<Double>) getTimesList.invoke(agent, requestParams, "test_key_1" );
        assertEquals(test_list_1, result);

    }

    @Test
    public void testRunCEA() throws Exception {
        try (MockedConstruction<RunCEATask> mockTask = mockConstruction(RunCEATask.class)) {

            ThreadPoolExecutor executor = mock(ThreadPoolExecutor.class);

            CEAAgent agent = new CEAAgent();
            Method runCEA = agent.getClass().getDeclaredMethod("runCEA", ArrayList.class, ArrayList.class, Integer.class, String.class);
            assertNotNull(runCEA);
            runCEA.setAccessible(true);

            Field CEAExecutor = agent.getClass().getDeclaredField("CEAExecutor");
            CEAExecutor.setAccessible(true);
            CEAExecutor.set(agent, executor);

            Field targetUrl = agent.getClass().getDeclaredField("targetUrl");
            targetUrl.setAccessible(true);
            targetUrl.set(agent, "test");

            ArrayList<CEAInputData> testData = new ArrayList<CEAInputData>();
            testData.add(new CEAInputData("test", "test"));
            ArrayList<String> testArray = new ArrayList<>();
            testArray.add("testUri");
            Integer test_thread = 0;
            String test_CRS = "27700";

            // Test executor called with run CEA task
            runCEA.invoke(agent, testData, testArray, test_thread, test_CRS);
            verify(executor, times(1)).execute(mockTask.constructed().get(0));
        }
    }

    @Test
    public void testCreateTimeSeries() throws Exception {
        try (MockedConstruction<TimeSeriesClient> mockTs = mockConstruction(TimeSeriesClient.class)) {

            CEAAgent agent = new CEAAgent();
            Method createTimeSeries = agent.getClass().getDeclaredMethod("createTimeSeries", String.class, List.class);
            assertNotNull(createTimeSeries);
            createTimeSeries.setAccessible(true);

            List<LinkedHashMap<String, String>> fixedIris = new ArrayList<>();
            String prefix = "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/";
            String testUri = prefix + "cityobject/UUID_test/";

            createTimeSeries.invoke(agent, testUri, fixedIris);

            Field TIME_SERIES = agent.getClass().getDeclaredField("TIME_SERIES");
            List<String> time_series_strings = (List<String>) TIME_SERIES.get(agent);

            // Ensure iris created correctly and time series initialised
            for (String time_series : time_series_strings) {
                assertTrue(fixedIris.get(0).get(time_series).contains(prefix + "energyprofile/" + time_series));
            }
            verify(mockTs.constructed().get(0), times(1)).initTimeSeries(anyList(), anyList(), anyString());
        }
    }

    @Test
    public void testAddDataToTimeSeries() throws Exception {
        try(MockedConstruction<TimeSeriesClient> mockTs = mockConstruction(TimeSeriesClient.class)) {

            CEAAgent agent = new CEAAgent();
            Method addDataToTimeSeries = agent.getClass().getDeclaredMethod("addDataToTimeSeries", List.class, List.class, LinkedHashMap.class);
            assertNotNull(addDataToTimeSeries);
            addDataToTimeSeries.setAccessible(true);

            LinkedHashMap<String, String> iris = new LinkedHashMap<>();
            iris.put("test_value_1", "test_iri_1");
            iris.put("test_value_2", "test_iri_2");

            List<List<String>> values = new ArrayList<>();
            List<String> test_list_1 = new ArrayList<>();
            test_list_1.add("1.5");
            test_list_1.add("2.5");
            List<String> test_list_2 = new ArrayList<>();
            test_list_2.add("3.5");
            test_list_2.add("4.5");
            values.add(test_list_1);
            values.add(test_list_2);

            List<OffsetDateTime> times = new ArrayList<>();
            times.add(OffsetDateTime.now());
            times.add(OffsetDateTime.now());

            addDataToTimeSeries.invoke(agent, values, times, iris);

            // Ensure correct methods on time series client are called
            verify(mockTs.constructed().get(0), times(1)).getMaxTime(anyString());
            verify(mockTs.constructed().get(0), times(1)).getMinTime(anyString());
            verify(mockTs.constructed().get(0), times(1)).addTimeSeriesData(any());
        }
    }

    @Test
    public void testTimeSeriesExist() throws Exception {
        TimeSeriesClient<OffsetDateTime> client = mock(TimeSeriesClient.class);

        CEAAgent agent = new CEAAgent();
        Method timeSeriesExist = agent.getClass().getDeclaredMethod("timeSeriesExist", List.class);
        assertNotNull(timeSeriesExist);
        timeSeriesExist.setAccessible(true);

        List<String> iris = new ArrayList<>();
        iris.add("test_1");
        iris.add("test_2");

        Field tsClient = agent.getClass().getDeclaredField("tsClient");
        tsClient.setAccessible(true);
        tsClient.set(agent, client);

        when(client.checkDataHasTimeSeries(anyString()))
                .thenReturn(false)
                .thenThrow(new DataAccessException("ERROR: relation \"dbTable\" does not exist"))
                .thenReturn(true);

        // Ensure returns result of checkDataHasTimeSeries
        Boolean result = (Boolean) timeSeriesExist.invoke(agent, iris);
        assertFalse(result);

        result = (Boolean) timeSeriesExist.invoke(agent, iris);
        assertFalse(result);

        result = (Boolean) timeSeriesExist.invoke(agent, iris);
        assertTrue(result);

    }

    @Test
    public void testGetNamespace()
         throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        CEAAgent agent = new CEAAgent();
        String uri = "http://localhost/berlin/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";

        Method getNamespace = agent.getClass().getDeclaredMethod("getNamespace", String.class);
        assertNotNull(getNamespace);
        getNamespace.setAccessible(true);

        // Ensure namespace is extracted correctly
        String result = (String) getNamespace.invoke(agent, uri);
        assertEquals("http://localhost/berlin/", result);

    }

    @Test
    public void testGetGraph()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        CEAAgent agent = new CEAAgent();
        String uri = "http://localhost/berlin/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";
        String graph = "building";

        Method getGraph = agent.getClass().getDeclaredMethod("getGraph", String.class, String.class);
        assertNotNull(getGraph);
        getGraph.setAccessible(true);

        // Ensure Graph IRI is formed correctly
        String result = (String) getGraph.invoke(agent, uri, graph);
        assertEquals("http://localhost/berlin/building/", result);

    }

    @Test
    public void testGetUUID()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        CEAAgent agent = new CEAAgent();
        String uri = "http://localhost/berlin/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";

        Method getUUID = agent.getClass().getDeclaredMethod("getUUID", String.class);
        assertNotNull(getUUID);
        getUUID.setAccessible(true);

        // Ensure UUID is extracted correctly
        String result = (String) getUUID.invoke(agent, uri);
        assertEquals("UUID_583747b0-1655-4761-8050-4036436a1052", result);

    }

    @Test
    public void testGetRoute()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        CEAAgent agent = new CEAAgent();
        String uri = "http://www.theworldavatar.com:83/citieskg/namespace/kingslynnEPSG3857/sparql/cityobject/test_UUID";
        ResourceBundle config = ResourceBundle.getBundle("CEAAgentConfig");

        Method getRoute = agent.getClass().getDeclaredMethod("getRoute", String.class);
        assertNotNull(getRoute);
        getRoute.setAccessible(true);

        // Ensure route is retrieved correctly
        String result = (String) getRoute.invoke(agent, uri);
        assertEquals(config.getString("kingslynnEPSG3857.targetresourceid"), result);

    }

    @Test
    public void testGetValue() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();

        String uriString = "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/cityobject/UUID_test/";
        String value = "Height";
        String result = "5.2";
        String route = "test_route";

        JSONArray expected = new JSONArray().put(new JSONObject().put(value, result));
        JSONArray expectedBlank = new JSONArray();

        Method getValue = agent.getClass().getDeclaredMethod("getValue", String.class, String.class, String.class);
        assertNotNull(getValue);
        getValue.setAccessible(true);

        try (MockedStatic<AccessAgentCaller> accessAgentCallerMock = mockStatic(AccessAgentCaller.class)) {

            //test with mocked AccessAgentCaller when it returns a string
            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(anyString(), anyString()))
                    .thenReturn(expected);
            assertEquals(result, getValue.invoke(agent, uriString, value, route));

            //test with mocked AccessAgentCaller when there is no string to return
            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(anyString(), anyString()))
                    .thenReturn((expectedBlank));
            assertEquals("", getValue.invoke(agent,uriString, value, route));
        }
    }

    @Test
    public void testGetFootprint()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        String geom1 = "559267.200000246#313892.7999989044#1.7#559280.5400002463#313892.7999989044#1.7#559280.5400002463#313908.7499989033#6.7#559267.200000246#313908.7499989033#6.7#559267.200000246#313892.7999989044#1.7";
        String geom2 = "559267.200000246#313892.7999989044#1.7#559280.5400002463#313892.7999989044#1.7#559280.5400002463#313908.7499989033#1.7#559267.200000246#313908.7499989033#1.7#559267.200000246#313892.7999989044#1.7";
        String geom3 = "559267.200000246#313892.7999989044#0.0#559280.5400002463#313892.7999989044#0.0#559280.5400002463#313908.7499989033#0.0#559267.200000246#313908.7499989033#0.0#559267.200000246#313892.7999989044#0.0";

        JSONArray results = new JSONArray("[{'Footprint': '"+geom1+"'}, {'Footprint': '"+geom2+"'},{'Footprint': '"+geom3+"'}]");

        Method getFootprint = agent.getClass().getDeclaredMethod("getFootprint", JSONArray.class);
        assertNotNull(getFootprint);
        getFootprint.setAccessible(true);

        // Ensure Footprint geometry is correctly found
        assertEquals(geom3, getFootprint.invoke(agent, results));

    }

    @Test
    public void testGetQuery()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        CEAAgent agent = new CEAAgent();
        String case1 = "Footprint";
        String case2 = "Height";
        String case3 = "CRS";
        String uri1 = "http://localhost/berlin/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";
        String uri2 = "http://localhost/kings-lynn-open-data/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";

        Method getQuery = agent.getClass().getDeclaredMethod("getQuery", String.class, String.class);
        assertNotNull(getQuery);
        getQuery.setAccessible(true);


        // Ensure queries contains correct predicates and objects depending on value sent
        Query q1 = (Query) getQuery.invoke(agent, uri1, case1);
        assertTrue(q1.toString().contains("ocgml:GeometryType"));
        Query q2 = (Query) getQuery.invoke(agent, uri1, case2);
        assertTrue(q2.toString().contains("ocgml:attrName"));
        assertTrue(q2.toString().contains("ocgml:realVal"));
        assertTrue(q2.toString().contains("height"));
        Query q3 = (Query) getQuery.invoke(agent, uri2, case2);
        assertTrue(q3.toString().contains("ocgml:measuredHeight"));
        Query q4 = (Query) getQuery.invoke(agent, uri2, case3);
        assertTrue(q4.toString().contains("ocgml:srid"));

    }

    @Test
    public void testGetGeometryQuery()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        String uri = "http://localhost/berlin/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";

        Method getGeometryQuery = agent.getClass().getDeclaredMethod("getGeometryQuery", String.class);
        assertNotNull(getGeometryQuery);
        getGeometryQuery.setAccessible(true);

        // Ensure query contains correct predicate and object
        Query q = (Query) getGeometryQuery.invoke(agent, uri);
        assertTrue(q.toString().contains("ocgml:GeometryType"));
        assertTrue(q.toString().contains("Footprint"));
    }

    @Test
    public void testGetHeightQuery()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        String uri1 = "http://localhost/berlin/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";
        String uri2 = "http://localhost/kings-lynn-open-data/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";

        Method getHeightQuery = agent.getClass().getDeclaredMethod("getHeightQuery", String.class);
        assertNotNull(getHeightQuery);
        getHeightQuery.setAccessible(true);

        // Ensure query contains correct predicate and object
        Query q1 = (Query) getHeightQuery.invoke(agent, uri1);
        assertTrue(q1.toString().contains("ocgml:attrName"));
        assertTrue(q1.toString().contains("ocgml:realVal"));
        assertTrue(q1.toString().contains("height"));
        Query q2 = (Query) getHeightQuery.invoke(agent, uri2);
        assertTrue(q2.toString().contains("ocgml:measuredHeight"));
        assertTrue(q2.toString().contains("Height"));

    }

    @Test
    public void testGetCrsQuery()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        String uri = "http://localhost/kings-lynn-open-data/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";

        Method getCrsQuery = agent.getClass().getDeclaredMethod("getCrsQuery", String.class);
        assertNotNull(getCrsQuery);
        getCrsQuery.setAccessible(true);

        // Ensure query contains correct predicate and object
        Query q = (Query) getCrsQuery.invoke(agent, uri);
        assertTrue(q.toString().contains("ocgml:srid"));
        assertTrue(q.toString().contains("CRS"));
    }

    @Test
   public void testSparqlGenAttributeUpdate()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = spy(new CEAAgent());
        Method sparqlGenAttributeUpdate = agent.getClass().getDeclaredMethod("sparqlGenAttributeUpdate", String.class, String.class, String.class);
        assertNotNull(sparqlGenAttributeUpdate);

        String uriString = "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/cityobject/UUID_test/";
        String energyProfileString = "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/energyprofile/UUID_test/";

        String route = "test_route";

        doNothing().when(agent).updateStore(anyString(), anyString());
        sparqlGenAttributeUpdate.invoke(agent, uriString, energyProfileString, route);

        //test update store is called once
        verify(agent, times(1)).updateStore(anyString(), anyString());
    }

    @Test
    public void testSparqlUpdate() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = spy(new CEAAgent());
        Method sparqlUpdate = agent.getClass().getDeclaredMethod("sparqlUpdate", LinkedHashMap.class, LinkedHashMap.class, String.class, Integer.class, String.class);
        assertNotNull(sparqlUpdate);

        LinkedHashMap<String,String> iris_mock = mock(LinkedHashMap.class);
        when(iris_mock.get(anyString())).thenReturn("test");

        LinkedHashMap<String,List<String>> scalars_mock = mock(LinkedHashMap.class);
        List<String> test_scalars = new ArrayList<>();
        test_scalars.add("test");
        when(scalars_mock.get(anyString())).thenReturn(test_scalars);

        String route = "test_route";

        Integer testCounter = 0;
        String uriString = "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/cityobject/UUID_test/";
        String expected = "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/energyprofile/";

        doNothing().when(agent).updateStore(anyString(), anyString());
        String result = (String) sparqlUpdate.invoke(agent, scalars_mock, iris_mock, uriString, testCounter, route );

        //test string contains correct graph and update store is called once
        assertTrue( result.contains(expected));
        verify(agent, times(1)).updateStore(anyString(), anyString());
    }

    @Test
    public void testGetDataIri() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();

        String uriString = "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/cityobject/UUID_test/";
        String test_value = "PV_area_roof";
        String measure = "measure";
        String test_measure = "35.2";
        String unit = "unit";
        String test_unit = "m^2";
        String route = "test_route";

        JSONArray expected = new JSONArray().put(new JSONObject().put(measure, test_measure).put(unit, test_unit));
        JSONArray expectedBlank = new JSONArray();

        Method getDataIRI = agent.getClass().getDeclaredMethod("getDataIRI", String.class, String.class, String.class);
        assertNotNull(getDataIRI);
        getDataIRI.setAccessible(true);

        try (MockedStatic<AccessAgentCaller> accessAgentCallerMock = mockStatic(AccessAgentCaller.class)) {

            //test with mocked AccessAgentCaller when it returns data
            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(anyString(), anyString()))
                    .thenReturn(expected);
            ArrayList<String> result = (ArrayList<String>) getDataIRI.invoke(agent, uriString, test_value, route);
            assertTrue(result.contains(test_measure));
            assertTrue(result.contains(test_unit));

            //test with mocked AccessAgentCaller when there is nothing returned
            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(anyString(), anyString()))
                    .thenReturn((expectedBlank));
            result = (ArrayList<String>) getDataIRI.invoke(agent, uriString, test_value, route);

            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testCheckGenAttributeInitialised() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();

        String uriString = "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/cityobject/UUID_test/";
        String measure = "energyProfileBuilding";
        String test_measure = "test_uri";
        String route = "test_route";

        JSONArray expected = new JSONArray().put(new JSONObject().put(measure, test_measure));
        JSONArray expectedBlank = new JSONArray();

        Method checkGenAttributeInitialised = agent.getClass().getDeclaredMethod("checkGenAttributeInitialised", String.class, String.class);
        assertNotNull(checkGenAttributeInitialised);
        checkGenAttributeInitialised.setAccessible(true);

        try (MockedStatic<AccessAgentCaller> accessAgentCallerMock = mockStatic(AccessAgentCaller.class)) {

            //test with mocked AccessAgentCaller when it returns a string.
            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(anyString(), anyString()))
                    .thenReturn(expected);
            assertEquals(test_measure, checkGenAttributeInitialised.invoke(agent, uriString, route));

            //test with mocked AccessAgentCaller when there is no string to return.
            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(anyString(), anyString()))
                    .thenReturn((expectedBlank));
            assertEquals("", checkGenAttributeInitialised.invoke(agent, uriString, route));
        }
    }

    @Test
    public void testCheckBlazegraphAndTimeSeriesInitialised() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();

        String uriString = "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/cityobject/UUID_test/";
        List<LinkedHashMap<String,String>> fixedIris = new ArrayList<>();
        String measure_grid = "grid_demand";
        String measure_building = "energyProfileBuilding";
        String test_measure = "test_uri1";
        String building = "test_building_uri";
        String route = "test_route";

        JSONArray expected_grid = new JSONArray().put(new JSONObject().put(measure_grid, test_measure));
        JSONArray expected_building = new JSONArray().put(new JSONObject().put(measure_building, building));
        JSONArray expectedBlank = new JSONArray();


        Method checkBlazegraphAndTimeSeriesInitialised = agent.getClass().getDeclaredMethod("checkBlazegraphAndTimeSeriesInitialised", String.class, List.class, String.class);
        assertNotNull(checkBlazegraphAndTimeSeriesInitialised);
        checkBlazegraphAndTimeSeriesInitialised.setAccessible(true);

        try (MockedStatic<AccessAgentCaller> accessAgentCallerMock = mockStatic(AccessAgentCaller.class)) {

            //test with mocked AccessAgentCaller when it returns a string.
            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(anyString(), anyString()))
                    .thenReturn(expected_building).thenReturn(expected_grid);
            assertEquals(building, checkBlazegraphAndTimeSeriesInitialised.invoke(agent, uriString, fixedIris, route));
            assertTrue(fixedIris.get(0).containsKey(measure_grid));
            assertTrue(fixedIris.get(0).containsValue(test_measure));

            //test with mocked AccessAgentCaller when there is no string to return.
            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(anyString(), anyString()))
                    .thenReturn((expectedBlank));
            assertEquals("", checkBlazegraphAndTimeSeriesInitialised.invoke(agent, uriString, fixedIris, route));
        }

    }

    @Test
    public void testGetUnit() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        Method getUnit = agent.getClass().getDeclaredMethod("getUnit", String.class);
        assertNotNull(getUnit);

        String test_kwh = "http://www.ontology-of-units-of-measure.org/resource/om-2/kilowattHour";
        String test_m2 = "http://www.ontology-of-units-of-measure.org/resource/om-2/squareMetre";
        String test_other = "test";

        // Ensure units retrieved correctly
        assertEquals(getUnit.invoke(agent, test_kwh), "kWh");
        assertEquals(getUnit.invoke(agent, test_m2), "m^2");
        assertEquals(getUnit.invoke(agent, test_other), "");
    }

    @Test
    public void testRetrieveData() throws Exception {
        try(MockedConstruction<TimeSeriesClient> mockTs = mockConstruction(TimeSeriesClient.class)) {
            CEAAgent agent = new CEAAgent();
            Method retrieveData = agent.getClass().getDeclaredMethod("retrieveData", String.class);
            assertNotNull(retrieveData);

            String iri = "test";
            List<String> iris = new ArrayList<>();
            iris.add(iri);

            retrieveData.invoke(agent, iri);

            // Ensure method to get time series client was invoked once
            verify(mockTs.constructed().get(0), times(1)).getTimeSeries(iris);
        }
    }

    @Test
    public void testCalculateAnnual() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException  {
        CEAAgent agent = new CEAAgent();
        Method calculateAnnual = agent.getClass().getDeclaredMethod("calculateAnnual", TimeSeries.class, String.class);
        assertNotNull(calculateAnnual);

        List<String> iris = new ArrayList<>();
        String iri1 = "test_iri_1";
        String iri2 = "test_iri_2";
        iris.add( iri1);
        iris.add( iri2);

        Double value1 = 1.687;
        Double value2 = 2.141;
        Double value3 = 3.621;
        Double value4 = 4.7;

        List<List<?>> values = new ArrayList<>();
        List<Double> test_list_1 = new ArrayList<>();
        test_list_1.add(value1);
        test_list_1.add(value2);
        List<Double> test_list_2 = new ArrayList<>();
        test_list_2.add(value3);
        test_list_2.add(value4);
        values.add(test_list_1);
        values.add(test_list_2);

        List<OffsetDateTime> times = new ArrayList<>();
        times.add(OffsetDateTime.now());
        times.add(OffsetDateTime.now());
        TimeSeries<OffsetDateTime> timeSeries = new TimeSeries<>(times, iris, values);

        Double expected1= 3.83;
        Double expected2= 8.32;

        // Ensure values in time series are summed and rounded correctly
        assertEquals(calculateAnnual.invoke(agent, timeSeries, iri1), expected1.toString());
        assertEquals(calculateAnnual.invoke(agent, timeSeries, iri2), expected2.toString());
    }
}
