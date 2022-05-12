package uk.ac.cam.cares.twa.cities.agents.geo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.json.JSONArray;
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

    // Export files names
    private String outFileName = "test";
    private String outFileExtension = ".kml";

    private String outTmpDir;
    private String requestUrl;
    private String[] gmlids;
    private String directory;
    private JSONObject serverInfo;

    // Default task parameters
    //@todo: ImpExp.main() fails if there is more than one thread of it at a time. It needs further investigation.
    private final int NUM_IMPORTER_THREADS = 1;
    private final ThreadPoolExecutor exporterExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_IMPORTER_THREADS);


    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {
        JSONObject result = new JSONObject();


        if (validateInput(requestParams)) {
            requestUrl = requestParams.getString(KEY_REQ_URL);
            gmlids = getInputGmlids(requestParams); // this method will process the input when it is a path or an array of gmlid
            serverInfo = getServerInfo(requestParams.getJSONArray(KEY_GMLID).get(0).toString());
            directory = requestParams.getString(KEY_DIR);

            // If gmlid contains "*", it requires the whole list of gmlid from the namespace
            // This step can take long as querying the database is long with this sparql query
            // 3 modes:
            // 1.1) [* , path to a directory where the list of gmlids is prepared]
            // 1.2) [*]
            // 2) explicit gmlids

            if (Files.exists(Paths.get(gmlids[1]))) {
                String rootDir = gmlids[1];
                ArrayList<Path> fileList = new ArrayList<>();
                try (Stream<Path> paths = Files.walk(Paths.get(rootDir))) {
                    paths
                        .filter(Files::isRegularFile)
                        .forEach(fileList::add);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                for (Path gmlidFile : fileList) {
                    String[] gmlidArray = getGmlidFromFile(gmlidFile);
                    String outputFileName = getOutputPath(gmlidFile);
                    result.put("outputPath", exportKml(gmlidArray, outputFileName, serverInfo));
                }
            }else{
                result.put("outputPath", exportKml(gmlids, getOutputPath(), serverInfo));
            }

        }
        // It will return the file path of the exported file
        System.out.println(result);
        return result;
    }

    @Override
    public boolean validateInput(JSONObject requestParams) throws BadRequestException {

        boolean error = true;
        if (!requestParams.isEmpty()) {
            Set<String> keys = requestParams.keySet();
            if (keys.contains(KEY_REQ_METHOD)) {
                if (requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.POST)) {
                    try {
                        // Check if gmlid is empty
                        if (!requestParams.getJSONArray(KEY_GMLID).isEmpty()){
                            error = false;
                        }
                        // Check if the output directory is empty or valid
                        if (!requestParams.getString(KEY_DIR).isEmpty() && Files.exists(Paths.get(requestParams.getString(KEY_DIR)))) {
                            System.out.println("Valid directory: " + requestParams.getString(KEY_DIR));
                        } else {
                            System.out.println("Error: The given input directory is invalid");
                            error = false;
                        }

                    } catch (Exception e) {
                        throw new BadRequestException();
                    }
                }
            }
        }
        if (error) {
            throw new BadRequestException();
        }
        return !error;
    }

    /**
     * retrieves a list of gmlids from a list of CityObjectUris.
     *
     * @param requestParams requestParams sent to the agent
     * @return a list of gmlids as string[].
     */
    private String[] getInputGmlids (JSONObject requestParams) {

        ArrayList<String> uris = new ArrayList<>();
        JSONArray uriGmlids = requestParams.getJSONArray(KEY_GMLID);

        for (Object iri : uriGmlids) {
            uris.add(iri.toString());
        }

        if (uris == null || uris.size() == 0){
            return null;
        }

        String[] gmlids = new String[uris.size()];

        for (int i = 0; i < uris.size(); ++i){
            String[] elemnets = uris.get(i).split("/");
            gmlids[i] = elemnets[elemnets.length-1];
        }

        return gmlids;
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

    private String getOutputPath (Path gmlidFile) {
        //ResourceBundle rb = ResourceBundle.getBundle("config");
        //outTmpDir = rb.getString("outputDir") + "export";
        outTmpDir = Paths.get(directory, "export").toString();
        File outputdir = new File (outTmpDir);
        if (!outputdir.exists()){
            outputdir.mkdirs();
        }
        String path = gmlidFile.toString().replace("\\", "/");
        String[] elem = path.split("/");
        //String index = elem[elem.length-1].replaceAll("[^0-9]", "");
        String filename = elem[elem.length-1];
        String index = filename.substring(filename.indexOf("_") + 1, filename.indexOf("."));
        String exportFilename = outFileName + "_" + index + outFileExtension;
        String outputPath = Paths.get(outTmpDir, exportFilename).toString();
        return outputPath;
    }

    /**
     * retrieves the server information from the endpointUri -- host, port, namespace
     *
     * @param  - requestParams sent to the agent
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

        String[] subarray = Arrays.copyOfRange(splitstr, 3, 7);
        String namespace = "/" + String.join("/", subarray) + "/";

        if (server.isEmpty() || port.isEmpty() || namespace.isEmpty()) {
            return null;
        }
        serverInfo.put ("host", server);
        serverInfo.put ("port", port);
        serverInfo.put ("namespace", namespace);
        return serverInfo;
    }

    /**
     * define the output path of the generated kml file.
     *
     * the root folder is predefined in config.properties, a folder called export will be created
     * @return the output location of the kml file
     */
    private String getOutputPath () {
        //esourceBundle rb = ResourceBundle.getBundle("config");
        //outTmpDir = rb.getString("outputDir") + "export";
        outTmpDir = Paths.get(directory, "export").toString();
        File outputdir = new File (outTmpDir);
        if (!outputdir.exists()){
            outputdir.mkdirs();
        }
        String outputPath = Paths.get(outTmpDir, outFileName, outFileExtension).toString();
        return outputPath;
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
