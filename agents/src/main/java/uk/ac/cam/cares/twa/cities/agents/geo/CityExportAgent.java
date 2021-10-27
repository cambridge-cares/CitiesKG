package uk.ac.cam.cares.twa.cities.agents.geo;

import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.twa.cities.tasks.ExporterTask;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.io.File;
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
        public String outTmpDir = "java.io.tmpdir";
        private String requestUrl;
        private String gmlids;
        public String propFileName = "config.properties";

        //@todo: ImpExp.main() fails if there is more than one thread of it at a time. It needs further investigation.
        public final int NUM_IMPORTER_THREADS = 1;
        private final ThreadPoolExecutor exporterExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_IMPORTER_THREADS);


    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {
        JSONObject result = new JSONObject();

        if (validateInput(requestParams)) {
            requestUrl = requestParams.getString(KEY_REQ_URL);
            gmlids = requestParams.getString(KEY_GMLID);
            ResourceBundle rb = ResourceBundle.getBundle("config");
            String outputDir = rb.getString("outputDir") + "export";
            File outputdir = new File (outputDir);
            if (!outputdir.exists()){
                outputdir.mkdirs();
            }
            String outputPath = outputDir + outFileName;

            result.put("outputPath", exportKml(gmlids, outputPath));
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
                        if (!requestParams.getString(KEY_GMLID).isEmpty()){
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

    private String exportKml (String gmlIds, String outputpath){
        String actualPath = outputpath.replace(".kml", "_extruded.kml");
        ExporterTask task = new ExporterTask(gmlIds, outputpath);
        exporterExecutor.execute(task);
        return actualPath;
    }

}
