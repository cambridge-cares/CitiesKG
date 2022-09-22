package uk.ac.cam.cares.twa.cities.tasks;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import com.hp.hpl.jena.rdf.model.Model;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.agents.GraphInferenceAgent;
import uk.ac.cam.cares.twa.cities.agents.InferenceAgent;

public class ConsistencyCheckingTask implements UninitialisedDataAndResultQueueTask {
  private final IRI taskIri = IRI.create(InferenceAgent.ONINF_SCHEMA + InferenceAgent.TASK_CC);
  private boolean stop = false;
  private BlockingQueue<Map<String, JSONArray>> dataQueue;
  private BlockingQueue<Map<String, JSONArray>> resultQueue;
  Node targetGraph;

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
          String ontoIri = (String) map.get(InferenceAgent.KEY_ONTO_IRI).get(0);

          //create model
          Model model = createModel(data);
          OutputStream out = new ByteArrayOutputStream();
          model.write(out, "N-TRIPLES");

          //evaluate
          OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
          OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(out.toString()));
          Reasoner reasoner=new Reasoner(ontology);

          //put data result back on the queue for the agent to pick up
          Map<String, JSONArray> result = new HashMap<>();
          result.put(this.taskIri.toString(), new JSONArray().put(ontoIri + " : " + reasoner.isConsistent()));
          resultQueue.put(result);

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
