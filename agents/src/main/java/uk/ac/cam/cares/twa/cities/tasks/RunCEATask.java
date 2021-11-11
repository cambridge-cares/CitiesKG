package uk.ac.cam.cares.twa.cities.tasks;

import uk.ac.cam.cares.jps.base.exception.JPSRuntimeException;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.io.FileReader;

public class RunCEATask implements Runnable {
    private final ArrayList<String> inputs;
    private final CEAOutputData result;
    private Boolean stop = false;

    public RunCEATask(ArrayList<String> buildingData, CEAOutputData output) {
        this.inputs = buildingData;
        this.result = output;
    }

    public void stop() {
        stop = true;
    }

    public void runCEAScript(String ceaArgs) {
        String CMDER_ROOT = "C:\\Users\\ELLO01\\Documents\\CityEnergyAnalyst\\Dependencies\\cmder";
        String FilePath = new File(CMDER_ROOT, "test_new.bat").getAbsolutePath();

        ArrayList<String> args = new ArrayList<>();
        args.add(FilePath);
        args.add(ceaArgs);

        ProcessBuilder builder = new ProcessBuilder(args);

        // starting the process
        try {
            Process process = builder.start();
        } catch ( IOException  e) {
            e.printStackTrace();
            throw new JPSRuntimeException(e);
        }
    }

    public CEAOutputData extractOutputs() {
        String line = "";
        String splitBy = ",";
        CEAOutputData result = new CEAOutputData();
        try{
            //parsing a CSV file into BufferedReader class constructor
            BufferedReader demand_file = new BufferedReader(new FileReader("C:\\Users\\ELLO01\\Documents\\testProject\\testProject1\\testScenario\\outputs\\data\\demand\\Total_demand.csv"));
            BufferedReader PV_file = new BufferedReader(new FileReader("C:\\Users\\ELLO01\\Documents\\testProject\\testProject1\\testScenario\\outputs\\data\\potentials\\solar\\PVT_total_buildings.csv"));
            int i=0;
            ArrayList<String[]> demand_columns = new ArrayList<>();
            ArrayList<String[] > PV_columns = new ArrayList<>();

            while ((line = demand_file.readLine()) != null)   //returns a Boolean value
            {
                String[] rows = line.split(splitBy);    // use comma as separator
                demand_columns.add(rows);
                i++;
            }
            for(int n=0; n<demand_columns.get(0).length; n++) {
                 if(demand_columns.get(0)[n].equals("GRID_MWhyr")) {
                     result.grid_demand=demand_columns.get(1)[n];
                 }
                 else if(demand_columns.get(0)[n].equals("QH_sys_MWhyr")) {
                     result.heating_demand=demand_columns.get(1)[n];
                 }
                 else if(demand_columns.get(0)[n].equals("QC_sys_MWhyr")) {
                     result.cooling_demand=demand_columns.get(1)[n];
                 }
            }
            i=0;
            while ((line = PV_file.readLine()) != null)   //returns a Boolean value
            {
                String[] rows = line.split(splitBy);    // use comma as separator
                PV_columns.add(rows);
                i++;
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
        return result;
    }

    @Override
    public void run() {
        while (!stop) {

            try {
                // Extract input data
                CEAInputData result = new CEAInputData();

                result.geometry = inputs.get(0);
                result.floors_ag = inputs.get(1);

                //Parse data to JSON
                String dataString = new Gson().toJson(result);
                String FilePath = new File("C:\\Users\\ELLO01\\Documents\\CitiesKG\\utils", "create_shapefile.py").getAbsolutePath();

                ArrayList<String> args2 = new ArrayList<>();
                args2.add("python");
                args2.add(FilePath);
                args2.add(dataString.replace("\"", "\\\""));

                // creating the process
                ProcessBuilder build = new ProcessBuilder(args2);
                // starting the process
                build.start();

                // Run a workflow that runs all CEA scripts
                String workflowArgs = "cea workflow --workflow C:\\Users\\ELLO01\\Documents\\CitiesKG\\utils\\workflow.yml";
                runCEAScript(workflowArgs);

                CEAOutputData results = extractOutputs();

            } catch (  IOException | NullPointerException e) {
                e.printStackTrace();
                throw new JPSRuntimeException(e);
            } finally {
                stop();
            }
        }
    }
}
