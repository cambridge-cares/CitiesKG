package uk.ac.cam.cares.twa.cities.agents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.semanticweb.owlapi.model.IRI;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.tasks.PageRankTask;
import uk.ac.cam.cares.twa.cities.tasks.UninitialisedDataQueueTask;

public class InferenceAgentTest {

  @Test
  public void testChooseTask() {
    GraphInferenceAgent agent = new GraphInferenceAgent();
    JSONArray result = new JSONArray().put(new JSONObject().put("o", "http://www.theworldavatar.com/ontologies/OntoInfer.owl#PageRankTask"));
    try (MockedStatic<AccessAgentCaller> aacMock = Mockito.mockStatic(AccessAgentCaller.class)) {
      aacMock.when(() -> AccessAgentCaller.queryStore(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
          .thenReturn(result);
      agent.getClass().getDeclaredField("route").set(agent, "http://localhost:48080/test");
      Method chooseTask = agent.getClass().getDeclaredMethod("chooseTask", IRI.class, IRI.class);
      chooseTask.setAccessible(true);
      UninitialisedDataQueueTask task = (UninitialisedDataQueueTask) chooseTask.invoke(agent, IRI.create("http://www.theworldavatar.com/ontologies/OntoInfer.owl#PageRankAlgorithm"),
          IRI.create("http://127.0.0.1:9999/blazegraph/namespace/test/sparql/"));
      assertEquals(PageRankTask.class, task.getClass());

      Field agentDataQueueField = agent.getClass().getDeclaredField("dataQueue");
      Field taskDataQueueField = task.getClass().getDeclaredField("dataQueue");
      taskDataQueueField.setAccessible(true);
      assertEquals(agentDataQueueField.get(agent), taskDataQueueField.get(task));

      Field targetGraph = task.getClass().getDeclaredField("targetGraph");
      targetGraph.setAccessible(true);
      assertEquals("http://127.0.0.1:9999/blazegraph/namespace/test/sparql/OntoInfer/", targetGraph.get(task).toString());
    } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      fail();
    }
  }

}
