package uk.ac.cam.cares.twa.cities.ceaagent;

import junit.framework.TestCase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest({CEAAgent.class})
@PowerMockIgnore({"javax.management.*", "javax.script.*"})
public class CEAAgentTest extends TestCase {
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

    @Test
    public void testCEAAgentMethods() {
        CEAAgent agent = new CEAAgent();
        assertEquals(29, agent.getClass().getDeclaredMethods().length);
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

        List<Double> result = (List<Double>) getTimesList.invoke(agent, requestParams, "test_key_1" );
        assertEquals(test_list_1, result);

    }

    @Test
    public void testRunCEA() throws Exception {
        ThreadPoolExecutor executor = mock(ThreadPoolExecutor.class);

        CEAAgent agent = new CEAAgent();
        Method runCEA = agent.getClass().getDeclaredMethod("runCEA", ArrayList.class, ArrayList.class, Integer.class);
        assertNotNull(runCEA);
        runCEA.setAccessible(true);

        Field CEAExecutor = agent.getClass().getDeclaredField("CEAExecutor");
        CEAExecutor.setAccessible(true);
        CEAExecutor.set(agent, executor);

        Field targetUrl = agent.getClass().getDeclaredField("targetUrl");
        targetUrl.setAccessible(true);
        targetUrl.set(agent, "test");

        ArrayList<CEAInputData> testData = new ArrayList<CEAInputData>();
        testData.add(new CEAInputData("test","test"));
        ArrayList<String> testArray = new ArrayList<>();
        testArray.add("testUri");
        Integer test_thread = 0;

        RunCEATask task = mock(RunCEATask.class);
        PowerMockito.whenNew(RunCEATask.class).withAnyArguments().thenReturn(task);

        runCEA.invoke(agent, testData, testArray, test_thread);
        verify(executor, times(1)).execute(task);
    }

    @Test
    public void testCreateTimeSeries() throws Exception {
        TimeSeriesClient<OffsetDateTime> client = mock(TimeSeriesClient.class);
        PowerMockito.whenNew(TimeSeriesClient.class).withAnyArguments().thenReturn(client);

        CEAAgent agent = new CEAAgent();
        Method createTimeSeries = agent.getClass().getDeclaredMethod("createTimeSeries", String.class, List.class);
        assertNotNull(createTimeSeries);
        createTimeSeries.setAccessible(true);

        List<LinkedHashMap<String,String>> fixedIris = new ArrayList<>();
        String prefix = "http://127.0.0.1:9999/blazegraph/namespace/kings-lynn-open-data/sparql/";
        String testUri = prefix+ "cityobject/UUID_test/";

        createTimeSeries.invoke(agent, testUri, fixedIris);

        Field TIME_SERIES = agent.getClass().getDeclaredField("TIME_SERIES");
        List<String> time_series_strings = (List<String>) TIME_SERIES.get(agent);

        for(String time_series: time_series_strings){
            assertTrue(fixedIris.get(0).get(time_series).contains(prefix+ "energyprofile/"+time_series));
        }
        verify(client, times(1)).initTimeSeries(anyList(), anyList(), anyString());
    }

    @Test
    public void testAddDataToTimeSeries() throws Exception {
        TimeSeriesClient<OffsetDateTime> client = mock(TimeSeriesClient.class);
        PowerMockito.whenNew(TimeSeriesClient.class).withAnyArguments().thenReturn(client);

        CEAAgent agent = new CEAAgent();
        Method addDataToTimeSeries = agent.getClass().getDeclaredMethod("addDataToTimeSeries", List.class, List.class, LinkedHashMap.class);
        assertNotNull(addDataToTimeSeries);
        addDataToTimeSeries.setAccessible(true);

        LinkedHashMap<String,String> iris = new LinkedHashMap<>();
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

        verify(client, times(1)).getMaxTime(anyString());
        verify(client, times(1)).getMinTime(anyString());
        verify(client, times(1)).addTimeSeriesData(any());
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

        Boolean result = (Boolean) timeSeriesExist.invoke(agent, iris);
        verify(client, times(1)).checkDataHasTimeSeries(anyString());

        assertFalse(result);
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testGetValue() throws Exception {
        CEAAgent agent = PowerMockito.spy(new CEAAgent());

        String uriString = "test";
        String value = "Height";
        String result = "5.2";

        Query dummy  = mock(Query.class);

        doReturn(new JSONArray("[{'"+value+"': '"+result+"'}]")).when(agent).queryStore(anyString(), anyString());

        PowerMockito.doReturn(dummy).when(agent, "getQuery", uriString, value);

        Method getValue = agent.getClass().getDeclaredMethod("getValue", String.class, String.class);
        assertNotNull(getValue);
        getValue.setAccessible(true);

        assertEquals( result, getValue.invoke(agent, uriString, value));
    }

    @Test
    public void testGetFootprint() throws Exception {
        CEAAgent agent = new CEAAgent();
        String geom1 = "559267.200000246#313892.7999989044#1.7#559280.5400002463#313892.7999989044#1.7#559280.5400002463#313908.7499989033#6.7#559267.200000246#313908.7499989033#6.7#559267.200000246#313892.7999989044#1.7";
        String geom2 = "559267.200000246#313892.7999989044#1.7#559280.5400002463#313892.7999989044#1.7#559280.5400002463#313908.7499989033#1.7#559267.200000246#313908.7499989033#1.7#559267.200000246#313892.7999989044#1.7";
        String geom3 = "559267.200000246#313892.7999989044#0.0#559280.5400002463#313892.7999989044#0.0#559280.5400002463#313908.7499989033#0.0#559267.200000246#313908.7499989033#0.0#559267.200000246#313892.7999989044#0.0";

        JSONArray results = new JSONArray("[{'Envelope': '"+geom1+"'}, {'Envelope': '"+geom2+"'},{'Envelope': '"+geom3+"'}]");

        Method getFootprint = agent.getClass().getDeclaredMethod("getFootprint", JSONArray.class);
        assertNotNull(getFootprint);
        getFootprint.setAccessible(true);

        assertEquals(geom3, getFootprint.invoke(agent, results));

    }

    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testSparqlGenAttributeUpdate() throws Exception {
        CEAAgent agent = spy(new CEAAgent());
        Method sparqlGenAttributeUpdate = agent.getClass().getDeclaredMethod("sparqlGenAttributeUpdate", String.class, String.class);
        assertNotNull(sparqlGenAttributeUpdate);

        String uriString = "http://localhost/kings-lynn-open-data/cityobject/UUID_test/";
        String energyProfileString = "http://localhost/kings-lynn-open-data/energyprofile/UUID_test/";

        doNothing().when(agent).updateStore(anyString(), anyString());
        sparqlGenAttributeUpdate.invoke(agent, uriString, energyProfileString);

        verify(agent, times(1)).updateStore(anyString(), anyString());
    }

    @Test
    public void testSparqlUpdate() throws Exception {
        CEAAgent agent = spy(new CEAAgent());
        Method sparqlUpdate = agent.getClass().getDeclaredMethod("sparqlUpdate", String.class, String.class, String.class, String.class, String.class, LinkedHashMap.class, String.class);
        assertNotNull(sparqlUpdate);

        LinkedHashMap<String,String> iris_mock = mock(LinkedHashMap.class);
        when(iris_mock.get(anyString())).thenReturn("test");

        String uriString = "http://localhost/kings-lynn-open-data/cityobject/UUID_test/";
        String expected = "http://localhost/kings-lynn-open-data/energyprofile/";

        doNothing().when(agent).updateStore(anyString(), anyString());
        String result = (String) sparqlUpdate.invoke(agent, "test", "test", "test", "test", "test", iris_mock, uriString );

        assertTrue( result.contains(expected));

        verify(agent, times(1)).updateStore(anyString(), anyString());
    }

    @Test
    public void testGetDataIri() throws Exception {
        CEAAgent agent = PowerMockito.spy(new CEAAgent());

        String uriString = "test";
        String test_value = "PV_area_roof";
        String measure = "measure";
        String test_measure = "35.2";
        String unit = "unit";
        String test_unit = "m^2";


        doReturn(new JSONArray("[{'"+measure+"': '"+test_measure+"', '"+unit+"': '"+test_unit+"'}]")).when(agent).queryStore(anyString(), anyString());
        PowerMockito.doReturn("testGraph").when(agent, "getGraph", anyString(), anyString());

        Method getDataIRI = agent.getClass().getDeclaredMethod("getDataIRI", String.class, String.class);
        assertNotNull(getDataIRI);
        getDataIRI.setAccessible(true);

        ArrayList<String> result = (ArrayList<String>) getDataIRI.invoke(agent, uriString, test_value);

        assertTrue(result.contains(test_measure));
        assertTrue(result.contains(test_unit));
    }

    @Test
    public void testCheckGenAttributeInitialised() throws Exception {
        CEAAgent agent = PowerMockito.spy(new CEAAgent());

        String uriString = "test";
        String measure = "energyProfileBuilding";
        String test_measure = "test_uri";
        String graph = "test_graph";

        doReturn(new JSONArray("[{'"+measure+"': '"+test_measure+"'}]")).when(agent).queryStore(anyString(), anyString());
        PowerMockito.doReturn(graph).when(agent, "getGraph", anyString(), anyString());

        Method checkGenAttributeInitialised = agent.getClass().getDeclaredMethod("checkGenAttributeInitialised", String.class);
        assertNotNull(checkGenAttributeInitialised);
        checkGenAttributeInitialised.setAccessible(true);

        String result = (String) checkGenAttributeInitialised.invoke(agent, uriString);

        assertTrue(result.equals(test_measure));
    }

    @Test
    public void testCheckBlazegraphAndTimeSeriesInitialised() throws Exception {
        CEAAgent agent = PowerMockito.spy(new CEAAgent());

        String uriString = "test";
        List<LinkedHashMap<String,String>> fixedIris = new ArrayList<>();
        String measure = "grid_demand";
        String test_measure = "test_uri";
        String graph = "test_graph";
        String building = "test_building_uri";

        doReturn(new JSONArray("[{'"+measure+"': '"+test_measure+"'}]")).when(agent).queryStore(anyString(), anyString());
        PowerMockito.doReturn(graph).when(agent, "getGraph", anyString(), anyString());
        PowerMockito.doReturn(building).when(agent, "checkGenAttributeInitialised", anyString());

        Method checkBlazegraphAndTimeSeriesInitialised = agent.getClass().getDeclaredMethod("checkBlazegraphAndTimeSeriesInitialised", String.class, List.class);
        assertNotNull(checkBlazegraphAndTimeSeriesInitialised);
        checkBlazegraphAndTimeSeriesInitialised.setAccessible(true);

        String result = (String) checkBlazegraphAndTimeSeriesInitialised.invoke(agent, uriString, fixedIris);

        assertTrue(result.equals(building));
        assertTrue(fixedIris.get(0).containsKey(measure));
        assertTrue(fixedIris.get(0).containsValue(test_measure));
    }

    @Test
    public void testGetUnit() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CEAAgent agent = new CEAAgent();
        Method getUnit = agent.getClass().getDeclaredMethod("getUnit", String.class);
        assertNotNull(getUnit);

        String test_kwh = "http://www.ontology-of-units-of-measure.org/resource/om-2/kilowattHour";
        String test_m2 = "http://www.ontology-of-units-of-measure.org/resource/om-2/squareMetre";
        String test_other = "test";

        assertEquals(getUnit.invoke(agent, test_kwh), "kWh");
        assertEquals(getUnit.invoke(agent, test_m2), "m^2");
        assertEquals(getUnit.invoke(agent, test_other), "");
    }

    @Test
    public void testRetrieveData() throws Exception {
        TimeSeriesClient<OffsetDateTime> client = mock(TimeSeriesClient.class);
        PowerMockito.whenNew(TimeSeriesClient.class).withAnyArguments().thenReturn(client);

        CEAAgent agent = new CEAAgent();
        Method retrieveData = agent.getClass().getDeclaredMethod("retrieveData", String.class);
        assertNotNull(retrieveData);

        String iri = "test";
        List<String> iris = new ArrayList<>();
        iris.add(iri);

        retrieveData.invoke(agent, iri);

        verify(client, times(1)).getTimeSeries(iris);

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

        List<List<?>> values = new ArrayList<>();
        List<Double> test_list_1 = new ArrayList<>();
        test_list_1.add(1.687);
        test_list_1.add(2.141);
        List<Double> test_list_2 = new ArrayList<>();
        test_list_2.add(3.621);
        test_list_2.add(4.7);
        values.add(test_list_1);
        values.add(test_list_2);

        List<OffsetDateTime> times = new ArrayList<>();
        times.add(OffsetDateTime.now());
        times.add(OffsetDateTime.now());
        TimeSeries<OffsetDateTime> timeSeries = new TimeSeries<>(times, iris, values);

        assertEquals(calculateAnnual.invoke(agent, timeSeries, iri1), "3.83");
        assertEquals(calculateAnnual.invoke(agent, timeSeries, iri2), "8.32");
    }
}