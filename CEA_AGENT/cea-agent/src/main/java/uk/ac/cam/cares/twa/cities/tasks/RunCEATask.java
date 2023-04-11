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
import java.util.*;
import java.lang.Process;

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
    private static final String WORKFLOW_SCRIPT2 = "workflow2.yml";
    private static final String CREATE_WORKFLOW_SCRIPT = "create_cea_workflow.py";
    private static final String FS = System.getProperty("file.separator");
    private Map<String, ArrayList<String>> solarSupply = new HashMap<>();

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
        String solarDir = tmpDir + FS + "testProject" + FS + "testScenario" + FS + "outputs" + FS + "data" + FS + "potentials" + FS + "solar" + FS;


        try{
            for (String generatorType : solarSupply.keySet()) {
                String generator = generatorType;

                if (generatorType.contains("PVT")) {generator = "PVT";}
                
                //parsing a CSV file into BufferedReader class constructor
                FileReader solar = new FileReader(solarDir + generatorType + "_total_buildings.csv");
                BufferedReader solarFile = new BufferedReader(solar);
                ArrayList<String[]> solarColumns = new ArrayList<>();

                while ((line = solarFile.readLine()) != null)   //returns a Boolean value
                {
                    String[] rows = line.split(splitBy);    // use comma as separator
                    solarColumns.add(rows);
                }
                
                if (generatorType.equals("PVT_FP")) {
                    for (int n = 0; n < solarColumns.get(0).length; n++) {
                        if (solarColumns.get(0)[n].equals(generator + "_roofs_top_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.PVTPlateRoofArea.add(solarColumns.get(m)[n]);
                            }
                        } else if (solarColumns.get(0)[n].equals(generator + "_walls_south_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.PVTPlateWallSouthArea.add(solarColumns.get(m)[n]);
                            }
                        } else if (solarColumns.get(0)[n].equals(generator + "_walls_north_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.PVTPlateWallNorthArea.add(solarColumns.get(m)[n]);
                            }
                        } else if (solarColumns.get(0)[n].equals(generator + "_walls_east_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.PVTPlateWallEastArea.add(solarColumns.get(m)[n]);
                            }
                        } else if (solarColumns.get(0)[n].equals(generator + "_walls_west_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.PVTPlateWallWestArea.add(solarColumns.get(m)[n]);
                            }
                        }
                    }
                } else if (generatorType.equals("PVT_ET")) {
                    for (int n = 0; n < solarColumns.get(0).length; n++) {
                        if (solarColumns.get(0)[n].equals(generator + "_roofs_top_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.PVTTubeRoofArea.add(solarColumns.get(m)[n]);
                            }
                        } else if (solarColumns.get(0)[n].equals(generator + "_walls_south_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.PVTTubeWallSouthArea.add(solarColumns.get(m)[n]);
                            }
                        } else if (solarColumns.get(0)[n].equals(generator + "_walls_north_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.PVTTubeWallNorthArea.add(solarColumns.get(m)[n]);
                            }
                        } else if (solarColumns.get(0)[n].equals(generator + "_walls_east_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.PVTTubeWallEastArea.add(solarColumns.get(m)[n]);
                            }
                        } else if (solarColumns.get(0)[n].equals(generator + "_walls_west_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.PVTTubeWallWestArea.add(solarColumns.get(m)[n]);
                            }
                        }
                    }
                } else if (generatorType.equals("PV")) {
                    for (int n = 0; n < solarColumns.get(0).length; n++) {
                        if (solarColumns.get(0)[n].equals(generator + "_roofs_top_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.PVRoofArea.add(solarColumns.get(m)[n]);
                            }
                        } else if (solarColumns.get(0)[n].equals(generator + "_walls_south_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.PVWallSouthArea.add(solarColumns.get(m)[n]);
                            }
                        } else if (solarColumns.get(0)[n].equals(generator + "_walls_north_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.PVWallNorthArea.add(solarColumns.get(m)[n]);
                            }
                        } else if (solarColumns.get(0)[n].equals(generator + "_walls_east_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.PVWallEastArea.add(solarColumns.get(m)[n]);
                            }
                        } else if (solarColumns.get(0)[n].equals(generator + "_walls_west_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.PVWallWestArea.add(solarColumns.get(m)[n]);
                            }
                        }
                    }
                } else if (generatorType.equals("SC_FP")) {
                    for (int n = 0; n < solarColumns.get(0).length; n++) {
                        if (solarColumns.get(0)[n].equals(generator + "_roofs_top_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.ThermalPlateRoofArea.add(solarColumns.get(m)[n]);
                            }
                        } else if (solarColumns.get(0)[n].equals(generator + "_walls_south_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.ThermalPlateWallSouthArea.add(solarColumns.get(m)[n]);
                            }
                        } else if (solarColumns.get(0)[n].equals(generator + "_walls_north_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.ThermalPlateWallNorthArea.add(solarColumns.get(m)[n]);
                            }
                        } else if (solarColumns.get(0)[n].equals(generator + "_walls_east_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.ThermalPlateWallEastArea.add(solarColumns.get(m)[n]);
                            }
                        } else if (solarColumns.get(0)[n].equals(generator + "_walls_west_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.ThermalPlateWallWestArea.add(solarColumns.get(m)[n]);
                            }
                        }
                    }
                } else if (generatorType.equals("SC_ET")) {
                    for (int n = 0; n < solarColumns.get(0).length; n++) {
                        if (solarColumns.get(0)[n].equals(generator + "_roofs_top_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.ThermalTubeRoofArea.add(solarColumns.get(m)[n]);
                            }
                        } else if (solarColumns.get(0)[n].equals(generator + "_walls_south_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.ThermalTubeWallSouthArea.add(solarColumns.get(m)[n]);
                            }
                        } else if (solarColumns.get(0)[n].equals(generator + "_walls_north_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.ThermalTubeWallNorthArea.add(solarColumns.get(m)[n]);
                            }
                        } else if (solarColumns.get(0)[n].equals(generator + "_walls_east_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.ThermalTubeWallEastArea.add(solarColumns.get(m)[n]);
                            }
                        } else if (solarColumns.get(0)[n].equals(generator + "_walls_west_m2")) {
                            for (int m = 1; m < solarColumns.size(); m++) {
                                result.ThermalTubeWallWestArea.add(solarColumns.get(m)[n]);
                            }
                        }
                    }
                }
                solarFile.close();
                solar.close();
            }
        } catch ( IOException e) {
            File file = new File(tmpDir);
            deleteDirectoryContents(file);
            file.delete();
            e.printStackTrace();
            throw new JPSRuntimeException("There are no CEA outputs, CEA encountered an error");
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
        boolean getTimes = true;

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

                String solar;

                if(i<10){
                    solar = projectDir+FS+"testScenario"+FS+"outputs"+FS+"data"+FS+"potentials"+FS+"solar"+FS+"B00"+i;
                }
                else if(i<100){
                    solar = projectDir+FS+"testScenario"+FS+"outputs"+FS+"data"+FS+"potentials"+FS+"solar"+FS+"B0"+i;
                }
                else{
                    solar = projectDir+FS+"testScenario"+FS+"outputs"+FS+"data"+FS+"potentials"+FS+"solar"+FS+"B"+i;
                }

                for (Map.Entry<String, ArrayList<String>> entry: solarSupply.entrySet()){
                    result = extractSolarSupply(result, entry.getKey(), entry.getValue(), splitBy, solar + "_" + entry.getKey().toString() + ".csv", tmpDir, getTimes);
                    getTimes = false;
                }

                result.GridConsumption.add(grid_results);
                result.ElectricityConsumption.add(electricity_results);
                result.HeatingConsumption.add(heating_results);
                result.CoolingConsumption.add(cooling_results);
            }
        } catch ( IOException e) {
            File file = new File(tmpDir);
            deleteDirectoryContents(file);
            file.delete();
            e.printStackTrace();
            throw new JPSRuntimeException("There are no CEA outputs, CEA encountered an error");
        }
        return result;
    }

    /**
     * Extract potential energy data of solar generators
     * @param result CEAOutputData to store the CEA outputs
     * @param generatorType type of solar generator
     * @param supplyTypes types of potential energy that generatorType can generate
     * @param dataSeparator separator of the CEA output csv files
     * @param solarFile file name of the csv storing the output data for generatorType
     * @param tmpDir root directory of CEA files
     * @param getTimes whether to extract timestamps
     */
    public CEAOutputData extractSolarSupply(CEAOutputData result, String generatorType, List<String> supplyTypes, String dataSeparator, String solarFile, String tmpDir, Boolean getTimes) {
        String line;
        String supply;
        String generator = generatorType;
        List<String> timestamps = new ArrayList();

        if (generatorType.contains("PVT")) {generator = "PVT";}
        
        try {
            FileReader solar = new FileReader(solarFile);

            BufferedReader solar_file = new BufferedReader(solar);
            ArrayList<String[]> solar_columns = new ArrayList<>();

            while ((line = solar_file.readLine()) != null)   //returns a Boolean value
            {
                String[] rows = line.split(dataSeparator);    // use comma as separator
                solar_columns.add(rows);
            }

            for (int i = 0; i < supplyTypes.size(); i++) {
                ArrayList<String> roof_results = new ArrayList<>();
                ArrayList<String> wall_south_results = new ArrayList<>();
                ArrayList<String> wall_north_results = new ArrayList<>();
                ArrayList<String> wall_east_results = new ArrayList<>();
                ArrayList<String> wall_west_results = new ArrayList<>();

                supply = supplyTypes.get(i);

                for (int n = 0; n < solar_columns.get(0).length; n++) {
                    if (getTimes && solar_columns.get(0)[n].equals("Date")) {
                        for (int m = 1; m < solar_columns.size(); m++) {
                            timestamps.add(solar_columns.get(m)[n].replaceAll("\\s", "T"));
                        }
                    } else if (solar_columns.get(0)[n].equals(generator + "_roofs_top_" + supply + "_kWh")) {
                        for (int m = 1; m < solar_columns.size(); m++) {
                            roof_results.add(solar_columns.get(m)[n]);
                        }
                    } else if (solar_columns.get(0)[n].equals(generator + "_walls_south_" + supply + "_kWh")) {
                        for (int m = 1; m < solar_columns.size(); m++) {
                            wall_south_results.add(solar_columns.get(m)[n]);
                        }
                    } else if (solar_columns.get(0)[n].equals(generator + "_walls_north_" + supply + "_kWh")) {
                        for (int m = 1; m < solar_columns.size(); m++) {
                            wall_north_results.add(solar_columns.get(m)[n]);
                        }
                    } else if (solar_columns.get(0)[n].equals(generator + "_walls_west_" + supply + "_kWh")) {
                        for (int m = 1; m < solar_columns.size(); m++) {
                            wall_west_results.add(solar_columns.get(m)[n]);
                        }
                    } else if (solar_columns.get(0)[n].equals(generator + "_walls_east_" + supply + "_kWh")) {
                        for (int m = 1; m < solar_columns.size(); m++) {
                            wall_east_results.add(solar_columns.get(m)[n]);
                        }
                    }
                }

                result = addSolarSupply(result, generatorType, "roof", supply, roof_results);
                result = addSolarSupply(result, generatorType, "wall_north", supply, wall_north_results);
                result = addSolarSupply(result, generatorType, "wall_south", supply, wall_south_results);
                result = addSolarSupply(result, generatorType, "wall_west", supply, wall_west_results);
                result = addSolarSupply(result, generatorType, "wall_east", supply, wall_east_results);

                if (getTimes) {
                    result.times = timestamps;
                }
            }

            solar_file.close();
            solar.close();

            return result;
        }
        catch ( IOException e) {
            File file = new File(tmpDir);
            deleteDirectoryContents(file);
            file.delete();
            e.printStackTrace();
            throw new JPSRuntimeException("There are no CEA outputs, CEA encountered an error");
        }
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

    /**
     * Add time series data on solar generator potential energy to CEAOutputData
     * @param result CEAOutputData to store the CEA outputs
     * @param generatorType type of solar generator
     * @param generatorLocation location of the solar generator
     * @param supplyType type of potential energy of data
     * @param data time series data of the potential energy for the solar generator
     */
    public CEAOutputData addSolarSupply(CEAOutputData result, String generatorType, String generatorLocation, String supplyType, ArrayList<String> data) {
        if (generatorType.equals("PVT_FP")) {
            if (supplyType.equals("E")) {
                if (generatorLocation.contains("roof")) {
                    result.PVTPlateRoofESupply.add(data);
                } else if (generatorLocation.contains("north")) {
                    result.PVTPlateWallNorthESupply.add(data);
                } else if (generatorLocation.contains("south")) {
                    result.PVTPlateWallSouthESupply.add(data);
                } else if (generatorLocation.contains("west")) {
                    result.PVTPlateWallWestESupply.add(data);
                } else if (generatorLocation.contains("east")) {
                    result.PVTPlateWallEastESupply.add(data);
                }
            } else if (supplyType.equals("Q")) {
                if (generatorLocation.contains("roof")) {
                    result.PVTPlateRoofQSupply.add(data);
                } else if (generatorLocation.contains("north")) {
                    result.PVTPlateWallNorthQSupply.add(data);
                } else if (generatorLocation.contains("south")) {
                    result.PVTPlateWallSouthQSupply.add(data);
                } else if (generatorLocation.contains("west")) {
                    result.PVTPlateWallWestQSupply.add(data);
                } else if (generatorLocation.contains("east")) {
                    result.PVTPlateWallEastQSupply.add(data);
                }
            }
        } else if (generatorType.equals("PVT_ET")) {
            if (supplyType.equals("E")) {
                if (generatorLocation.contains("roof")) {
                    result.PVTTubeRoofESupply.add(data);
                } else if (generatorLocation.contains("north")) {
                    result.PVTTubeWallNorthESupply.add(data);
                } else if (generatorLocation.contains("south")) {
                    result.PVTTubeWallSouthESupply.add(data);
                } else if (generatorLocation.contains("west")) {
                    result.PVTTubeWallWestESupply.add(data);
                } else if (generatorLocation.contains("east")) {
                    result.PVTTubeWallEastESupply.add(data);
                }
            } else if (supplyType.equals("Q")) {
                if (generatorLocation.contains("roof")) {
                    result.PVTTubeRoofQSupply.add(data);
                } else if (generatorLocation.contains("north")) {
                    result.PVTTubeWallNorthQSupply.add(data);
                } else if (generatorLocation.contains("south")) {
                    result.PVTTubeWallSouthQSupply.add(data);
                } else if (generatorLocation.contains("west")) {
                    result.PVTTubeWallWestQSupply.add(data);
                } else if (generatorLocation.contains("east")) {
                    result.PVTTubeWallEastQSupply.add(data);
                }
            }
        } else if (generatorType.equals("PV")) {
            if (generatorLocation.contains("roof")) {
                result.PVRoofSupply.add(data);
            } else if (generatorLocation.contains("north")) {
                result.PVWallNorthSupply.add(data);
            } else if (generatorLocation.contains("south")) {
                result.PVWallSouthSupply.add(data);
            } else if (generatorLocation.contains("west")) {
                result.PVWallWestSupply.add(data);
            } else if (generatorLocation.contains("east")) {
                result.PVWallEastSupply.add(data);
            }
        } else if (generatorType.equals("SC_FP")) {
            if (generatorLocation.contains("roof")) {
                result.ThermalPlateRoofSupply.add(data);
            } else if (generatorLocation.contains("north")) {
                result.ThermalPlateWallNorthSupply.add(data);
            } else if (generatorLocation.contains("south")) {
                result.ThermalPlateWallSouthSupply.add(data);
            } else if (generatorLocation.contains("west")) {
                result.ThermalPlateWallWestSupply.add(data);
            } else if (generatorLocation.contains("east")) {
                result.ThermalPlateWallEastSupply.add(data);
            }
        } else if (generatorType.equals("SC_ET")) {
            if (generatorLocation.contains("roof")) {
                result.ThermalTubeRoofSupply.add(data);
            } else if (generatorLocation.contains("north")) {
                result.ThermalTubeWallNorthSupply.add(data);
            } else if (generatorLocation.contains("south")) {
                result.ThermalTubeWallSouthSupply.add(data);
            } else if (generatorLocation.contains("west")) {
                result.ThermalTubeWallWestSupply.add(data);
            } else if (generatorLocation.contains("east")) {
                result.ThermalTubeWallEastSupply.add(data);
            }
        }

        return result;
    }

    /**
     * Sets solarSupply with the keys being the type of solar generator, and the values being the type of energy that the generator can generate
     */
    private void setSolarSupply() {
        ArrayList<String> EQ = new ArrayList<>();
        ArrayList<String> E = new ArrayList<>();
        ArrayList<String> Q = new ArrayList<>();

        EQ.add("E");
        EQ.add("Q");
        E.add("E");
        Q.add("Q");

        solarSupply.put("PVT_FP", EQ);
        solarSupply.put("PVT_ET", EQ);
        solarSupply.put("PV", E);
        solarSupply.put("SC_FP", Q);
        solarSupply.put("SC_ET", Q);
    }

    /**
     * Renames CEA outputs files on PVT since CEA uses the same file names for both plate PVT and tube PVT
     * @param solarDir directory that stores CEA output files on solar generators
     * @param PVTType the type of PVT used, FP for plate, and ET for tube
     */
    private void renamePVT(String solarDir, String PVTType) {
        File dir = new File(solarDir);

        for (final File f : dir.listFiles()) {
            if (f.getAbsolutePath().contains("PVT") && !f.getAbsolutePath().contains("FP") && !f.getAbsolutePath().contains("ET")) {
                File newFile = new File(f.getAbsolutePath().replace("PVT", "PVT_" + PVTType));
                f.renameTo(newFile);
            }
        }
    }

    @Override
    public void run() {
        setSolarSupply();
        while (!stop) {

            try {
                String strTmp = System.getProperty("java.io.tmpdir")+FS+"thread_"+threadNumber;

                String OS = System.getProperty("os.name").toLowerCase();
                ArrayList<String> args = new ArrayList<>();
                ArrayList<String> args2 = new ArrayList<>();
                ArrayList<String> args3 = new ArrayList<>();
                ArrayList<String> args4 = new ArrayList<>();
                ArrayList<String> args5 = new ArrayList<>();
                ArrayList<String> args6 = new ArrayList<>();
                ArrayList<String> args7 = new ArrayList<>();
                String workflowPath = strTmp + FS + "workflow.yml";
                String workflowPath2 = strTmp + FS + "workflow2.yml";
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
                    args4.add("workflow.yml");
                    args4.add(strTmp);
                    args4.add(flag);

                    args5.add("cmd.exe");
                    args5.add("/C");
                    args5.add("conda activate cea && cea workflow --workflow " + workflowPath);

                    args6.add("cmd.exe");
                    args6.add("/C");
                    args6.add("conda activate cea && ");
                    args6.add("python");
                    args6.add(new File(
                            Objects.requireNonNull(getClass().getClassLoader().getResource(CREATE_WORKFLOW_SCRIPT)).toURI()).getAbsolutePath());
                    args6.add(new File(
                            Objects.requireNonNull(getClass().getClassLoader().getResource(WORKFLOW_SCRIPT2)).toURI()).getAbsolutePath());
                    args6.add("workflow2.yml");
                    args6.add(strTmp);
                    args6.add("null");

                    args7.add("cmd.exe");
                    args7.add("/C");
                    args7.add("conda activate cea && cea workflow --workflow " + workflowPath2);
                }
                else {
                    String shapefile = FS+"target"+FS+"classes"+FS+SHAPEFILE_SCRIPT;
                    String typologyfile = FS+"target"+FS+"classes"+FS+TYPOLOGY_SCRIPT;
                    String createWorkflowFile = FS+"target"+FS+"classes"+FS+CREATE_WORKFLOW_SCRIPT;
                    String workflowFile = FS+"target"+FS+"classes"+FS+WORKFLOW_SCRIPT;
                    String workflowFile2 = FS+"target"+FS+"classes"+FS+WORKFLOW_SCRIPT2;

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
                    args4.add("export PROJ_LIB=/venv/share/lib && python " + createWorkflowFile + " " + workflowFile + " " + "workflow.yml" + " " + strTmp + " " + flag);

                    args5.add("/bin/bash");
                    args5.add("-c");
                    args5.add("export PATH=/venv/bin:/venv/cea/bin:/venv/Daysim:$PATH && source /venv/bin/activate && cea workflow --workflow " + workflowPath);

                    args6.add("/bin/bash");
                    args6.add("-c");
                    args6.add("export PROJ_LIB=/venv/share/lib && python " + createWorkflowFile + " " + workflowFile2 + " " + "workflow2.yml" + " " + strTmp + " " + "null");

                    args7.add("/bin/bash");
                    args7.add("-c");
                    args7.add("export PATH=/venv/bin:/venv/cea/bin:/venv/Daysim:$PATH && source /venv/bin/activate && cea workflow --workflow " + workflowPath2);
                }

                // create the shapefile process and run
                runProcess(args);
                // if there are surrounding data, create the shapefile process for surroundings and run
                if (!noSurroundings){runProcess(args2);}
                // create the typologyfile process and run
                runProcess(args3);
                // create the workflow process and run
                runProcess(args4);

                // CEA output file names for PVT plate collectors and PVT tube collectors are the same, so one PVT collector type has to be run first then the output files renamed before running the other PVT collector type
                // run workflow that runs all CEA scripts with PVT plate collectors
                runProcess(args5);

                // rename PVT output files to PVT plate
                renamePVT(strTmp+FS+"testProject"+FS+"testScenario"+FS+"outputs"+FS+"data"+FS+"potentials"+FS+"solar", "FP");

                // create workflow process for PVT tube collectors
                runProcess(args6);
                // run CEA for PVT tube collectors
                runProcess(args7);

                // rename PVT output files to PVT tube
                renamePVT(strTmp+FS+"testProject"+FS+"testScenario"+FS+"outputs"+FS+"data"+FS+"potentials"+FS+"solar", "ET");

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
