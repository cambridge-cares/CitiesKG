package uk.ac.cam.cares.twa.cities.agents.geo;

import org.json.JSONObject;
import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.twa.cities.tasks.ExporterTask;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.io.File;
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
        public static final String KEY_OUTPUTPATH = "outputpath";
        public static final String KEY_REQ_METHOD = "method";

        //@todo: ImpExp.main() fails if there is more than one thread of it at a time. It needs further investigation.
        public final int NUM_IMPORTER_THREADS = 1;
        private final ThreadPoolExecutor exporterExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_IMPORTER_THREADS);

    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {
        if (validateInput(requestParams)) {
            //TODO: after validation, the gmlId and outputpath should be retrieved
            String gmlids = requestParams.getString(KEY_GMLID);
            String outputpath = requestParams.getString(KEY_OUTPUTPATH);
            exportKml(gmlids,outputpath);
        }
        //TODO
        return null;
    }

    @Override
    public boolean validateInput(JSONObject requestParams) throws BadRequestException {

        boolean error = true;
        if (!requestParams.isEmpty()) {
            Set<String> keys = requestParams.keySet();
            if (keys.contains(KEY_REQ_METHOD)) {
                if (requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.POST)) {
                    try {
                        // Check if GMLID and OUTPUTPATH is empty and if OUTPUTPATH exists
                        if (!requestParams.getString(KEY_GMLID).isEmpty() && !requestParams.getString(KEY_OUTPUTPATH).isEmpty()){
                            File file = new File(requestParams.getString(KEY_OUTPUTPATH));
                            file.createNewFile();
                            if (file.exists()){
                                error = false;   // no error
                            }
                        }
                    } catch (Exception e) {
                        throw new BadRequestException(e);
                    }
                }
            }
        }
        if (error) {
            throw new BadRequestException();
        }
        return !error;
    }

    private ExporterTask exportKml (String gmlIds, String outputpath){
        ExporterTask task = new ExporterTask(gmlIds, outputpath);
        exporterExecutor.execute(task);  // Note: If no breakpoint is put here or in this script, the debugger can enter task scriptva
        return task;
    }

}
