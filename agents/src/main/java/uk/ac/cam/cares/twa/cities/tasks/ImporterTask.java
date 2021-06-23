package uk.ac.cam.cares.twa.cities.tasks;

import org.citydb.ImpExp;
import java.io.File;
import java.net.URI;

public class ImporterTask implements Runnable {
    private BlazegraphServerTask serverTask;
    private File importFile;

    private Boolean stop = false;

    public ImporterTask(BlazegraphServerTask serverTask, File importFile) {
        this.serverTask = serverTask;
        this.importFile = importFile;
    }

    public boolean getStop() {
        return stop;
    }

    public void stop() {
        stop = true;
    }

    @Override
    public void run() {
        while (!stop) {
            while (!serverTask.getStop()) {
                URI endpointUri = serverTask.getServiceUri();
                if (endpointUri != null) {
                    String[] args = {"-shell", "-import " + importFile.getAbsolutePath()};
                    ImpExp.main(args);
                }
            }
        }
    }

}