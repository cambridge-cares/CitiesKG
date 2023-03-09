package uk.ac.cam.cares.twa.cities.tasks;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.json.JSONArray;
import org.semanticweb.owlapi.model.IRI;

/**
 * Interface common to all tasks with a queue for passing data between an agent and a task.
 */
public interface UninitialisedDataQueueTask extends Runnable {

  public IRI getTaskIri();

  void setStringMapQueue(BlockingQueue<Map<String, JSONArray>> queue);

  void setTargetGraph(String targetGraph);

  public abstract boolean isRunning();

  public  abstract void stop();

}
