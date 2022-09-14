package uk.ac.cam.cares.twa.cities.tasks;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.json.JSONArray;
import org.semanticweb.owlapi.model.IRI;

public class ConsistencyCheckingTask implements UninitialisedDataAndResultQueueTask {

  @Override
  public void setResultQueue(BlockingQueue<Map<String, JSONArray>> queue) {

  }

  @Override
  public IRI getTaskIri() {
    return null;
  }

  @Override
  public void setStringMapQueue(BlockingQueue<Map<String, JSONArray>> queue) {

  }

  @Override
  public void setTargetGraph(String targetGraph) {

  }

  @Override
  public boolean isRunning() {
    return false;
  }

  @Override
  public void stop() {

  }

  @Override
  public void run() {

  }
}
