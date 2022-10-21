package uk.ac.cam.cares.twa.cities.tasks;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.json.JSONArray;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.util.AnonymousNodeChecker;
import uk.ac.cam.cares.twa.cities.agents.GraphInferenceAgent;

public abstract class TaxonomicReasoningTask implements UninitialisedDataAndResultQueueTask {
  protected final IRI taskIri = IRI.create("");
  protected boolean stop = false;
  protected BlockingQueue<Map<String, JSONArray>> dataQueue;
  protected BlockingQueue<Map<String, JSONArray>> resultQueue;
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


}
