package uk.ac.cam.cares.twa.cities.tasks;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNode;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.agents.GraphInferenceAgent;
import uk.ac.cam.cares.twa.cities.agents.InferenceAgent;

public class ClassSpecialisationCheckingTask extends TaxonomicReasoningTask {
  private final IRI taskIri = IRI.create(InferenceAgent.ONINF_SCHEMA + InferenceAgent.TASK_CSC);

  @Override
  public void run() {
    while (isRunning()) {
      while (!dataQueue.isEmpty()) {
        try {
          // get data
          Map<String, JSONArray> map = this.dataQueue.take();
          JSONArray data = map.get(this.taskIri.toString());
          String srcIri = (String) map.get(GraphInferenceAgent.KEY_SRC_IRI).get(0);
          String dstIri = (String) map.get(GraphInferenceAgent.KEY_DST_IRI).get(0);
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

  private JSONArray getReasonerOutput(Reasoner reasoner, String ontoIri, String srcIri, String dstIri) {
    JSONArray output = new JSONArray();
    OWLDataFactory df = OWLManager.getOWLDataFactory();

    if (!srcIri.equals("*") && !dstIri.equals("*")) {
      boolean member = reasoner.getSubClasses(df.getOWLClass(IRI.create(dstIri)), false)
          .containsEntity(df.getOWLClass(IRI.create(srcIri)));
      output.put(new JSONObject().put(ontoIri, member));
    } else if (srcIri.equals("*")) {
      //return all subclasses of the class with dstIRI
      for (Object node : reasoner.getSubClasses(df.getOWLClass(IRI.create(dstIri)), false).getNodes().toArray()) {
        for (Object owlClass : ((OWLClassNode) node).getEntities().toArray()) {
          output.put(((OWLClass) owlClass).getIRI());
        }
      }
    } else {
      //return all superclasses of the class with srcIRI
      for (Object node : reasoner.getSuperClasses(df.getOWLClass(IRI.create(srcIri)), false).getNodes().toArray()) {
        for (Object owlClass : ((OWLClassNode) node).getEntities().toArray()) {
          output.put(((OWLClass) owlClass).getIRI());
        }
      }
    }
    return output;
  }


}
