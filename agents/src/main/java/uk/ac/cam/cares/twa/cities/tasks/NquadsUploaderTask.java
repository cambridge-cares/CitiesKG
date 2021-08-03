package uk.ac.cam.cares.twa.cities.tasks;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpException;
import org.apache.http.protocol.HTTP;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;

public class NquadsUploaderTask implements Runnable {
    public static final String CTYPE_NQ = "text/x-nquads";
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
            while (!nqQueue.isEmpty()) {
                try {
                    File nqFile = nqQueue.take();

                    HttpResponse<?> response = Unirest.post(endpointUri.toString())
                            .header(HTTP.CONTENT_TYPE, CTYPE_NQ)
                            .body(FileUtils.readFileToString(nqFile, String.valueOf(StandardCharsets.UTF_8)))
                            .asEmpty();

                    if (response.getStatus() != HttpURLConnection.HTTP_OK) {
                        throw new HttpException(endpointUri + " " + response.getStatus());
                    }
                } catch (InterruptedException | IOException | HttpException e) {
                    throw  new JPSRuntimeException(e);
                } finally {
                    stop();
                }
            }
        }
    }
}
