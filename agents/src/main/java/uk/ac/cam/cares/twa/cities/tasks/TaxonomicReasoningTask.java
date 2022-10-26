package uk.ac.cam.cares.twa.cities.tasks;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.NTriplesDocumentFormat;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import uk.ac.cam.cares.twa.cities.agents.GraphInferenceAgent;

public abstract class TaxonomicReasoningTask implements UninitialisedDataAndResultQueueTask {
  protected final IRI taskIri = IRI.create("");
  protected boolean stop = false;
  protected BlockingQueue<Map<String, JSONArray>> dataQueue;
  protected BlockingQueue<Map<String, JSONArray>> resultQueue;
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

  protected OWLOntology createModel(JSONArray data) throws OWLOntologyCreationException {
    String ntriples = prepareNtriples(data);

    return OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new StreamDocumentSource(
            IOUtils.toInputStream(ntriples, Charset.defaultCharset()), IRI.create(""), new NTriplesDocumentFormat(), null),
        new OWLOntologyLoaderConfiguration());
  }

  private String prepareNtriples(JSONArray data) {
    String ntriples = "";
    ArrayList<String> skolemized = new ArrayList<>();

    for (Object triple : data) {
      JSONObject obj = (JSONObject) triple;

      String subject = obj.getString("s");
      String predicate = "<" + obj.getString("p") + ">";
      String object = obj.getString("o");

      if (object.equals("http://www.w3.org/2002/07/owl#Restriction")) {
        skolemized.add(subject);
      }

      if (!subject.contains("_:")) {
        subject = "<" + subject + ">";
      }

      String ntriple = subject + " " + predicate + " ";

      if (object.contains("http")) {
        if (object.contains("^^")) {
          String[] splitObj = object.split("\\^\\^");
          object = splitObj[0] + "^^<" + splitObj[1] +">";
        } else {
          object = "<" + object + ">";
        }
      }
      ntriple = ntriple + object + " .\n";
      ntriples = ntriples.concat(ntriple);
    }

    for (String iri: skolemized) {
      ntriples = ntriples.replaceAll("<" + iri + ">", "_:" + UUID.randomUUID());
    }

    return ntriples;
  }

}
