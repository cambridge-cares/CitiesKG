package uk.ac.cam.cares.twa.cities.tasks;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.apache.http.HttpException;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import com.google.gson.Gson;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.lang.Process;
import java.util.List;
import java.util.Objects;

public class RunCEATask implements Runnable {
    private final ArrayList<CEAInputData> inputs;
    private final ArrayList<String> uris;
    private final URI endpointUri;
    private final int threadNumber;
    public static final String CTYPE_JSON = "application/json";
    private Boolean stop = false;
    private static final String SHAPEFILE_SCRIPT = "create_shapefile.py";
    private static final String WORKFLOW_SCRIPT = "workflow.yml";
    private static final String CREATE_WORKFLOW_SCRIPT = "create_cea_workflow.py";
    private static final String FS = System.getProperty("file.separator");

    public RunCEATask(ArrayList<CEAInputData> buildingData, URI endpointUri, ArrayList<String> uris, int thread) {
        this.inputs = buildingData;
        this.endpointUri = endpointUri;
        this.uris = uris;
        this.threadNumber = thread;
    }

    public void stop() {
        stop = true;
    }

    public Process runProcess(ArrayList<String> args) {
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectErrorStream(true);


        // starting the process
        try {
            Process p = builder.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            int ch;
            while ((ch = br.read()) != -1)
                System.out.println((char)ch);
            br.close();
            int exitVal = p.waitFor();
            System.out.println("Process exitValue: " + exitVal);
            return p;

        } catch ( IOException | InterruptedException e) {
            e.printStackTrace();
            throw new JPSRuntimeException(e);
        }
    }

    /**
     * Recursively delete contents of a directory
     * @param file given directory
     */
    public void deleteDirectoryContents(File file)
    {
        for (File subFile : file.listFiles()) {
            // if it is a subfolder recursively call function to empty it
            if (subFile.isDirectory()) {
                deleteDirectoryContents(subFile);
            }
            subFile.delete();
        }
    }

    /**
     * Extract areas from excel files and add to output data, then delete excel files
     * @param tmpDir temporary directory path
     * @param result output data
     * @return output data
     */
    public CEAOutputData extractArea(String tmpDir, CEAOutputData result) {
        String line = "";
        String splitBy = ",";
        String projectDir = tmpDir+FS+"testProject";

        try{
            //parsing a CSV file into BufferedReader class constructor
            FileReader PV = new FileReader(projectDir+FS+"testScenario"+FS+"outputs"+FS+"data"+FS+"potentials"+FS+"solar"+FS+"PV_total_buildings.csv");
            BufferedReader PV_file = new BufferedReader(PV);
            ArrayList<String[] > PV_columns = new ArrayList<>();

            while ((line = PV_file.readLine()) != null)   //returns a Boolean value
            {
                String[] rows = line.split(splitBy);    // use comma as separator
                PV_columns.add(rows);
            }
            for(int n=0; n<PV_columns.get(0).length; n++) {
                if(PV_columns.get(0)[n].equals("PV_roofs_top_m2")) {
                    for(int m=1; m<PV_columns.size(); m++) {
                        result.PV_area_roof.add(PV_columns.get(m)[n]);
                    }
                }
                else if(PV_columns.get(0)[n].equals("PV_walls_south_m2")) {
                    for(int m=1; m<PV_columns.size(); m++) {
                        result.PV_area_wall_south.add(PV_columns.get(m)[n]);
                    }
                }
                else if(PV_columns.get(0)[n].equals("PV_walls_north_m2")) {
                    for(int m=1; m<PV_columns.size(); m++) {
                        result.PV_area_wall_north.add(PV_columns.get(m)[n]);
                    }
                }
                else if(PV_columns.get(0)[n].equals("PV_walls_east_m2")) {
                    for(int m=1; m<PV_columns.size(); m++) {
                        result.PV_area_wall_east.add(PV_columns.get(m)[n]);
                    }
                }
                else if(PV_columns.get(0)[n].equals("PV_walls_west_m2")) {
                    for(int m=1; m<PV_columns.size(); m++) {
                        result.PV_area_wall_west.add(PV_columns.get(m)[n]);
                    }
                }
            }
            PV_file.close();
            PV.close();
        } catch ( IOException e) {

        }
        result.targetUrl=endpointUri.toString();
        result.iris=uris;
        File file = new File(tmpDir);
        deleteDirectoryContents(file);
        file.delete();

        return result;
    }

    /**
     * Extract time series data from excel files and add to output data
     * @param tmpDir temporary directory path
     * @return output data
     */
    public CEAOutputData extractTimeSeriesOutputs(String tmpDir) {
        String line = "";
        String splitBy = ",";
        String projectDir = tmpDir+FS+"testProject";
        CEAOutputData result = new CEAOutputData();

        try{
            for(int i=0; i<inputs.size(); i++){
                FileReader demand;
                if(i<10){
                    demand = new FileReader(projectDir+FS+"testScenario"+FS+"outputs"+FS+"data"+FS+"demand"+FS+"B00"+i+".csv");
                }
                else if(i<100){
                    demand = new FileReader(projectDir+FS+"testScenario"+FS+"outputs"+FS+"data"+FS+"demand"+FS+"B0"+i+".csv");
                }
                else{
                    demand = new FileReader(projectDir+FS+"testScenario"+FS+"outputs"+FS+"data"+FS+"demand"+FS+"B"+i+".csv");
                }
                BufferedReader demand_file = new BufferedReader(demand);
                ArrayList<String[]> demand_columns = new ArrayList<>();
                ArrayList<String> grid_results = new ArrayList<>();
                ArrayList<String> heating_results = new ArrayList<>();
                ArrayList<String> cooling_results = new ArrayList<>();
                ArrayList<String> electricity_results = new ArrayList<>();
                ArrayList<String> PV_roof_results = new ArrayList<>();
                ArrayList<String> PV_wall_south_results = new ArrayList<>();
                ArrayList<String> PV_wall_north_results = new ArrayList<>();
                ArrayList<String> PV_wall_east_results = new ArrayList<>();
                ArrayList<String> PV_wall_west_results = new ArrayList<>();

                List<String> timestamps = new ArrayList();

                while ((line = demand_file.readLine()) != null)   //returns a Boolean value
                {
                    String[] rows = line.split(splitBy);    // use comma as separator
                    demand_columns.add(rows);
                }
                for(int n=0; n<demand_columns.get(0).length; n++) {
                    if (demand_columns.get(0)[n].equals("GRID_kWh")) {
                        for (int m = 1; m < demand_columns.size(); m++) {
                            grid_results.add(demand_columns.get(m)[n]);
                        }
                    } else if (demand_columns.get(0)[n].equals("QH_sys_kWh")) {
                        for (int m = 1; m < demand_columns.size(); m++) {
                            heating_results.add(demand_columns.get(m)[n]);
                        }
                    } else if (demand_columns.get(0)[n].equals("QC_sys_kWh")) {
                        for (int m = 1; m < demand_columns.size(); m++) {
                            cooling_results.add(demand_columns.get(m)[n]);
                        }
                    } else if (demand_columns.get(0)[n].equals("E_sys_kWh")) {
                        for (int m = 1; m < demand_columns.size(); m++) {
                            electricity_results.add(demand_columns.get(m)[n]);
                        }
                    }
                }
                demand_file.close();
                demand.close();

                FileReader PV;
                if(i<10){
                    PV = new FileReader(projectDir+FS+"testScenario"+FS+"outputs"+FS+"data"+FS+"potentials"+FS+"solar"+FS+"B00"+i+"_PV.csv");
                }
                else if(i<100){
                    PV = new FileReader(projectDir+FS+"testScenario"+FS+"outputs"+FS+"data"+FS+"potentials"+FS+"solar"+FS+"B0"+i+"_PV.csv");
                }
                else{
                    PV = new FileReader(projectDir+FS+"testScenario"+FS+"outputs"+FS+"data"+FS+"potentials"+FS+"solar"+FS+"B"+i+"_PV.csv");
                }
                BufferedReader PV_file = new BufferedReader(PV);
                ArrayList<String[]> PV_columns = new ArrayList<>();

                while ((line = PV_file.readLine()) != null)   //returns a Boolean value
                {
                    String[] rows = line.split(splitBy);    // use comma as separator
                    PV_columns.add(rows);
                }
                for(int n=0; n<PV_columns.get(0).length; n++) {
                    if (i==0 && PV_columns.get(0)[n].equals("Date")) {
                        for (int m = 1; m < PV_columns.size(); m++) {
                            timestamps.add(demand_columns.get(m)[n].replaceAll("\\s","T")+"+00:00");
                        }
                    } else if(PV_columns.get(0)[n].equals("PV_roofs_top_E_kWh")) {
                        for(int m=1; m<PV_columns.size(); m++) {
                            PV_roof_results.add(PV_columns.get(m)[n]);
                        }
                    } else if(PV_columns.get(0)[n].equals("PV_walls_south_E_kWh")) {
                        for(int m=1; m<PV_columns.size(); m++) {
                            PV_wall_south_results.add(PV_columns.get(m)[n]);
                        }
                    } else if(PV_columns.get(0)[n].equals("PV_walls_north_E_kWh")) {
                        for(int m=1; m<PV_columns.size(); m++) {
                            PV_wall_north_results.add(PV_columns.get(m)[n]);
                        }
                    } else if(PV_columns.get(0)[n].equals("PV_walls_west_E_kWh")) {
                        for(int m=1; m<PV_columns.size(); m++) {
                            PV_wall_west_results.add(PV_columns.get(m)[n]);
                        }
                    } else if(PV_columns.get(0)[n].equals("PV_walls_east_E_kWh")) {
                        for(int m=1; m<PV_columns.size(); m++) {
                            PV_wall_east_results.add(PV_columns.get(m)[n]);
                        }
                    }
                }
                PV_file.close();
                PV.close();

                result.grid_demand.add(grid_results);
                result.electricity_demand.add(electricity_results);
                result.heating_demand.add(heating_results);
                result.cooling_demand.add(cooling_results);
                result.PV_supply_roof.add(PV_roof_results);
                result.PV_supply_wall_south.add(PV_wall_south_results);
                result.PV_supply_wall_north.add(PV_wall_north_results);
                result.PV_supply_wall_east.add(PV_wall_east_results);
                result.PV_supply_wall_west.add(PV_wall_west_results);

                if(i==0) result.times = timestamps; //only add times once
            }
        } catch ( IOException e) {

        }
        return result;
    }

    /**
     * Return output data to CEA Agent via http request
     * @param output output data
     */
    public void returnOutputs(CEAOutputData output) {
        try {
            String JSONOutput = new Gson().toJson(output);
            if (!JSONOutput.isEmpty()) {
                HttpResponse<?> response = Unirest.post(endpointUri.toString())
                        .header(HTTP.CONTENT_TYPE, CTYPE_JSON)
                        .body(JSONOutput)
                        .socketTimeout(300000)
                        .asEmpty();
                int responseStatus = response.getStatus();
                if (responseStatus != HttpURLConnection.HTTP_OK) {
                    throw new HttpException(endpointUri + " " + responseStatus);
                }
            }

        } catch ( HttpException | UnirestException e) {
            throw new JPSRuntimeException(e);
        }
    }

    @Override
    public void run() {
        while (!stop) {

            try {
                //Parse input data to JSON
                String dataString="[";
                for(int i=0; i<inputs.size(); i++) {
                    dataString += new Gson().toJson(inputs.get(i));
                    if(i!=inputs.size()-1) dataString += ", ";
                }
                dataString+="]";
                String strTmp = System.getProperty("java.io.tmpdir")+FS+"thread_"+threadNumber;

                File dir = new File(strTmp);
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new JPSRuntimeException(new FileNotFoundException(strTmp));
                }

                ArrayList<String> args = new ArrayList<>();
                args.add("python");
                args.add(new File(
                        Objects.requireNonNull(getClass().getClassLoader().getResource(SHAPEFILE_SCRIPT)).toURI()).getAbsolutePath());
                args.add(dataString.replace("\"", "\\\""));
                args.add(strTmp);

                // create the shapefile process and run
                runProcess(args);

                ArrayList<String> args2 = new ArrayList<>();
                args2.add("python");
                args2.add(new File(
                        Objects.requireNonNull(getClass().getClassLoader().getResource(CREATE_WORKFLOW_SCRIPT)).toURI()).getAbsolutePath());
                args2.add(new File(
                        Objects.requireNonNull(getClass().getClassLoader().getResource(WORKFLOW_SCRIPT)).toURI()).getAbsolutePath());
                args2.add(strTmp);

                // create the workflow process and run
                runProcess(args2);

                String workflowPath = strTmp+FS+"workflow.yml";

                ArrayList<String> args3 = new ArrayList<>();
                args3.add("cmd.exe");
                args3.add("/C");
                args3.add("conda activate cea && cea workflow --workflow " + workflowPath);

                // Run workflow that runs all CEA scripts
                runProcess(args3);

                CEAOutputData result = extractTimeSeriesOutputs(strTmp);
                returnOutputs(extractArea(strTmp,result));

            } catch ( NullPointerException | URISyntaxException e) {
                e.printStackTrace();
                throw new JPSRuntimeException(e);
            } finally {
                stop();
            }
        }
    }
}
