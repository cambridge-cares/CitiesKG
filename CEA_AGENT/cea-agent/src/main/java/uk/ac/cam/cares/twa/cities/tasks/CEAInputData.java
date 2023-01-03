package uk.ac.cam.cares.twa.cities.tasks;

import java.util.ArrayList;

public class CEAInputData {
    public String geometry;
    public String height;
    public String usage;
    public String uri;
    public ArrayList<CEAInputData> surrounding;

    public CEAInputData(String geometry_value, String height_value, String usage_value, String uri_value, ArrayList<CEAInputData> surrounding_value) {
        this.geometry = geometry_value;
        this.height = height_value;
        this.usage = usage_value;
        this.uri = uri_value;
        this.surrounding = surrounding_value;
    }

    public String getGeometry() {
        return this.geometry;
    }

    public String getHeight() {
        return this.height;
    }

    public String getUsage() {
        return this.usage;
    }

    public String getUri() {
        return this.uri;
    }

    public ArrayList<CEAInputData> getSurrounding() {
        return this.surrounding;
    }

    public void setGeometry(String geometry_value) {
        this.geometry = geometry_value;
    }

    public  void setHeight(String height_value) {
        this.height = height_value;
    }

    public void setUsage(String usage_value) {
        this.usage = usage_value;
    }

    public void setUri(String uri_value) {
        this.uri = uri_value;
    }

    public void setSurrounding(ArrayList<CEAInputData> surrounding_value) {
        this.surrounding = surrounding_value;
    }
}
