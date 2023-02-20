package uk.ac.cam.cares.twa.cities.tasks;

import edu.uci.ics.jung.algorithms.shortestpath.UnweightedShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;
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
import java.util.concurrent.BlockingQueue;

/**
 * A graph inference task that calculates unweighted shortest paths between nodes on a graph.
 *
 * @author <a href="mailto:arkadiusz.chadzynski@cares.cam.ac.uk">Arkadiusz Chadzynski</a>
 */
public class UnweightedShortestPathTask implements UninitialisedDataAndResultQueueTask {
    private final IRI taskIri = IRI.create(GraphInferenceAgent.ONINF_SCHEMA + GraphInferenceAgent.TASK_USP);
    private boolean stop = false;
    private BlockingQueue<Map<String, JSONArray>> dataQueue;
    private BlockingQueue<Map<String, JSONArray>> resultQueue;
    private Node targetGraph;
    private Map<String, Integer> urlToNum = new HashMap();

    @Override
    public IRI getTaskIri() {
        return taskIri;
    }

    @Override
    public void setStringMapQueue(BlockingQueue<Map<String, JSONArray>> queue) {
        this.dataQueue = queue;
    }

    @Override
    public void setResultQueue(BlockingQueue<Map<String, JSONArray>> queue) {
        this.resultQueue = queue;
    }

    @Override
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
                    String srcIri = (String) map.get(GraphInferenceAgent.KEY_SRC_IRI).get(0);
                    String dstIri = (String) map.get(GraphInferenceAgent.KEY_DST_IRI).get(0);

                    // convert to jung graph
                    Graph graph = createGraph(data);

                    //get shortest path score
                    UnweightedShortestPath<Integer, int[]> shortestPath = new UnweightedShortestPath<>(graph);
                    Number distance = shortestPath.getDistance(urlToNum.get(srcIri), urlToNum.get(dstIri));
                    if (distance == null) {
                        distance = -1;
                    }

                    //put data result back on the queue for the agent to pick up
                    Map<String, JSONArray> result = new HashMap<>();
                    result.put(this.taskIri.toString(), new JSONArray().put(srcIri).put(dstIri).put(distance));
                    resultQueue.put(result);

                    //save result into KG
                    storeResults(srcIri, dstIri, (Integer) distance);
                } catch (Exception e) {
                    throw new JPSRuntimeException(e);
                } finally {
                    stop();
                }
            }
        }
    }

    /**
     * Creates compressed graph out of array containing S-P-O SPARQL query results.
     * All the elements of the array are mapped to integers and each array of integers corresponding
     * to each statement is added to the resulting graph as an edge betwee integers corresponding to
     * S & P elements.
     *
     * @param array SPARQL results.
     * @return Graph integers mapped to the input array elements.
     */
    public UndirectedSparseGraph createGraph(JSONArray array) {

        UndirectedSparseGraph graph = new UndirectedSparseGraph();

        int num = 1;

        for (Object data : array) {
            JSONObject obj = (JSONObject) data;
            String s = obj.getString("s");
            String p = obj.getString("p");
            String o = obj.getString("o");
            int sN = num++;
            int pN = num++;
            int oN = num++;
            //map URLs to integers to reduce graph size
            if (urlToNum.containsKey(s)) {
                sN = urlToNum.get(s);
            } else {
                urlToNum.put(s, sN);
            }
            if (urlToNum.containsKey(p)) {
                pN = urlToNum.get(p);
            } else {
                urlToNum.put(p, pN);
            }
            if (urlToNum.containsKey(o)) {
                oN = urlToNum.get(o);
            } else {
                urlToNum.put(o, oN);
            }

            graph.addEdge(new int[]{sN, pN, oN}, sN, oN);
        }

        return graph;
    }

    /**
     *
     * Stores inferred result in the knowledge graph using AccessAgent
     *
     * @param srcIri object of the sp algorithm inference
     * @param dstIri target of the sp algorithm inference
     * @param distance value of the sp algorithm inference
     */
    private void storeResults(String srcIri, String dstIri, int distance) {
        UpdateBuilder ub = prepareUpdateBuilder();
        prepareUpdate(ub, srcIri, dstIri, distance);
        persistUpdate(ub);
    }

    /**
     * Prepares 4 insert s-p-o statements:
     * (1) id hasInferenceObject srcIri
     * (2) id hasInferenceAlgorithm UnweightedShortestPathAlgorithm
     * (3) id hasInferredValue distance
     * (4) id hasInferredValue dstIri
     *
     * @param srcIri object of the sp algorithm inference
     * @param dstIri target of the sp algorithm inference
     * @param distance value of the sp algorithm inference
     */
    private void prepareUpdate(UpdateBuilder ub, String srcIri, String dstIri, int distance) {
        String pfix = GraphInferenceAgent.ONINF_PREFIX;
        Node id = NodeFactory.createURI(targetGraph.getURI() + UUID.randomUUID());
        ub.addInsert(targetGraph, id, pfix + ":" + GraphInferenceAgent.ONINT_P_INOBJ,
            NodeFactory.createURI(srcIri));
        ub.addInsert(targetGraph, id, pfix + ":" + GraphInferenceAgent.ONINT_P_INALG,
            NodeFactory.createURI(GraphInferenceAgent.ONINF_SCHEMA + GraphInferenceAgent.ONINT_C_USPALG));
        ub.addInsert(targetGraph, id, pfix + ":" + GraphInferenceAgent.ONINT_P_INVAL,
            NodeFactory.createLiteral(String.valueOf(distance), XSDDatatype.XSDinteger));
        ub.addInsert(targetGraph, id, pfix + ":" + GraphInferenceAgent.ONINT_P_INVAL,
            NodeFactory.createURI(dstIri));
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
