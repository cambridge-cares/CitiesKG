package uk.ac.cam.cares.twa.cities.agents.geo.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import org.apache.http.client.methods.HttpPost;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import uk.ac.cam.cares.jps.base.http.Http;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.agents.geo.CityInformationAgent;
import uk.ac.cam.cares.ogm.models.ModelContext;
import uk.ac.cam.cares.twa.cities.model.geo.CityObject;


import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.ResourceBundle;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class CityInformationAgentTest {

    @Test
    public void testNewCityInformationAgent() {
        CityInformationAgent agent;

        try {
            agent = new CityInformationAgent();
            assertNotNull(agent);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testNewCityInformationAgentFields() {
        CityInformationAgent agent = new CityInformationAgent();

        assertEquals(35, agent.getClass().getDeclaredFields().length);

        try {
            assertEquals("/cityobjectinformation", agent.getClass().getDeclaredField("URI_CITY_OBJECT_INFORMATION").get(agent));
            assertEquals("method", agent.getClass().getDeclaredField("KEY_REQ_METHOD").get(agent));
            assertEquals("iris", agent.getClass().getDeclaredField("KEY_IRIS").get(agent));
            assertEquals("context", agent.getClass().getDeclaredField("KEY_CONTEXT").get(agent));
            assertEquals("cityobjectinformation", agent.getClass().getDeclaredField("KEY_CITY_OBJECT_INFORMATION").get(agent));

            // test readConfig
            Field route = agent.getClass().getDeclaredField("route");
            route.setAccessible(true);
            assertEquals(ResourceBundle.getBundle("CKGAgentConfig").getString("uri.route"), route.get(agent));
            Field lazyload = agent.getClass().getDeclaredField("lazyload");
            lazyload.setAccessible(true);
            assertEquals(Boolean.getBoolean(ResourceBundle.getBundle("CKGAgentConfig").getString("loading.status")), lazyload.get(agent));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    public void testNewCityInformationAgentMethods() {
        CityInformationAgent agent = new CityInformationAgent();

        assertEquals(22, agent.getClass().getDeclaredMethods().length);
    }

    @Test
    public void testProcessRequestParameters() {
        CityInformationAgent agent = new CityInformationAgent();
        JSONObject requestParams = new JSONObject();

        // set lazyload to true for mocking purposes
        try {
            Field lazyload = agent.getClass().getDeclaredField("lazyload");
            lazyload.setAccessible(true);
            lazyload.set(agent, true);
        } catch (Exception e) {
            fail();
        }

        // test case when request parameters are empty, validateInput should throw BadRequestException
        try {
            agent.processRequestParameters(requestParams);
        } catch (Exception e) {
            assertTrue(e instanceof BadRequestException);
        }

        // tests for sending iris to agent
        // test case when iris are empty, should return empty cityObjectInformation json array
        requestParams.put(CityInformationAgent.KEY_REQ_METHOD, HttpMethod.POST);
        JSONArray iris = new JSONArray();
        requestParams.put(CityInformationAgent.KEY_IRIS, iris);
        try {
            assertTrue(agent.processRequestParameters(requestParams).getJSONArray(CityInformationAgent.KEY_CITY_OBJECT_INFORMATION).getJSONArray(0).isEmpty());
        } catch (Exception e) {
            fail();
        } finally {
            requestParams.remove(CityInformationAgent.KEY_CITY_OBJECT_INFORMATION);
        }

        // test case when route is not overridden
        iris.put("http://www.theworldavatar.com:83/citieskg/namespace/example/sparql/cityobject/UUID_1/");
        try (MockedConstruction<ModelContext> modelContext = Mockito.mockConstruction(ModelContext.class, (mock, context) -> {
            when(mock.createHollowModel(any(Class.class), anyString())).thenReturn(new CityObject());
            doNothing().when(mock).pullAll(any(CityObject.class));
        })) {
            Field route = agent.getClass().getDeclaredField("route");
            route.setAccessible(true);
            agent.processRequestParameters(requestParams);
            assertEquals(ResourceBundle.getBundle("CKGAgentConfig").getString("uri.route"), route.get(agent));
        } catch (Exception e) {
            fail();
        } finally {
            requestParams.remove(CityInformationAgent.KEY_CITY_OBJECT_INFORMATION);
        }

        // test case when route is overridden
        iris.remove(0);
        iris.put("http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG24500/sparql/cityobject/UUID_1/");
        try (MockedConstruction<ModelContext> modelContext = Mockito.mockConstruction(ModelContext.class, (mock, context) -> {
            when(mock.createHollowModel(any(Class.class), anyString())).thenReturn(new CityObject());
            doNothing().when(mock).pullAll(ArgumentMatchers.any(CityObject.class));
        })) {
            Field route = agent.getClass().getDeclaredField("route");
            route.setAccessible(true);
            agent.processRequestParameters(requestParams);
            assertEquals("singaporeEPSG24500", route.get(agent));
        } catch (Exception e) {
            fail();
        } finally {
            requestParams.remove(CityInformationAgent.KEY_CITY_OBJECT_INFORMATION);
        }

        // test case when lazyload is true
        try (MockedConstruction<ModelContext> modelContext = Mockito.mockConstruction(ModelContext.class, (mock, context) -> {
            when(mock.createHollowModel(any(Class.class), anyString())).thenReturn(new CityObject());
            Mockito.doNothing().when(mock).pullAll(ArgumentMatchers.any(CityObject.class));
        })) {
            JSONObject response = agent.processRequestParameters(requestParams);
            verify(modelContext.constructed().get(0), times(1)).pullAll(any(CityObject.class));
            assertEquals(1, response.getJSONArray(CityInformationAgent.KEY_CITY_OBJECT_INFORMATION).getJSONArray(0).getJSONArray(0).length());
        } catch (Exception e) {
            fail();
        } finally {
            requestParams.remove(CityInformationAgent.KEY_CITY_OBJECT_INFORMATION);
        }

        // test case when lazyload is false
        try (MockedConstruction<ModelContext> modelContext = Mockito.mockConstruction(ModelContext.class, (mock, context) -> {
            when(mock.createHollowModel(any(Class.class), anyString())).thenReturn(new CityObject());
            Mockito.doNothing().when(mock).recursivePullAll(any(CityObject.class), anyInt());
        })) {
            Field lazyload = agent.getClass().getDeclaredField("lazyload");
            lazyload.setAccessible(true);
            lazyload.set(agent, false);
            JSONObject response =  agent.processRequestParameters(requestParams);
            verify(modelContext.constructed().get(0), times(1)).recursivePullAll(any(CityObject.class), anyInt());
            assertEquals(1, response.getJSONArray(CityInformationAgent.KEY_CITY_OBJECT_INFORMATION).getJSONArray(0).getJSONArray(0).length());
        } catch (Exception e) {
            fail();
        } finally {
            requestParams.remove(CityInformationAgent.KEY_CITY_OBJECT_INFORMATION);
        }

        // tests for sending context to agent
        // test case when agent iri is not valid, should return BadRequestException
        JSONObject otherAgents = new JSONObject();
        HashMap<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        otherAgents.put("testAgentIri", map);
        requestParams.put(CityInformationAgent.KEY_CONTEXT, otherAgents);
        try {
            agent.processRequestParameters(requestParams);
        } catch (Exception e) {
            assertTrue(e instanceof BadRequestException);
        }

        // test case when agent successfully sends request to another agent
        otherAgents.remove("testAgentIri");
        otherAgents.put("http://www.theworldavatar.com:83/citieskg/otheragentIRI", map);
        try (MockedConstruction<ModelContext> modelContext = Mockito.mockConstruction(ModelContext.class, (mock, context) -> {
            when(mock.createHollowModel(any(Class.class), anyString())).thenReturn(new CityObject());
            Mockito.doNothing().when(mock).recursivePullAll(any(CityObject.class), anyInt());
        })) {
            try (MockedStatic<Http> http = Mockito.mockStatic(Http.class, CALLS_REAL_METHODS)) {
                http.when(() -> Http.execute(ArgumentMatchers.any(HttpPost.class))).thenReturn("{otherAgentKey: result}");
                JSONObject response = agent.processRequestParameters(requestParams);
                assertEquals("result", response
                        .getJSONArray("http://www.theworldavatar.com:83/citieskg/otheragentIRI")
                        .getJSONObject(0).get("otherAgentKey"));
                assertEquals(1, response.getJSONArray(CityInformationAgent.KEY_CITY_OBJECT_INFORMATION).getJSONArray(0).getJSONArray(0).length());
            }
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testValidateInput() {
        CityInformationAgent agent = new CityInformationAgent();
        JSONObject requestParams = new JSONObject();

        // test case where request params is empty
        try {
            agent.validateInput(requestParams);
        } catch (Exception e) {
            assertEquals(BadRequestException.class, e.getClass());
        }

        // test case where keys have KEY_REQ_METHOD but no KEY_IRIS
        requestParams.put(CityInformationAgent.KEY_REQ_METHOD, HttpMethod.GET);
        try {
            agent.validateInput(requestParams);
        } catch (Exception e) {
            assertEquals(BadRequestException.class, e.getClass());
        }

        // test case where KEY_REQ_METHOD is not POST
        JSONArray iris = new JSONArray();
        iris.put(0, "test");
        requestParams.put(CityInformationAgent.KEY_IRIS, iris);
        try {
            agent.validateInput(requestParams);
        } catch (Exception e) {
            assertEquals(BadRequestException.class, e.getClass());
        }

        // test case where iri is not a valid URL
        requestParams.put(CityInformationAgent.KEY_REQ_METHOD, HttpMethod.POST);
        try {
            agent.validateInput(requestParams);
        } catch (Exception e) {
            assertEquals(BadRequestException.class, e.getClass());
        }

        // test case where request params with iris pass
        iris.remove(0);
        iris.put(0, "http://localhost:9999/blazegraph/namespace/berlin/sparql/cityobject/UUID_123/");
        requestParams.put(CityInformationAgent.KEY_IRIS, iris);
        try {
            assertTrue(agent.validateInput(requestParams));
        } catch (Exception e) {
            fail();
        }

        // test case where agent iri is not a valid URL
        JSONObject agentIri = new JSONObject();
        agentIri.put("test", "agent");
        requestParams.put(CityInformationAgent.KEY_CONTEXT, agentIri);
        try {
            agent.validateInput(requestParams);
        } catch (Exception e) {
            assertEquals(BadRequestException.class, e.getClass());
        }

        // test case where request params with context param will pass
        HashMap<String, String> map = new HashMap<>();
        map.put("key1", "value1");

        agentIri = new JSONObject();
        agentIri.put("http://localhost:9999/agent", map);
        requestParams.put(CityInformationAgent.KEY_CONTEXT, agentIri);
        try {
            assertTrue(agent.validateInput(requestParams));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testReadConfig() {
        // this test is deliberately left blank
        // method is already tested in testNewCityInformationAgentFields
    }

    @Test
    public  void testOrderGFAResults()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CityInformationAgent agent = new CityInformationAgent();

        //test that the method exist
        assertNotNull(agent.getClass().getDeclaredMethod("orderGFAResults", JSONArray.class));
        Method orderGFAResults = agent.getClass().getDeclaredMethod("orderGFAResults", JSONArray.class);
        orderGFAResults.setAccessible(true);

        String cityObject = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_65c912d7-8cfa-415e-94d4-3a6ba81194ba/";
        JSONObject query_result_obj = new JSONObject();
        query_result_obj.put("GFAvalue", 1413.);
        query_result_obj.put("cityObjectId", cityObject);
        JSONArray query_result_Array = new JSONArray();
        query_result_Array.put(query_result_obj);

        // test if city object id added to the hashmap as a key.
        HashMap<String, HashMap<String, Double>> ordered_results = (HashMap<String, HashMap<String, Double>>) orderGFAResults.invoke(agent, query_result_Array);
        assertTrue(ordered_results.containsKey(cityObject));
        assertEquals(1413., ordered_results.get(cityObject).get("default"));
        assertFalse(ordered_results.toString().contains("#"));
    }

    @Test
    public void testReturnRelevantGFAs()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CityInformationAgent agent = new CityInformationAgent();

        assertNotNull(agent.getClass().getDeclaredMethod("returnRelevantGFAs", HashMap.class, HashMap.class));
        Method returnRelevantGFAs = agent.getClass().getDeclaredMethod("returnRelevantGFAs", HashMap.class, HashMap.class);
        returnRelevantGFAs.setAccessible(true);

        HashMap<String, Double> cityObjectGFAs = new HashMap<>();
        cityObjectGFAs.put("Flat", 1500.);

        // test when input zoning case exist in cityObjectGFAs
        HashMap<String, Double> inputGFAs =  new HashMap<>();
        inputGFAs.put("Flat", 2000.);
        ArrayList<Double> relevantGFAs = (ArrayList<Double>) returnRelevantGFAs.invoke(agent, cityObjectGFAs, inputGFAs);
        assertEquals(1, relevantGFAs.size());
        assertTrue(relevantGFAs.contains(1500.));

        // test when input zoning case does not exist in cityObjectGFAs
        inputGFAs.clear();
        inputGFAs.put("Clinic", 1000.);
        ArrayList<Double> relevantGFAs2 = (ArrayList<Double>) returnRelevantGFAs.invoke(agent, cityObjectGFAs, inputGFAs);
        assertEquals(0, relevantGFAs2.size());
        assertFalse(relevantGFAs2.contains(1000.));
    }

    @Test void testApplyFiltersOnlyTotalGFA()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        CityInformationAgent agent = new CityInformationAgent();

        // test that the method exists.
        assertNotNull(agent.getClass().getDeclaredMethod("applyFiltersOnlyTotalGFA", String.class, HashMap.class, double.class, HashMap.class));
        Method applyFiltersOnlyTotalGFA = agent.getClass().getDeclaredMethod("applyFiltersOnlyTotalGFA", String.class, HashMap.class, double.class, HashMap.class);
        applyFiltersOnlyTotalGFA.setAccessible(true);

        double totalGFAValue = 3000.;
        HashMap<String, Double> filteredCityObjects = new HashMap<>();
        String cityObject = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_65c912d7-8cfa-415e-94d4-3a6ba81194ba/";

        // test when max value in the list is larger than input.
        HashMap<String, Double> currentCityObjectGFA =  new HashMap<>();
        currentCityObjectGFA.put("Flat", 4000.);
        applyFiltersOnlyTotalGFA.invoke(agent, cityObject, currentCityObjectGFA, totalGFAValue, filteredCityObjects);
        assertTrue(filteredCityObjects.containsKey(cityObject));
        assertEquals(4000. , filteredCityObjects.get(cityObject));
        assertEquals(1, filteredCityObjects.size());

        // test when max value in the list is smaller than input.
        currentCityObjectGFA.clear();
        currentCityObjectGFA.put("Flat", 2000.);
        filteredCityObjects =  new HashMap<>();
        applyFiltersOnlyTotalGFA.invoke(agent, cityObject, currentCityObjectGFA, totalGFAValue, filteredCityObjects);
        assertEquals(0, filteredCityObjects.size());
    }

    @Test void testApplyFiltersOnlyZoneCase()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CityInformationAgent agent = new CityInformationAgent();

        assertNotNull(agent.getClass().getDeclaredMethod("applyFiltersOnlyZoneCase", JSONArray.class, JSONArray.class));
        Method returnRelevantGFAs = agent.getClass().getDeclaredMethod("applyFiltersOnlyZoneCase", JSONArray.class, JSONArray.class);
        returnRelevantGFAs.setAccessible(true);

        String cityObject = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_65c912d7-8cfa-415e-94d4-3a6ba81194ba/";
        JSONObject query_result_obj = new JSONObject();
        query_result_obj.put("cityObjectId", cityObject);
        JSONArray query_result_Array = new JSONArray();
        query_result_Array.put(query_result_obj);
        JSONArray returnedCityObjects =  new JSONArray();
        returnRelevantGFAs.invoke(agent, query_result_Array, returnedCityObjects);

        // test if city object id is added to returned list of valid city objects.
        assertEquals(1, returnedCityObjects.length());

    }

    @Test void testApplyCapToFilteredResults()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        CityInformationAgent agent =  new CityInformationAgent();

        assertNotNull(agent.getClass().getDeclaredMethod("applyCapToFilteredResults", HashMap.class, JSONArray.class, boolean.class, boolean.class));
        Method applyCapToFilteredResults = agent.getClass().getDeclaredMethod("applyCapToFilteredResults", HashMap.class, JSONArray.class, boolean.class, boolean.class);
        applyCapToFilteredResults.setAccessible(true);

        HashMap<String, Double> filteredCityObjects =  new HashMap<>();
        filteredCityObjects.put("dfhh", 50.);
        filteredCityObjects.put("yukh", 100.);
        filteredCityObjects.put("qwer", 150.);
        filteredCityObjects.put("ljht", 200.);
        filteredCityObjects.put("cvnn", 250.);
        filteredCityObjects.put("uyoy", 300.);
        filteredCityObjects.put("dfgh", 350.);
        filteredCityObjects.put("yhnf", 400.);
        filteredCityObjects.put("pliu", 450.);
        filteredCityObjects.put("zxcv", 500.);
        filteredCityObjects.put("edsc", 550.);
        filteredCityObjects.put("qazx", 600.);

        // test if only 10 objects returned, test if it contains the smallest values.
        JSONArray returnedCityObjects = new JSONArray();
        applyCapToFilteredResults.invoke(agent, filteredCityObjects, returnedCityObjects, true, false);
        assertEquals(10, returnedCityObjects.length());
        assertTrue(returnedCityObjects.toString().contains("dfhh"));
        assertFalse(returnedCityObjects.toString().contains("qazx"));

        // test if only 10 objects returned, test if it contains the smallest values.
        returnedCityObjects = new JSONArray();
        applyCapToFilteredResults.invoke(agent, filteredCityObjects, returnedCityObjects, false, true);
        assertEquals(10, returnedCityObjects.length());
        assertTrue(returnedCityObjects.toString().contains("qazx"));
        assertFalse(returnedCityObjects.toString().contains("dfhh"));
    }

    @Test void testApplyFiltersGFAWithZoneCaseWithoutTotalGFA()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        CityInformationAgent agent =  new CityInformationAgent();

        assertNotNull(agent.getClass().getDeclaredMethod("applyFiltersGFAWithZoneCaseWithoutTotalGFA", String.class , boolean.class, ArrayList.class, double.class, double.class, HashMap.class));
        Method applyFiltersGFAWithZoneCaseWithoutTotalGFA= agent.getClass().getDeclaredMethod("applyFiltersGFAWithZoneCaseWithoutTotalGFA", String.class , boolean.class, ArrayList.class, double.class, double.class, HashMap.class);
        applyFiltersGFAWithZoneCaseWithoutTotalGFA.setAccessible(true);

        String cityObject = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_65c912d7-8cfa-415e-94d4-3a6ba81194ba/";
        ArrayList<Double> cityObjectZoneCaseGFAValues = new ArrayList<>();
        cityObjectZoneCaseGFAValues.add(1500.);
        cityObjectZoneCaseGFAValues.add(3000.);
        HashMap<String, Double> filteredCityObjects = new HashMap<>();

        // test  when zone case gfas are available and minimum is larger than sum --> should be added to the final list.
        applyFiltersGFAWithZoneCaseWithoutTotalGFA.invoke(agent, cityObject, false,
            cityObjectZoneCaseGFAValues, 1000., 2000., filteredCityObjects);
        assertTrue(filteredCityObjects.containsKey(cityObject));
        assertTrue(filteredCityObjects.containsValue(1500.));

        // test  when zone case gfas are available but minimum is smaller than sum --> should not be added to the final list.
        filteredCityObjects.clear();
        cityObjectZoneCaseGFAValues.add(500.);
        applyFiltersGFAWithZoneCaseWithoutTotalGFA.invoke(agent, cityObject, false,
            cityObjectZoneCaseGFAValues, 1000., 2000., filteredCityObjects);
        assertEquals(0, filteredCityObjects.size());

        // test when defaultGFA is true and default value is larger than sum.
        filteredCityObjects.clear();
        applyFiltersGFAWithZoneCaseWithoutTotalGFA.invoke(agent, cityObject, true,
            cityObjectZoneCaseGFAValues, 1000., 2000., filteredCityObjects);
        assertEquals(1, filteredCityObjects.size());
        assertTrue(filteredCityObjects.containsValue(2000.));

        // test when defaultGFA is true and default value is smaller than sum.
        filteredCityObjects.clear();
        applyFiltersGFAWithZoneCaseWithoutTotalGFA.invoke(agent, cityObject, true,
            cityObjectZoneCaseGFAValues, 1000., 500., filteredCityObjects);
        assertEquals(0, filteredCityObjects.size());
    }

    @Test void testApplyFiltersGFAWithZoneCaseWithTotalGFA()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CityInformationAgent agent =  new CityInformationAgent();

        assertNotNull(agent.getClass().getDeclaredMethod("applyFiltersGFAWithZoneCaseWithTotalGFA", String.class , boolean.class, double.class, ArrayList.class, double.class, double.class, HashMap.class));
        Method applyFiltersGFAWithZoneCaseWithTotalGFA= agent.getClass().getDeclaredMethod("applyFiltersGFAWithZoneCaseWithTotalGFA", String.class , boolean.class, double.class, ArrayList.class, double.class, double.class, HashMap.class);
        applyFiltersGFAWithZoneCaseWithTotalGFA.setAccessible(true);

        String cityObject = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_65c912d7-8cfa-415e-94d4-3a6ba81194ba/";
        HashMap<String, Double> filteredCityObjects = new HashMap<>();
        ArrayList<Double> cityObjectZoneCaseGFAValues = new ArrayList<>();
        cityObjectZoneCaseGFAValues.add(1200.);
        cityObjectZoneCaseGFAValues.add(3000.);

        // test when defaultGFA is true and default value is larger than sumOfZoneCaseGFAValues.
        applyFiltersGFAWithZoneCaseWithTotalGFA.invoke(agent, cityObject, true, 1500.,
            cityObjectZoneCaseGFAValues, 1000., 2000., filteredCityObjects);
        assertEquals(1, filteredCityObjects.size());
        assertTrue(filteredCityObjects.containsValue(2000.));

        // test when defaultGFA is true and default value is smaller than sumOfZoneCaseGFAValues.
        filteredCityObjects.clear();
        applyFiltersGFAWithZoneCaseWithTotalGFA.invoke(agent, cityObject, true, 1500.,
            cityObjectZoneCaseGFAValues, 1000., 500., filteredCityObjects);
        assertEquals(0, filteredCityObjects.size());

        //test defaultGFA false, zoneCaseGFA true, min gfa value larger than sum --> add cityObject.
        filteredCityObjects.clear();
        applyFiltersGFAWithZoneCaseWithTotalGFA.invoke(agent, cityObject, false, 1500.,
            cityObjectZoneCaseGFAValues, 1000., 1500., filteredCityObjects);
        assertEquals(1, filteredCityObjects.size());

        //test defaultGFA false, zoneCaseGFA true, min gfa value smaller than sum --> list is empty.
        filteredCityObjects.clear();
        applyFiltersGFAWithZoneCaseWithTotalGFA.invoke(agent, cityObject, false, 1500.,
            cityObjectZoneCaseGFAValues, 1500., 1500., filteredCityObjects);
        assertEquals(0, filteredCityObjects.size());

        // test defaultGFA false, zoneCaseGFA false, sum of gfa values larger than totalGFA --> add cityObject to list.;
        filteredCityObjects.clear();
        applyFiltersGFAWithZoneCaseWithTotalGFA.invoke(agent, cityObject, false, 1300.,
            cityObjectZoneCaseGFAValues, 0., 1500., filteredCityObjects);
        assertEquals(1, filteredCityObjects.size());

        // test defaultGFA false, zoneCaseGFA false, sum of gfa values smaller than totalGFA --> add cityObject to list.
        filteredCityObjects.clear();
        applyFiltersGFAWithZoneCaseWithTotalGFA.invoke(agent, cityObject, false, 5000.,
            cityObjectZoneCaseGFAValues, 0., 700., filteredCityObjects);
        assertEquals(0, filteredCityObjects.size());
    }

    @Test void testApplyFiltersGFAWithZoneCase()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        CityInformationAgent agent = new CityInformationAgent();

        assertNotNull(agent.getClass().getDeclaredMethod("applyFiltersGFAWithZoneCase", String.class, boolean.class, double.class, HashMap.class, HashMap.class, HashMap.class));
        Method applyFiltersGFAWithZoneCase = agent.getClass().getDeclaredMethod("applyFiltersGFAWithZoneCase",  String.class, boolean.class, double.class, HashMap.class, HashMap.class, HashMap.class);
        applyFiltersGFAWithZoneCase.setAccessible(true);

        //KG values for user inputs.
        String cityObject = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_65c912d7-8cfa-415e-94d4-3a6ba81194ba/";
        HashMap<String, Double> currentCityObjectGFA = new HashMap<>();
        currentCityObjectGFA.put("default", 2000.);

        //user inputs.
        HashMap<String, Double> inputZoneCaseGFAValues =  new HashMap<>();
        inputZoneCaseGFAValues.put("Flat", 500.);
        inputZoneCaseGFAValues.put("Clinic", 500.);
        HashMap<String, Double> filteredCityObjects =  new HashMap<>();

        // test when total GFA true-> default gfa true -> plot does not have zonecase gfas -> defautl gfa > total gfa input.
        applyFiltersGFAWithZoneCase.invoke(agent, cityObject, true, 1500., currentCityObjectGFA, inputZoneCaseGFAValues, filteredCityObjects);
        assertEquals(1, filteredCityObjects.size());
        assertTrue(filteredCityObjects.containsValue(2000.));

        // test when total GFA false -> default gfa true -> plot has all zone case gfas -> both are > input sum of zonecase gfas.
        filteredCityObjects.clear();
        currentCityObjectGFA.put("Flat",1500.);
        currentCityObjectGFA.put("Clinic", 3000.);
        applyFiltersGFAWithZoneCase.invoke(agent, cityObject, false, 0., currentCityObjectGFA, inputZoneCaseGFAValues, filteredCityObjects);
        assertEquals(1, filteredCityObjects.size());
        assertTrue(filteredCityObjects.containsValue(1500.));
    }

    @Test void testApplyFiltersGFA()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CityInformationAgent agent =  new CityInformationAgent();

        assertNotNull(agent.getClass().getDeclaredMethod("applyFiltersGFA", JSONArray.class, double.class, HashMap.class, JSONArray.class, boolean.class, boolean.class));
        Method applyFiltersGFA = agent.getClass().getDeclaredMethod("applyFiltersGFA", JSONArray.class, double.class, HashMap.class, JSONArray.class, boolean.class, boolean.class);
        applyFiltersGFA.setAccessible(true);

        String cityObject = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_65c912d7-8cfa-415e-94d4-3a6ba81194ba/";
        JSONObject default_gfa = new JSONObject();
        default_gfa.put("GFAvalue", 2000.);
        default_gfa.put("cityObjectId", cityObject);

        HashMap<String, Double> inputZoneCaseGFAValues =  new HashMap<>();
        inputZoneCaseGFAValues.put("Flat", 500.);
        inputZoneCaseGFAValues.put("Clinic", 500.);

        JSONArray query_result_Array = new JSONArray();
        query_result_Array.put(default_gfa);
        JSONArray returnCityObjects = new JSONArray();
        applyFiltersGFA.invoke(agent, query_result_Array, 1500., inputZoneCaseGFAValues, returnCityObjects, false, false);
        assertFalse(returnCityObjects.isEmpty());
        assertEquals(cityObject, returnCityObjects.get(0));

        returnCityObjects = new JSONArray();
        applyFiltersGFA.invoke(agent, query_result_Array, 1500., new HashMap<>(), returnCityObjects, false, false);
        assertFalse(returnCityObjects.isEmpty());
        assertEquals(cityObject, returnCityObjects.get(0));
    }

    @Test void testGetFilteredObjects() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CityInformationAgent agent = new CityInformationAgent();

        assertNotNull(agent.getClass()
            .getDeclaredMethod("getFilteredObjects", String.class, HashMap.class, double.class,
                boolean.class, boolean.class));
        Method getFilteredObjects = agent.getClass()
            .getDeclaredMethod("getFilteredObjects", String.class, HashMap.class, double.class,
                boolean.class, boolean.class);
        getFilteredObjects.setAccessible(true);

        String cityObject = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/cityobject/UUID_65c912d7-8cfa-415e-94d4-3a6ba81194ba/";
        JSONObject default_gfa = new JSONObject();
        default_gfa.put("GFAvalue", 2000.);
        default_gfa.put("cityObjectId", cityObject);
        JSONArray query_result = new JSONArray();
        query_result.put(default_gfa);


        JSONObject query_result_obj = new JSONObject();
        query_result_obj.put("cityObjectId", cityObject);
        JSONObject query_result_obj2 = new JSONObject();
        query_result_obj2.put("cityObjectId", "ID2");
        JSONArray query_result_noGFA = new JSONArray();
        query_result_noGFA.put(query_result_obj);
        query_result_noGFA.put(query_result_obj2);

        HashMap<String, Double> inputZoneCaseGFAValues =  new HashMap<>();
        inputZoneCaseGFAValues.put("Flat", 500.);
        inputZoneCaseGFAValues.put("Clinic", 500.);

        try (MockedStatic<AccessAgentCaller> accessAgentCallerMock = Mockito.mockStatic(
            AccessAgentCaller.class)) {

            //test with mocked AccessAgentCaller when gfaCase true.
            accessAgentCallerMock.when(
                    () -> AccessAgentCaller.queryStore(ArgumentMatchers.anyString(),
                        ArgumentMatchers.anyString()))
                .thenReturn(query_result);

            JSONArray returnCityObjects = (JSONArray) getFilteredObjects.invoke(agent, "predicate", inputZoneCaseGFAValues, 1500., false, false);
            assertFalse(returnCityObjects.isEmpty());
            assertEquals(cityObject, returnCityObjects.get(0));

            //test with mocked AccessAgentCaller when gfaCase false.
            accessAgentCallerMock.when(
                    () -> AccessAgentCaller.queryStore(ArgumentMatchers.anyString(),
                        ArgumentMatchers.anyString()))
                .thenReturn(query_result_noGFA);

            returnCityObjects = (JSONArray) getFilteredObjects.invoke(agent, "predicate", new HashMap<>(), 0., false, false);
            assertEquals(2, returnCityObjects.length());
            assertTrue(returnCityObjects.toString().contains("ID2"));

        }
    }

    @Test void testGetGFAFilterQuery()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CityInformationAgent agent = new CityInformationAgent();

        //test that the method exist
        assertNotNull(agent.getClass().getDeclaredMethod("getGFAFilterQuery", SelectBuilder.class, String.class));
        Method getGFAFilterQuery = agent.getClass().getDeclaredMethod("getGFAFilterQuery", SelectBuilder.class, String.class);
        getGFAFilterQuery.setAccessible(true);

        // test that parameters are added to the selectBuilder and predicates are constructed properly.
        String graphName = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/buildablespace/";
        String obsPredicate = "obs:hasBuildableSpace";
        String omPredicate = "om:hasValue/om:hasNumericValue";
        SelectBuilder selectBuilder = new SelectBuilder();
        getGFAFilterQuery.invoke(agent, selectBuilder, graphName);
        assertTrue(selectBuilder.buildString().contains(obsPredicate));
        assertTrue(selectBuilder.buildString().contains(omPredicate));
    }

    @Test void testGetOntoZoneFilterQuery()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        CityInformationAgent agent = new CityInformationAgent();

        //test that the method exists
        assertNotNull(agent.getClass().getDeclaredMethod("getOntoZoneFilterQuery", String.class, ArrayList.class, SelectBuilder.class, String.class));
        Method getOntoZoneFilterQuery = agent.getClass().getDeclaredMethod("getOntoZoneFilterQuery", String.class, ArrayList.class, SelectBuilder.class, String.class);
        getOntoZoneFilterQuery.setAccessible(true);

        String graphName = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/ontozone/";
        String usePredicate = "allowsUse";
        String programmePrecidate = "allowsProgramme";
        ArrayList<String> useArray = new ArrayList<String>();
        useArray.add("ParkUse");
        ArrayList<String> programmeArray = new ArrayList<String>();
        programmeArray.add("Flat");
        programmeArray.add("Clinic");

        SelectBuilder selectBuilder = new SelectBuilder();
        getOntoZoneFilterQuery.invoke(agent, usePredicate, useArray, selectBuilder, graphName);

        // test that prefixes constructed correctly.
        assertTrue(selectBuilder.buildString().contains(graphName));

        // test when use array passed.
        assertTrue(selectBuilder.buildString().contains("zo:allowsUse|zo:mayAllowUse"));
        assertTrue(selectBuilder.buildString().contains("zo:ParkUse"));

        // test when programme array passed.
        selectBuilder.clearWhereValues();
        getOntoZoneFilterQuery.invoke(agent, programmePrecidate, programmeArray, selectBuilder, graphName);
        assertTrue(selectBuilder.buildString().contains("zo:allowsProgramme|zo:mayAllowProgramme"));
        assertTrue(selectBuilder.buildString().contains("zo:Flat"));
        assertTrue(selectBuilder.buildString().contains("zo:Clinic"));

    }

    @Test void testGetFilterQuery()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CityInformationAgent agent = new CityInformationAgent();

        assertNotNull(agent.getClass().getDeclaredMethod("getFilterQuery", String.class, ArrayList.class,  boolean.class));
        Method getFilterQuery = agent.getClass().getDeclaredMethod("getFilterQuery", String.class, ArrayList.class, boolean.class);
        getFilterQuery.setAccessible(true);

        String ontoZoneGraph = "http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/ontozone/";
        String buildableSpaceGraph = "<http://www.theworldavatar.com:83/citieskg/namespace/singaporeEPSG4326/sparql/buildablespace/>";
        ArrayList<String> programmeArray = new ArrayList<String>();
        programmeArray.add("Flat");
        programmeArray.add("Clinic");

        // test when not gfa case
        String query = getFilterQuery.invoke(agent, "allowsProgramme", programmeArray, false).toString();
        assertFalse(query.contains(buildableSpaceGraph));
        assertTrue(query.contains(ontoZoneGraph));

        //test when gfa case
        String query2 = getFilterQuery.invoke(agent, "allowsProgramme", programmeArray, true).toString();
        assertTrue(query2.contains(buildableSpaceGraph));
    }

}

