package rpi.aut.rpi_monit.timeseries;

import org.json.JSONException;
import org.json.JSONObject;

import rpi.aut.rpi_monit.lib.RpiSensor;

public class TimeSeriesPoint {
    public final long timestamp;
    public final int value;
    public final RpiSensor rpiSensor;

    public static TimeSeriesPoint create(JSONObject json){
        if(json.has("type") && json.has("value") && json.has("ts")){
            try{
                String type = json.getString("type");
                RpiSensor sensor = RpiSensor.byRawType(type);
                if(sensor == null){
                    return null;
                }
                int value = json.getInt("value");
                long timestamp = json.getLong("ts");
                return new TimeSeriesPoint(sensor, timestamp, value);
            }catch (JSONException e){
                //ignore
            }
        }

        return null;
    }

    public static TimeSeriesPoint create(RpiSensor sensor, long timestamp, int value){
        return new TimeSeriesPoint(sensor, timestamp, value);
    }

    protected TimeSeriesPoint(RpiSensor sensor, long timestamp, int value){
        this.rpiSensor = sensor;
        this.timestamp = timestamp;
        this.value = value;
    }
}
