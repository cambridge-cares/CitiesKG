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
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.agents.GraphInferenceAgent;
import uk.ac.cam.cares.twa.cities.agents.InferenceAgent;

/**
 * A taxonomic reasoning task that checks for class membership between an individual and a class.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 */
public class ClassMembershipCheckingTask extends TaxonomicReasoningTask {
  private final IRI taskIri = IRI.create(InferenceAgent.ONINF_SCHEMA + InferenceAgent.TASK_CMC);

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

  /**
   * Method to check for class membership between a class with srcIri and an individual with dstIri.
   *
   * @param reasoner Reasoner to make inference with.
   * @param ontoIri IRI of the Ontology
   * @param srcIri Source class IRI
   * @param dstIri Destination individual IRI
   * @return JSONArray with a JSONObject that contains boolean information about class membership
   */
  private JSONArray getReasonerOutput(Reasoner reasoner, String ontoIri, IRI srcIri, IRI dstIri) {
    JSONArray output = new JSONArray();
    OWLDataFactory df = OWLManager.getOWLDataFactory();

    boolean member = reasoner.getInstances(df.getOWLClass(srcIri), false).containsEntity(
        df.getOWLNamedIndividual(dstIri));

    output.put(new JSONObject().put(ontoIri, member));

    return output;
  }

}
