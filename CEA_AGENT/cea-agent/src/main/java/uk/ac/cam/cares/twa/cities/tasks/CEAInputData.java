package uk.ac.cam.cares.twa.cities.tasks;

public class CEAInputData {
    public String geometry;
    public String height;
    public String usage;

    public CEAInputData(String geometry_value, String height_value, String usage_value) {
        this.geometry = geometry_value;
        this.height = height_value;
        this.usage = usage_value;
    }
}
