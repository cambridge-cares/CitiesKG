package uk.ac.cam.cares.twa.cities.tasks;

import edu.uci.ics.jung.algorithms.cluster.EdgeBetweennessClusterer;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import org.apache.commons.collections4.MapUtils;
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

import java.util.*;
import java.util.concurrent.BlockingQueue;

public class EdgeBetweennessTask implements UninitialisedDataQueueTask {

    private final IRI taskIri = IRI.create(
        GraphInferenceAgent.ONINF_SCHEMA + GraphInferenceAgent.TASK_EB);
    private boolean stop = false;
    private BlockingQueue<Map<String, JSONArray>> dataQueue;
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
                    long startTime = System.currentTimeMillis();
                    // convert to jung graph
                    Graph graph = createGraph(data);
                    System.out.println("Graph done in: " + String.valueOf(System.currentTimeMillis() - startTime));
                    // execute algo
                    EdgeBetweennessClusterer clusterer = new EdgeBetweennessClusterer<>(3);
                    HashSet<HashSet<String>> set = remapSet(clusterer.apply(graph));
                    System.out.println("Clustered in: " + String.valueOf(System.currentTimeMillis() - startTime));

                    // sparql update triples to endpoint
                    storeResults(set);
                } catch (Exception e) {
                    throw new JPSRuntimeException(e);
                } finally {
                    stop();
                }
            }
        }
    }

    public UndirectedSparseGraph createGraph(JSONArray array) {
        /*
        @todo: check ontology for duplicates:
        java.lang.IllegalArgumentException: Nodes http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#CemeteryUse and http://www.theworldavatar.com/ontology/ontozoning/OntoZoning.owl#Cemetery are already connected by a different edge. To construct a graph that allows parallel edges, call allowsParallelEdges(true) on the Builder.
         */
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

    private HashSet<HashSet<String>> remapSet(Set set) {
        Map<Integer, String> revUrlToNum = MapUtils.invertMap(urlToNum);

        HashSet<HashSet<String>> remapped = new HashSet<>();

        for (Object subset : set) {
            HashSet<String> subsetS = new HashSet<>();
            for (Object element : (HashSet) subset) {
                subsetS.add(revUrlToNum.get(element));
            }
            remapped.add(subsetS);
        }

        return remapped;
    }

    private void storeResults(HashSet<HashSet<String>> clusters) {

        UpdateBuilder ub = prepareUpdateBuilder();

        int counter = 0;
        int clusterNum = 0;
        for (HashSet<String> cluster : clusters) {
            clusterNum = clusterNum + 1;
            //prepare inference update for a node
            for (String element : cluster) {
                prepareUpdate(clusterNum, ub, element);
                counter++;

                //store data if counter of nodes is 100 and start over
                if (counter >= 100) {
                    ub = persistUpdate(ub);

                    counter = 0;
                }
            }

        }
        //store any remaining data
        if (counter > 0)  {
            persistUpdate(ub);
        }

    }

    private void prepareUpdate(int clusterNum, UpdateBuilder ub, String vert) {
        String pfix = GraphInferenceAgent.ONINF_PREFIX;
        Node id = NodeFactory.createURI(targetGraph.getURI() + UUID.randomUUID());
        ub.addInsert(targetGraph, id, pfix + ":" + GraphInferenceAgent.ONINT_P_INOBJ,
            NodeFactory.createURI(vert));
        ub.addInsert(targetGraph, id, pfix + ":" + GraphInferenceAgent.ONINT_P_INALG,
            NodeFactory.createURI(GraphInferenceAgent.ONINF_SCHEMA + GraphInferenceAgent.ONINT_C_EBALG));
        ub.addInsert(targetGraph, id, pfix + ":" + GraphInferenceAgent.ONINT_P_INVAL,
            NodeFactory.createLiteral(String.valueOf(clusterNum), XSDDatatype.XSDinteger));
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