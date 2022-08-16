package uk.ac.cam.cares.twa.cities.tasks;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.graph.Graph;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import net.rootdev.jenajung.JenaJungGraph;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.model.IRI;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
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
          PageRank<RDFNode, Integer> ranker = new PageRank<RDFNode, Integer>(graph, 0.3);
          ranker.evaluate();

          Map<Node, Double> result = new HashMap<>();
          for (Object v : graph.getVertices()) {
            result.put(((ResourceImpl) v).asNode(), ranker.getVertexScore((RDFNode) v));
          }


          //store results - @todo: save it to the KG

          String eol = System.getProperty("line.separator");

          try (Writer writer = new FileWriter(System.getProperty("java.io.tmpdir") + "/somefile.csv")) {
            writer.append("Link")
                .append(',')
                .append("Score")
                .append(eol);
            for (Map.Entry<Node, Double> entry : result.entrySet()) {
              writer.append("\"" + entry.getKey().toString() + "\"")
                  .append(',')
                  .append(new BigDecimal(entry.getValue().doubleValue()).setScale(20, RoundingMode.FLOOR).toString())
                  .append(eol);
            }
            System.out.println(System.getProperty("java.io.tmpdir") + "/somefile.csv");
          } catch (IOException ex) {
            ex.printStackTrace(System.err);
          }


        } catch (Exception e) {
          throw new JPSRuntimeException(e);
        } finally {
          stop();
        }
      }
    }
  }

  public Graph<RDFNode, Statement> createGraph(JSONArray array) {
    Model model = ModelFactory.createDefaultModel();

    for (Object data : array) {
      JSONObject obj = (JSONObject) data;
      model.add(ResourceFactory.createResource(obj.getString("s")),
          ResourceFactory.createProperty(obj.getString("p")),
              ResourceFactory.createResource(obj.getString("o")));
    }

    return new JenaJungGraph(model);
  }


}
