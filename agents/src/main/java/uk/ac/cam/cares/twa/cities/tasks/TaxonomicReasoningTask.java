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

/**
 * A taxonomic reasoning task superclass that groups methods common to the other taxonomic reasoning
 * task classes.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 */
public abstract class TaxonomicReasoningTask implements UninitialisedDataAndResultQueueTask {
  protected final String OWL_RESTRICTION_IRI = "http://www.w3.org/2002/07/owl#Restriction";
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

  /**
   * Creates OWL API model of an ontology represents as s-p-o triples in JSONArray.
   *
   * @param data S-P-O triples of an Ontology, in JSONArray format.
   * @return ontology modeled in OWLAPI.
   * @throws OWLOntologyCreationException
   */
  protected OWLOntology createModel(JSONArray data) throws OWLOntologyCreationException {
    String ntriples = prepareNtriples(data);

    return OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new StreamDocumentSource(
            IOUtils.toInputStream(ntriples, Charset.defaultCharset()), IRI.create(""), new NTriplesDocumentFormat(), null),
        new OWLOntologyLoaderConfiguration());
  }

  /**
   * Converts JSONArray S-P-O ontology representation in triples to N-Triples format.
   * Performs string replacements to do that. "Deskolemises" restriction IRIs to blank nodes for
   * the purpose of N-Triples to be parsed correctly by OWLAPI.
   *
   * @param data S-P-O triples of an Ontology, in JSONArray format.
   * @return S-P-O triples of an Ontology, in N-Triples format.
   */
  private String prepareNtriples(JSONArray data) {
    String ntriples = "";
    ArrayList<String> skolemized = new ArrayList<>();

    for (Object triple : data) {
      JSONObject obj = (JSONObject) triple;

      String subject = obj.getString("s");
      String predicate = "<" + obj.getString("p") + ">";
      String object = obj.getString("o");

      if (object.equals(OWL_RESTRICTION_IRI)) {
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
