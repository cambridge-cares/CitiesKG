package uk.ac.cam.cares.twa.cities.tasks;

import java.util.ArrayList;
import java.util.List;

public class CEAOutputData {
    public ArrayList<String> grid_demand = new ArrayList<>();
    public ArrayList<String> electricity_demand = new ArrayList<>();
    public ArrayList<String> heating_demand = new ArrayList<>();
    public ArrayList<String> cooling_demand = new ArrayList<>();
    public ArrayList<String> PV_area = new ArrayList<>();
    public ArrayList<String> PV_supply = new ArrayList<>();
    public String targetUrl;
    public ArrayList<String> iri = new ArrayList<>();
    public List<List<List<?>>> timeSeries = new ArrayList<>();
    public List<String> times = new ArrayList<>();

}
