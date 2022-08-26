package uk.ac.cam.cares.twa.cities.tasks;

import com.hp.hpl.jena.rdf.model.*;
import edu.uci.ics.jung.algorithms.cluster.EdgeBetweennessClusterer;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.model.IRI;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.agents.GraphInferenceAgent;

import java.util.*;
import java.util.concurrent.BlockingQueue;

public class EdgeBetweennessTask implements UninitialisedDataQueueTask {

    private final IRI taskIri = IRI.create(
        GraphInferenceAgent.ONINF_SCHEMA + GraphInferenceAgent.TASK_EB);
    private boolean stop = false;
    private BlockingQueue<Map<String, JSONArray>> dataQueue;
    private String targetGraph;

    @Override
    public IRI getTaskIri() {
        return taskIri;
    }

    @Override
    public void setStringMapQueue(BlockingQueue<Map<String, JSONArray>> queue) {
        this.dataQueue = queue;
    }

    @Override
    public void setTargetGraph(String tg) {
        targetGraph = tg;
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
                    EdgeBetweennessClusterer clusterer = new EdgeBetweennessClusterer<>(3);
                    Set<Set<RDFNode>> set = clusterer.apply(graph);

                    int size = set.size();

                    // sparql update triples to endpoint
                    //update(set);
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



        for (Object data : array) {
            JSONObject obj = (JSONObject) data;

            Resource s = ResourceFactory.createResource(obj.getString("s"));
            Property p = ResourceFactory.createProperty(obj.getString("p"));
            Resource o = ResourceFactory.createResource(obj.getString("o"));
            graph.addEdge(ResourceFactory.createStatement(s, p, o), s, o);
        }

        return graph;
    }

    /*
    public void update(Set<HashSet> set) {
        Collection<Quad> collection = new ArrayList<>();
        Node g = NodeFactory.createURI("http://127.0.0.1:9999/blazegraph/namespace/singaporeEPSG4326/sparql/results/");
        Node p = NodeFactory.createURI("http://www.theworldavatar.com/ontologies/OntoInfer.owl#cluster");
        Iterator<HashSet> iterator = set.stream().iterator();
        int cluster = 0;
        while (iterator.hasNext()) {
            cluster++;
            HashSet<ResourceImpl> hs = iterator.next();
            for (ResourceImpl resource : hs) {
                collection.add(new Quad(g, NodeFactory.createURI(resource.getURI()), p, NodeFactory.createLiteral(String.valueOf(cluster))));
            }
        }
        UpdateBuilder ub = new UpdateBuilder();
        ub.addInsertQuads(collection);
        AccessAgentCaller.updateStore("http://localhost:48080/singaporeEPSG4326", ub.build().toString());
    }
     */

}