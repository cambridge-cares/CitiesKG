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
 * A taxonomic reasoning task that checks for a value restrictions on a certain property.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 */
public class ValueRestrictionCheckingTask extends TaxonomicReasoningTask {
  protected final IRI taskIri = IRI.create(InferenceAgent.ONINF_SCHEMA + InferenceAgent.TASK_VRC);

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

  /**
   * Method to check whether a class with srcIri is within a domain of a property with propIri
   * and a class with dstIri is within a range of that property.
   *
   * @param reasoner Reasoner to make inference with.
   * @param ontoIri IRI of the Ontology
   * @param srcIri Source class IRI
   * @param dstIri Destination class IRI
   * @return JSONArray with a JSONObject that contains boolean value restriction.
   */
  private JSONArray getReasonerOutput(Reasoner reasoner, String ontoIri, IRI srcIri, IRI dstIri, IRI propIri) {
    JSONArray output = new JSONArray();
    OWLDataFactory df = OWLManager.getOWLDataFactory();

    boolean member = reasoner.getObjectPropertyDomains(df.getOWLObjectProperty(propIri), false)
        .containsEntity(df.getOWLClass(srcIri)) && reasoner.getObjectPropertyRanges(
            df.getOWLObjectProperty(propIri), false).containsEntity(df.getOWLClass(dstIri));
    output.put(new JSONObject().put(ontoIri, member));

    return output;
  }


}
