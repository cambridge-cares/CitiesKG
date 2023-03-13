package uk.ac.cam.cares.twa.cities.tasks;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.apache.http.HttpException;
import org.apache.http.protocol.HTTP;
import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import com.google.gson.Gson;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.lang.Process;
import java.util.List;
import java.util.Objects;

public class RunCEATask implements Runnable {
    private final ArrayList<CEAInputData> inputs;
    private final ArrayList<String> uris;
    private final URI endpointUri;
    private final int threadNumber;
    private final String crs;
    public static final String CTYPE_JSON = "application/json";
    private Boolean stop = false;
    private Boolean noSurroundings = false;
    private static final String DATA_FILE = "datafile.txt";
    private static final String SURROUNDINGS_FILE = "surroundingdata.txt";
    private static final String SHAPEFILE_SCRIPT = "create_shapefile.py";
    private static final String TYPOLOGY_SCRIPT = "create_typologyfile.py";
    private static final String WORKFLOW_SCRIPT = "workflow.yml";
    private static final String CREATE_WORKFLOW_SCRIPT = "create_cea_workflow.py";
    private static final String FS = System.getProperty("file.separator");

    public RunCEATask(ArrayList<CEAInputData> buildingData, URI endpointUri, ArrayList<String> uris, int thread, String crs) {
        this.inputs = buildingData;
        this.endpointUri = endpointUri;
        this.uris = uris;
        this.threadNumber = thread;
        this.crs = crs;
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
                        result.PVRoofArea.add(PV_columns.get(m)[n]);
                    }
                }
                else if(PV_columns.get(0)[n].equals("PV_walls_south_m2")) {
                    for(int m=1; m<PV_columns.size(); m++) {
                        result.PVWallSouthArea.add(PV_columns.get(m)[n]);
                    }
                }
                else if(PV_columns.get(0)[n].equals("PV_walls_north_m2")) {
                    for(int m=1; m<PV_columns.size(); m++) {
                        result.PVWallNorthArea.add(PV_columns.get(m)[n]);
                    }
                }
                else if(PV_columns.get(0)[n].equals("PV_walls_east_m2")) {
                    for(int m=1; m<PV_columns.size(); m++) {
                        result.PVWallEastArea.add(PV_columns.get(m)[n]);
                    }
                }
                else if(PV_columns.get(0)[n].equals("PV_walls_west_m2")) {
                    for(int m=1; m<PV_columns.size(); m++) {
                        result.PVWallWestArea.add(PV_columns.get(m)[n]);
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
                            timestamps.add(PV_columns.get(m)[n].replaceAll("\\s","T"));
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

                result.GridConsumption.add(grid_results);
                result.ElectricityConsumption.add(electricity_results);
                result.HeatingConsumption.add(heating_results);
                result.CoolingConsumption.add(cooling_results);
                result.PVRoofSupply.add(PV_roof_results);
                result.PVWallSouthSupply.add(PV_wall_south_results);
                result.PVWallNorthSupply.add(PV_wall_north_results);
                result.PVWallEastSupply.add(PV_wall_east_results);
                result.PVWallWestSupply.add(PV_wall_west_results);

                if(i==0) result.times = timestamps; //only add times once
            }
        } catch ( IOException e) {

        }
        return result;
    }

    /**
     * Return output data to CEA Agent via http POST request
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

    /**
     * Converts input data (except for surrounding) to CEA into text file to be read by the Python scripts
     * @param dataInputs ArrayList of the CEA input data
     * @param directory_path directory path
     * @param file_path path to store data file, excluding surrounding data
     * @param surrounding_path path to store surrounding data file
     */
    private void dataToFile(ArrayList<CEAInputData> dataInputs, String directory_path, String file_path, String surrounding_path) {
        //Parse input data to JSON
        String dataString = "[";
        ArrayList<CEAInputData> surroundings = new ArrayList<>();

        for(int i = 0; i < dataInputs.size(); i++) {
            if (!(dataInputs.get(i).getSurrounding() == null)) {surroundings.addAll(dataInputs.get(i).getSurrounding());}
            dataInputs.get(i).setSurrounding(null);
            dataString += new Gson().toJson(dataInputs.get(i));
            if(i!=dataInputs.size()-1) dataString += ", ";
        }
        dataString+="]";

        File dir = new File(directory_path);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new JPSRuntimeException(new FileNotFoundException(directory_path));
        }

        try {
            BufferedWriter f_writer = new BufferedWriter(new FileWriter(file_path));
            f_writer.write(dataString);
            f_writer.close();
        } catch (IOException e) {
            throw new JPSRuntimeException(e);
        }

        // if there is surrounding data, call dataToFile to store surrounding data as a temporary text file
        if (surroundings.isEmpty()){
            noSurroundings = true;
        }
        else{
            dataToFile(surroundings, directory_path, surrounding_path);
        }
    }

    /**
     * Converts surrounding data into text file to be read by the Python scripts
     * @param dataInputs ArrayList of the CEA input data
     * @param directory_path directory path
     * @param file_path path to store data file, excluding surrounding data
     */
    private void dataToFile(ArrayList<CEAInputData> dataInputs, String directory_path, String file_path) {
        //Parse input data to JSON
        String dataString = "[";

        for(int i = 0; i < dataInputs.size(); i++) {
            dataInputs.get(i).setSurrounding(null);
            dataString += new Gson().toJson(dataInputs.get(i));
            if(i!=dataInputs.size()-1) dataString += ", ";
        }
        dataString+="]";

        File dir = new File(directory_path);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new JPSRuntimeException(new FileNotFoundException(directory_path));
        }

        try {
            BufferedWriter f_writer = new BufferedWriter(new FileWriter(file_path));
            f_writer.write(dataString);
            f_writer.close();
        } catch (IOException e) {
            throw new JPSRuntimeException(e);
        }
    }

    @Override
    public void run() {
        while (!stop) {

            try {
                String strTmp = System.getProperty("java.io.tmpdir")+FS+"thread_"+threadNumber;

                String OS = System.getProperty("os.name").toLowerCase();
                ArrayList<String> args = new ArrayList<>();
                ArrayList<String> args2 = new ArrayList<>();
                ArrayList<String> args3 = new ArrayList<>();
                ArrayList<String> args4 = new ArrayList<>();
                ArrayList<String> args5 = new ArrayList<>();
                String workflowPath = strTmp + FS + "workflow.yml";
                String data_path = strTmp + FS + DATA_FILE;
                String surroundings_path = strTmp + FS + SURROUNDINGS_FILE;

                dataToFile(this.inputs, strTmp, data_path, surroundings_path);

                String flag = noSurroundings ? "1" : "0";


                if(OS.contains("win")){
                    String f_path;

                    args.add("cmd.exe");
                    args.add("/C");
                    f_path = new File(Objects.requireNonNull(getClass().getClassLoader().getResource(SHAPEFILE_SCRIPT)).toURI()).getAbsolutePath();
                    args.add("conda activate cea && python " + f_path + " " + data_path + " " + strTmp + " " + crs+" zone.shp");

                    args2.add("cmd.exe");
                    args2.add("/C");
                    f_path = new File(Objects.requireNonNull(getClass().getClassLoader().getResource(SHAPEFILE_SCRIPT)).toURI()).getAbsolutePath();
                    args2.add("conda activate cea && python " + f_path + " " + surroundings_path + " " + strTmp + " " + crs+" surroundings.shp");

                    args3.add("cmd.exe");
                    args3.add("/C");
                    f_path = new File(Objects.requireNonNull(getClass().getClassLoader().getResource(TYPOLOGY_SCRIPT)).toURI()).getAbsolutePath();
                    args3.add("conda activate cea && python " + f_path + " " + data_path + " " + strTmp);

                    args4.add("cmd.exe");
                    args4.add("/C");
                    args4.add("conda activate cea && ");
                    args4.add("python");
                    args4.add(new File(
                            Objects.requireNonNull(getClass().getClassLoader().getResource(CREATE_WORKFLOW_SCRIPT)).toURI()).getAbsolutePath());
                    args4.add(new File(
                            Objects.requireNonNull(getClass().getClassLoader().getResource(WORKFLOW_SCRIPT)).toURI()).getAbsolutePath());
                    args4.add(strTmp);
                    args4.add(flag);

                    args5.add("cmd.exe");
                    args5.add("/C");
                    args5.add("conda activate cea && cea workflow --workflow " + workflowPath);
                }
                else {
                    String shapefile = FS+"target"+FS+"classes"+FS+SHAPEFILE_SCRIPT;
                    String typologyfile = FS+"target"+FS+"classes"+FS+TYPOLOGY_SCRIPT;
                    String createWorkflowFile = FS+"target"+FS+"classes"+FS+CREATE_WORKFLOW_SCRIPT;
                    String workflowFile = FS+"target"+FS+"classes"+FS+WORKFLOW_SCRIPT;

                    args.add("/bin/bash");
                    args.add("-c");
                    args.add("export PROJ_LIB=/venv/share/lib && python " + shapefile +" "+ data_path +" " +strTmp+" "+crs+" zone.shp");

                    args2.add("/bin/bash");
                    args2.add("-c");
                    args2.add("export PROJ_LIB=/venv/share/lib && python " + shapefile +" "+ surroundings_path +" " +strTmp+" "+crs+" surroundings.shp");

                    args3.add("/bin/bash");
                    args3.add("-c");
                    args3.add("export PROJ_LIB=/venv/share/lib && python " + typologyfile +" '"+ data_path +"' " + strTmp);

                    args4.add("/bin/bash");
                    args4.add("-c");
                    args4.add("export PROJ_LIB=/venv/share/lib && python " + createWorkflowFile + " " + workflowFile + " " + strTmp + " " + flag);


                    args5.add("/bin/bash");
                    args5.add("-c");
                    args5.add("export PATH=/venv/bin:/venv/cea/bin:/venv/Daysim:$PATH && source /venv/bin/activate && cea workflow --workflow " + workflowPath);

                }

                // create the shapefile process and run
                runProcess(args);
                // if there are surrounding data, create the shapefile process for surroundings and run
                if (!noSurroundings){runProcess(args2);}
                // create the typologyfile process and run
                runProcess(args3);
                // create the workflow process and run
                runProcess(args4);
                // Run workflow that runs all CEA scripts
                runProcess(args5);

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
