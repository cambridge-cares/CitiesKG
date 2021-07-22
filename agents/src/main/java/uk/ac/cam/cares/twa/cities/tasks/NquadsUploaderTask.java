package uk.ac.cam.cares.twa.cities.tasks;

import java.io.File;
import java.net.URI;
import java.util.concurrent.BlockingQueue;

public class NquadsUploaderTask implements Runnable {
    private Boolean stop = false;
    private final BlockingQueue<File> nqQueue;
    private final URI endpointUri;

    public NquadsUploaderTask(BlockingQueue<File> nqQueue, URI endpointUri) {
        this.nqQueue = nqQueue;
        this.endpointUri = endpointUri;
    }

    public void stop() {
        stop = true;
    }

    @Override
    public void run() {
        while (!stop) {
            //@todo implementation
            stop();
        }
    }
}
