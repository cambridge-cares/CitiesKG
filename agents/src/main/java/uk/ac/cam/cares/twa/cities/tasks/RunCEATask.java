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
        } catch ( IOException  e) {
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

                // Run a workflow that runs all CEA scripts
                String workflowArgs = "cea workflow --workflow C:\\Users\\ELLO01\\Documents\\CitiesKG\\utils\\workflow.yml";
                runCEAScript(workflowArgs);

            } catch (  IOException | NullPointerException e) {
                e.printStackTrace();
                throw new JPSRuntimeException(e);
            } finally {
                stop();
            }
        }
    }
}
