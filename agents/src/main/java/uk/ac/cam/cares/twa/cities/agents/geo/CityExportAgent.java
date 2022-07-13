package uk.ac.cam.cares.twa.cities.agents.geo;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.apache.http.HttpException;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.tasks.geo.ExporterTask;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.io.File;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
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

    public static class Params {
        public String namespaceIri;
        public String outputDir;
        public JSONObject serverInfo;
        public String[] displayMode;
        public String outputPath;
        public String[] gmlIds;
        public int srid;
        public int lod;
        public Params (String namespaceIri, JSONObject serverInfo, int srid, String outputDir, String outputPath, String[] displayMode, int lod, String[] gmlIds){
            this.namespaceIri = namespaceIri;
            this.outputDir = outputDir;
            this.serverInfo = serverInfo;
            this.displayMode = displayMode;
            this.outputPath = outputPath;  // export/kmlFiles
            this.lod = lod;
            this.gmlIds = gmlIds;
            this.srid = srid;
        }
    }

    // Agent endpoint and parameter keys
    public static final String URI_ACTION = "/export/kml";
    public static final String KEY_GMLID = "gmlid";
    public static final String KEY_REQ_URL = "requestUrl";
    public static final String KEY_REQ_METHOD = "method";
    public static final String KEY_NAMESPACE = "namespace";
    public static final String KEY_DISPLAYFORM = "displayform";
    public static final String KEY_LOD = "lod";

    // Export files names
    private static final String outFileName = "test";
    private static final String outFileExtension = ".kml";

    private String namespaceIri;
    private String outputDir;
    private int srid;
    private int lod;

    private String inputDisplayForm;
    private static final String[] displayOptions = {"FOOTPRINT", "EXTRUDED", "GEOMETRY", "COLLADA"};
    private String namespaceName;
    private static final String tmpDirsLocation = System.getProperty("java.io.tmpdir");    //System.getProperty("java.io.tmpdir");   //"C:/tmp";  // can not have empty space on the path
    // Default task parameters
    //@todo: ImpExp.main() fails if there is more than one thread of it at a time. It needs further investigation.
    private final int NUM_EXPORTER_THREADS = 1;
    private final ThreadPoolExecutor exporterExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_EXPORTER_THREADS);


    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {
        JSONObject result = new JSONObject();

        if (validateInput(requestParams)) {

            // Process "namespaceIri"
            namespaceIri = requestParams.getString(KEY_NAMESPACE);
            JSONObject serverInfo = getServerInfo(namespaceIri);
            srid = getCrsInfo(namespaceIri);  // "EPSG:25833"
            //srsName = "EPSG:4326";

            // Process "displayform"
            List<String> availOptions = Arrays.asList(displayOptions);
            inputDisplayForm = requestParams.getString(KEY_DISPLAYFORM);
            int index = availOptions.indexOf(inputDisplayForm.toUpperCase());
            String[] displayMode = {"false","false", "false", "false"};
            displayMode[index] = "true";

            // Process "lod"
            lod = requestParams.getInt(KEY_LOD);

            //gmlids = getInputGmlids(requestParams); // this method will process the input when it is a path or an array of gmlid
            // If gmlid contains "*", it requires the whole list of gmlid from the namespace
            // This step can take long as querying the database is long with this sparql query
            // 2 modes:
            // 1) [*]  --> tiling is mandatory
            // 2) explicit gmlids in an array

            outputDir = Paths.get(tmpDirsLocation, "export").toString();
            JSONArray gmlidParams = requestParams.getJSONArray(KEY_GMLID);

            if (gmlidParams.length() == 1 && gmlidParams.get(0).equals("*")) {
                String preparedGmlids = Paths.get(tmpDirsLocation, namespaceName).toString();
                File gmlidDir = new File(preparedGmlids);

                if (gmlidDir.isDirectory() && Objects.requireNonNull(gmlidDir.list()).length > 0) {
                    System.out.println("Found the prepared GMLID list and number of files = " + Objects.requireNonNull(gmlidDir.list()).length);
                    ArrayList<Path> fileList = new ArrayList<>();
                    try (Stream<Path> paths = Files.walk(Paths.get(preparedGmlids))) {
                        paths
                            .filter(Files::isRegularFile)
                            .forEach(fileList::add);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    for (Path path : fileList) {
                        // Retrieve a list of gmlids from a file, and execute the export process
                        String[] gmlidArray = getGmlidFromFile(path);
                        String outputFileName = getOutputName(path);
                        Params taskParams = new Params(namespaceIri, serverInfo, srid, outputDir, outputFileName, displayMode, lod, gmlidArray);
                        exportKml(taskParams);
                    }
                    System.out.println("After for loop, Thread Name: " + Thread.currentThread().getName());
                } else {
                    // the gmlidParams in this case is * without pre-prepared gmlid list, tiling will be triggered.
                    exportFromDefinedGmlids(gmlidParams, serverInfo, displayMode);
                }
                tilingKml();
            }else{
                // the gmlidParams in this case is a list of explicit gmlids, no tiling will be triggered.
                exportFromDefinedGmlids(gmlidParams, serverInfo, displayMode);
            }
        }
        // It will return the file path of the exported file
        System.out.println("This msg is coming from the end of CityExportAgent");
        System.out.println("Thread Name: " + Thread.currentThread().getName());
        System.out.println(result);  //{}
        return result;
    }

    @Override
    public boolean validateInput(JSONObject requestParams) throws BadRequestException {

        if (!requestParams.isEmpty() && requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.POST)) {
            try {
                // Check if namespace is empty or valid
                if (!requestParams.getString(KEY_NAMESPACE).isEmpty()){
                    namespaceIri = new URI(requestParams.getString(KEY_NAMESPACE)).toString();
                }
                // Check if gmlid is empty or valid
                if (!requestParams.getJSONArray(KEY_GMLID).isEmpty()){
                    System.out.println("Not empty gmlid inputs");
                }

                // Check the displayform
                if (!requestParams.getString(KEY_DISPLAYFORM).isEmpty()) {
                    List<String> availOptions = Arrays.asList(displayOptions);
                    String inputDisplayForm = requestParams.getString(KEY_DISPLAYFORM);
                    int index = availOptions.indexOf(inputDisplayForm.toUpperCase());
                    if (index >= 0){
                        System.out.println("Valid displayform: " + inputDisplayForm);
                    } else {
                        System.out.println("InValid displayform: " + inputDisplayForm);
                        throw new IllegalArgumentException();
                    }
                }

                // Check if the lod is correctly set
                if (requestParams.getInt(KEY_LOD) >= 0 && requestParams.getInt(KEY_LOD) <= 5){
                    System.out.println("Valid LOD : " + requestParams.getInt(KEY_LOD));
                }

                return true;
            } catch (Exception e) {
                throw new BadRequestException();
            }
        }
        throw new BadRequestException();
    }

    /**
     * Retrieve the gmlids from the file containing pre-prepared gmlis
     *
     * @param gmlidFile - path to the pre-prepared gmlid file
     * @return String[] - a string array containing the gmlids
     */
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

    /** Using HTTP request to query the TWA and get the srsInfo
     *  The postprocessing of the response is a bit complicated
     *  create the output path of the generated kml file for indexed input files.
     *
     * @param namespaceIri - the namespace iri for querying the crs information
     * @return int - srid
     */
    private int getCrsInfo (String namespaceIri)  {
        String sparqlquery = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#> \n" +
            "SELECT ?s ?srid ?srsname { ?s ocgml:srid ?srid; ocgml:srsname ?srsname }";

        int srid;

        try { HttpResponse<?> response = Unirest.post(namespaceIri)
            .header(HTTP.CONTENT_TYPE, "application/sparql-query")
            .body(sparqlquery)
            .socketTimeout(300000)
            .asString();
            int respStatus = response.getStatus();
            String responseBody;
            // handle the response
            if (respStatus != HttpURLConnection.HTTP_OK) {
                throw new HttpException(namespaceIri + " " + respStatus);
            } else {
                responseBody = (String) response.getBody();
                JSONObject srsObject = (JSONObject) XML.toJSONObject(responseBody).getJSONObject("sparql").getJSONObject("results").getJSONObject("result").getJSONArray("binding").get(1);  // determined by the query
                srid = srsObject.getJSONObject("literal").getInt("content");
            }
        } catch ( HttpException | UnirestException e) {
            throw new JPSRuntimeException(e);
        }

        return srid;
    }

    /**
     * create the output path of the generated kml file for indexed input files.
     *
     * @param inputFile - indexed file
     * @return String - the output location of the kml file
     */
    private String getOutputName (Path inputFile) {

        // Create the export directory if this does not exist
        if (!new File(outputDir).exists()){ new File(outputDir).mkdirs(); }

        String outputPath;
        String path;
        if (inputFile ==  null) {
            outputPath =  Paths.get(outputDir, outFileName + outFileExtension).toString();

        } else {
            path = inputFile.toString().replace("\\", "/");
            String[] elem = path.split("/");
            String filename = elem[elem.length - 1];
            String index = filename.substring(filename.indexOf("_") + 1, filename.indexOf("."));
            String exportFilename = outFileName + "_" + index + outFileExtension;
            outputPath = Paths.get(outputDir, exportFilename).toString();
        }

        return outputPath;
    }

    /**
     * retrieves the server information from the endpointUri -- host, port, namespace
     *
     * @param endpointUri - endpointUri which is namespaceIri that contains
     * @return JSONObject - JSONObject with key/value: host, port, namespace
     */
    private JSONObject getServerInfo (String endpointUri) {

        JSONObject serverInfo = new JSONObject();

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
        namespaceName = subarray[2];

        assert server != null;
        if (server.isEmpty() || port.isEmpty()) {
            throw new IllegalArgumentException();
        }
        serverInfo.put ("host", server);
        serverInfo.put ("port", port);
        serverInfo.put ("namespace", namespace);
        return serverInfo;
    }

    /**
     * Exporting the kml files
     *
     * @return ExporterTask - running task
     */
    private ExporterTask exportKml (Params taskParams){

        ExporterTask exporterTask = new ExporterTask(taskParams);
        exporterExecutor.execute(exporterTask);

        return exporterTask;
    }

    /**
     * Codesnippet for preparing the gmlid list, e.g., * or particular gmlids
     *
     * @param gmlidParams - JsonArray from the agent arguments
     * @param serverInfo - server information for the query
     * @param displayMode - a string array contains true and false for the displayForm
     */
    private void exportFromDefinedGmlids(JSONArray gmlidParams, JSONObject serverInfo, String[] displayMode){
        ArrayList<String> buildingIds = new ArrayList<>();
        for (Object id : gmlidParams) {
            buildingIds.add(id.toString());
        }
        String[] gmlidsArray = new String[buildingIds.size()];
        gmlidsArray = buildingIds.toArray(gmlidsArray);
        String outSingleFileName = getOutputName(null);
        Params taskParams = new Params(namespaceIri, serverInfo, srid, outputDir, outSingleFileName, displayMode, lod, gmlidsArray);
        exportKml(taskParams);
    }

    /**
     * Re-organise the exported kml files according the pre-prepared list to geospatial tiles
     * For the srid 4326, it requires a correspoding "srid in meter" for the tiling task
     *
     * @return KMLTilingTask - running task
     */
    private KMLTilingTask tilingKml(){
        String path2unsortedKML = Paths.get(outputDir, "kmlFiles").toString() ;
        String path2sortedKML = Paths.get(outputDir).toString();
        //int databaseCRS = Integer.valueOf(srsName.split(":")[1]);
        int databaseCRS;
        if (srid == 4326){
            ResourceBundle config = ResourceBundle.getBundle("config");
            databaseCRS = Integer.parseInt(config.getString("crsInMeter.singapore"));
        } else {
            databaseCRS = srid;
        }
        KMLTilingTask kmlTilingTask = new KMLTilingTask(path2unsortedKML, path2sortedKML, databaseCRS, inputDisplayForm, namespaceIri);
        exporterExecutor.execute(kmlTilingTask);  // this step will add the final task to the exporterExecutor
        return kmlTilingTask;
    }

    /*
    private KMLTilingTask tilingKml(String path2unsortedKML, String path2sortedKML){
        String displayForm = "extruded";
        int srid = 4326; // singapore

        int databaseCRS;
        if (srid == 4326){
            ResourceBundle config = ResourceBundle.getBundle("config");
            databaseCRS = Integer.parseInt(config.getString("crsInMeter.singapore"));
        } else {
            databaseCRS = srid;
        }
        KMLTilingTask kmlTilingTask = new KMLTilingTask(path2unsortedKML, path2sortedKML, databaseCRS, displayForm, namespaceIri);
        exporterExecutor.execute(kmlTilingTask);  // this step will add the final task to the exporterExecutor
        return kmlTilingTask;
    }

    public static void main(String[] args) {
        String path2unsortedKML = "C:\\Program Files\\Apache Software Foundation\\Tomcat 9.0\\temp\\export\\kmlFiles";
        String path2sortedKML = "C:\\Program Files\\Apache Software Foundation\\Tomcat 9.0\\temp\\export";
        CityExportAgent exportAgent = new CityExportAgent();
        exportAgent.tilingKml(path2unsortedKML, path2sortedKML);
    }
    */
}
