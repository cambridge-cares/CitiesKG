package uk.ac.cam.cares.twa.cities.tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.rdf.rdfxml.parser.OWLRDFConsumer;
import org.semanticweb.owlapi.util.AnonymousNodeChecker;
import org.xml.sax.SAXException;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.agents.GraphInferenceAgent;
import uk.ac.cam.cares.twa.cities.agents.InferenceAgent;

public class ConsistencyCheckingTask implements UninitialisedDataAndResultQueueTask {
  private final IRI taskIri = IRI.create(InferenceAgent.ONINF_SCHEMA + InferenceAgent.TASK_CC);
  private boolean stop = false;
  private BlockingQueue<Map<String, JSONArray>> dataQueue;
  private BlockingQueue<Map<String, JSONArray>> resultQueue;
  Node targetGraph;
  AnonymousNodeChecker anonymousNodeChecker = new AnonymousNodeChecker() {
    @Override
    public boolean isAnonymousNode(IRI iri) {
      return false;
    }

    @Override
    public boolean isAnonymousNode(String s) {
      return false;
    }

    @Override
    public boolean isAnonymousSharedNode(String s) {
      return false;
    }
  };

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
          OWLOntology ontology = createModel(data);
          Reasoner reasoner = new Reasoner(new Configuration(), ontology);

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

  private OWLOntology createModel(JSONArray data) throws OWLOntologyCreationException, SAXException {
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLOntology ontology = manager.createOntology();
    OWLRDFConsumer consumer = new OWLRDFConsumer(ontology, anonymousNodeChecker , new OWLOntologyLoaderConfiguration());
    //consumer.setOntologyFormat(new DefaultOntologyFormat());

    for (Object triple : data) {
      JSONObject obj = (JSONObject) triple;
      consumer.statementWithResourceValue(obj.getString("s"),
          obj.getString("p"), obj.getString("o"));
    }

    consumer.endModel();

    return ontology;
  }

}
