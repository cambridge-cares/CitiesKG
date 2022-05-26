package uk.ac.cam.cares.twa.cities.tasks;

import java.util.ArrayList;
import java.util.List;

public class CEAOutputData {
    public ArrayList<ArrayList<String>> grid_demand = new ArrayList<>();
    public ArrayList<ArrayList<String>> electricity_demand = new ArrayList<>();
    public ArrayList<ArrayList<String>> heating_demand = new ArrayList<>();
    public ArrayList<ArrayList<String>> cooling_demand = new ArrayList<>();
    public ArrayList<String> PV_area_roof = new ArrayList<>();
    public ArrayList<String> PV_area_wall_south = new ArrayList<>();
    public ArrayList<String> PV_area_wall_north = new ArrayList<>();
    public ArrayList<String> PV_area_wall_east = new ArrayList<>();
    public ArrayList<String> PV_area_wall_west = new ArrayList<>();
    public ArrayList<ArrayList<String>> PV_supply_roof = new ArrayList<>();
    public ArrayList<ArrayList<String>> PV_supply_wall_south = new ArrayList<>();
    public ArrayList<ArrayList<String>> PV_supply_wall_north = new ArrayList<>();
    public ArrayList<ArrayList<String>> PV_supply_wall_east = new ArrayList<>();
    public ArrayList<ArrayList<String>> PV_supply_wall_west = new ArrayList<>();
    public String targetUrl;
    public ArrayList<String> iris = new ArrayList<>();
    public List<String> times = new ArrayList<>();

}
