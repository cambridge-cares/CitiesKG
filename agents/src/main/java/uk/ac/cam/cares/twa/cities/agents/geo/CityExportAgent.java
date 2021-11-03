package uk.ac.cam.cares.twa.cities.agents.geo;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openrdf.query.algebra.Str;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.twa.cities.tasks.ExporterTask;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
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


        //@todo: ImpExp.main() fails if there is more than one thread of it at a time. It needs further investigation.
        public final int NUM_IMPORTER_THREADS = 1;
        private final ThreadPoolExecutor exporterExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_IMPORTER_THREADS);


    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {
        JSONObject result = new JSONObject();

        if (validateInput(requestParams)) {
            requestUrl = requestParams.getString(KEY_REQ_URL);
            gmlids = getInputGmlids(requestParams);
            result.put("outputPath", exportKml(gmlids, getOutputPath()));
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

    private String exportKml (String[] gmlIds, String outputpath){
        String actualPath = outputpath.replace(".kml", "_extruded.kml");
        ExporterTask task = new ExporterTask(gmlIds, outputpath);
        exporterExecutor.execute(task);
        return actualPath;
    }

}
