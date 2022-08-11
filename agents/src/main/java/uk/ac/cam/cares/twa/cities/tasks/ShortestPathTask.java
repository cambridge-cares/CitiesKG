package uk.ac.cam.cares.twa.cities.tasks;

import com.hp.hpl.jena.rdf.model.*;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import edu.uci.ics.jung.algorithms.shortestpath.ShortestPathUtils;
import edu.uci.ics.jung.algorithms.shortestpath.UnweightedShortestPath;
import edu.uci.ics.jung.graph.Graph;
import net.rootdev.jenajung.JenaJungGraph;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.model.IRI;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.agents.GraphInferenceAgent;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

public class ShortestPathTask implements UninitialisedDataQueueTask {
    private final IRI taskIri = IRI.create(GraphInferenceAgent.ONINF_SCHEMA + GraphInferenceAgent.TASK_SP);
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
                    ArrayList<RDFNode> vertices = (ArrayList<RDFNode>) graph.getVertices().stream().collect(Collectors.toList());
                    int total = vertices.size();
                    int count = 1;
                    int fileNum = 0;

                    UnweightedShortestPath<RDFNode, Statement> shortestPath = new UnweightedShortestPath<>(graph);

                    List<String[]> dataContent = new ArrayList<>();
                    String[] header = {"source", "target", "shortest path", "edge1", "edge2", "edge3", "edge4"}; //@TODO: max length of shortest path hardcoded as 4
                    dataContent.add(header);
                    for (RDFNode source : vertices) {
                        System.out.println("Computing shortest path for node " + source.toString() + ", " + count + "/" + total);
                        computeShortestPath(graph, shortestPath, source, dataContent);
                        if (count % 500 == 0 || count == total) {
                            String path = "" + fileNum + ".csv";
                            if (writeCsv(new File(path), dataContent)) {
                                dataContent.clear();
                                dataContent.add(header);
                                fileNum++;
                            }
                        }
                        count++;
                    }
                } catch (Exception e) {
                    throw new JPSRuntimeException(e);
                } finally {
                    stop();
                }
            }
        }
    }

    public Boolean writeCsv(File outputCSV, List<String[]> dataContent) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputCSV.toString()))) {
            System.out.println("Writing csv to " + outputCSV.getAbsolutePath());
            writer.writeAll(dataContent);
        } catch (IOException e) {
            throw new JPSRuntimeException("Writing csv failed");
        }
        return true;
    }

    public void computeShortestPath(Graph graph, UnweightedShortestPath<RDFNode, Statement> shortestPath, RDFNode source, List<String[]> dataContent) {
        Map<RDFNode, Number> distMap = shortestPath.getDistanceMap(source);
        for (RDFNode target : distMap.keySet()) {
            List<Statement> list = ShortestPathUtils.getPath(graph, shortestPath, source, target);
            String[] row = new String[7]; //@TODO: max length of shortest path hardcoded as 4
            row[0] = source.toString();
            row[1] = target.toString();
            row[2] = distMap.get(target).toString();
            for (int i = 0; i < list.size(); i++) {
                row[i + 3] = list.get(i).toString();
            }
            dataContent.add(row);
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

    // main method to run analysis on csv files
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        ShortestPathTask task = new ShortestPathTask();
        String dirPath = ""; // replace with path to dir

        // read csv
        File dir = new File(dirPath);
        File[] files = dir.listFiles();

        // count the distribution of shortest path lengths
        HashMap<Integer, Integer> lengthCount = new HashMap<>(); // key -> length of shortest path, value -> occurrence
        // count the distribution of edges
        HashMap<String, Integer> edgeDistributionCount = new HashMap<>(); // key -> statement string, value -> occurrence
        // count the distribution of target vertices
        HashMap<String, HashMap<Integer, Integer>> targetDistributionCount = new HashMap<>(); // key -> target vertex string, value -> map of shortest path length, occurrence
        // count the distribution of source vertices
        HashMap<String, HashMap<Integer, Integer>> sourceDistributionCount = new HashMap<>(); // key -> source vertex string, value -> map of shortest path length, occurrence

        // loop through
        for (File file : files) {
            if (file.isDirectory()) continue;
            try (CSVReader reader = new CSVReader(new FileReader(file.getAbsolutePath()))) {
                System.out.println("process: " + file.getAbsolutePath());
                String[] csvHeader = reader.readNext();
                List<String[]> fileData = reader.readAll();
                for (int i = 0; i < fileData.size(); i++) {
                    String[] row = fileData.get(i);

                    // get length count
                    int newLengthCount = lengthCount.getOrDefault(Integer.valueOf(row[2]), 0) + 1;
                    lengthCount.put(Integer.valueOf(row[2]), newLengthCount);

                    if (Integer.valueOf(row[2]) == 0) {
                        continue;
                    }

                    // get edge count
                    for (int j = 3; j <= 6; j++) {
                        if (row[j].isEmpty()) continue;
                        int newEdgeCount = edgeDistributionCount.getOrDefault(row[j], 0) + 1;
                        edgeDistributionCount.put(row[j], newEdgeCount);
                    }

                    // get target count
                    String target = row[1];
                    String length = row[2];
                    if (targetDistributionCount.containsKey(target)) {
                        HashMap<Integer, Integer> map = targetDistributionCount.get(target);
                        int newTargetCount = map.getOrDefault(Integer.valueOf(length), 0) + 1;
                        map.put(Integer.valueOf(length), newTargetCount);
                    } else {
                        HashMap<Integer, Integer> map = new HashMap<>();
                        map.put(Integer.valueOf(length), 1);
                        targetDistributionCount.put(target, map);
                    }

                    // get source count
                    String source = row[0];
                    if (sourceDistributionCount.containsKey(source)) {
                        HashMap<Integer, Integer> map = sourceDistributionCount.get(source);
                        int newSourceCount = map.getOrDefault(Integer.valueOf(length), 0) + 1;
                        map.put(Integer.valueOf(length), newSourceCount);
                    } else {
                        HashMap<Integer, Integer> map = new HashMap<>();
                        map.put(Integer.valueOf(length), 1);
                        sourceDistributionCount.put(source, map);
                    }
                }
            } catch (IOException | CsvException e) {
                e.printStackTrace();
            }
        }

        for (int len : lengthCount.keySet()) {
            System.out.println(len + ": " + lengthCount.get(len));
        }

        ArrayList<String[]> edgeData = new ArrayList<>();
        String[] header = {"edge", "count"};
        edgeData.add(header);
        for (String key : edgeDistributionCount.keySet()) {
            String[] row = {key, edgeDistributionCount.get(key).toString()};
            edgeData.add(row);
        }
        task.writeCsv(new File(dirPath + "\\number of times each edge appears on a shortest path.csv"), edgeData);

        ArrayList<String[]> targetData = new ArrayList<>();
        String[] targetHeader = {"target", "0", "1", "2", "3", "4"}; //@TODO: max length of shortest path hardcoded as 4
        targetData.add(targetHeader);
        for (String key : targetDistributionCount.keySet()) {
            HashMap<Integer, Integer> map = targetDistributionCount.get(key);
            String[] row = {key, map.getOrDefault(0, 0).toString(),
                    map.getOrDefault(1, 0).toString(),
                    map.getOrDefault(2, 0).toString(),
                    map.getOrDefault(3, 0).toString(),
                    map.getOrDefault(4, 0).toString()};
            targetData.add(row);
        }
        task.writeCsv(new File(dirPath + "\\shortest path length to each target.csv"), targetData);

        ArrayList<String[]> sourceData = new ArrayList<>();
        String[] sourceHeader = {"source", "0", "1", "2", "3", "4"}; //@TODO: max length of shortest path hardcoded as 4
        sourceData.add(sourceHeader);
        for (String key : sourceDistributionCount.keySet()) {
            HashMap<Integer, Integer> map = sourceDistributionCount.get(key);
            String[] row = {key, map.getOrDefault(0, 0).toString(),
                    map.getOrDefault(1, 0).toString(),
                    map.getOrDefault(2, 0).toString(),
                    map.getOrDefault(3, 0).toString(),
                    map.getOrDefault(4, 0).toString()};
            sourceData.add(row);
        }
        task.writeCsv(new File(dirPath + "\\shortest path length from each source.csv"), sourceData);

        long end = System.currentTimeMillis();
        System.out.println("Analysis took " + (end - start) / 1000 + " s");
    }
}
