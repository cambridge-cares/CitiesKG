package uk.ac.cam.cares.twa.cities.tasks;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.semanticweb.owlapi.model.IRI;

public interface UninitialisedDataQueueTask extends Runnable {

  public IRI getTaskIri();

  void setStringMapQueue(BlockingQueue<Map<String, String>> queue);

  public abstract boolean isRunning();

  public  abstract void stop();

}
