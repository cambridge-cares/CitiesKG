package uk.ac.cam.cares.twa.cities.agents;

import javax.servlet.annotation.WebServlet;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.json.JSONArray;
import org.semanticweb.owlapi.model.IRI;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;

/**
 * A JPSAgent framework based Graph Inference  Agent class used to infer structural information about
 * graphs.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 */
@WebServlet(
    urlPatterns = {
        uk.ac.cam.cares.twa.cities.agents.GraphInferenceAgent.URI_ACTION
    })
public class GraphInferenceAgent extends InferenceAgent {
  public static final String URI_ACTION = "/inference/graph";

  @Override
  protected JSONArray getAllTargetData(IRI sparqlEndpoint, String tBoxGraph) throws ParseException {
    SelectBuilder sb = getSparqlBuilder(sparqlEndpoint, tBoxGraph);
    sb.addFilter("?o != \"only\"")
        .addFilter("?p != " + IRI_RDF_TYP)
        .addFilter("?o != " + IRI_RDF_NIL)
        .addFilter("?o != " + IRI_OWL_THG);

    return AccessAgentCaller.queryStore(route, sb.buildString());

  }


}
