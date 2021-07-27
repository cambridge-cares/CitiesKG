package uk.ac.cam.cares.twa.cities.tasks;

import org.apache.commons.io.FileUtils;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;

import javax.ws.rs.HttpMethod;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.concurrent.BlockingQueue;

public class NquadsUploaderTask implements Runnable {
    private Boolean stop = false;
    private final BlockingQueue<File> nqQueue;
    private final URI endpointUri;
    private final String HEADER_CTYPE = "Content-Type";
    private final String HEADER_CLEN = "Content-Length";
    private final String CTYPE_NQ = "text/x-nquads";

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

                    //@todo implementatio

                    HttpURLConnection conn = (HttpURLConnection) endpointUri.toURL().openConnection();
                    OutputStream os = conn.getOutputStream();
                    conn.setDoOutput(true);
                    conn.setRequestMethod(HttpMethod.POST);
                    conn.setRequestProperty(HEADER_CTYPE, CTYPE_NQ);
                    conn.setRequestProperty(HEADER_CLEN, String.valueOf(nqFile.length()));
                    os.write(FileUtils.readFileToByteArray(nqFile));



                } catch (InterruptedException | IOException e) {
                    throw  new JPSRuntimeException(e);
                } finally {
                    stop();
                }
            }
        }
    }
}
