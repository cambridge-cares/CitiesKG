package uk.ac.cam.cares.twa.cities.agents;

import javax.servlet.annotation.WebServlet;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.json.JSONArray;
import org.semanticweb.owlapi.model.IRI;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;

/**
 * A JPSAgent framework based Ontology Inference Agent class used to infer semantic information
 * with knowledge graphs.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 */
@WebServlet(
    urlPatterns = {
        uk.ac.cam.cares.twa.cities.agents.OntologyInferenceAgent.URI_ACTION
    })
public class OntologyInferenceAgent extends InferenceAgent {
  public static final String URI_ACTION = "/inference/ontology";

  @Override
  protected JSONArray getAllTargetData(IRI sparqlEndpoint, String tBoxGraph) throws ParseException {
    SelectBuilder sb = getSparqlBuilder(sparqlEndpoint, tBoxGraph);

    return AccessAgentCaller.queryStore(route, sb.buildString());
  }


}
