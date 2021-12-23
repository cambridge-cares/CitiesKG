package uk.ac.cam.cares.twa.cities.tasks;

import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import com.google.gson.Gson;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.lang.Process;
import java.util.Objects;
import java.util.concurrent.Callable;

public class RunCEATask implements Callable<CEAOutputData> {
    private final CEAInputData inputs;
    private Boolean stop = false;
    private static final String SHAPEFILE_SCRIPT = "create_shapefile.py";
    private static final String WORKFLOW_SCRIPT = "workflow.yml";
    private static final String CREATE_WORKFLOW_SCRIPT = "create_cea_workflow.py";
    private static final String FS = System.getProperty("file.separator");
    public RunCEATask(CEAInputData buildingData) {  this.inputs = buildingData; }

    public void stop() {
        stop = true;
    }

    public void runProcess(ArrayList<String> args) {
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        // starting the process
        try {
            Process p = builder.start();
            p.waitFor();
        } catch ( IOException | InterruptedException e) {
            e.printStackTrace();
            throw new JPSRuntimeException(e);
        }
    }

    public void deleteDirectory(File file)
    {
        for (File subFile : file.listFiles()) {
            // if it is a subfolder recursively call function to empty it
            if (subFile.isDirectory()) {
                deleteDirectory(subFile);
            }
            subFile.delete();
        }
    }

    public CEAOutputData extractOutputs() {
        String line = "";
        String splitBy = ",";
        CEAOutputData result = new CEAOutputData();
        String projectDir = System.getProperty("java.io.tmpdir")+FS+"testProject";

        try{
            //parsing a CSV file into BufferedReader class constructor
            BufferedReader demand_file = new BufferedReader(new FileReader(projectDir+FS+"testScenario"+FS+"outputs"+FS+"data"+FS+"demand"+FS+"Total_demand.csv"));
            BufferedReader PV_file = new BufferedReader(new FileReader(projectDir+FS+"testScenario"+FS+"outputs"+FS+"data"+FS+"potentials"+FS+"solar"+FS+"PV_total_buildings.csv"));
            ArrayList<String[]> demand_columns = new ArrayList<>();
            ArrayList<String[] > PV_columns = new ArrayList<>();

            while ((line = demand_file.readLine()) != null)   //returns a Boolean value
            {
                String[] rows = line.split(splitBy);    // use comma as separator
                demand_columns.add(rows);
            }
            for(int n=0; n<demand_columns.get(0).length; n++) {
                 if(demand_columns.get(0)[n].equals("GRID_MWhyr")) {
                     result.grid_demand = demand_columns.get(1)[n];
                 }
                 else if(demand_columns.get(0)[n].equals("QH_sys_MWhyr")) {
                     result.heating_demand = demand_columns.get(1)[n];
                 }
                 else if(demand_columns.get(0)[n].equals("QC_sys_MWhyr")) {
                     result.cooling_demand = demand_columns.get(1)[n];
                 }
                 else if(demand_columns.get(0)[n].equals("E_sys_MWhyr")) {
                     result.electricity_demand = demand_columns.get(1)[n];
                 }
            }
            while ((line = PV_file.readLine()) != null)   //returns a Boolean value
            {
                String[] rows = line.split(splitBy);    // use comma as separator
                PV_columns.add(rows);
            }
            for(int n=0; n<PV_columns.get(0).length; n++) {
                if(PV_columns.get(0)[n].equals("E_PVT_gen_kWh")) {
                    result.PV_supply=PV_columns.get(1)[n];
                }
                else if(PV_columns.get(0)[n].equals("Area_PVT_m2")) {
                    result.PV_area=PV_columns.get(1)[n];
                }
            }
        } catch ( IOException e) {

        }
        File file = new File(projectDir);
        deleteDirectory(file);
        file.delete();

        return result;
    }

    @Override
    public CEAOutputData call() {
        while (!stop) {

            try {
                //Parse input data to JSON
                String dataString = new Gson().toJson(inputs);

                String strTmp = System.getProperty("java.io.tmpdir");

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

            } catch ( NullPointerException | URISyntaxException e) {
                e.printStackTrace();
                throw new JPSRuntimeException(e);
            } finally {
                stop();
            }
        }
        return extractOutputs();
    }
}
