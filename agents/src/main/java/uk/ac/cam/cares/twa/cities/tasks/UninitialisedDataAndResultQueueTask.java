package uk.ac.cam.cares.twa.cities.tasks;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.json.JSONArray;
import org.semanticweb.owlapi.model.IRI;

public interface UninitialisedDataAndResultQueueTask extends UninitialisedDataQueueTask {

  void setResultQueue(BlockingQueue<Map<String, JSONArray>> queue);

}
