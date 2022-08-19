package uk.ac.cam.cares.twa.cities.tasks;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.graph.Graph;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import net.rootdev.jenajung.JenaJungGraph;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.model.IRI;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.jps.base.query.AccessAgentCaller;
import uk.ac.cam.cares.twa.cities.agents.GraphInferenceAgent;

/**
 * Runnable task implementing Page Rank algorithm.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 */
public class PageRankTask implements UninitialisedDataQueueTask {
  private final IRI taskIri = IRI.create(GraphInferenceAgent.ONINF_SCHEMA + GraphInferenceAgent.TASK_PR);
  private boolean stop = false;
  private BlockingQueue<Map<String, JSONArray>> dataQueue;
  private Node targetGraph;

  @Override
  public IRI getTaskIri() {
    return taskIri;
  }

  @Override
  public void setStringMapQueue(BlockingQueue<Map<String, JSONArray>> queue) {
    this.dataQueue = queue;
  }

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

          // convert to jung graph
          Graph<RDFNode, Statement> graph = createGraph(data);

          //run Page Rank
          PageRank<RDFNode, Statement> ranker = new PageRank<>(graph, 0.3);
          ranker.evaluate();

          //store results
          storeResults(graph, ranker);

        } catch (Exception e) {
          throw new JPSRuntimeException(e);
        } finally {
          stop();
        }
      }
    }
  }

  /**
   * Converts JSON array of s-p-o statements to a Jena Jung Graph
   *
   * @param array initial s-p-o stmt array
   * @return resulting graph
   */
  private JenaJungGraph createGraph(JSONArray array) {
    Model model = ModelFactory.createDefaultModel();

    for (Object data : array) {
      JSONObject obj = (JSONObject) data;
      model.add(ResourceFactory.createResource(obj.getString("s")),
          ResourceFactory.createProperty(obj.getString("p")),
              ResourceFactory.createResource(obj.getString("o")));
    }

    return new JenaJungGraph(model);
  }

  /**
   * Stores algorithm application results in the knowledge graph.
   *
   * @param graph - initial JenaJung graph
   * @param ranker - graph with page rank scores applied
   */
  private void storeResults(Graph<RDFNode, Statement> graph, PageRank<RDFNode, Statement> ranker) {

    UpdateBuilder ub = prepareUpdateBuilder();

    int counter = 0;

    for (Object vert : graph.getVertices()) {
      //prepare inference update for a node
      prepareUpdate(ranker, ub, vert);
      counter++;

      //store data if counter of nodes is 100 and start over
      if (counter >= 100) {
        ub = persistUpdate(ub);

        counter = 0;
      }
    }
    //store any remaining data
    if (counter > 0)  {
      persistUpdate(ub);
    }

  }

  /**
   * Prepares 3 insert s-p-o statements for each node:
   * (1) id hasInferenceObject nodeIRI
   * (2) id hasInferenceAlgorithm PageRankAlgorithm
   * (3) id hasInferredValue nodePRScore
   * Adds those statements to the UpdateBuilder.
   *
   * @param ranker - page rank algorithm implementation.
   * @param ub - Update builder to add statements into
   * @param vert - ranked graph node
   */
  private void prepareUpdate(PageRank<RDFNode, Statement> ranker, UpdateBuilder ub, Object vert) {
    String pfix = GraphInferenceAgent.ONINF_PREFIX;
    Node id = NodeFactory.createURI(targetGraph.getURI() + UUID.randomUUID());
    ub.addInsert(targetGraph, id, pfix + ":" + GraphInferenceAgent.ONINT_P_INOBJ,
        NodeFactory.createURI(String.valueOf(vert)));
    ub.addInsert(targetGraph, id, pfix + ":" + GraphInferenceAgent.ONINT_P_INALG,
        NodeFactory.createURI(GraphInferenceAgent.ONINF_SCHEMA + GraphInferenceAgent.ONINT_C_PRALG));
    ub.addInsert(targetGraph, id, pfix + ":" + GraphInferenceAgent.ONINT_P_INVAL,
        NodeFactory.createLiteral(String.valueOf(
                BigDecimal.valueOf(ranker.getVertexScore((RDFNode) vert)).setScale(20, RoundingMode.FLOOR)),
            XSDDatatype.XSDdouble));
  }

  /**
   * Creates UpdateBuilder and adds OntoInter prefix into it.
   *
   * @return update builder with prefix.
   */
  private UpdateBuilder prepareUpdateBuilder() {
    UpdateBuilder ub = new UpdateBuilder();
    ub.addPrefix(GraphInferenceAgent.ONINF_PREFIX, GraphInferenceAgent.ONINF_SCHEMA);

    return ub;
  }

  /**
   * Stores given UpdateBuilder contents in the knowledge graph via access agent and returns fresh
   * builder with prefix after that.
   *
   * @param ub UpdateBuilder with statements to store in the knowledge graph
   * @return fresh update builder.
   */
  private UpdateBuilder persistUpdate(UpdateBuilder ub) {
    AccessAgentCaller.updateStore(ResourceBundle.getBundle("config").getString("uri.route"),
        ub.build().toString());
    ub = prepareUpdateBuilder();

    return ub;
  }

}
