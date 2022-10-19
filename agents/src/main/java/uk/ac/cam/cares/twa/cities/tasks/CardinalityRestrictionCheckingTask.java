package uk.ac.cam.cares.twa.cities.tasks;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.NTriplesDocumentFormat;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectCardinalityRestriction;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.util.AnonymousNodeChecker;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.agents.GraphInferenceAgent;
import uk.ac.cam.cares.twa.cities.agents.InferenceAgent;

public class CardinalityRestrictionCheckingTask implements UninitialisedDataAndResultQueueTask {
  private final IRI taskIri = IRI.create(InferenceAgent.ONINF_SCHEMA + InferenceAgent.TASK_CRC);
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
          IRI srcIri = IRI.create((String) map.get(GraphInferenceAgent.KEY_SRC_IRI).get(0));
          IRI dstIri = IRI.create((String) map.get(GraphInferenceAgent.KEY_DST_IRI).get(0));
          IRI propIri = IRI.create((String) map.get(GraphInferenceAgent.KEY_PROP_IRI).get(0));
          String ontoIri = (String) map.get(InferenceAgent.KEY_ONTO_IRI).get(0);

          //create model
          OWLOntology ontology = createModel(data);
          Reasoner reasoner = new Reasoner(new Configuration(), ontology);

          //put data result back on the queue for the agent to pick up
          Map<String, JSONArray> result = new HashMap<>();

          JSONArray output = getReasonerOutput(reasoner, ontoIri, srcIri, dstIri, propIri);

          result.put(this.taskIri.toString(), output);

          resultQueue.put(result);

        } catch (Exception e) {
          throw new JPSRuntimeException(e);
        } finally {
          stop();
        }
      }
    }
  }

  private JSONArray getReasonerOutput(Reasoner reasoner, String ontoIri, IRI srcIri, IRI dstIri, IRI propIri) {
    JSONArray output = new JSONArray();
    JSONObject outputMsg = new JSONObject().put(ontoIri, false);
    OWLDataFactory df = OWLManager.getOWLDataFactory();
    OWLClass srcCls = df.getOWLClass(srcIri);
    Set<OWLClassAxiom> tempAx = reasoner.getRootOntology().getAxioms(srcCls);
    for(OWLClassAxiom ax: tempAx) {
      for(OWLClassExpression nce: ax.getNestedClassExpressions())
        if((nce instanceof OWLObjectCardinalityRestriction)) {
          OWLClassExpression filler = ((OWLObjectCardinalityRestriction) nce).getFiller();
          if (filler.getClassExpressionType() == ClassExpressionType.OWL_CLASS) {
            if (((OWLClass) filler).getIRI().toString().equals(dstIri.toString())) {
              outputMsg = new JSONObject().put(ontoIri, new JSONObject().put(nce.getClassExpressionType().getName(), ((OWLObjectCardinalityRestriction) nce).getCardinality()));
            }
          }
        }
    }

    output.put(outputMsg);

    return output;
  }
  
  private OWLOntology createModel(JSONArray data) throws OWLOntologyCreationException {
    String ntriples = prepareNtriples(data);

    OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new StreamDocumentSource(
            IOUtils.toInputStream(ntriples, Charset.defaultCharset()), IRI.create(""), new NTriplesDocumentFormat(), null),
        new OWLOntologyLoaderConfiguration());

    return ontology;
  }

  private String prepareNtriples(JSONArray data) {
    String ntriples = new String();
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
