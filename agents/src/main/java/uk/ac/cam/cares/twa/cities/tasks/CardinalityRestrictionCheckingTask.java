package uk.ac.cam.cares.twa.cities.tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectCardinalityRestriction;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.agents.GraphInferenceAgent;
import uk.ac.cam.cares.twa.cities.agents.InferenceAgent;

public class CardinalityRestrictionCheckingTask extends TaxonomicReasoningTask {
  protected final IRI taskIri = IRI.create(InferenceAgent.ONINF_SCHEMA + InferenceAgent.TASK_CRC);

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
          OWLObjectProperty prop = ((OWLObjectCardinalityRestriction) nce).getProperty().asOWLObjectProperty();
          if (filler.getClassExpressionType() == ClassExpressionType.OWL_CLASS) {
            if (((OWLClass) filler).getIRI().toString().equals(dstIri.toString()) && prop.getIRI().toString().equals(propIri.toString())) {
              outputMsg = new JSONObject().put(ontoIri, new JSONObject().put(nce.getClassExpressionType().getName(),
                  ((OWLObjectCardinalityRestriction) nce).getCardinality()));
            }
          }
        }
    }

    output.put(outputMsg);

    return output;
  }

}
