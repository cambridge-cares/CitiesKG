package uk.ac.cam.cares.twa.cities.agents.geo;

import com.google.gson.JsonElement;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Stream;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.apache.http.HttpException;
import org.apache.http.protocol.HTTP;
import org.citydb.config.project.database.DatabaseSrs;
import org.citydb.database.connection.DatabaseMetaData;
import org.eclipse.jetty.server.Server;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.query.algebra.Str;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import uk.ac.cam.cares.twa.cities.tasks.ExporterExpTask;
import uk.ac.cam.cares.twa.cities.tasks.ExporterTask;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.io.File;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import uk.ac.cam.cares.twa.cities.tasks.ImporterTask;
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

    public static class Params {
        public String namespaceIri;
        public String outputDir;
        public JSONObject serverInfo;
        public String[] displayMode;
        public String outputPath;
        public String[] gmlIds;
        public Params (String namespaceIri, String directory, JSONObject serverInfo, String[] displayMode, String outputPath, String[] gmlIds){
            this.namespaceIri = namespaceIri;
            this.outputDir = directory;
            this.serverInfo = serverInfo;
            this.displayMode = displayMode;
            this.outputPath = outputPath;
            this.gmlIds = gmlIds;
        }
    }

    // Agent endpoint and parameter keys
    public static final String URI_ACTION = "/export/kml";
    public static final String KEY_GMLID = "gmlid";
    public static final String KEY_REQ_URL = "requestUrl";
    public static final String KEY_REQ_METHOD = "method";
    public static final String KEY_NAMESPACE = "namespace";
    public static final String KEY_DISPLAYFORM = "displayform";
    public static final String FS = System.getProperty("file.separator");

    // Export files names
    private String outFileName = "test";
    private String outFileExtension = ".kml";

    private String namespaceIri;
    private JSONArray gmlidParams;
    private String outputDir;
    private JSONObject serverInfo;
    private String[] displayMode = {"false","false", "false", "false"};

    private String[] displayOptions = {"FOOTPRINT", "EXTRUDED", "GEOMETRY", "COLLADA"};
    private String namespaceName;
    private String tmpDirsLocation = System.getProperty("java.io.tmpdir");    //System.getProperty("java.io.tmpdir");   //"C:/tmp";  // can not have empty space on the path
    // Default task parameters
    //@todo: ImpExp.main() fails if there is more than one thread of it at a time. It needs further investigation.
    private final int NUM_EXPORTER_THREADS = 1;
    private final ThreadPoolExecutor exporterExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_EXPORTER_THREADS);

    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {
        JSONObject result = new JSONObject();

        if (validateInput(requestParams)) {

            namespaceIri = requestParams.getString(KEY_NAMESPACE);
            serverInfo = getServerInfo(namespaceIri);

            // test
            String srsname = getCrsInfo(namespaceIri);

            // Process "displayform"
            List<String> availOptions = Arrays.asList(displayOptions);

            JSONArray inputDisplayForm = requestParams.getJSONArray(KEY_DISPLAYFORM);
            for ( int i = 0; i < inputDisplayForm.length(); i ++) {
                int index = availOptions.indexOf(
                    inputDisplayForm.get(i).toString().toUpperCase());
                displayMode[index] = "true";
            }
            //gmlids = getInputGmlids(requestParams); // this method will process the input when it is a path or an array of gmlid
            // If gmlid contains "*", it requires the whole list of gmlid from the namespace
            // This step can take long as querying the database is long with this sparql query
            // 2 modes:
            // 1) [*]
            // 2) explicit gmlids

            outputDir = Paths.get(tmpDirsLocation, "export").toString();
            gmlidParams = requestParams.getJSONArray(KEY_GMLID);

            if (gmlidParams.length() == 1 && gmlidParams.get(0).equals("*")) {
                String preparedGmlids = Paths.get(tmpDirsLocation, namespaceName).toString();

                ArrayList<Path> fileList = new ArrayList<>();
                try (Stream<Path> paths = Files.walk(Paths.get(preparedGmlids))) {
                    paths
                        .filter(Files::isRegularFile)
                        .forEach(fileList::add);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ExporterTask lastTask = null;
                BlockingQueue<Params> taskParamsQueue = new LinkedBlockingDeque<>();
                for (int i = 0 ; i < fileList.size(); ++i){
                    //Path gmlidFile : fileList) {
                    // Retrieve a list of gmlids from a file, and execute the export process
                    String[] gmlidArray = getGmlidFromFile(fileList.get(i));
                    String outputFileName = getOutputName(fileList.get(i));
                    Params taskParams = new Params(namespaceIri, outputDir, serverInfo, displayMode, outputFileName, gmlidArray);
                    taskParamsQueue.add(taskParams);
                    //lastTask = exportKml(gmlidArray, taskParams);
                    //result.put("outputPath", exportKml(gmlidArray, taskParams));  // this can not indicate that the process is done
                }

                System.out.println("After for loop, Thread Name: " + Thread.currentThread().getName());

                ExporterExpTask exporterExpTask = new ExporterExpTask(taskParamsQueue);
                exporterExecutor.execute(exporterExpTask);


                // @todo: how to capture the signal that the export process is done
                /*
                while (lastTask.isRunning()){

                    if (!lastTask.isRunning()){
                        System.out.println(lastTask.getOutputpath() + " has stopped");
                        tilingKML(outputDir, outputDir);
                    }
                }*/

            }else{
                ArrayList<String> buildingIds = new ArrayList<>();
                for (Object id : gmlidParams) {
                    buildingIds.add(id.toString());
                }
                String[] gmlidsArray = new String[buildingIds.size()];
                gmlidsArray = buildingIds.toArray(gmlidsArray);
                String outSingleFileName = getOutputName(null);
                Params taskParams = new Params(namespaceIri, outputDir, serverInfo, displayMode, outSingleFileName, gmlidsArray);
                //result.put("outputPath", exportKml(gmlidsArray, taskParams));
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
            Set<String> keys = requestParams.keySet();
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
                if (!requestParams.getJSONArray(KEY_DISPLAYFORM).isEmpty()) {
                    List<String> availOptions = Arrays.asList(displayOptions);
                    JSONArray inputDisplayForm = requestParams.getJSONArray(KEY_DISPLAYFORM);
                    for ( int i = 0; i < inputDisplayForm.length(); i ++){
                        int index = availOptions.indexOf(
                            inputDisplayForm.get(i).toString().toUpperCase());
                        if (index >= 0){
                            System.out.println("Valid displayform: " + inputDisplayForm.get(i).toString());
                        } else {
                            System.out.println("InValid displayform: " + inputDisplayForm.get(i).toString());
                        }
                    }

                }
                /*
                if (!requestParams.getString(KEY_DISPLAYFORM).isEmpty()) {
                    List<String> availOptions = Arrays.asList(displayOptions);
                    int index = availOptions.indexOf(
                        requestParams.getString(KEY_DISPLAYFORM).toUpperCase());
                    if (index > 0) {
                        System.out.println("Valid displayform: " + requestParams.getString(KEY_DISPLAYFORM));
                    }
                }*/

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

    /** Using jdbc framework to query the TWA and get the srsInfo */
    private String getCrsInfo (String namespaceIri)  {
        String sparqlquery = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#> \n" +
            "SELECT ?s ?srid ?srsname { ?s ocgml:srid ?srid; ocgml:srsname ?srsname }";

        try { HttpResponse<?> response = Unirest.post(namespaceIri)
            .header(HTTP.CONTENT_TYPE, "application/sparql-query")
            .body(sparqlquery)
            .socketTimeout(300000)
            .asEmpty();
            int respStatus = response.getStatus();
            if (respStatus != HttpURLConnection.HTTP_OK) {
                throw new HttpException(namespaceIri + " " + respStatus);
            }
        } catch ( HttpException | UnirestException e) {
            throw new JPSRuntimeException(e);
        }

        String srsname = null;
        return srsname;
        /*
        // jdbc:jena:remote:query=http://www.theworldavatar.com:83/citieskg/namespace/berlin/sparql/&update=http://www.theworldavatar.com:83/citieskg/namespace/berlin/sparql/
        String connectionStr = "jdbc:jena:remote:query=" + namespaceIri + "&update=" + namespaceIri;

        String srsname = null;

        try (Connection conn = DriverManager.getConnection(connectionStr);
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sparqlquery)) {
            if (rs.next()) {
                srsname = rs.getString("srsname");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return srsname;
        */

    }


    /**
     * create the output path of the generated kml file for indexed input files.
     * @param - indexed file
     * @return - the output location of the kml file
     */
    private String getOutputName (Path inputFile) {

        // Create the export directory if this does not exist
        if (!new File(outputDir).exists()){ new File(outputDir).mkdirs(); }

        String outputPath = null;
        String path = null;
        if (inputFile ==  null) {
            outputPath =  Paths.get(outputDir, outFileName + outFileExtension).toString();

        } else {
            path = inputFile.toString().replace("\\", "/");
            String[] elem = path.split("/");
            String filename = elem[elem.length - 1];
            String index = filename.substring(filename.indexOf("_") + 1, filename.indexOf("."));
            String exportFilename = outFileName + "_" + index + outFileExtension;
            outputPath = Paths.get(outputDir.toString(), exportFilename).toString();
        }
        /*
        File outputFile = new File(outputPath);
        if (!outputFile.exists()){
            try {
                outputFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
        return outputPath;
    }

    /**
     * create the output path of the generated kml file as default no arguments.
     * @return the output location of the kml file
     */
    private String getOutputName () {
        return getOutputName(null);
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
        namespaceName = subarray[2];
        if (server.isEmpty() || port.isEmpty() || namespace.isEmpty()) {
            return null;
        }
        serverInfo.put ("host", server);
        serverInfo.put ("port", port);
        serverInfo.put ("namespace", namespace);
        return serverInfo;
    }

    private ExporterTask exportKml (String[] gmlIds, Params taskParams){

        ExporterTask task = new ExporterTask(gmlIds, taskParams.outputPath, serverInfo, taskParams.displayMode);
        exporterExecutor.execute(task);

        return task;
    }

    /**
     * Re-arrange and tiling the unsorted KMLs output from ImpExp tool
     *
     * @param path2unsortedKML the path of the directory that contains the unsorted KMLs
     */
    private static void tilingKML(String path2unsortedKML, String path2sortedKML){
        //String inputfile = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\testfolder_1\\charlottenberg_extruded_blaze.kml";
        //String inputDir = "C:\\Users\\Shiying\\Documents\\CKG\\Exported_data\\exported_data_whole\\";
        String CRSinDegree = "4326";  // global WGS 84
        String CRSinMeter = "25833";//"32648"; "25833"
        int initTileSize = 250;
        long start = System.currentTimeMillis();

        // 1. KMLParserTask
        KMLParserTask parserTask = new KMLParserTask(path2unsortedKML, path2sortedKML, CRSinDegree, CRSinMeter, initTileSize);
        parserTask.run();
        double[] updatedExtent = parserTask.getUpdatedExtent();
        String summaryCSV = parserTask.getOutFile();

        // 2. KMLTilingTask
        KMLTilingTask kmltiling = new KMLTilingTask(CRSinDegree, CRSinMeter, path2sortedKML, initTileSize);  // 25833
        kmltiling.setUp(summaryCSV);
        kmltiling.updateExtent(updatedExtent);  // whole berlin: new double[]{13.09278683392157, 13.758936971880468, 52.339762874361156, 52.662766032905616}
        kmltiling.run();
        String masterJSONFile = kmltiling.getmasterJSONFile();
        String sortedCSVFile = kmltiling.getsortedCSVFile();


        // 3. KMLSorterTask
        KMLSorterTask kmlSorter=new KMLSorterTask(path2unsortedKML, path2sortedKML, masterJSONFile, sortedCSVFile);
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
