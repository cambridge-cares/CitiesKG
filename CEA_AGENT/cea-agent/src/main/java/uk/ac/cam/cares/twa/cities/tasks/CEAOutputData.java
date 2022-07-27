package uk.ac.cam.cares.twa.cities.tasks;

import java.util.ArrayList;
import java.util.List;

public class CEAOutputData {
    public ArrayList<ArrayList<String>> GridConsumption = new ArrayList<>();
    public ArrayList<ArrayList<String>> ElectricityConsumption = new ArrayList<>();
    public ArrayList<ArrayList<String>> HeatingConsumption = new ArrayList<>();
    public ArrayList<ArrayList<String>> CoolingConsumption = new ArrayList<>();
    public ArrayList<String> PVRoofArea = new ArrayList<>();
    public ArrayList<String> PVWallSouthArea = new ArrayList<>();
    public ArrayList<String> PVWallNorthArea = new ArrayList<>();
    public ArrayList<String> PVWallEastArea = new ArrayList<>();
    public ArrayList<String> PVWallWestArea = new ArrayList<>();
    public ArrayList<ArrayList<String>> PVRoofSupply = new ArrayList<>();
    public ArrayList<ArrayList<String>> PVWallSouthSupply = new ArrayList<>();
    public ArrayList<ArrayList<String>> PVWallNorthSupply = new ArrayList<>();
    public ArrayList<ArrayList<String>> PVWallEastSupply = new ArrayList<>();
    public ArrayList<ArrayList<String>> PVWallWestSupply = new ArrayList<>();
    public String targetUrl;
    public ArrayList<String> iris = new ArrayList<>();
    public List<String> times = new ArrayList<>();

}
