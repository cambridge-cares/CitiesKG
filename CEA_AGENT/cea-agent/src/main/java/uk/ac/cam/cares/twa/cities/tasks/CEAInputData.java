package uk.ac.cam.cares.twa.cities.tasks;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CEAInputData {
    public String geometry;
    public String height;
    public Map<String, Double> usage;
    public ArrayList<CEAInputData> surrounding;

    public List<OffsetDateTime> weatherTimes;

    public Map<String, List<Double>> weather;
    public List<Double> weatherCoordinate;

    public CEAInputData(String geometry_value, String height_value, Map<String, Double> usage_value, ArrayList<CEAInputData> surrounding_value, List<OffsetDateTime> weatherTimes_value, Map<String, List<Double>> weather_value, List<Double> weatherCoordinate_value) {
        this.geometry = geometry_value;
        this.height = height_value;
        this.usage = usage_value;
        this.surrounding = surrounding_value;
        this.weatherTimes = weatherTimes_value;
        this.weather = weather_value;
        this.weatherCoordinate = weatherCoordinate_value;
    }

    public String getGeometry() {
        return this.geometry;
    }

    public String getHeight() {
        return this.height;
    }

    public Map<String, Double> getUsage() {
        return this.usage;
    }

    public ArrayList<CEAInputData> getSurrounding() {
        return this.surrounding;
    }

    public List<OffsetDateTime> getWeatherTimes() {return this.weatherTimes;}
    public Map<String, List<Double>> getWeather() {return this.weather;}
    public List<Double> getWeatherCoordinate() {return this.weatherCoordinate;}

    public void setGeometry(String geometry_value) {
        this.geometry = geometry_value;
    }

    public  void setHeight(String height_value) {
        this.height = height_value;
    }

    public void setUsage(Map<String, Double> usage_value) {
        this.usage = usage_value;
    }

    public void setSurrounding(ArrayList<CEAInputData> surrounding_value) {
        this.surrounding = surrounding_value;
    }

    public void setWeatherTimes(List<OffsetDateTime> weatherTimes_value) {this.weatherTimes = weatherTimes_value;}

    public void setWeather(Map<String, List<Double>> weather_value) {this.weather = weather_value;}
    public void setWeatherCoordinate(List<Double> weatherCoordinate_value) {this.weatherCoordinate = weatherCoordinate_value;}
}
