package uk.ac.cam.cares.twa.cities.tasks;

import junit.framework.TestCase;
import kong.unirest.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RunCEATask.class, ProcessBuilder.class})
public class RunCEATaskTest extends TestCase {

    @Test
    public void testRunCEATask() {
        RunCEATask task;

        try {
            URI testURI = new URI("http://localhost/test");
            ArrayList<CEAInputData> testData = new ArrayList<CEAInputData>();
            testData.add(new CEAInputData("test","test"));
            ArrayList<String> testArray = new ArrayList<>();
            testArray.add("testUri");
            Integer test_thread = 0;
            String test_CRS = "27700";
            task = new RunCEATask(testData, testURI, testArray,test_thread, test_CRS);
            assertNotNull(task);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testRunCEATaskFields() {
        try {
            ArrayList<CEAInputData> testData = new ArrayList<CEAInputData>();
            testData.add(new CEAInputData("test","test"));
            URI testURI = new URI("http://localhost/test");
            ArrayList<String> testArray = new ArrayList<>();
            testArray.add("testUri");
            Integer test_thread = 0;
            String test_CRS = "27700";
            RunCEATask task = new RunCEATask(testData, testURI, testArray,test_thread, test_CRS);

            assertEquals(11, task.getClass().getDeclaredFields().length);

            Field inputs;
            Field uris;
            Field endpointUri;
            Field threadNumber;
            Field crs;
            Field CTYPE_JSON;
            Field stop;
            Field SHAPEFILE_SCRIPT;
            Field WORKFLOW_SCRIPT;
            Field CREATE_WORKFLOW_SCRIPT;
            Field FS;

            inputs = task.getClass().getDeclaredField("inputs");
            inputs.setAccessible(true);
            assertEquals(inputs.get(task), testData);
            uris = task.getClass().getDeclaredField("uris");
            uris.setAccessible(true);
            assertEquals(uris.get(task), testArray);
            endpointUri = task.getClass().getDeclaredField("endpointUri");
            endpointUri.setAccessible(true);
            assertEquals(endpointUri.get(task), testURI);
            threadNumber = task.getClass().getDeclaredField("threadNumber");
            threadNumber.setAccessible(true);
            assertEquals(threadNumber.get(task), test_thread);
            threadNumber = task.getClass().getDeclaredField("crs");
            threadNumber.setAccessible(true);
            assertEquals(threadNumber.get(task), test_CRS);
            CTYPE_JSON = task.getClass().getDeclaredField("CTYPE_JSON");
            assertEquals(CTYPE_JSON.get(task), "application/json");
            stop = task.getClass().getDeclaredField("stop");
            stop.setAccessible(true);
            assertFalse((boolean) stop.get(task));
            SHAPEFILE_SCRIPT = task.getClass().getDeclaredField("SHAPEFILE_SCRIPT");
            SHAPEFILE_SCRIPT.setAccessible(true);
            assertEquals(SHAPEFILE_SCRIPT.get(task), "create_shapefile.py");
            WORKFLOW_SCRIPT = task.getClass().getDeclaredField("WORKFLOW_SCRIPT");
            WORKFLOW_SCRIPT.setAccessible(true);
            assertEquals(WORKFLOW_SCRIPT.get(task), "workflow.yml");
            CREATE_WORKFLOW_SCRIPT = task.getClass().getDeclaredField("CREATE_WORKFLOW_SCRIPT");
            CREATE_WORKFLOW_SCRIPT.setAccessible(true);
            assertEquals(CREATE_WORKFLOW_SCRIPT.get(task), "create_cea_workflow.py");
            FS = task.getClass().getDeclaredField("FS");
            FS.setAccessible(true);
            assertEquals(FS.get(task), System.getProperty("file.separator"));

        } catch (NoSuchFieldException | IllegalAccessException | URISyntaxException e) {
            fail();
        }
    }

    @Test
    public void testRunCEATaskMethods() {
        try{
            URI testURI = new URI("http://localhost/test");
            ArrayList<CEAInputData> testData = new ArrayList<CEAInputData>();
            testData.add(new CEAInputData("test","test"));
            ArrayList<String> testArray = new ArrayList<>();
            testArray.add("testUri");
            Integer test_thread = 0;
            String test_CRS = "27700";
            RunCEATask task = new RunCEATask(testData, testURI, testArray,test_thread, test_CRS);

            assertEquals(7, task.getClass().getDeclaredMethods().length);

        } catch (URISyntaxException e) {
            fail();
        }

    }

    @Test
    public void testStop() {
        try {
            URI testURI = new URI("http://localhost/test");
            ArrayList<CEAInputData> testData = new ArrayList<CEAInputData>();
            testData.add(new CEAInputData("test","test"));
            ArrayList<String> testArray = new ArrayList<>();
            testArray.add("testUri");
            Integer test_thread = 0;
            String test_CRS = "27700";
            RunCEATask task = new RunCEATask(testData, testURI, testArray,test_thread, test_CRS);

            Field stopField = task.getClass().getDeclaredField("stop");
            stopField.setAccessible(true);
            assertFalse((Boolean) stopField.get(task));
            Method stopMethod = task.getClass().getDeclaredMethod("stop");
            stopMethod.setAccessible(true);
            stopMethod.invoke(task);
            assertTrue((Boolean) stopField.get(task));
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException | URISyntaxException e) {
            fail();
        }
    }

    @Test
    public void testRunProcess() throws Exception {
        URI testURI = new URI("http://localhost/test");
        ArrayList<CEAInputData> testData = new ArrayList<CEAInputData>();
        testData.add(new CEAInputData("test","test"));
        ArrayList<String> testArray = new ArrayList<>();
        testArray.add("testUri");
        Integer test_thread = 0;
        String test_CRS = "27700";
        RunCEATask task = PowerMockito.spy(new RunCEATask(testData, testURI, testArray,test_thread, test_CRS));

        ProcessBuilder builder = mock(ProcessBuilder.class);
        PowerMockito.whenNew(ProcessBuilder.class).withAnyArguments().thenReturn(builder);

        BufferedReader reader = mock(BufferedReader.class);
        PowerMockito.whenNew(BufferedReader.class).withAnyArguments().thenReturn(reader);

        InputStreamReader streamReader = mock(InputStreamReader.class);
        PowerMockito.whenNew(InputStreamReader.class).withAnyArguments().thenReturn(streamReader);

        Process dummyProcess  = mock(Process.class);
        doReturn(dummyProcess).when(builder).start();
        doReturn(-1).when(reader).read();

        Method runProcess = task.getClass().getDeclaredMethod("runProcess", ArrayList.class);

        ArrayList<String> args = new ArrayList<>();
        args.add("test");
        Process result = (Process) runProcess.invoke(task, args);

        assertEquals(dummyProcess, result);

        verify(builder, times(1)).redirectOutput(ProcessBuilder.Redirect.INHERIT);
        verify(builder, times(1)).redirectErrorStream(true);
        verify(builder, times(1)).start();
        verify(reader, times(1)).close();
        verify(dummyProcess, times(1)).waitFor();
    }

    @Test
    public void testDeleteDirectoryContents() {
        try {
            URI testURI = new URI("http://localhost/test");
            ArrayList<CEAInputData> testData = new ArrayList<CEAInputData>();
            testData.add(new CEAInputData("test","test"));
            ArrayList<String> testArray = new ArrayList<>();
            testArray.add("testUri");
            Integer test_thread = 0;
            String test_CRS = "27700";
            RunCEATask task = new RunCEATask(testData, testURI, testArray,test_thread, test_CRS);

            File myTempDir = new File(System.getProperty("java.io.tmpdir"));
            File newDirectory = new File(myTempDir, "new_directory");
            assertTrue(newDirectory.mkdir() || newDirectory.isDirectory());

            File tempFile = File.createTempFile("text", ".temp", newDirectory);

            Method deleteDirectoryContents = task.getClass().getDeclaredMethod("deleteDirectoryContents", File.class);
            deleteDirectoryContents.setAccessible(true);
            deleteDirectoryContents.invoke(task, newDirectory);

            assertFalse(tempFile.isFile());

        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | IOException | URISyntaxException e) {
            System.out.println(e.getMessage());
            fail();
        }
    }

    @Test
    public void testExtractArea() throws Exception {
        String titleRow = "PV_roofs_top_m2,PV_walls_south_m2,PV_walls_north_m2,Other,PV_walls_east_m2,PV_walls_west_m2";
        String valuesRow = "10.0,20.0,30.0,40.0,50.0,60.0";
        CEAOutputData data = new CEAOutputData();
        String tmpDir = "test";

        URI testURI = new URI("http://localhost/test");
        ArrayList<CEAInputData> testData = new ArrayList<CEAInputData>();
        testData.add(new CEAInputData("test","test"));
        ArrayList<String> testArray = new ArrayList<>();
        testArray.add("testUri");
        Integer test_thread = 0;
        String test_CRS = "27700";
        RunCEATask task = PowerMockito.spy(new RunCEATask(testData, testURI, testArray,test_thread, test_CRS));

        FileReader fReader = mock(FileReader.class);
        PowerMockito.whenNew(FileReader.class).withAnyArguments().thenReturn(fReader);

        BufferedReader bReader = mock(BufferedReader.class);
        PowerMockito.whenNew(BufferedReader.class).withAnyArguments().thenReturn(bReader);

        doReturn(titleRow, valuesRow, null).when(bReader).readLine();

        doNothing().when(task).deleteDirectoryContents(any());

        Method extractArea = task.getClass().getDeclaredMethod("extractArea", String.class, CEAOutputData.class);
        CEAOutputData result = (CEAOutputData) extractArea.invoke(task, tmpDir, data);

        assertTrue(result.PV_area_roof.contains("10.0"));
        assertTrue(result.PV_area_wall_south.contains("20.0"));
        assertTrue(result.PV_area_wall_north.contains("30.0"));
        assertTrue(result.PV_area_wall_east.contains("50.0"));
        assertTrue(result.PV_area_wall_west.contains("60.0"));
        assertTrue(result.targetUrl.contains(testURI.toString()));
        assertTrue(result.iris.get(0).contains(testArray.get(0)));
    }

    @Test
    public void testExtractTimeSeriesOutputs() throws Exception {
        String demandTitleRow = "GRID_kWh,QH_sys_kWh,QC_sys_kWh,Other,E_sys_kWh";
        String demandValuesRow1 = "10.0,20.0,30.0,40.0,50.0";
        String demandValuesRow2 = "11.0,21.0,31.0,41.0,51.0";

        String PVTitleRow = "Date,PV_roofs_top_E_kWh,PV_walls_south_E_kWh,Other,PV_walls_north_E_kWh,PV_walls_west_E_kWh,PV_walls_east_E_kWh";
        String PVValuesRow1 = "2005-01-01 00:00:00+00:00,20.0,30.0,40.0,50.0,60.0,70.0";
        String PVValuesRow2 = "2005-01-01 01:00:00+00:00,21.0,31.0,41.0,51.0,61.0,71.0";

        String tmpDir = "test";

        URI testURI = new URI("http://localhost/test");
        ArrayList<CEAInputData> testData = new ArrayList<CEAInputData>();
        testData.add(new CEAInputData("test","test"));
        ArrayList<String> testArray = new ArrayList<>();
        testArray.add("testUri");
        Integer test_thread = 0;
        String test_CRS = "27700";
        RunCEATask task = PowerMockito.spy(new RunCEATask(testData, testURI, testArray,test_thread, test_CRS));

        FileReader fReader = mock(FileReader.class);
        PowerMockito.whenNew(FileReader.class).withAnyArguments().thenReturn(fReader);

        BufferedReader bReader = mock(BufferedReader.class);
        PowerMockito.whenNew(BufferedReader.class).withAnyArguments().thenReturn(bReader);

        doReturn(demandTitleRow, demandValuesRow1,demandValuesRow2, null, PVTitleRow, PVValuesRow1, PVValuesRow2, null).when(bReader).readLine();

        Method extractTimeSeriesOutputs = task.getClass().getDeclaredMethod("extractTimeSeriesOutputs", String.class);
        CEAOutputData result = (CEAOutputData) extractTimeSeriesOutputs.invoke(task, tmpDir);

        assertTrue(result.grid_demand.get(0).get(0).contains("10.0"));
        assertTrue(result.grid_demand.get(0).get(1).contains("11.0"));
        assertTrue(result.electricity_demand.get(0).get(0).contains("50.0"));
        assertTrue(result.electricity_demand.get(0).get(1).contains("51.0"));
        assertTrue(result.heating_demand.get(0).get(0).contains("20.0"));
        assertTrue(result.heating_demand.get(0).get(1).contains("21.0"));
        assertTrue(result.cooling_demand.get(0).get(0).contains("30.0"));
        assertTrue(result.cooling_demand.get(0).get(1).contains("31.0"));
        assertTrue(result.PV_supply_roof.get(0).get(0).contains("20.0"));
        assertTrue(result.PV_supply_roof.get(0).get(1).contains("21.0"));
        assertTrue(result.PV_supply_wall_south.get(0).get(0).contains("30.0"));
        assertTrue(result.PV_supply_wall_south.get(0).get(1).contains("31.0"));
        assertTrue(result.PV_supply_wall_north.get(0).get(0).contains("50.0"));
        assertTrue(result.PV_supply_wall_north.get(0).get(1).contains("51.0"));
        assertTrue(result.PV_supply_wall_east.get(0).get(0).contains("70.0"));
        assertTrue(result.PV_supply_wall_east.get(0).get(1).contains("71.0"));
        assertTrue(result.PV_supply_wall_west.get(0).get(0).contains("60.0"));
        assertTrue(result.PV_supply_wall_west.get(0).get(1).contains("61.0"));

        String expectedTime = result.times.get(0);
        String expectedTime2 = result.times.get(1);
        assertTrue(expectedTime.contains("2005-01-01T00:00:00+00:00"));
        assertTrue(expectedTime2.contains("2005-01-01T01:00:00+00:00"));
    }

    @Test
    public void testReturnOutputs() {
        CEAOutputData data = null;
        Method returnOutputs = null;
        RunCEATask task = null;
        URI testURI = null;

        try {
            testURI = new URI("http://localhost/test");
            ArrayList<CEAInputData> testData = new ArrayList<CEAInputData>();
            testData.add(new CEAInputData("test", "test"));
            ArrayList<String> testArray = new ArrayList<>();
            testArray.add("testUri");
            Integer test_thread = 0;
            String test_CRS = "27700";
            task = new RunCEATask(testData, testURI, testArray, test_thread, test_CRS);

            data = new CEAOutputData();

            returnOutputs = task.getClass().getDeclaredMethod("returnOutputs", CEAOutputData.class);
        } catch (URISyntaxException| NoSuchMethodException e){
            fail();
        }

        try{
            returnOutputs.invoke(task, data);
        } catch (Exception e) {
            assert e instanceof InvocationTargetException;
            assertEquals(((InvocationTargetException) e).getTargetException().getClass(), JPSRuntimeException.class);
        }

        try{
            HttpResponse<?> response = mock(HttpResponse.class);
            try (MockedStatic<Unirest> unirest = mockStatic(Unirest.class, RETURNS_MOCKS)) {
                unirest.when(() -> Unirest.post(anyString())
                                .header(anyString(), anyString())
                                .body(anyString())
                                .socketTimeout(anyInt())
                                .asEmpty())
                        .thenReturn(response);

                returnOutputs.invoke(task, data);
            } catch (Exception e) {
                assert e instanceof InvocationTargetException;
                assertEquals(((InvocationTargetException) e).getTargetException().getClass(), JPSRuntimeException.class);
                assertEquals(((InvocationTargetException) e).getTargetException().getCause().getMessage(), testURI.toString()+" 0");
            }

        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testRun() throws NoSuchMethodException, URISyntaxException, InvocationTargetException, IllegalAccessException {
        URI testURI = new URI("http://localhost/test");
        ArrayList<CEAInputData> testData = new ArrayList<CEAInputData>();
        testData.add(new CEAInputData("test", "test"));
        ArrayList<String> testArray = new ArrayList<>();
        testArray.add("testUri");
        Integer test_thread = 0;
        String test_CRS = "27700";
        RunCEATask task = PowerMockito.spy(new RunCEATask(testData, testURI, testArray, test_thread, test_CRS));

        Method run = task.getClass().getDeclaredMethod("run");

        Process dummy_process = mock(Process.class);
        CEAOutputData dummy_data = mock(CEAOutputData.class);

        doReturn(dummy_process).when(task).runProcess(any());
        doReturn(dummy_data).when(task).extractTimeSeriesOutputs(anyString());
        doReturn(dummy_data).when(task).extractArea(anyString(), any());
        doNothing().when(task).returnOutputs(any());

        run.invoke(task);

        verify(task, times(3)).runProcess(any());
        verify(task, times(1)).extractTimeSeriesOutputs(anyString());
        verify(task, times(1)).extractArea(anyString(), any());
        verify(task, times(1)).returnOutputs(any());
    }
}
