package uk.ac.cam.cares.twa.cities.tasks;

import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.util.CommandHelper;

import java.io.File;
import java.util.ArrayList;

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

    @Override
    public void run() {
        while (!stop) {

            try {
                String CMDER_ROOT = "C:\\Users\\ELLO01\\Documents\\CityEnergyAnalyst\\Dependencies\\cmder";
                String FilePath = new File(CMDER_ROOT, "test_new.bat").getAbsolutePath();

                // Create a new scenario
                String createNewScenarioArgs = "cea create_new_scenario --project testProject1 --scenario testScenario --output-path  C:\\Users\\ELLO01\\Documents\\testProject";
                ArrayList<String> args = new ArrayList<>();
                args.add(FilePath);
                args.add(createNewScenarioArgs);

                CommandHelper.executeCommands("C:\\Users\\ELLO01\\Documents\\testProject\\testProject1\\testScenario\\inputs\\building-geometry", args);

            } catch ( NullPointerException e) {
                e.printStackTrace();
                throw new JPSRuntimeException(e);
            } finally {
                stop();
            }
        }
    }
}
