package uk.ac.cam.cares.twa.cities.tasks;

import com.hp.hpl.jena.rdf.model.*;
import edu.uci.ics.jung.algorithms.cluster.EdgeBetweennessClusterer;
import edu.uci.ics.jung.graph.Graph;
import net.rootdev.jenajung.JenaJungGraph;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.model.IRI;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.agents.GraphInferenceAgent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class EdgeBetweennessTask implements UninitialisedDataQueueTask {
    private final IRI taskIri = IRI.create(GraphInferenceAgent.ONINF_SCHEMA + GraphInferenceAgent.TASK_EB);
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

                    // execute algo
                    EdgeBetweennessClusterer<?, ?> clusterer = new EdgeBetweennessClusterer(0);
                    Set<?> set = clusterer.apply(graph);

                    // sparql update triples to endpoint
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
        List<Statement> list = new ArrayList<>();

        for (Object data : array) {
            JSONObject obj = (JSONObject) data;
            list.add(model.createStatement(ResourceFactory.createResource(obj.getString("s")),
                    ResourceFactory.createProperty(obj.getString("p")),
                    ResourceFactory.createResource(obj.getString("o"))));
        }

        model.add(list);
        return new JenaJungGraph(model);
    }
}
