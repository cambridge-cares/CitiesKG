package uk.ac.cam.cares.twa.cities.tasks;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpException;
import org.apache.http.protocol.HTTP;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;

/**
 * Runnable task uploading a data stored in an N-Quads format {@link <https://www.w3.org/TR/n-quads/>} into a given
 * endpoint capable of bulk data uploads. The upload starts after a file is picked up from the {@link BlockingQueue}.
 * The task stops itself after the upload process is finished.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 * @version $Id$
 *
 */
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
                            .socketTimeout(300000)
                            .asEmpty();
                    int respStatus = response.getStatus();
                    if (respStatus != HttpURLConnection.HTTP_OK) {
                        throw new HttpException(endpointUri + " " + respStatus);
                    }
                } catch (InterruptedException | IOException | HttpException | UnirestException e) {
                    throw  new JPSRuntimeException(e);
                } finally {
                    stop();
                }
            }
        }
    }
}
