package uk.ac.cam.cares.twa.cities.tasks;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
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

  @Override
  public IRI getTaskIri() {
    return taskIri;
  }

  @Override
  public void setStringMapQueue(BlockingQueue<Map<String, JSONArray>> queue) {
    this.dataQueue = queue;
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
          Graph graph = createGraph(data);

          //run Page Rank
          PageRank<RDFNode, Double> ranker = new PageRank<RDFNode, Double>(graph, 0.3);
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

  //@todo: remove hardcoding
  private void storeResults(Graph graph, PageRank<RDFNode, Double>  ranker) {

    Node inGraph = NodeFactory.createURI( "http://127.0.0.1:9999/blazegraph/namespace/singaporeEPSG4326/sparql/OntoInfer/");

    UpdateBuilder ub = new UpdateBuilder();
    ub.addPrefix(GraphInferenceAgent.ONINF_PREFIX, GraphInferenceAgent.ONINF_SCHEMA);
    int counter = 0;

    for (Object v : graph.getVertices()) {
      //prepare inference update for a node
      Node id = NodeFactory.createURI(inGraph.getURI() + UUID.randomUUID());
      ub.addInsert(inGraph, id, GraphInferenceAgent.ONINF_PREFIX + ":hasInferenceObject",
          NodeFactory.createURI(String.valueOf(v)));
      ub.addInsert(inGraph, id, GraphInferenceAgent.ONINF_PREFIX + ":hasInferenceAlgorithm",
          NodeFactory.createURI(GraphInferenceAgent.ONINF_SCHEMA + "PageRankAlgorithm"));
      ub.addInsert(inGraph, id, GraphInferenceAgent.ONINF_PREFIX + ":hasInferredValue",
          NodeFactory.createLiteral(String.valueOf(
                  new BigDecimal(ranker.getVertexScore((RDFNode) v)).setScale(20, RoundingMode.FLOOR)),
              XSDDatatype.XSDdouble));
      counter++;

      //store data if counter of nodes is 100 and start over
      if (counter >= 100) {
        System.out.println(ub.build().toString());
        AccessAgentCaller.updateStore(ResourceBundle.getBundle("config").getString("uri.route"),
            ub.build().toString());
        ub = new UpdateBuilder();
        ub.addPrefix(GraphInferenceAgent.ONINF_PREFIX, GraphInferenceAgent.ONINF_SCHEMA);
        counter = 0;
      }
    }

  }

}
