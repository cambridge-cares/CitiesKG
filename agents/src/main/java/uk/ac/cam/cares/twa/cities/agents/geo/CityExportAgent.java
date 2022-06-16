package uk.ac.cam.cares.twa.cities.agents.geo;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.apache.http.HttpException;
import org.apache.http.protocol.HTTP;
import org.checkerframework.checker.units.qual.K;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
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
        public String srsname;
        public Params (String namespaceIri, String directory, JSONObject serverInfo, String[] displayMode, String outputPath, String[] gmlIds, String srsname){
            this.namespaceIri = namespaceIri;
            this.outputDir = directory;
            this.serverInfo = serverInfo;
            this.displayMode = displayMode;
            this.outputPath = outputPath;
            this.gmlIds = gmlIds;
            this.srsname = srsname;
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
    private String srsName;
    private String[] displayMode = {"false","false", "false", "false"};
    private String inputDisplayForm;
    private String[] displayOptions = {"FOOTPRINT", "EXTRUDED", "GEOMETRY", "COLLADA"};
    private String namespaceName;
    private String tmpDirsLocation = System.getProperty("java.io.tmpdir");    //System.getProperty("java.io.tmpdir");   //"C:/tmp";  // can not have empty space on the path
    // Default task parameters
    //@todo: ImpExp.main() fails if there is more than one thread of it at a time. It needs further investigation.
    private final int NUM_EXPORTER_THREADS = 1;
    private final ThreadPoolExecutor exporterExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_EXPORTER_THREADS);
    //private final ThreadPoolExecutor tilingExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_EXPORTER_THREADS);

    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {
        JSONObject result = new JSONObject();

        if (validateInput(requestParams)) {

            // Process "namespaceIri"
            namespaceIri = requestParams.getString(KEY_NAMESPACE);
            serverInfo = getServerInfo(namespaceIri);
            srsName = getCrsInfo(namespaceIri);  // "EPSG:25833"

            // Process "displayform"
            List<String> availOptions = Arrays.asList(displayOptions);
            inputDisplayForm = requestParams.getString(KEY_DISPLAYFORM);
            int index = availOptions.indexOf(inputDisplayForm.toUpperCase());
            displayMode[index] = "true";

            //gmlids = getInputGmlids(requestParams); // this method will process the input when it is a path or an array of gmlid
            // If gmlid contains "*", it requires the whole list of gmlid from the namespace
            // This step can take long as querying the database is long with this sparql query
            // 2 modes:
            // 1) [*]
            // 2) explicit gmlids in an array

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

                BlockingQueue<Params> taskParamsQueue = new LinkedBlockingDeque<>();
                for (int i = 0 ; i < fileList.size(); ++i){
                    // Retrieve a list of gmlids from a file, and execute the export process
                    String[] gmlidArray = getGmlidFromFile(fileList.get(i));
                    String outputFileName = getOutputName(fileList.get(i));
                    Params taskParams = new Params(namespaceIri, outputDir, serverInfo, displayMode, outputFileName, gmlidArray, srsName);
                    taskParamsQueue.add(taskParams);
                }

                System.out.println("After for loop, Thread Name: " + Thread.currentThread().getName());

                ExporterExpTask exporterExpTask = new ExporterExpTask(taskParamsQueue);
                exporterExecutor.execute(exporterExpTask);

                String path2unsortedKML = Paths.get(outputDir, "kmlFiles").toString() ;
                String path2sortedKML = Paths.get(outputDir).toString();

                int databaseCRS = Integer.valueOf(srsName.split(":")[1]);

                KMLTilingTask kmlTilingTask = new KMLTilingTask(path2unsortedKML, path2sortedKML, databaseCRS, inputDisplayForm);

                exporterExecutor.execute(kmlTilingTask);  // this step will add the final task to the exporterExecutor

            }else{
                ArrayList<String> buildingIds = new ArrayList<>();
                for (Object id : gmlidParams) {
                    buildingIds.add(id.toString());
                }
                String[] gmlidsArray = new String[buildingIds.size()];
                gmlidsArray = buildingIds.toArray(gmlidsArray);
                String outSingleFileName = getOutputName(null);
                Params taskParams = new Params(namespaceIri, outputDir, serverInfo, displayMode, outSingleFileName, gmlidsArray,srsName);
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
                if (!requestParams.getString(KEY_DISPLAYFORM).isEmpty()) {
                    List<String> availOptions = Arrays.asList(displayOptions);
                    String inputDisplayForm = requestParams.getString(KEY_DISPLAYFORM);
                    int index = availOptions.indexOf(inputDisplayForm.toUpperCase());
                    if (index >= 0){
                        System.out.println("Valid displayform: " + inputDisplayForm);
                    } else {
                        System.out.println("InValid displayform: " + inputDisplayForm);
                        throw new BadRequestException();
                    }
                }
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

    /** Using HTTP request to query the TWA and get the srsInfo
     *  The postprocessing of the response is a bit complicated */
    private String getCrsInfo (String namespaceIri)  {
        String sparqlquery = "PREFIX ocgml: <http://www.theworldavatar.com/ontology/ontocitygml/citieskg/OntoCityGML.owl#> \n" +
            "SELECT ?s ?srid ?srsname { ?s ocgml:srid ?srid; ocgml:srsname ?srsname }";

        String srsname = null;

        try { HttpResponse<?> response = Unirest.post(namespaceIri)
            .header(HTTP.CONTENT_TYPE, "application/sparql-query")
            .body(sparqlquery)
            .socketTimeout(300000)
            .asString();
            int respStatus = response.getStatus();
            String responseBody = null;
            // handle the response
            if (respStatus != HttpURLConnection.HTTP_OK) {
                throw new HttpException(namespaceIri + " " + respStatus);
            } else {
                responseBody = (String) response.getBody();
                JSONObject json = XML.toJSONObject(responseBody);
                JSONObject srsObject = (JSONObject) XML.toJSONObject(responseBody).getJSONObject("sparql").getJSONObject("results").getJSONObject("result").getJSONArray("binding").get(2);
                srsname = srsObject.getString("literal");
            }
        } catch ( HttpException | UnirestException e) {
            throw new JPSRuntimeException(e);
        }

        return srsname;
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

}
