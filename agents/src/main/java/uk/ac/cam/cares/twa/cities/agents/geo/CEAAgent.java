package uk.ac.cam.cares.twa.cities.agents.geo;

import uk.ac.cam.cares.jps.base.agent.JPSAgent;
import uk.ac.cam.cares.twa.cities.tasks.RunCEATask;
import org.json.JSONObject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.HttpMethod;
import java.util.ArrayList;
import java.util.Set;
import javax.servlet.annotation.WebServlet;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@WebServlet(
        urlPatterns = {
                CEAAgent.URI_ACTION
        })
public class CEAAgent extends JPSAgent {
    public static final String KEY_REQ_METHOD = "method";
    public static final String URI_ACTION = "/cea";
    public static final String KEY_IRI = "iri";

    public final int NUM_IMPORTER_THREADS = 1;
    private final ThreadPoolExecutor CEAExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUM_IMPORTER_THREADS);

    @Override
    public JSONObject processRequestParameters(JSONObject requestParams) {
        JSONObject result = new JSONObject();

        if (validateInput(requestParams)) {
            ArrayList<String> testData =  new ArrayList<>();
            String geometry = "21366.87669#31778.47577#0.0#21515.09049#31778.47577#0.0#21515.09049#31873.86068#34.0#21366.87669#31873.86068#34.0#21366.87669#31778.47577#0.0#";
            String floors_ag = "5";
            testData.add(geometry);
            testData.add(floors_ag);
            result.put("outputPath", runCEA(testData, "test"));
        }

        return requestParams;
    }

    @Override
    public boolean validateInput(JSONObject requestParams) throws BadRequestException {
        boolean error = true;

        if (!requestParams.isEmpty()) {
            Set<String> keys = requestParams.keySet();
            if (keys.contains(KEY_REQ_METHOD) && keys.contains(KEY_IRI)) {
                if (requestParams.get(KEY_REQ_METHOD).equals(HttpMethod.POST)) {
                    try {
                        if (!requestParams.getString(KEY_IRI).isEmpty()){
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

        return true;
    }

    private String runCEA(ArrayList<String> buildingData, String outputpath) {
        RunCEATask task = new RunCEATask(buildingData, outputpath);
        CEAExecutor.execute(task);
        return outputpath;
    }
}
