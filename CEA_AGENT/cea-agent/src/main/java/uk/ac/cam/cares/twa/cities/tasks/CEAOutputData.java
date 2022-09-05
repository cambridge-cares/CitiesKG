package uk.ac.cam.cares.twa.cities.tasks;

import java.util.ArrayList;
import java.util.List;

public class CEAOutputData {
    public ArrayList<ArrayList<String>> HourlyGridConsumption = new ArrayList<>();
    public ArrayList<ArrayList<String>> HourlyElectricityConsumption = new ArrayList<>();
    public ArrayList<ArrayList<String>> HourlyHeatingConsumption = new ArrayList<>();
    public ArrayList<ArrayList<String>> HourlyCoolingConsumption = new ArrayList<>();
    public ArrayList<String> PVRoofArea = new ArrayList<>();
    public ArrayList<String> PVWallSouthArea = new ArrayList<>();
    public ArrayList<String> PVWallNorthArea = new ArrayList<>();
    public ArrayList<String> PVWallEastArea = new ArrayList<>();
    public ArrayList<String> PVWallWestArea = new ArrayList<>();
    public ArrayList<ArrayList<String>> HourlyPVRoofSupply = new ArrayList<>();
    public ArrayList<ArrayList<String>> HourlyPVWallSouthSupply = new ArrayList<>();
    public ArrayList<ArrayList<String>> HourlyPVWallNorthSupply = new ArrayList<>();
    public ArrayList<ArrayList<String>> HourlyPVWallEastSupply = new ArrayList<>();
    public ArrayList<ArrayList<String>> HourlyPVWallWestSupply = new ArrayList<>();
    public String targetUrl;
    public ArrayList<String> iris = new ArrayList<>();
    public List<String> times = new ArrayList<>();

}
