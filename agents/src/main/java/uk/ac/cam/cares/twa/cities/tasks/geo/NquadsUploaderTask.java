package uk.ac.cam.cares.twa.cities.tasks.geo;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpException;
import org.apache.http.protocol.HTTP;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.agents.geo.CityImportAgent;

/**
 * Runnable task uploading a data stored in an N-Quads format {@link
 * <https://www.w3.org/TR/n-quads/>} into a given endpoint capable of bulk data uploads. The upload
 * starts after a file is picked up from the {@link BlockingQueue}. The task stops itself after the
 * upload process is finished.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 * @version $Id$
 */
public class NquadsUploaderTask implements Runnable {

  public static final String CTYPE_NQ = "text/x-nquads";
  private final BlockingQueue<File> nqQueue;
  private final URI endpointUri;
  private Boolean stop = false;

  public NquadsUploaderTask(BlockingQueue<File> nqQueue, URI endpointUri) {
    this.nqQueue = nqQueue;
    this.endpointUri = endpointUri;
  }

  public boolean isRunning() {
    return !stop;
  }

  public void stop() {
    stop = true;
  }

  @Override
  public void run() {
    while (isRunning()) {
      while (!nqQueue.isEmpty()) {
        File nqFile = null;
        try {
          nqFile = nqQueue.take();
          String nQuads = FileUtils.readFileToString(nqFile,
              String.valueOf(StandardCharsets.UTF_8));
          if (!nQuads.isEmpty()) {
            HttpResponse<?> response = Unirest.post(endpointUri.toString())
                .header(HTTP.CONTENT_TYPE, CTYPE_NQ)
                .body(nQuads)
                .socketTimeout(300000)
                .asEmpty();
            int respStatus = response.getStatus();
            if (respStatus != HttpURLConnection.HTTP_OK) {
              throw new HttpException(endpointUri + " " + respStatus);
            }
          }
         
        } catch (InterruptedException | IOException | HttpException | UnirestException e) {
          throw new JPSRuntimeException(e);
        } finally {
          CityImportAgent.archiveImportFiles(Objects.requireNonNull(nqFile));
          stop();
        }
      }
    }
  }


}
