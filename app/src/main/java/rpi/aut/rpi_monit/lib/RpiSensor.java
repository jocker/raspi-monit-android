package rpi.aut.rpi_monit.lib;

import android.content.Context;
import android.graphics.drawable.Drawable;

import rpi.aut.rpi_monit.R;

public enum RpiSensor {
    Sound("sound", R.drawable.ic_sensor_sound, R.string.sound, R.color.pink, R.color.pinkLight),
    Light("light", R.drawable.ic_sensor_light, R.string.light, R.color.green, R.color.greenLight),
    Temperature("temperature", R.drawable.ic_sensor_temperature, R.string.temperature, R.color.orange, R.color.orangeLight),
    Humidity("humidity", R.drawable.ic_sensor_humidity, R.string.humidity, R.color.blue, R.color.blueLight);


    static final int TEMP_MAX = 40, TEMP_MIN = 15;

    public static final RpiSensor[] ALL = new RpiSensor[]{
            Sound, Light, Temperature, Humidity,Sound
    };

    public static RpiSensor byId(int id){
        for(RpiSensor sensor: values()){
            if(sensor.id == id){
                return sensor;
            }
        }
        return null;
    }

    public static RpiSensor byRawType(String rawType){
        for(RpiSensor sensor: values()){
            if(sensor.rawType.equals(rawType)){
                return sensor;
            }
        }
        return null;
    }

    public final String rawType;

    private final int mColorResId, mLightColorResId;
    private final int mNameColorResId, mIconResId;

    public final int id;

    RpiSensor(String rawType, int iconResId, int nameResId, int color, int colorLight){
        this.rawType = rawType;
        mColorResId = color;
        mLightColorResId = colorLight;
        mNameColorResId = nameResId;
        mIconResId = iconResId;

        this.id = ordinal();
    }

    public int getColor(Context context){
        return Utils.getColor(context, mColorResId);
    }

    public int getLightColor(Context context){
        return Utils.getColor(context, mLightColorResId);
    }

    public Drawable getIcon(Context context){
        return DrawableUtils.getTintedDrawable(context, mIconResId, getColor(context));
    }

    // range is 0 to 100
    public float getDisplayPercent(Float value){
        if(value == null){
            return 0;
        }
        switch (this){
            case Sound:
            case Light:
            case Humidity:
                return value/100;
            case Temperature:
                return (float) Utils.mapValue(value, TEMP_MIN, TEMP_MAX, 0, 100);
        }
        return 0;
    }

    public CharSequence getDisplayText(Float value){
        if(value == null){
            return "-";
        }
        switch (this){
            case Sound:
            case Light:
            case Humidity:
                return getDisplayPercent(value)+"%";
            case Temperature:
                return (Math.round(value*100)/100)+"\u2103";
        }
        return "";
    }


}
