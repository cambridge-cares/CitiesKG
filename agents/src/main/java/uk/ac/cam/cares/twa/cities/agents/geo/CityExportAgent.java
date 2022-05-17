package uk.ac.cam.cares.twa.cities.agents.geo;

import com.google.gson.JsonElement;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.query.algebra.Str;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.twa.cities.tasks.ExporterTask;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.io.File;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import uk.ac.cam.cares.twa.cities.tasks.KMLParserTask;
import uk.ac.cam.cares.twa.cities.tasks.KMLSorterTask;
import uk.ac.cam.cares.twa.cities.tasks.KMLTilingTask;

/**
 * A JPSAgent framework-based CityExportAgent class used to export urban model
 * from the Semantic 3D City Database {@link <ahref="https://www.cares.cam.ac.uk/research/cities/">}.
 * The agent listens to GET requests containing either the IRI of a building or
 * the IRI of a namespace denoted with *, and queries the building or buildings in the namespace
 * with different level of detail ("lod2" or "lod3") , different displayforms ("extruded" or "geometry")
 * The exported City model is stored in KML file
 *
 * @author <a href="mailto:shiying.li@sec.ethz.ch">Shiying Li</a>
 * @version $Id$
 */

@WebServlet(
        urlPatterns = {
                CityExportAgent.URI_ACTION
        })

public class CityExportAgent extends JPSAgent {

    // Agent endpoint and parameter keys
    public static final String URI_ACTION = "/export/kml";
    public static final String KEY_GMLID = "gmlid";
    public static final String KEY_DIR = "directory";
    public static final String KEY_REQ_URL = "requestUrl";
    public static final String KEY_REQ_METHOD = "method";
    public static final String KEY_NAMESPACE = "namespace";
    public static final String KEY_LOD = "lod";
    public static final String KEY_DISPLAYFORM = "displayform";

    // Export files names
    private String outFileName = "test";
    private String outFileExtension = ".kml";

    private String outTmpDir;
    private String namespaceIri;
    private String requestUrl;
    private JSONArray gmlidParams;
    private String directory;
    private JSONObject serverInfo;
    private int lod;
    private String displayForm;


    // Default task parameters
    //@todo: ImpExp.main() fails if there is more than one thread of it at a time. It needs further investigation.
    private final int NUM_IMPORTER_THREADS = 1;
    private final ThreadPoolExecutor exporterExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_IMPORTER_THREADS);


    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {
        JSONObject result = new JSONObject();

        if (validateInput(requestParams)) {
            requestUrl = requestParams.getString(KEY_REQ_URL);
            namespaceIri = requestParams.getString(KEY_NAMESPACE);
            serverInfo = getServerInfo(namespaceIri);
            directory = requestParams.getString(KEY_DIR);

            //gmlids = getInputGmlids(requestParams); // this method will process the input when it is a path or an array of gmlid
            // If gmlid contains "*", it requires the whole list of gmlid from the namespace
            // This step can take long as querying the database is long with this sparql query
            // 3 modes:
            // 1.1) [* , path to a directory where the list of gmlids is prepared]
            // 1.2) [*]
            // 2) explicit gmlids

            gmlidParams = requestParams.getJSONArray(KEY_GMLID);

            if (gmlidParams.length() == 2 && gmlidParams.get(0).equals("*")) {
                String preparedGmlids = gmlidParams.get(1).toString();
                ArrayList<Path> fileList = new ArrayList<>();
                try (Stream<Path> paths = Files.walk(Paths.get(preparedGmlids))) {
                    paths
                        .filter(Files::isRegularFile)
                        .forEach(fileList::add);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                for (Path gmlidFile : fileList) {
                    // Retrieve a list of gmlids from a file, and execute the export process
                    String[] gmlidArray = getGmlidFromFile(gmlidFile);
                    String outputFileName = getOutputPath(gmlidFile);
                    result.put("outputPath", exportKml(gmlidArray, outputFileName, serverInfo));
                }
            }else{
                ArrayList<String> buildingIds = new ArrayList<>();
                for (Object id : gmlidParams) {
                    buildingIds.add(id.toString());
                }
                String[] gmlidsArray = new String[buildingIds.size()];
                gmlidsArray = buildingIds.toArray(gmlidsArray);
                result.put("outputPath", exportKml(gmlidsArray, getOutputPath(), serverInfo));
            }

        }
        // It will return the file path of the exported file
        System.out.println(result);
        return result;
    }

    @Override
    public boolean validateInput(JSONObject requestParams) throws BadRequestException {

        if (!requestParams.isEmpty() && requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.POST)) {
            Set<String> keys = requestParams.keySet();
            try {
                // Check if namespace is empty or valid
                if (!requestParams.getString(KEY_NAMESPACE).isEmpty()){
                    namespaceIri = new URI(requestParams.getString(KEY_NAMESPACE)).toString();
                }
                // Check if gmlid is empty or valid
                if (!requestParams.getJSONArray(KEY_GMLID).isEmpty()){
                    JSONArray gmlidArgs = requestParams.getJSONArray(KEY_GMLID);
                    if (gmlidArgs.length() == 2 && gmlidArgs.get(0).equals("*")) {
                        // For the * case: Check if the second field of gmlid array is valid director
                        if (!Files.exists(Paths.get(gmlidArgs.get(1).toString()))){
                            throw new BadRequestException("Error: The prepared_gmlid directory does not exist.");
                        }
                    }
                }
                // Check if the output directory is empty or valid
                if (!requestParams.getString(KEY_DIR).isEmpty() && Files.exists(Paths.get(requestParams.getString(KEY_DIR)))) {
                    System.out.println("Valid directory: " + requestParams.getString(KEY_DIR));
                } else {
                    throw new BadRequestException("Error: The given input directory is invalid");
                }

                // Check if the LOD is within 1 - 3


                // Check the displayform




                return true;
            } catch (Exception e) {
                throw new BadRequestException();
            }
        }
        throw new BadRequestException();
    }

    private String[] getGmlidFromFile(Path gmlidFile){
        List<String> gmlidsList =  new ArrayList<>();
        try {
            gmlidsList = Files.readAllLines(Paths.get(gmlidFile.toString()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] gmlidsArray = new String[gmlidsList.size()];

        for (int i = 0; i < gmlidsList.size(); ++i){
            String[] elemnets = gmlidsList.get(i).split("/");
            gmlidsArray[i] = elemnets[elemnets.length-1];
        }
        return gmlidsArray;
    }

    /**
     * create the output path of the generated kml file for indexed input files.
     * @param - indexed file
     * @return - the output location of the kml file
     */
    private String getOutputPath (Path inputFile) {

        File outputDir = new File (Paths.get(directory, "export").toString());
        if (!outputDir.exists()){ outputDir.mkdirs(); }

        if (inputFile ==  null) {
            return Paths.get(outputDir.toString(), outFileName + outFileExtension).toString();
        }

        String path = inputFile.toString().replace("\\", "/");
        String[] elem = path.split("/");
        String filename = elem[elem.length - 1];
        String index = filename.substring(filename.indexOf("_") + 1, filename.indexOf("."));
        String exportFilename = outFileName + "_" + index + outFileExtension;
        return Paths.get(outputDir.toString(), exportFilename).toString();

    }

    /**
     * create the output path of the generated kml file as default no arguments.
     * @return the output location of the kml file
     */
    private String getOutputPath () {
        return getOutputPath(null);
    }

    /**
     * retrieves the server information from the endpointUri -- host, port, namespace
     *
     * @param  - endpointUri which is namespaceIri that contains
     * @return - JSONObject with key/value: host, port, namespace
     */

    private JSONObject getServerInfo (String endpointUri) {

        JSONObject serverInfo = new JSONObject();
        //JSONArray uriGmlids = requestParams.getJSONArray(KEY_GMLID);
        //String endpointUri = uriGmlids.get(0).toString();

        String[] splitstr = endpointUri.split("/");

        String url = splitstr[2];
        String server = null;
        String port = null;
        if (url.contains(":")){
            String[] serverport = url.split(":");
            server = serverport[0];
            port = serverport[1];
        }

        String[] subarray = Arrays.copyOfRange(splitstr, 3, splitstr.length);
        String namespace = "/" + String.join("/", subarray) + "/";

        if (server.isEmpty() || port.isEmpty() || namespace.isEmpty()) {
            return null;
        }
        serverInfo.put ("host", server);
        serverInfo.put ("port", port);
        serverInfo.put ("namespace", namespace);
        return serverInfo;
    }

    private String exportKml (String[] gmlIds, String outputpath, JSONObject serverInfo){
        String actualPath = outputpath.replace(".kml", "_extruded.kml");
        ExporterTask task = new ExporterTask(gmlIds, outputpath, serverInfo);
        exporterExecutor.execute(task);
        return actualPath;
    }

    /**
     * Re-arrange and tiling the unsorted KMLs output from ImpExp tool
     *
     * @param path2unsortedKML the path of the directory that contains the unsorted KMLs
     */
    private static void tilingKML(String path2unsortedKML, String outputDir){
        //String inputfile = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder_1\\charlottenberg_extruded_blaze.kml";
        //String inputDir = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\exported_data_whole\\";
        String CRSinDegree = "4326";  // global WGS 84
        String CRSinMeter = "32648";//"32648"; "25833"
        int initTileSize = 250;
        long start = System.currentTimeMillis();

        // 1. KMLParserTask
        KMLParserTask parserTask = new KMLParserTask(path2unsortedKML, outputDir, CRSinDegree, CRSinMeter, initTileSize);
        parserTask.run();
        double[] updatedExtent = parserTask.getUpdatedExtent();
        String summaryCSV = parserTask.getOutFile();

        // 2. KMLTilingTask
        KMLTilingTask kmltiling = new KMLTilingTask(CRSinDegree, CRSinMeter, outputDir, initTileSize);  // 25833
        kmltiling.setUp(summaryCSV);
        kmltiling.updateExtent(updatedExtent);  // whole berlin: new double[]{13.09278683392157, 13.758936971880468, 52.339762874361156, 52.662766032905616}
        kmltiling.run();
        String masterJSONFile = kmltiling.getmasterJSONFile();
        String sortedCSVFile = kmltiling.getsortedCSVFile();


        // 3. KMLSorterTask
        KMLSorterTask kmlSorter=new KMLSorterTask(path2unsortedKML, outputDir, masterJSONFile, sortedCSVFile);
        kmlSorter.run();
        long end = System.currentTimeMillis();
        System.out.println("The re-arrangement of KMLs takes: " + (end - start) / 1000 + " seconds.");

    }

    public static void main(String[] args) {
        //String inputDir = "C:\\Users\\ShiyingLi\\Documents\\CKG\\Exported_data\\exported_data_whole\\";
        String inputDir = "C:\\Users\\ShiyingLi\\Documents\\CKG\\Exported_data\\ura_footprint_2000";
        //String inputDir = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\charlottenberg_extruded_blaze.kml";
        String outputDir = "C:\\Users\\ShiyingLi\\Documents\\CKG\\Exported_data\\testfolder\\";
        tilingKML(inputDir, outputDir);
    }

}
