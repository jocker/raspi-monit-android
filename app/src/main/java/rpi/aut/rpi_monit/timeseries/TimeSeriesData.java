package rpi.aut.rpi_monit.timeseries;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import rpi.aut.rpi_monit.lib.RpiSensor;

public class TimeSeriesData {


    public static TimeSeriesData parse(JsonElement el){
        JsonObject root = el.getAsJsonObject();
        String sensorType = root.get("type").getAsString();
        RpiSensor rpiSensor = RpiSensor.byRawType(sensorType);
        if(rpiSensor == null){
            return null;
        }
        long minTs = root.get("min").getAsLong();
        int count = root.get("count").getAsInt();
        int stepSize = root.get("step").getAsInt();
        int minValue = root.get("series_min").getAsInt();
        int maxValue = root.get("series_max").getAsInt();

        JsonArray jsonPoints = root.get("points").getAsJsonArray();
        int[] points = new int[count];
        for(int i=0; i< count;i++){
            points[i] = jsonPoints.get(i).getAsInt();
        }

        return new TimeSeriesData(rpiSensor, minTs, count, stepSize, minValue, maxValue, points);
    }


    public final RpiSensor sensor;
    public final long timestamp;
    public final int size, stepSize, seriesMin, seriesMax;
    public final int[] points;


    private TimeSeriesData(RpiSensor sensor, long minTs, int count, int stepSize, int minValue, int maxValue, int[] points){
        this.sensor = sensor;
        this.timestamp  =minTs;
        this.size = count;
        this.stepSize = stepSize;
        this.seriesMin = minValue;
        this.seriesMax = maxValue;
        this.points = points;
    }


    public boolean isEmpty(){
        return this.points == null || this.points.length == 0;
    }



}
