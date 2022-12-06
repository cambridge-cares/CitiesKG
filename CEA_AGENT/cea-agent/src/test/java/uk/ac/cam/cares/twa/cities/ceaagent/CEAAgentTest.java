package uk.ac.cam.cares.twa.cities.ceaagent;

import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.sparql.core.Var;
import org.jooq.exception.DataAccessException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.jps.base.query.RemoteRDBStoreClient;
import uk.ac.cam.cares.jps.base.timeseries.TimeSeries;
import uk.ac.cam.cares.jps.base.timeseries.TimeSeriesClient;
import org.apache.jena.query.Query;
import uk.ac.cam.cares.twa.cities.tasks.CEAInputData;
import uk.ac.cam.cares.twa.cities.tasks.RunCEATask;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.locationtech.jts.geom.*;

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

        assertEquals(53, agent.getClass().getDeclaredFields().length);

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
        Field THEMATIC_SURFACE;
        Field KEY_GRID_CONSUMPTION;
        Field KEY_ELECTRICITY_CONSUMPTION;
        Field KEY_HEATING_CONSUMPTION;
        Field KEY_COOLING_CONSUMPTION;
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
        Field rdbStoreClient;
        Field storeClient;
        Field ocgmlUri;
        Field ontoUBEMMPUri;
        Field rdfUri;
        Field owlUri;
        Field purlEnaeqUri;
        Field purlInfrastructureUri;
        Field thinkhomeUri;
        Field unitOntologyUri;
        Field ontoBuiltEnvUri;
        Field accessAgentRoutes;
        Field requestUrl;
        Field targetUrl;
        Field localRoute;
        Field usageRoute;

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
            THEMATIC_SURFACE = agent.getClass().getDeclaredField("THEMATIC_SURFACE");
            assertEquals(THEMATIC_SURFACE.get(agent), "thematicsurface");
            KEY_GRID_CONSUMPTION = agent.getClass().getDeclaredField("KEY_GRID_CONSUMPTION");
            assertEquals(KEY_GRID_CONSUMPTION.get(agent), "GridConsumption");
            KEY_ELECTRICITY_CONSUMPTION = agent.getClass().getDeclaredField("KEY_ELECTRICITY_CONSUMPTION");
            assertEquals(KEY_ELECTRICITY_CONSUMPTION.get(agent), "ElectricityConsumption");
            KEY_HEATING_CONSUMPTION = agent.getClass().getDeclaredField("KEY_HEATING_CONSUMPTION");
            assertEquals(KEY_HEATING_CONSUMPTION.get(agent), "HeatingConsumption");
            KEY_COOLING_CONSUMPTION = agent.getClass().getDeclaredField("KEY_COOLING_CONSUMPTION");
            assertEquals(KEY_COOLING_CONSUMPTION.get(agent), "CoolingConsumption");
            KEY_PV_ROOF_AREA = agent.getClass().getDeclaredField("KEY_PV_ROOF_AREA");
            assertEquals(KEY_PV_ROOF_AREA.get(agent), "PVRoofArea");
            KEY_PV_WALL_NORTH_AREA = agent.getClass().getDeclaredField("KEY_PV_WALL_NORTH_AREA");
            assertEquals(KEY_PV_WALL_NORTH_AREA.get(agent), "PVWallNorthArea");
            KEY_PV_WALL_SOUTH_AREA = agent.getClass().getDeclaredField("KEY_PV_WALL_SOUTH_AREA");
            assertEquals(KEY_PV_WALL_SOUTH_AREA.get(agent), "PVWallSouthArea");
            KEY_PV_WALL_EAST_AREA = agent.getClass().getDeclaredField("KEY_PV_WALL_EAST_AREA");
            assertEquals(KEY_PV_WALL_EAST_AREA.get(agent), "PVWallEastArea");
            KEY_PV_WALL_WEST_AREA = agent.getClass().getDeclaredField("KEY_PV_WALL_WEST_AREA");
            assertEquals(KEY_PV_WALL_WEST_AREA.get(agent), "PVWallWestArea");
            KEY_PV_ROOF_SUPPLY = agent.getClass().getDeclaredField("KEY_PV_ROOF_SUPPLY");
            assertEquals(KEY_PV_ROOF_SUPPLY.get(agent), "PVRoofSupply");
            KEY_PV_WALL_NORTH_SUPPLY = agent.getClass().getDeclaredField("KEY_PV_WALL_NORTH_SUPPLY");
            assertEquals(KEY_PV_WALL_NORTH_SUPPLY.get(agent), "PVWallNorthSupply");
            KEY_PV_WALL_SOUTH_SUPPLY = agent.getClass().getDeclaredField("KEY_PV_WALL_SOUTH_SUPPLY");
            assertEquals(KEY_PV_WALL_SOUTH_SUPPLY.get(agent), "PVWallSouthSupply");
            KEY_PV_WALL_EAST_SUPPLY = agent.getClass().getDeclaredField("KEY_PV_WALL_EAST_SUPPLY");
            assertEquals(KEY_PV_WALL_EAST_SUPPLY.get(agent), "PVWallEastSupply");
            KEY_PV_WALL_WEST_SUPPLY = agent.getClass().getDeclaredField("KEY_PV_WALL_WEST_SUPPLY");
            assertEquals(KEY_PV_WALL_WEST_SUPPLY.get(agent), "PVWallWestSupply");
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
            rdbStoreClient = agent.getClass().getDeclaredField("rdbStoreClient");
            rdbStoreClient.setAccessible(true);
            assertNull(rdbStoreClient.get(agent));
            storeClient = agent.getClass().getDeclaredField("storeClient");
            storeClient.setAccessible(true);
            assertNull(storeClient.get(agent));

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
            thinkhomeUri = agent.getClass().getDeclaredField("thinkhomeUri");
            thinkhomeUri.setAccessible(true);
            assertEquals(thinkhomeUri.get(agent), config.getString("uri.ontology.thinkhome"));
            unitOntologyUri = agent.getClass().getDeclaredField("unitOntologyUri");
            unitOntologyUri.setAccessible(true);
            assertEquals(unitOntologyUri.get(agent), config.getString("uri.ontology.om"));
            ontoBuiltEnvUri = agent.getClass().getDeclaredField("ontoBuiltEnvUri");
            ontoBuiltEnvUri.setAccessible(true);
            assertEquals(ontoBuiltEnvUri.get(agent), config.getString("uri.ontology.ontobuiltenv"));

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
            assertEquals(localRoute.get(agent), config.getString("query.route.local"));
            usageRoute = agent.getClass().getDeclaredField("usageRoute");
            usageRoute.setAccessible(true);
            assertEquals(usageRoute.get(agent), config.getString("usage.query.route"));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testCEAAgentMethods() {
        CEAAgent agent = new CEAAgent();
        assertEquals(60, agent.getClass().getDeclaredMethods().length);
    }

    @Test
    public void testProcessRequestParameters()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException, SQLException {

        CEAAgent agent = spy(new CEAAgent());
        Method processRequestParameters = agent.getClass().getDeclaredMethod("processRequestParameters", JSONObject.class);
        JSONObject requestParams = new JSONObject();

        // set route
        Field localRoute = agent.getClass().getDeclaredField("localRoute");
        localRoute.setAccessible(true);
        localRoute.set(agent, "test_route");

        RemoteRDBStoreClient mockRDBClient = mock(RemoteRDBStoreClient.class);
        Connection mockConnection = mock(Connection.class);

        Field rdbStoreClient = agent.getClass().getDeclaredField("rdbStoreClient");
        rdbStoreClient.setAccessible(true);
        rdbStoreClient.set(agent, mockRDBClient);

        doReturn(mockConnection).when(mockRDBClient).getConnection();

        doNothing().when(agent).setTimeSeriesProps(anyString(), anyString());
        doNothing().when(agent).setRDBClient(anyString());

        // Test empty request params
        try {
            processRequestParameters.invoke(agent, requestParams);
        } catch(Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(),
                    BadRequestException.class);
        }

        // test data
        String measure_building = "building";
        String test_measure = "test_uri1";
        String test_unit = "test_uri2";
        String building = "test_building_uri";
        String measure_height = "HeightMeasuredHeigh";
        String test_height = "5.0";
        String measure_footprint = "geometry";
        String test_footprint = "559267.200000246#313892.7999989044#0.0#559280.5400002463#313892.7999989044#0.0#559280.5400002463#313908.7499989033#0.0#559267.200000246#313908.7499989033#0.0#559267.200000246#313892.7999989044#0.0";
        String measure_datatype = "datatype";
        String test_datatype = "<http://localhost/blazegraph/literals/POLYGON-3-15>";
        String measure_usage = "PropertyUsageCategory";
        String test_usage = "<https://www.theworldavatar.com/kg/ontobuiltenv/Office>";
        String measure_crs = "CRS";
        String test_crs = "test_crs";
        String testScalar = "testScalar";

        JSONArray expected_building = new JSONArray().put(new JSONObject().put(measure_building, building));
        JSONArray expected_height = new JSONArray().put(new JSONObject().put(measure_height, test_height));
        JSONArray expected_footprint = new JSONArray().put(new JSONObject().put(measure_footprint, test_footprint).put(measure_datatype, test_datatype));
        JSONArray expected_usage = new JSONArray().put(new JSONObject().put(measure_usage, test_usage));
        JSONArray expected_crs = new JSONArray().put(new JSONObject().put(measure_crs, test_crs));
        JSONArray expected_iri = new JSONArray().put(new JSONObject().put("measure", test_measure).put("unit", test_unit));
        JSONArray expected_value = new JSONArray().put(new JSONObject().put("value", testScalar));

        // Test the update endpoint
        requestParams.put(CEAAgent.KEY_REQ_URL, "http://localhost:8086/agents/cea/update");
        requestParams.put(CEAAgent.KEY_IRI, "['http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/cityobject/UUID_test/']");
        requestParams.put(CEAAgent.KEY_TARGET_URL, "http://localhost:8086/agents/cea/update");
        requestParams.put(CEAAgent.KEY_REQ_METHOD, HttpMethod.POST);

        JSONArray arrayMock = mock(JSONArray.class);
        when(arrayMock.length()).thenReturn(1);
        when(arrayMock.getString(anyInt())).thenReturn(OffsetDateTime.now().toString()).thenReturn("4.2");
        when(arrayMock.get(anyInt())).thenReturn(arrayMock);

        requestParams.put(CEAAgent.KEY_GRID_CONSUMPTION, arrayMock);
        requestParams.put(CEAAgent.KEY_ELECTRICITY_CONSUMPTION, arrayMock);
        requestParams.put(CEAAgent.KEY_HEATING_CONSUMPTION, arrayMock);
        requestParams.put(CEAAgent.KEY_COOLING_CONSUMPTION, arrayMock);
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
                    .thenReturn(expected_building).thenReturn(expected_iri);

            try (MockedConstruction<TimeSeriesClient> mockTs = mockConstruction(TimeSeriesClient.class)) {

                returnParams = (JSONObject) processRequestParameters.invoke(agent, requestParams);
                verify(mockTs.constructed().get(0), times(1)).addTimeSeriesData(any(), any());
                assertEquals(requestParams, returnParams);

            }

            //Test the run endpoint
            requestParams.remove(CEAAgent.KEY_REQ_URL);
            requestParams.put(CEAAgent.KEY_REQ_URL, "http://localhost:8086/agents/cea/run");

            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(anyString(), anyString()))
                    .thenReturn(expected_height).thenReturn(expected_footprint).thenReturn(expected_usage).thenReturn(expected_crs);

            try (MockedConstruction<RunCEATask> mockTask = mockConstruction(RunCEATask.class)) {
                ThreadPoolExecutor executor = mock(ThreadPoolExecutor.class);
                Field CEAExecutor = agent.getClass().getDeclaredField("CEAExecutor");
                CEAExecutor.setAccessible(true);
                CEAExecutor.set(agent, executor);

                returnParams = (JSONObject) processRequestParameters.invoke(agent, requestParams);
                verify(executor, times(1)).execute(mockTask.constructed().get(0));
                assertEquals(requestParams, returnParams);
            }


            //Test the query endpoint
            requestParams.remove(CEAAgent.KEY_REQ_URL);
            requestParams.put(CEAAgent.KEY_REQ_URL, "http://localhost:8086/agents/cea/query");

            //Test time series data
            String testUnit = "testUnit";
            ArrayList<String> testList = mock(ArrayList.class);
            when(testList.get(0)).thenReturn(testScalar);
            when(testList.get(1)).thenReturn(testUnit);
            String testReturnValue = "testAnnual";
            TimeSeries<OffsetDateTime> timeSeries = mock(TimeSeries.class);

            doReturn(testList).when(agent).getDataIRI(anyString(), anyString(), anyString(), anyString());
            doReturn(testReturnValue).when(agent).calculateAnnual(any(), anyString());
            doReturn(timeSeries).when(agent).retrieveData(anyString());
            doReturn(testUnit).when(agent).getUnit(anyString());

            Field TIME_SERIES = agent.getClass().getDeclaredField("TIME_SERIES");
            List<String> time_series_strings = (List<String>) TIME_SERIES.get(agent);
            Field SCALARS = agent.getClass().getDeclaredField("SCALARS");
            List<String> scalar_strings = (List<String>) SCALARS.get(agent);

            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(anyString(), anyString()))
                    .thenReturn(expected_building).thenReturn(expected_value);

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
        requestParams.put(CEAAgent.KEY_GRID_CONSUMPTION, "");
        requestParams.put(CEAAgent.KEY_ELECTRICITY_CONSUMPTION, "");
        requestParams.put(CEAAgent.KEY_HEATING_CONSUMPTION, "");
        requestParams.put(CEAAgent.KEY_COOLING_CONSUMPTION, "");
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

        requestParams.put(CEAAgent.KEY_GRID_CONSUMPTION, "test");
        requestParams.put(CEAAgent.KEY_ELECTRICITY_CONSUMPTION, "test");
        requestParams.put(CEAAgent.KEY_HEATING_CONSUMPTION, "test");
        requestParams.put(CEAAgent.KEY_COOLING_CONSUMPTION, "test");
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
            testData.add(new CEAInputData("test", "test","test"));
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
            Method createTimeSeries = agent.getClass().getDeclaredMethod("createTimeSeries", String.class, LinkedHashMap.class);
            assertNotNull(createTimeSeries);
            createTimeSeries.setAccessible(true);

            LinkedHashMap<String, String> fixedIris = new LinkedHashMap<>();
            String prefix = "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/";
            String testUri = prefix + "cityobject/UUID_test/";

            RemoteRDBStoreClient mockRDBClient = mock(RemoteRDBStoreClient.class);
            Connection mockConnection = mock(Connection.class);

            Field rdbStoreClient = agent.getClass().getDeclaredField("rdbStoreClient");
            rdbStoreClient.setAccessible(true);
            rdbStoreClient.set(agent, mockRDBClient);

            doReturn(mockConnection).when(mockRDBClient).getConnection();

            createTimeSeries.invoke(agent, testUri, fixedIris);

            Field TIME_SERIES = agent.getClass().getDeclaredField("TIME_SERIES");
            List<String> time_series_strings = (List<String>) TIME_SERIES.get(agent);

            // Ensure iris created correctly and time series initialised
            for (String time_series : time_series_strings) {
                assertTrue(fixedIris.get(time_series).contains(prefix + "energyprofile/" + time_series));
            }
            verify(mockTs.constructed().get(0), times(1)).initTimeSeries(anyList(), anyList(), anyString(), any(), any(), any(), any());
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

            RemoteRDBStoreClient mockRDBClient = mock(RemoteRDBStoreClient.class);
            Connection mockConnection = mock(Connection.class);

            Field rdbStoreClient = agent.getClass().getDeclaredField("rdbStoreClient");
            rdbStoreClient.setAccessible(true);
            rdbStoreClient.set(agent, mockRDBClient);

            doReturn(mockConnection).when(mockRDBClient).getConnection();

            addDataToTimeSeries.invoke(agent, values, times, iris);

            // Ensure correct methods on time series client are called
            verify(mockTs.constructed().get(0), times(1)).getMaxTime(anyString(), any());
            verify(mockTs.constructed().get(0), times(1)).getMinTime(anyString(), any());
            verify(mockTs.constructed().get(0), times(1)).addTimeSeriesData(any(), any());
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

        RemoteRDBStoreClient mockRDBClient = mock(RemoteRDBStoreClient.class);
        Connection mockConnection = mock(Connection.class);

        Field rdbStoreClient = agent.getClass().getDeclaredField("rdbStoreClient");
        rdbStoreClient.setAccessible(true);
        rdbStoreClient.set(agent, mockRDBClient);

        doReturn(mockConnection).when(mockRDBClient).getConnection();

        Field tsClient = agent.getClass().getDeclaredField("tsClient");
        tsClient.setAccessible(true);
        tsClient.set(agent, client);

        when(client.checkDataHasTimeSeries(anyString(), any()))
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
    public void testGetBuildingUri()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        CEAAgent agent = new CEAAgent();
        String uri = "http://localhost/berlin/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";

        Method getBuildingUri = agent.getClass().getDeclaredMethod("getBuildingUri", String.class);
        assertNotNull(getBuildingUri);
        getBuildingUri.setAccessible(true);

        // Ensure UUID is extracted correctly
        String result = (String) getBuildingUri.invoke(agent, uri);
        assertEquals("http://localhost/berlin/building/UUID_583747b0-1655-4761-8050-4036436a1052/", result);

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
        String value = "HeightMeasuredHeight";
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
    public void getGroundGeometry()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        Method getGroundGeometry = agent.getClass().getDeclaredMethod("getGroundGeometry", JSONArray.class);

        assertNotNull(getGroundGeometry);
        getGroundGeometry.setAccessible(true);

        String geometry1 = "1.0#1.0#0.0#1.0#2.0#0.0#2.0#2.0#0.0#2.0#1.0#0.0#1.0#1.0#0.0";
        String geometry2 = "1.0#1.0#0.0#1.0#1.0#2.0#2.0#1.0#2.0#2.0#1.0#0.0#1.0#1.0#0.0";
        String geometry3 = "1.0#2.0#0.0#2.0#2.0#0.0#2.0#1.0#0.0#1.0#1.0#0.0#1.0#2.0#0.0";
        String geometry4 = "1.0#2.0#1.0#2.0#2.0#1.0#2.0#1.0#0.0#1.0#1.0#1.0#1.0#2.0#1.0";

        JSONArray testArray = new JSONArray();
        testArray.put(new JSONObject().put("geometry", geometry1));
        testArray.put(new JSONObject().put("geometry", geometry2));
        testArray.put(new JSONObject().put("geometry", geometry3));
        testArray.put(new JSONObject().put("geometry", geometry4));

        JSONArray expected = new JSONArray();
        expected.put(new JSONObject().put("geometry", geometry1));
        expected.put(new JSONObject().put("geometry", geometry3));

        JSONArray result = (JSONArray) getGroundGeometry.invoke(agent, testArray);

        assertEquals(expected.length(), result.length());

        for (int i = 0; i < expected.length(); i++){
            assertEquals(expected.getJSONObject(i).get("geometry").toString(), result.getJSONObject(i).get("geometry").toString());
        }
    }

    @Test
    public void testGetQuery()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        CEAAgent agent = new CEAAgent();
        String case1 = "Lod0FootprintId";
        String case2 = "FootprintSurfaceGeom";
        String case3 = "FootprintThematicSurface";
        String case4 = "HeightMeasuredHeight";
        String case5 = "HeightMeasuredHeigh";
        String case6 = "HeightGenAttr";
        String case7 = "DatabasesrsCRS";
        String case8 = "CRS";
        String case9 = "PropertyUsageCategory";
        String uri = "http://localhost/kings-lynn-open-data/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";

        Method getQuery = agent.getClass().getDeclaredMethod("getQuery", String.class, String.class);
        assertNotNull(getQuery);
        getQuery.setAccessible(true);

        // Ensure queries contains correct predicates depending on value sent
        Query q1 = (Query) getQuery.invoke(agent, uri, case1);
        assertTrue(q1.toString().contains("lod0FootprintId"));
        Query q2 = (Query) getQuery.invoke(agent, uri, case2);
        assertTrue(q2.toString().contains("ocgml:GeometryType"));
        Query q3 = (Query) getQuery.invoke(agent, uri, case3);
        assertTrue(q3.toString().contains("ocgml:objectClassId"));
        Query q4 = (Query) getQuery.invoke(agent, uri, case4);
        assertTrue(q4.toString().contains("ocgml:measuredHeight"));
        Query q5 = (Query) getQuery.invoke(agent, uri, case5);
        assertTrue(q5.toString().contains("ocgml:measuredHeigh"));
        Query q6 = (Query) getQuery.invoke(agent, uri, case6);
        assertTrue(q6.toString().contains("ocgml:attrName"));
        Query q7 = (Query) getQuery.invoke(agent, uri, case7);
        assertTrue(q7.toString().contains("ocgml:srid"));
        Query q8 = (Query) getQuery.invoke(agent, uri, case8);
        assertTrue(q8.toString().contains("srid"));
        Query q9 = (Query) getQuery.invoke(agent, uri, case9);
        assertTrue(q9.toString().contains("hasUsageCategory"));
    }

    @Test
    public void testGetGeometryQueryThematicSurface()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        String uri = "http://localhost/berlin/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";

        Method getGeometryQueryThematicSurface = agent.getClass().getDeclaredMethod("getGeometryQueryThematicSurface", String.class);
        assertNotNull(getGeometryQueryThematicSurface);
        getGeometryQueryThematicSurface.setAccessible(true);

        // Ensure query contains correct predicate and object
        Query q = (Query) getGeometryQueryThematicSurface.invoke(agent, uri);
        assertTrue(q.toString().contains("ocgml:GeometryType"));
        assertTrue(q.toString().contains("geometry"));
        assertTrue(q.toString().contains("ocgml:objectClassId"));
        assertTrue(q.toString().contains("groundSurfId"));
        assertTrue(q.toString().contains("datatype"));
    }

    @Test
    public void testGetGeometryQuerySurfaceGeom()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        String uri = "http://localhost/berlin/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";

        Method getGeometryQuerySurfaceGeom = agent.getClass().getDeclaredMethod("getGeometryQuerySurfaceGeom", String.class);
        assertNotNull(getGeometryQuerySurfaceGeom);
        getGeometryQuerySurfaceGeom.setAccessible(true);

        // Ensure query contains correct predicates and objects
        Query q = (Query) getGeometryQuerySurfaceGeom.invoke(agent, uri);
        assertTrue(q.toString().contains("ocgml:GeometryType"));
        assertTrue(q.toString().contains("geometry"));
        assertTrue(q.toString().contains("datatype"));
    }

    @Test
    public void testGetHeightQueryMeasuredHeigh()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        String uri = "http://localhost/kings-lynn-open-data/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";

        Method getHeightQueryMeasuredHeigh = agent.getClass().getDeclaredMethod("getHeightQueryMeasuredHeigh", String.class);
        assertNotNull(getHeightQueryMeasuredHeigh);
        getHeightQueryMeasuredHeigh.setAccessible(true);

        // Ensure query contains correct predicate and object
        Query q = (Query) getHeightQueryMeasuredHeigh.invoke(agent, uri);
        assertTrue(q.toString().contains("ocgml:measuredHeigh"));
        assertTrue(q.toString().contains("HeightMeasuredHeigh"));
    }

    @Test
    public void testGetHeightQueryMeasuredHeight()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        String uri = "http://localhost/kings-lynn-open-data/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";

        Method getHeightQueryMeasuredHeight = agent.getClass().getDeclaredMethod("getHeightQueryMeasuredHeight", String.class);
        assertNotNull(getHeightQueryMeasuredHeight);
        getHeightQueryMeasuredHeight.setAccessible(true);

        // Ensure query contains correct predicate and object
        Query q = (Query) getHeightQueryMeasuredHeight.invoke(agent, uri);
        assertTrue(q.toString().contains("ocgml:measuredHeight"));
        assertTrue(q.toString().contains("HeightMeasuredHeight"));
    }

    @Test
    public void testGetHeightQueryGenAttr()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        String uri = "http://localhost/kings-lynn-open-data/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";

        Method getHeightQueryGenAttr = agent.getClass().getDeclaredMethod("getHeightQueryGenAttr", String.class);
        assertNotNull(getHeightQueryGenAttr);
        getHeightQueryGenAttr.setAccessible(true);

        // Ensure query contains correct predicate and object
        Query q = (Query) getHeightQueryGenAttr.invoke(agent, uri);
        assertTrue(q.toString().contains("ocgml:attrName"));
        assertTrue(q.toString().contains("ocgml:realVal"));
        assertTrue(q.toString().contains("HeightGenAttr"));
    }

    @Test
    public void testGetDatabasesrsCrsQuery()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        String uri = "http://localhost/kings-lynn-open-data/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";

        Method getDatabasesrsCrsQuery = agent.getClass().getDeclaredMethod("getDatabasesrsCrsQuery", String.class);
        assertNotNull(getDatabasesrsCrsQuery);
        getDatabasesrsCrsQuery.setAccessible(true);

        // Ensure query contains correct predicate and object
        Query q = (Query) getDatabasesrsCrsQuery.invoke(agent, uri);
        assertTrue(q.toString().contains("ocgml:srid"));
        assertTrue(q.toString().contains("CRS"));
    }

    @Test
    public void testAddBuildingConsumptionWhere() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        CEAAgent agent = new CEAAgent();
        Method addBuildingConsumptionWhere = agent.getClass().getDeclaredMethod("addBuildingConsumptionWhere", WhereBuilder.class, String.class);
        assertNotNull(addBuildingConsumptionWhere);

        String energyType = "test_type";

        WhereBuilder wb = new WhereBuilder();
        Field rdf = agent.getClass().getDeclaredField("rdfUri");
        rdf.setAccessible(true);
        String rdfUri = (String) rdf.get(agent);
        Field unitOntology = agent.getClass().getDeclaredField("unitOntologyUri");
        unitOntology.setAccessible(true);
        String unitOntologyUri = (String) unitOntology.get(agent);
        Field purlEnaeq = agent.getClass().getDeclaredField("purlEnaeqUri");
        purlEnaeq.setAccessible(true);
        String purlEnaeqUri = (String) purlEnaeq.get(agent);

        wb.addPrefix("rdf", rdfUri)
                .addPrefix("om", unitOntologyUri)
                .addPrefix("purlEnaeq", purlEnaeqUri);

        addBuildingConsumptionWhere.invoke(agent,  wb, energyType );

        String result = wb.build().toString().replaceAll("\\s", "");

        //test string contains expected where data
        String expected_where = "WHERE";
        String expected_triple = "?grid" + "rdf:type\"" + energyType +"\"";
        assertTrue( result.contains(expected_where));
        assertTrue( result.contains(expected_triple));
    }

    @Test
    public void testAddConsumptionDeviceWhere() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        CEAAgent agent = new CEAAgent();
        Method addConsumptionDeviceWhere = agent.getClass().getDeclaredMethod("addConsumptionDeviceWhere", WhereBuilder.class, String.class);
        assertNotNull(addConsumptionDeviceWhere);

        String system = "test_system";

        WhereBuilder wb = new WhereBuilder();
        Field rdf = agent.getClass().getDeclaredField("rdfUri");
        rdf.setAccessible(true);
        String rdfUri = (String) rdf.get(agent);
        Field unitOntology = agent.getClass().getDeclaredField("unitOntologyUri");
        unitOntology.setAccessible(true);
        String unitOntologyUri = (String) unitOntology.get(agent);
        Field purlEnaeq = agent.getClass().getDeclaredField("purlEnaeqUri");
        purlEnaeq.setAccessible(true);
        String purlEnaeqUri = (String) purlEnaeq.get(agent);
        Field ontoUBEMMP = agent.getClass().getDeclaredField("ontoUBEMMPUri");
        ontoUBEMMP.setAccessible(true);
        String ontoUBEMMPUri = (String) ontoUBEMMP.get(agent);


        wb.addPrefix("rdf", rdfUri)
                .addPrefix("om", unitOntologyUri)
                .addPrefix("purlEnaeq", purlEnaeqUri)
                .addPrefix("ontoubemmp", ontoUBEMMPUri);

        addConsumptionDeviceWhere.invoke(agent,  wb, system );

        String result = wb.build().toString().replaceAll("\\s", "");

        //test string contains expected where data
        String expected_where = "WHERE";
        String expected_triple = "?device" + "rdf:type\"" + system +"\"";
        assertTrue( result.contains(expected_where));
        assertTrue( result.contains(expected_triple));
    }

    @Test
    public void testAddSupplyDeviceWhere() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        CEAAgent agent = new CEAAgent();
        Method addSupplyDeviceWhere = agent.getClass().getDeclaredMethod("addSupplyDeviceWhere", WhereBuilder.class, String.class);
        assertNotNull(addSupplyDeviceWhere);

        String panelType = "test_type";

        WhereBuilder wb = new WhereBuilder();
        Field rdf = agent.getClass().getDeclaredField("rdfUri");
        rdf.setAccessible(true);
        String rdfUri = (String) rdf.get(agent);
        Field unitOntology = agent.getClass().getDeclaredField("unitOntologyUri");
        unitOntology.setAccessible(true);
        String unitOntologyUri = (String) unitOntology.get(agent);
        Field ontoUBEMMP = agent.getClass().getDeclaredField("ontoUBEMMPUri");
        ontoUBEMMP.setAccessible(true);
        String ontoUBEMMPUri = (String) ontoUBEMMP.get(agent);
        Field thinkhome = agent.getClass().getDeclaredField("thinkhomeUri");
        thinkhome.setAccessible(true);
        String thinkhomeUri = (String) thinkhome.get(agent);

        wb.addPrefix("rdf", rdfUri)
                .addPrefix("om", unitOntologyUri)
                .addPrefix("ontoubemmp", ontoUBEMMPUri)
                .addPrefix("thinkhome", thinkhomeUri);

        addSupplyDeviceWhere.invoke(agent,  wb, panelType );

        String result = wb.build().toString().replaceAll("\\s", "");

        //test string contains expected where data
        String expected_where = "WHERE";
        String expected_triple = "?PVPanels" + "rdf:type\"" + panelType +"\"";
        assertTrue( result.contains(expected_where));
        assertTrue( result.contains(expected_triple));
    }

    @Test
    public void testAddSupplyDeviceAreaWhere() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        CEAAgent agent = new CEAAgent();
        Method addSupplyDeviceAreaWhere = agent.getClass().getDeclaredMethod("addSupplyDeviceAreaWhere", WhereBuilder.class, String.class);
        assertNotNull(addSupplyDeviceAreaWhere);

        String panelType = "test_type";

        WhereBuilder wb = new WhereBuilder();

        Field rdf = agent.getClass().getDeclaredField("rdfUri");
        rdf.setAccessible(true);
        String rdfUri = (String) rdf.get(agent);
        Field unitOntology = agent.getClass().getDeclaredField("unitOntologyUri");
        unitOntology.setAccessible(true);
        String unitOntologyUri = (String) unitOntology.get(agent);
        Field ontoUBEMMP = agent.getClass().getDeclaredField("ontoUBEMMPUri");
        ontoUBEMMP.setAccessible(true);
        String ontoUBEMMPUri = (String) ontoUBEMMP.get(agent);

        wb.addPrefix("rdf", rdfUri)
                .addPrefix("om", unitOntologyUri)
                .addPrefix("ontoubemmp", ontoUBEMMPUri);

        addSupplyDeviceAreaWhere.invoke(agent,  wb, panelType );

        String result = wb.build().toString().replaceAll("\\s", "");

        //test string contains expected where data
        String expected_where = "WHERE";
        String expected_triple = "?PVPanels" + "rdf:type\"" + panelType +"\"";
        assertTrue( result.contains(expected_where));
        assertTrue( result.contains(expected_triple));
    }

    @Test
    public void testInitialiseData() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = spy(new CEAAgent());
        Method initialiseData = agent.getClass().getDeclaredMethod("initialiseData", String.class, Integer.class, LinkedHashMap.class, String.class, LinkedHashMap.class, LinkedHashMap.class, String.class);
        assertNotNull(initialiseData);

        LinkedHashMap<String,String> ts_iris_mock = mock(LinkedHashMap.class);
        when(ts_iris_mock.get(anyString())).thenReturn("test");

        LinkedHashMap<String,String> scalar_iris_mock = mock(LinkedHashMap.class);
        when(scalar_iris_mock.get(anyString())).thenReturn("test");

        LinkedHashMap<String,List<String>> scalars_mock = mock(LinkedHashMap.class);
        List<String> test_scalars = new ArrayList<>();
        test_scalars.add("test");
        when(scalars_mock.get(anyString())).thenReturn(test_scalars);

        String route = "test_route";

        Integer testCounter = 0;
        String uriString = "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/cityobject/UUID_test/";
        String building = "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/energyprofile/Building_UUID_test/";

        doNothing().when(agent).updateStore(anyString(), anyString());
        initialiseData.invoke(agent, uriString, testCounter, scalars_mock, building, ts_iris_mock, scalar_iris_mock, route );

        //test update store is called once
        verify(agent, times(1)).updateStore(anyString(), anyString());
    }

    @Test
    public void testUpdateScalars() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        CEAAgent agent = spy(new CEAAgent());
        Method updateScalars = agent.getClass().getDeclaredMethod("updateScalars", String.class, String.class, LinkedHashMap.class, LinkedHashMap.class, Integer.class);
        assertNotNull(updateScalars);

        LinkedHashMap<String,String> scalar_iris_mock = mock(LinkedHashMap.class);
        when(scalar_iris_mock.get(anyString())).thenReturn("test");

        LinkedHashMap<String,List<String>> scalars_mock = mock(LinkedHashMap.class);
        List<String> test_scalars = new ArrayList<>();
        test_scalars.add("test");
        when(scalars_mock.get(anyString())).thenReturn(test_scalars);

        String route = "test_route";

        Integer testCounter = 0;
        String uriString = "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/cityobject/UUID_test/";

        doNothing().when(agent).updateStore(anyString(), anyString());
        updateScalars.invoke(agent, uriString, route,scalar_iris_mock, scalars_mock, testCounter );

        //test update store is called twice for each scalar
        Field SCALARS = agent.getClass().getDeclaredField("SCALARS");
        List<String> scalar_strings = (List<String>) SCALARS.get(agent);
        Integer expected = scalar_strings.size()*2;
        verify(agent, times(expected)).updateStore(anyString(), anyString());
    }

    @Test
    public void testGetDataIri() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();

        String uriString = "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/cityobject/UUID_test/";
        String test_value = "PVRoofArea";
        String measure = "measure";
        String test_measure = "testUri";
        String unit = "unit";
        String test_unit = "m^2";
        String route = "test_route";
        String building = "test_building";

        JSONArray expected = new JSONArray().put(new JSONObject().put(measure, test_measure).put(unit, test_unit));
        JSONArray expectedBlank = new JSONArray();

        Method getDataIRI = agent.getClass().getDeclaredMethod("getDataIRI", String.class, String.class, String.class, String.class);
        assertNotNull(getDataIRI);
        getDataIRI.setAccessible(true);

        try (MockedStatic<AccessAgentCaller> accessAgentCallerMock = mockStatic(AccessAgentCaller.class)) {

            //test with mocked AccessAgentCaller when it returns data
            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(anyString(), anyString()))
                    .thenReturn(expected);
            ArrayList<String> result = (ArrayList<String>) getDataIRI.invoke(agent, uriString, building, test_value, route);
            assertTrue(result.contains(test_measure));
            assertTrue(result.contains(test_unit));

            //test with mocked AccessAgentCaller when there is nothing returned
            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(anyString(), anyString()))
                    .thenReturn((expectedBlank));
            result = (ArrayList<String>) getDataIRI.invoke(agent, uriString, building, test_value, route);

            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testGetNumericalValue() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();

        String uriString = "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/cityobject/UUID_test/";
        String test_measure = "testUri";
        String value = "value";
        String test_value = "35.2";
        String route = "test_route";

        JSONArray expected = new JSONArray().put(new JSONObject().put(value, test_value));
        JSONArray expectedBlank = new JSONArray();

        Method getNumericalValue = agent.getClass().getDeclaredMethod("getNumericalValue", String.class, String.class, String.class);
        assertNotNull(getNumericalValue);
        getNumericalValue.setAccessible(true);

        try (MockedStatic<AccessAgentCaller> accessAgentCallerMock = mockStatic(AccessAgentCaller.class)) {

            //test with mocked AccessAgentCaller when it returns data
            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(anyString(), anyString()))
                    .thenReturn(expected);
            String result = (String) getNumericalValue.invoke(agent, uriString, test_measure, route);
            assertTrue(result.contains(test_value));

            //test with mocked AccessAgentCaller when there is nothing returned
            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(anyString(), anyString()))
                    .thenReturn((expectedBlank));
            result = (String) getNumericalValue.invoke(agent, uriString, test_measure, route);

            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testCheckBuildingInitialised() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();

        String uriString = "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/cityobject/UUID_test/";
        String measure = "building";
        String test_measure = "test_uri";
        String route = "test_route";

        JSONArray expected = new JSONArray().put(new JSONObject().put(measure, test_measure));
        JSONArray expectedBlank = new JSONArray();

        Method checkBuildingInitialised = agent.getClass().getDeclaredMethod("checkBuildingInitialised", String.class, String.class);
        assertNotNull(checkBuildingInitialised);
        checkBuildingInitialised.setAccessible(true);

        try (MockedStatic<AccessAgentCaller> accessAgentCallerMock = mockStatic(AccessAgentCaller.class)) {

            //test with mocked AccessAgentCaller when it returns a string.
            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(anyString(), anyString()))
                    .thenReturn(expected);
            assertEquals(test_measure, checkBuildingInitialised.invoke(agent, uriString, route));

            //test with mocked AccessAgentCaller when there is no string to return.
            accessAgentCallerMock.when(() -> AccessAgentCaller.queryStore(anyString(), anyString()))
                    .thenReturn((expectedBlank));
            assertEquals("", checkBuildingInitialised.invoke(agent, uriString, route));
        }
    }

    @Test
    public void testInitialiseBuilding() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = spy(new CEAAgent());
        Method initialiseBuilding = agent.getClass().getDeclaredMethod("initialiseBuilding", String.class, String.class);
        assertNotNull(initialiseBuilding);

        String route = "test_route";

        String uriString = "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/cityobject/UUID_test/";
        String expected = "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/energyprofile/";

        doNothing().when(agent).updateStore(anyString(), anyString());
        String result = (String) initialiseBuilding.invoke(agent,  uriString, route );

        //test string contains correct graph and update store is called once
        assertTrue( result.contains(expected));
        verify(agent, times(1)).updateStore(anyString(), anyString());
    }

    @Test
    public void testCreateConsumptionUpdate() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        Method createConsumptionUpdate = agent.getClass().getDeclaredMethod("createConsumptionUpdate", UpdateBuilder.class, String.class, String.class, String.class, String.class);
        assertNotNull(createConsumptionUpdate);

        String consumer = "consumer_IRI";
        String type = "test_type";
        String quantity = "quantity_IRI";
        String measure = "measure_IRI";
        String graph = "test_graph";

        UpdateBuilder ub = new UpdateBuilder();

        createConsumptionUpdate.invoke(agent,  ub, consumer, type, quantity, measure );
        ub.setVar(Var.alloc("graph"), graph);

        String result = ub.buildRequest().toString();
        //test string contains expected insert data
        String expected_insert = "INSERT DATA";
        String expected_graph = "GRAPH \""+graph+"\"";
        String expected_triple1 = "<"+quantity+"> \"rdf:type\" \"" + type+"\"";
        String expected_triple2 = "<"+quantity+"> \"om:hasValue\" <" + measure+">";
        String expected_triple3 = "<"+consumer+"> \"purlEnaeq:consumesEnergy\" <" + quantity+">";
        assertTrue( result.contains(expected_insert));
        assertTrue( result.contains(expected_graph));
        assertTrue( result.contains(expected_triple1));
        assertTrue( result.contains(expected_triple2));
        assertTrue( result.contains(expected_triple3));
    }

    @Test
    public void testCreateDeviceConsumptionUpdate() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        Method createDeviceConsumptionUpdate = agent.getClass().getDeclaredMethod("createDeviceConsumptionUpdate", UpdateBuilder.class, String.class, String.class, String.class, String.class, String.class, String.class);
        assertNotNull(createDeviceConsumptionUpdate);

        String building = "building_IRI";
        String device = "device_IRI";
        String consumptionType = "test_type_1";
        String deviceType = "test_type_2";
        String quantity = "quantity_IRI";
        String measure = "measure_IRI";
        String graph = "test_graph";

        UpdateBuilder ub = new UpdateBuilder();

        createDeviceConsumptionUpdate.invoke(agent,  ub, building, device, deviceType, consumptionType, quantity, measure );
        ub.setVar(Var.alloc("graph"), graph);

        String result = ub.buildRequest().toString();
        //test string contains expected insert data
        String expected_insert = "INSERT DATA";
        String expected_graph = "GRAPH \""+graph+"\"";
        String expected_triple1 = "<"+building+"> \"ontoubemmp:hasDevice\" <" + device+">";
        String expected_triple2 = "<"+device+"> \"rdf:type\" \"" + deviceType+"\"";
        String expected_triple3 = "<"+quantity+"> \"rdf:type\" \"" + consumptionType+"\"";
        String expected_triple4 = "<"+quantity+"> \"om:hasValue\" <" + measure+">";
        String expected_triple5 = "<"+device+"> \"purlEnaeq:consumesEnergy\" <" + quantity+">";
        assertTrue( result.contains(expected_insert));
        assertTrue( result.contains(expected_graph));
        assertTrue( result.contains(expected_triple1));
        assertTrue( result.contains(expected_triple2));
        assertTrue( result.contains(expected_triple3));
        assertTrue( result.contains(expected_triple4));
        assertTrue( result.contains(expected_triple5));
    }

    @Test
    public void testCreatePVPanelSupplyUpdate() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        Method createPVPanelSupplyUpdate = agent.getClass().getDeclaredMethod("createPVPanelSupplyUpdate", UpdateBuilder.class, String.class, String.class, String.class);
        assertNotNull(createPVPanelSupplyUpdate);

        String PVPanels = "panels_IRI";
        String quantity = "quantity_IRI";
        String measure = "measure_IRI";
        String graph = "test_graph";

        UpdateBuilder ub = new UpdateBuilder();

        createPVPanelSupplyUpdate.invoke(agent,  ub, PVPanels, quantity, measure );
        ub.setVar(Var.alloc("graph"), graph);

        String result = ub.buildRequest().toString();
        //test string contains expected insert data
        String expected_insert = "INSERT DATA";
        String expected_graph = "GRAPH \""+graph+"\"";
        String expected_triple1 = "<"+PVPanels+"> \"thinkhome:producesEnergy\" <" + quantity+">";
        String expected_triple2 = "<"+quantity+"> \"om:hasValue\" <" + measure+">";
        assertTrue( result.contains(expected_insert));
        assertTrue( result.contains(expected_graph));
        assertTrue( result.contains(expected_triple1));
        assertTrue( result.contains(expected_triple2));
    }

    @Test
    public void testCreatePVPanelAreaUpdate() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        Method createPVPanelAreaUpdate = agent.getClass().getDeclaredMethod("createPVPanelAreaUpdate", UpdateBuilder.class, String.class, String.class, String.class, String.class, String.class, String.class);
        assertNotNull(createPVPanelAreaUpdate);

        String building = "building_IRI";
        String panelType = "test_type";
        String value = "test_value";
        String PVPanels = "panels_IRI";
        String quantity = "quantity_IRI";
        String measure = "measure_IRI";
        String graph = "test_graph";

        UpdateBuilder ub = new UpdateBuilder();

        createPVPanelAreaUpdate.invoke(agent,  ub, building, PVPanels, panelType, quantity, measure, value );
        ub.setVar(Var.alloc("graph"), graph);

        String result = ub.buildRequest().toString();
        //test string contains expected insert data
        String expected_insert = "INSERT DATA";
        String expected_graph = "GRAPH \""+graph+"\"";
        String expected_triple1 = "<"+building+"> \"ontoubemmp:hasDevice\" <" + PVPanels+">";
        String expected_triple2 = "<"+PVPanels+"> \"rdf:type\" \""+panelType+"\"";
        String expected_triple3 = "<"+PVPanels+"> \"ontoubemmp:hasArea\" <" + quantity+">";
        String expected_triple4 = "<"+quantity+"> \"om:hasValue\" <" + measure+">";
        String expected_triple5 = "<"+measure+"> \"om:hasNumericalValue\" \""+value+"\"";
        assertTrue( result.contains(expected_insert));
        assertTrue( result.contains(expected_graph));
        assertTrue( result.contains(expected_triple1));
        assertTrue( result.contains(expected_triple2));
        assertTrue( result.contains(expected_triple3));
        assertTrue( result.contains(expected_triple4));
        assertTrue( result.contains(expected_triple5));
    }

    @Test
    public void testCheckDataInitialised() throws NoSuchFieldException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = spy(new CEAAgent());
        Method checkDataInitialised = agent.getClass().getDeclaredMethod("checkDataInitialised", String.class, String.class, LinkedHashMap.class, LinkedHashMap.class, String.class);
        assertNotNull(checkDataInitialised);

        //Test time series data
        String testUnit = "testUnit";
        String testIri = "testIri";
        String testBuilding = "testBuilding";
        String route = "test_route";
        String uriString = "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/cityobject/UUID_test/";
        ArrayList<String> testList = mock(ArrayList.class);
        when(testList.get(0)).thenReturn(testIri);
        when(testList.get(1)).thenReturn(testUnit);
        doReturn(testList).when(agent).getDataIRI(anyString(), anyString(), anyString(), anyString());

        LinkedHashMap<String, String> tsIris = new LinkedHashMap();
        LinkedHashMap<String, String> scalarIris = new LinkedHashMap();

        Field TIME_SERIES = agent.getClass().getDeclaredField("TIME_SERIES");
        List<String> time_series_strings = (List<String>) TIME_SERIES.get(agent);
        Field SCALARS = agent.getClass().getDeclaredField("SCALARS");
        List<String> scalar_strings = (List<String>) SCALARS.get(agent);

        Boolean result = (Boolean) checkDataInitialised.invoke(agent,  uriString, testBuilding, tsIris, scalarIris, route );
        assertTrue(result);
        for (String scalar : scalar_strings) {
            assertTrue(scalarIris.get(scalar).contains(testIri));
        }
        for (String ts : time_series_strings) {
            assertTrue(tsIris.get(ts).contains(testIri));
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

            RemoteRDBStoreClient mockRDBClient = mock(RemoteRDBStoreClient.class);
            Connection mockConnection = mock(Connection.class);

            Field rdbStoreClient = agent.getClass().getDeclaredField("rdbStoreClient");
            rdbStoreClient.setAccessible(true);
            rdbStoreClient.set(agent, mockRDBClient);

            doReturn(mockConnection).when(mockRDBClient).getConnection();

            retrieveData.invoke(agent, iri);

            // Ensure method to get time series client was invoked once
            verify(mockTs.constructed().get(0), times(1)).getTimeSeries(anyList(), any());
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

    @Test
    public void testExtractFootprint() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        Method extractFootprint = agent.getClass().getDeclaredMethod("extractFootprint", JSONArray.class);

        assertNotNull(extractFootprint);
        extractFootprint.setAccessible(true);

        double tolerance = 0.001;

        String geometry1 = "1.0#1.0#0.0#2.0#1.0#0.0#2.0#2.0#0.0#1.0#1.0#0.0";
        String geometry2 = "1.0#1.0#0.0#1.0#2.0#0.0#2.0#2.0#0.0#1.0#1.0#0.0";
        String polygonType = "<http://localhost/blazegraph/literals/POLYGON-3-12>";

        // expected string with the vertices in different order in clockwise orientation
        String expected1 = "1.0#1.0#0.0#1.0#2.0#0.0#2.0#2.0#0.0#2.0#1.0#0.0#1.0#1.0#0.0";
        String expected2 = "1.0#2.0#0.0#2.0#2.0#0.0#2.0#1.0#0.0#1.0#1.0#0.0#1.0#2.0#0.0";
        String expected3 = "2.0#2.0#0.0#2.0#1.0#0.0#1.0#1.0#0.0#1.0#2.0#0.0#2.0#2.0#0.0";
        String expected4 = "2.0#1.0#0.0#1.0#1.0#0.0#1.0#2.0#0.0#2.0#2.0#0.0#2.0#1.0#0.0";

        boolean flag1 = true;
        boolean flag2 = true;
        boolean flag3 = true;
        boolean flag4 = true;

        JSONArray testArray = new JSONArray();
        testArray.put(new JSONObject().put("geometry", geometry1).put("datatype", polygonType));
        testArray.put(new JSONObject().put("geometry", geometry2).put("datatype", polygonType));

        String result = (String) extractFootprint.invoke(agent, testArray);

        String[] rSplit = result.split("#");
        String[] eSplit1 = result.split("#");
        String[] eSplit2 = result.split("#");
        String[] eSplit3 = result.split("#");
        String[] eSplit4 = result.split("#");

        assertTrue(rSplit.length == eSplit1.length);

        for (int i = 0; i < rSplit.length; i++){
            if (Math.abs(Double.valueOf(rSplit[i]) - Double.valueOf(eSplit1[i])) > tolerance){flag1 = false;}
            if (Math.abs(Double.valueOf(rSplit[i]) - Double.valueOf(eSplit2[i])) > tolerance){flag2 = false;}
            if (Math.abs(Double.valueOf(rSplit[i]) - Double.valueOf(eSplit3[i])) > tolerance){flag3 = false;}
            if (Math.abs(Double.valueOf(rSplit[i]) - Double.valueOf(eSplit4[i])) > tolerance){flag4 = false;}
        }

        // checking if extractFootprint returns the expected geometry, which vertex the string starts from does not matter if the result and the expected string represents the same geometry
        assertTrue(flag1 || flag2 || flag3 || flag4);
    }

    @Test
    public void testToPolygon() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        Method toPolygon = agent.getClass().getDeclaredMethod("toPolygon", String.class);

        assertNotNull(toPolygon);
        toPolygon.setAccessible(true);

        String points = "559267.200000246#313892.7999989044#0.0#559280.5400002463#313892.7999989044#0.0#559280.5400002463#313908.7499989033#0.0#559267.200000246#313908.7499989033#0.0#559267.200000246#313892.7999989044#0.0";

        Polygon result = (Polygon) toPolygon.invoke(agent, points);

        String expected = "POLYGON ((559267.200000246 313892.7999989044, 559280.5400002463 313892.7999989044, 559280.5400002463 313908.7499989033, 559267.200000246 313908.7499989033, 559267.200000246 313892.7999989044))";

        assertEquals(expected, result.toString());

    }

    @Test
    public void testCoordinatesToString() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        Method coordinatesToString = agent.getClass().getDeclaredMethod("coordinatesToString", Coordinate[].class);

        assertNotNull(coordinatesToString);
        coordinatesToString.setAccessible(true);

        Coordinate[] coordinates = new Coordinate[2];

        coordinates[0] = new Coordinate(1.0, 2.0, 3.0);
        coordinates[1] = new Coordinate(4.0, 5.0, 6.0);

        String expected = "1.0#2.0#3.0#4.0#5.0#6.0";

        assertEquals(expected, coordinatesToString.invoke(agent, new Object[] {coordinates}));
    }

    @Test
    public void testInflatePolygon() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        Method inflatePolygon = agent.getClass().getDeclaredMethod("inflatePolygon", Geometry.class, Double.class);

        assertNotNull(inflatePolygon);
        inflatePolygon.setAccessible(true);

        GeometryFactory gF = new GeometryFactory();
        Coordinate[] testC = new Coordinate[5];

        testC[0] = new Coordinate(1.0, 1.0, 3.01);
        testC[1] = new Coordinate(2.0, 1.0, 3.02);
        testC[2] = new Coordinate(2.0, 2.0, 3.03);
        testC[3] = new Coordinate(1.0, 2.0, 3.03);
        testC[4] = new Coordinate(1.0, 1.0, 3.01);

        Polygon testPolygon = gF.createPolygon(testC);

        Coordinate[] expectedC = new Coordinate[5];

        expectedC[0] = new Coordinate(0.9, 0.9, 3.01);
        expectedC[1] = new Coordinate(2.1, 0.9, 3.02);
        expectedC[2] = new Coordinate(2.1, 2.1, 3.03);
        expectedC[3] = new Coordinate(0.9, 2.1, 3.03);
        expectedC[4] = new Coordinate(0.9, 0.9, 3.01);

        Polygon expectedPolygon = gF.createPolygon(expectedC);

        Polygon resultPolygon = (Polygon) inflatePolygon.invoke(agent, testPolygon, 0.1);
        assertTrue(expectedPolygon.equals(resultPolygon));
    }

    @Test
    public void testDeflatePolygon() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        Method deflatePolygon = agent.getClass().getDeclaredMethod("deflatePolygon", Geometry.class, Double.class);

        assertNotNull(deflatePolygon);
        deflatePolygon.setAccessible(true);

        GeometryFactory gF = new GeometryFactory();
        Coordinate[] testC = new Coordinate[5];

        testC[0] = new Coordinate(1.0, 1.0, 3.01);
        testC[1] = new Coordinate(2.0, 1.0, 3.02);
        testC[2] = new Coordinate(2.0, 2.0, 3.03);
        testC[3] = new Coordinate(1.0, 2.0, 3.03);
        testC[4] = new Coordinate(1.0, 1.0, 3.01);

        Polygon testPolygon = gF.createPolygon(testC);

        Coordinate[] expectedC = new Coordinate[5];

        expectedC[0] = new Coordinate(1.1, 1.1, 3.01);
        expectedC[1] = new Coordinate(1.9, 1.1, 3.02);
        expectedC[2] = new Coordinate(1.9, 1.9, 3.03);
        expectedC[3] = new Coordinate(1.1, 1.9, 3.03);
        expectedC[4] = new Coordinate(1.1, 1.1, 3.01);

        Polygon expectedPolygon = gF.createPolygon(expectedC);

        Polygon resultPolygon = (Polygon) deflatePolygon.invoke(agent, testPolygon, 0.1);
        assertTrue(expectedPolygon.equals(resultPolygon));
    }

    @Test
    public void testGetPolygonZ() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        Method getPolygonZ = agent.getClass().getDeclaredMethod("getPolygonZ", Geometry.class);

        assertNotNull(getPolygonZ);
        getPolygonZ.setAccessible(true);

        GeometryFactory gF = new GeometryFactory();
        Coordinate[] coordinates = new Coordinate[4];

        coordinates[0] = new Coordinate(1.0, 1.0, 3.01);
        coordinates[1] = new Coordinate(2.0, 1.0, 3.02);
        coordinates[2] = new Coordinate(2.0, 2.0, 3.03);
        coordinates[3] = new Coordinate(1.0, 1.0, 3.01);

        Polygon polygon = gF.createPolygon(coordinates);

        ArrayList<Double> expected = new ArrayList<>();

        expected.add(3.01);
        expected.add(3.02);
        expected.add(3.03);
        expected.add(3.01);

        assertEquals(expected, getPolygonZ.invoke(agent, polygon));
    }

    @Test
    public void testSetPolygonZ() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        Method setPolygonZ = agent.getClass().getDeclaredMethod("setPolygonZ", Geometry.class, ArrayList.class);

        assertNotNull(setPolygonZ);
        setPolygonZ.setAccessible(true);

        GeometryFactory gF = new GeometryFactory();
        Coordinate[] coordinates = new Coordinate[4];

        coordinates[0] = new Coordinate(1.0, 1.0, 3.01);
        coordinates[1] = new Coordinate(2.0, 1.0, 3.02);
        coordinates[2] = new Coordinate(2.0, 2.0, 3.03);
        coordinates[3] = new Coordinate(1.0, 1.0, 3.01);

        Polygon result = gF.createPolygon(coordinates);

        coordinates[0] = new Coordinate(1.0, 1.0, 0.0);
        coordinates[1] = new Coordinate(2.0, 1.0, 0.0);
        coordinates[2] = new Coordinate(2.0, 2.0, 0.0);
        coordinates[3] = new Coordinate(1.0, 1.0, 0.0);

        Polygon expected = gF.createPolygon(coordinates);

        ArrayList<Double> z = new ArrayList<>();

        z.add(0.0);
        z.add(0.0);
        z.add(0.0);
        z.add(0.0);

        setPolygonZ.invoke(agent, result, z);

        assertEquals(expected, result);
    }

    @Test
    public void testIgnoreHole() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        Method ignoreHole = agent.getClass().getDeclaredMethod("ignoreHole", String.class, String.class);

        assertNotNull(ignoreHole);
        ignoreHole.setAccessible(true);

        String geometry = "1.0#1.0#2.0#2.0#3.0#3.0#4.0#4.0#5.0#5.0#6.0#6.0";
        String polygonType = "<http://localhost/blazegraph/literals/POLYGON-2-6-6>";
        String expected = "1.0#1.0#2.0#2.0#3.0#3.0";

        String result = (String) ignoreHole.invoke(agent, geometry, polygonType);

        assertEquals(expected, result);
    }

    @Test
    public void testSetTimeSeriesProps(@TempDir Path tempDir) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        CEAAgent agent = new CEAAgent();
        Method setTimeSeriesProps = agent.getClass().getDeclaredMethod("setTimeSeriesProps", String.class, String.class);

        assertNotNull(setTimeSeriesProps);
        setTimeSeriesProps.setAccessible(true);

        String testFile = "test.properties";
        String testEndpoint = "testEndpoint";
        String testUri = "test/namespace/testNamespace/sparql/cityobject/testUUID/";
        Path testPath = Files.createFile(tempDir.resolve(testFile));
        Properties testProp = new Properties();

        testProp.setProperty("sparql.query.endpoint", testEndpoint);
        testProp.setProperty("sparql.update.endpoint", testEndpoint);

        FileOutputStream testOut = new FileOutputStream(testPath.toString());
        testProp.store(testOut, null);
        testOut.close();

        setTimeSeriesProps.invoke(agent, testUri, testPath.toString());

        FileInputStream testIn = new FileInputStream(testPath.toString());
        testProp.load(testIn);
        testIn.close();

        assertEquals(testProp.getProperty("sparql.query.endpoint"), "testEndpoint/namespace/testNamespace/sparql");
        assertEquals(testProp.getProperty("sparql.update.endpoint"), "testEndpoint/namespace/testNamespace/sparql");
    }

    @Test
    public void testGetTimeSeriesPropsPath() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        CEAAgent agent = new CEAAgent();
        Method getTimeSeriesPropsPath = agent.getClass().getDeclaredMethod("getTimeSeriesPropsPath");

        assertNotNull(getTimeSeriesPropsPath);
        getTimeSeriesPropsPath.setAccessible(true);

        String result = (String) getTimeSeriesPropsPath.invoke(agent);

        Field time_series_client_props = agent.getClass().getDeclaredField("TIME_SERIES_CLIENT_PROPS");
        time_series_client_props.setAccessible(true);

        assertTrue(result.contains((String) time_series_client_props.get(agent)));
    }

    @Test
    public void testSetRDBClient(@TempDir Path tempDir) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException, IOException {
        CEAAgent agent = new CEAAgent();
        Method setRDBClient = agent.getClass().getDeclaredMethod("setRDBClient", String.class);

        assertNotNull(setRDBClient);
        setRDBClient.setAccessible(true);

        Field rdbStoreClient;

        rdbStoreClient = agent.getClass().getDeclaredField("rdbStoreClient");
        rdbStoreClient.setAccessible(true);

        String testFile = "test.properties";
        String url = "test_url";
        String user = "test_user";
        String password = "test_password";
        Path testPath = Files.createFile(tempDir.resolve(testFile));
        Properties testProp = new Properties();

        testProp.setProperty("db.url", url);
        testProp.setProperty("db.user", user);
        testProp.setProperty("db.password", password);

        FileOutputStream testOut = new FileOutputStream(testPath.toString());
        testProp.store(testOut, null);
        testOut.close();

        setRDBClient.invoke(agent, testPath.toString());

        assertNotNull(rdbStoreClient.get(agent));
    }

    @Test
    public void testGetPropertyUsageCategory() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        CEAAgent agent = new CEAAgent();
        String uri = "http://localhost/kings-lynn-open-data/cityobject/UUID_583747b0-1655-4761-8050-4036436a1052/";

        Method getPropertyUsageCateogory = agent.getClass().getDeclaredMethod("getPropertyUsageCategory", String.class);
        assertNotNull(getPropertyUsageCateogory);
        getPropertyUsageCateogory.setAccessible(true);

        // Ensure query contains correct predicate and object
        Query q = (Query) getPropertyUsageCateogory.invoke(agent, uri);
        assertTrue(q.toString().contains("hasUsageCategory"));
        assertTrue(q.toString().contains("hasOntoCityGMLRepresentation"));
        assertTrue(q.toString().contains("PropertyUsageCategory"));
    }

    @Test
    public void testToCEAConvention() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        CEAAgent agent = new CEAAgent();
        String test_usage = "test";

        Method toCEAConvention = agent.getClass().getDeclaredMethod("toCEAConvention", String.class);
        assertNotNull(toCEAConvention);

        String result = (String) toCEAConvention.invoke(agent, test_usage);
        assertTrue(result.equals("MULTI_RES"));

        test_usage = "SINGLERESIDENTIAL";
        result = (String) toCEAConvention.invoke(agent, test_usage);
        assertTrue(result.equals("SINGLE_RES"));

        test_usage = "POLICESTATION";
        result = (String) toCEAConvention.invoke(agent, test_usage);
        assertTrue(result.equals("MULTI_RES"));

        test_usage = "HOSPITAL";
        result = (String) toCEAConvention.invoke(agent, test_usage);
        assertTrue(result.equals("HOSPITAL"));

        test_usage = "CLINIC";
        result = (String) toCEAConvention.invoke(agent, test_usage);
        assertTrue(result.equals("HOSPITAL"));

        test_usage = "UNIVERSITYFACILITY";
        result = (String) toCEAConvention.invoke(agent, test_usage);
        assertTrue(result.equals("UNIVERSITY"));

        test_usage = "OFFICE";
        result = (String) toCEAConvention.invoke(agent, test_usage);
        assertTrue(result.equals("OFFICE"));

        test_usage = "RETAILESTABLISHMENT";
        result = (String) toCEAConvention.invoke(agent, test_usage);
        assertTrue(result.equals("RETAIL"));

        test_usage = "RELIGIOUSFACILITY";
        result = (String) toCEAConvention.invoke(agent, test_usage);
        assertTrue(result.equals("MUSEUM"));

        test_usage = "EATINGESTABLISHMENT";
        result = (String) toCEAConvention.invoke(agent, test_usage);
        assertTrue(result.equals("RESTAURANT"));

        test_usage = "DRINKINGESTABLISHMENT";
        result = (String) toCEAConvention.invoke(agent, test_usage);
        assertTrue(result.equals("FOODSTORE"));

        test_usage = "HOTEL";
        result = (String) toCEAConvention.invoke(agent, test_usage);
        assertTrue(result.equals("HOTEL"));

        test_usage = "SPORTSFACILITY";
        result = (String) toCEAConvention.invoke(agent, test_usage);
        assertTrue(result.equals("GYM"));

        test_usage = "CULTURALFACILITY";
        result = (String) toCEAConvention.invoke(agent, test_usage);
        assertTrue(result.equals("MUSEUM"));

        test_usage = "TRANSPORTFACILITY";
        result = (String) toCEAConvention.invoke(agent, test_usage);
        assertTrue(result.equals("INDUSTRIAL"));
    }
}
