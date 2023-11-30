package uk.ac.cam.cares.twa.cities.tasks;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.json.JSONArray;

/**
 * Interface common to all tasks with a queue for passing end result of a task data
 *  to an agent that initiated a task.
 */
public interface UninitialisedDataAndResultQueueTask extends UninitialisedDataQueueTask {

  void setResultQueue(BlockingQueue<Map<String, JSONArray>> queue);

}
