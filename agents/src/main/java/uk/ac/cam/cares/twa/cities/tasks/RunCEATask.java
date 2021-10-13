package uk.ac.cam.cares.twa.cities.tasks;

import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
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

            } catch ( NullPointerException e) {
                e.printStackTrace();
                throw new JPSRuntimeException(e);
            } finally {
                stop();
            }
        }
    }
}
