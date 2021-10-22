package uk.ac.cam.cares.twa.cities.tasks;

import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class RunCEATask implements Runnable {
    private final ArrayList<String> inputs;
    private final String path;
    private Boolean stop = false;

    public RunCEATask(ArrayList<String> buildingData, String outputPath) {
        this.inputs = buildingData;
        this.path = outputPath;
    }

    public void stop() {
        stop = true;
    }

    public void runCEAScript(String ceaArgs) {
        String CMDER_ROOT = "C:\\Users\\ELLO01\\Documents\\CityEnergyAnalyst\\Dependencies\\cmder";
        String FilePath = new File(CMDER_ROOT, "test_new.bat").getAbsolutePath();

        ArrayList<String> args = new ArrayList<>();
        args.add(FilePath);
        args.add(ceaArgs);

        ProcessBuilder builder = new ProcessBuilder(args);

        // starting the process
        try {
            Process process = builder.start();
            process.waitFor(10, TimeUnit.SECONDS);
            process.destroy();                     // tell the process to stop
            process.waitFor(10, TimeUnit.SECONDS); // give it a chance to stop
            if (process.isAlive()) {
                process.destroyForcibly();             // tell the OS to kill the process
                process.waitFor();                     // the process is now dead
            }
        } catch ( InterruptedException | IOException  e) {
            e.printStackTrace();
            throw new JPSRuntimeException(e);
        }
    }

    @Override
    public void run() {
        while (!stop) {

            try {
                // Create dummy input data
                CEAInputData result = new CEAInputData();

                result.geometry = inputs.get(0);
                result.floors_ag = inputs.get(1);
                double height_ag = Integer.parseInt(result.floors_ag)*3.5;

                //Parse data to JSON
                String dataString = new Gson().toJson(result);
                String FilePath = new File("C:\\Users\\ELLO01\\Documents\\CitiesKG\\utils", "create_shapefile.py").getAbsolutePath();

                ArrayList<String> args2 = new ArrayList<>();
                args2.add("python");
                args2.add(FilePath);
                args2.add(dataString.replace("\"", "\\\""));

                // creating the process
                ProcessBuilder build = new ProcessBuilder(args2);
                // starting the process
                build.start();

                // Create a new scenario and run CEA scripts
                String createNewScenarioArgs = "cea create_new_scenario --project testProject1 --scenario testScenario --output-path  C:\\Users\\ELLO01\\Documents\\testProject --zone C:\\Users\\ELLO01\\Documents\\testProject\\zone.shp";
                runCEAScript(createNewScenarioArgs);
                String weatherHelperArgs = "cea weather-helper --scenario C:\\Users\\ELLO01\\Documents\\testProject\\testProject1\\testScenario";
                runCEAScript(weatherHelperArgs);
                String surroundingsHelperArgs = "cea surroundings-helper --scenario C:\\Users\\ELLO01\\Documents\\testProject\\testProject1\\testScenario --buffer 250 --height-ag --floors-ag ";
                runCEAScript(surroundingsHelperArgs);
                String terrainHelperArgs = "cea terrain-helper --scenario C:\\Users\\ELLO01\\Documents\\testProject\\testProject1\\testScenario";
                runCEAScript(terrainHelperArgs);
                String databaseInitializerArgs = "cea data-initializer --scenario C:\\Users\\ELLO01\\Documents\\testProject\\testProject1\\testScenario --databases-path C:\\Users\\ELLO01\\Documents\\cityenergyanalyst\\cityenergyanalyst\\cea\\databases\\SG";
                runCEAScript(databaseInitializerArgs);
                String archetypesMapperArgs = "cea archetypes-mapper --scenario C:\\Users\\ELLO01\\Documents\\testProject\\testProject1\\testScenario --buildings ['B001']";
                runCEAScript(archetypesMapperArgs);
                String radiationArgs = "cea radiation --scenario C:\\Users\\ELLO01\\Documents\\testProject\\testProject1\\testScenario --buildings ['B001']";
                runCEAScript(radiationArgs);
                String scheduleMakerArgs = "cea schedule-maker --scenario C:\\Users\\ELLO01\\Documents\\testProject\\testProject1\\testScenario --buildings ['B001']";
                runCEAScript(scheduleMakerArgs);
                String demandArgs = "cea demand --scenario C:\\Users\\ELLO01\\Documents\\testProject\\testProject1\\testScenario --buildings ['B001'] --loads-output ['GRID', 'QH_sys', 'QC_sys']";
                runCEAScript(demandArgs);
                String PVArgs = "cea photovoltaic-thermal --scenario C:\\Users\\ELLO01\\Documents\\testProject\\testProject1\\testScenario --buildings ['B001']";
                runCEAScript(PVArgs);


            } catch (  IOException | NullPointerException e) {
                e.printStackTrace();
                throw new JPSRuntimeException(e);
            } finally {
                stop();
            }
        }
    }
}
