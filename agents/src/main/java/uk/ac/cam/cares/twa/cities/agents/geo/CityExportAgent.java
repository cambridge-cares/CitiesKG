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

@WebServlet(
        urlPatterns = {
                CityExportAgent.URI_ACTION
        })
public class CityExportAgent extends JPSAgent {
        public static final String URI_ACTION = "/export/kml";
        public static final String KEY_GMLID = "gmlid";
        public static final String KEY_REQ_URL = "requestUrl";
        public static final String KEY_REQ_METHOD = "method";
        public String outFileName = "/test.kml";
        public String outTmpDir;
        private String requestUrl;
        private String[] gmlids;
        private String sourceUrl;
        private JSONObject serverInfo;


        //@todo: ImpExp.main() fails if there is more than one thread of it at a time. It needs further investigation.
        public final int NUM_IMPORTER_THREADS = 1;
        private final ThreadPoolExecutor exporterExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_IMPORTER_THREADS);


    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {
        JSONObject result = new JSONObject();


        if (validateInput(requestParams)) {
            requestUrl = requestParams.getString(KEY_REQ_URL);
            gmlids = getInputGmlids(requestParams);
            serverInfo = getServerInfo(requestParams);

            if (gmlids[0].contains("*")){ // need to extract the whole database
                //String rootDir = "C:\\Users\\Shiying\\Documents\\CKG\\Imported_data\\testfolder";
                String rootDir = "C:\\Users\\Admin\\Documents\\CKG\\Imported_data\\ura";
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
        ResourceBundle rb = ResourceBundle.getBundle("config");
        outTmpDir = rb.getString("outputDir") + "export";
        File outputdir = new File (outTmpDir);
        if (!outputdir.exists()){
            outputdir.mkdirs();
        }
        String path = gmlidFile.toString().replace("\\", "/");
        String[] elem = path.split("/");
        //String index = elem[elem.length-1].replaceAll("[^0-9]", "");
        String filename = elem[elem.length-1];
        String index = filename.substring(filename.indexOf("_") + 1, filename.indexOf("."));
        String outputPath = outTmpDir + "/test_" + index + ".kml";
        return outputPath;
    }
    /**
     * retrieves the server information from the endpointUri -- host, port, namespace
     *
     * @param  - requestParams sent to the agent
     * @return - JSONObject with key/value: host, port, namespace
     */

    private JSONObject getServerInfo (JSONObject requestParams) {

        JSONObject serverInfo = new JSONObject();
        JSONArray uriGmlids = requestParams.getJSONArray(KEY_GMLID);
        String endpointUri = uriGmlids.get(0).toString();

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

    /**
     * define the output path of the generated kml file.
     *
     * the root folder is predefined in config.properties, a folder called export will be created
     * @return the output location of the kml file
     */
    private String getOutputPath () {
        ResourceBundle rb = ResourceBundle.getBundle("config");
        outTmpDir = rb.getString("outputDir") + "export";
        File outputdir = new File (outTmpDir);
        if (!outputdir.exists()){
            outputdir.mkdirs();
        }
        String outputPath = outTmpDir + outFileName;
        return outputPath;
    }

    private String exportKml (String[] gmlIds, String outputpath, JSONObject serverInfo){
        String actualPath = outputpath.replace(".kml", "_extruded.kml");
        ExporterTask task = new ExporterTask(gmlIds, outputpath, serverInfo);
        exporterExecutor.execute(task);
        return actualPath;
    }

}
