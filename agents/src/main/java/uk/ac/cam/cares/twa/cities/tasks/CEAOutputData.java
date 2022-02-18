package uk.ac.cam.cares.twa.cities.tasks;

import org.json.JSONArray;

import java.util.ArrayList;

public class CEAOutputData {
    public ArrayList<String> grid_demand = new ArrayList<>();
    public ArrayList<String> electricity_demand = new ArrayList<>();
    public ArrayList<String> heating_demand = new ArrayList<>();
    public ArrayList<String> cooling_demand = new ArrayList<>();
    public ArrayList<String> PV_area = new ArrayList<>();
    public ArrayList<String> PV_supply = new ArrayList<>();
    public String targetUrl;
    public ArrayList<String> iri = new ArrayList<>();

}
