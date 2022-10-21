package uk.ac.cam.cares.twa.cities.tasks;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.rdf.rdfxml.parser.OWLRDFConsumer;
import org.xml.sax.SAXException;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.agents.GraphInferenceAgent;
import uk.ac.cam.cares.twa.cities.agents.InferenceAgent;

public class ClassDisjointnessCheckingTask extends TaxonomicReasoningTask {
  private final IRI taskIri = IRI.create(InferenceAgent.ONINF_SCHEMA + InferenceAgent.TASK_CDC);

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
          String ontoIri = (String) map.get(InferenceAgent.KEY_ONTO_IRI).get(0);


          //create model
          OWLOntology ontology = createModel(data);
          Reasoner reasoner = new Reasoner(new Configuration(), ontology);

          //put data result back on the queue for the agent to pick up
          Map<String, JSONArray> result = new HashMap<>();

          JSONArray output = getReasonerOutput(reasoner, ontoIri, srcIri, dstIri);

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

  private JSONArray getReasonerOutput(Reasoner reasoner, String ontoIri, IRI srcIri, IRI dstIri) {
    JSONArray output = new JSONArray();
    OWLDataFactory df = OWLManager.getOWLDataFactory();

    boolean member = reasoner.getDisjointClasses(df.getOWLClass(srcIri))
        .containsEntity(df.getOWLClass(dstIri));
    output.put(new JSONObject().put(ontoIri, member));

    return output;
  }
  
  private OWLOntology createModel(JSONArray data) throws OWLOntologyCreationException, SAXException {
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLOntology ontology = manager.createOntology();
    OWLRDFConsumer consumer = new OWLRDFConsumer(ontology, anonymousNodeChecker , new OWLOntologyLoaderConfiguration());

    for (Object triple : data) {
      JSONObject obj = (JSONObject) triple;
      consumer.statementWithResourceValue(obj.getString("s"),
          obj.getString("p"), obj.getString("o"));
    }

    consumer.endModel();

    return ontology;
  }

}
