package uk.ac.cam.cares.twa.cities.tasks;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.semanticweb.owlapi.model.IRI;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.agents.GraphInferenceAgent;

/**
 * Runnable task implementing Page Rank algorithm.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 */
public class PageRankTask implements UninitialisedDataQueueTask {
  private final IRI taskIri = IRI.create(GraphInferenceAgent.PREF_ONT_INFER + GraphInferenceAgent.TASK_PR);
  private boolean stop = false;
  private BlockingQueue<Map<String, String>> dataQueue;

  @Override
  public IRI getTaskIri() {
    return taskIri;
  }

  @Override
  public void setStringMapQueue(BlockingQueue<Map<String, String>> queue) {
    this.dataQueue = queue;
  }

  @Override
  public boolean isRunning() {
    return !stop;
  }

  @Override
  public void stop() {
    stop = true;
  }

  @Override
  public void run() {
    while (isRunning()) {
      while (!dataQueue.isEmpty()) {
        try {
          //get data and run Page Rank
        } catch (Exception e) {
          throw new JPSRuntimeException(e);
        } finally {
          stop();
        }
      }
    }
  }

}
