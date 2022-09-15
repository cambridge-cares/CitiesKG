package uk.ac.cam.cares.twa.cities.tasks;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import com.hp.hpl.jena.rdf.model.Model;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.agents.GraphInferenceAgent;
import uk.ac.cam.cares.twa.cities.agents.InferenceAgent;

public class ConsistencyCheckingTask implements UninitialisedDataAndResultQueueTask {
  private final IRI taskIri = IRI.create(InferenceAgent.ONINF_SCHEMA + InferenceAgent.TASK_CC);
  private boolean stop = false;
  private BlockingQueue<Map<String, JSONArray>> dataQueue;
  private BlockingQueue<Map<String, JSONArray>> resultQueue;
  private Node targetGraph;

  @Override
  public IRI getTaskIri() {
    return taskIri;
  }

  @Override
  public void setStringMapQueue(BlockingQueue<Map<String, JSONArray>> queue) {
    this.dataQueue = queue;
  }

  @Override
  public void setResultQueue(BlockingQueue<Map<String, JSONArray>> queue) {
    this.resultQueue = queue;
  }

  @Override
  public void setTargetGraph(String endpointIRI) {
    targetGraph = NodeFactory.createURI(endpointIRI + GraphInferenceAgent.ONTOINFER_GRAPH);
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
          // get data
          Map<String, JSONArray> map = this.dataQueue.take();
          JSONArray data = map.get(this.taskIri.toString());
          Model model = createModel(data);
          //todo: check and finish implementation
          OutputStream out = new ByteArrayOutputStream();
          model.write(out);
          ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(out.toString().getBytes()));
          OWLOntologyManager manager= OWLManager.createOWLOntologyManager();
          manager.loadOntologyFromOntologyDocument(ois);

        } catch (Exception e) {
          throw new JPSRuntimeException(e);
        } finally {
          stop();
        }
      }
    }
  }

  private Model createModel(JSONArray data) {
    Model model = ModelFactory.createDefaultModel();

    for (Object triple : data) {
      JSONObject obj = (JSONObject) triple;
      model.add(model.createStatement(ResourceFactory.createResource(obj.getString("s")),
          ResourceFactory.createProperty(obj.getString("p")),
          ResourceFactory.createResource(obj.getString("o"))));
    }

    return model;
  }

}
