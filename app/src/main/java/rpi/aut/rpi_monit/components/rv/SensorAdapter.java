package rpi.aut.rpi_monit.components.rv;

import android.support.annotation.LayoutRes;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import rpi.aut.rpi_monit.lib.RpiSensor;

public abstract class SensorAdapter extends BaseAdapter {
    private final RpiSensor[] mItems;
    private final SparseArray<Float> mSensorValues;

    public SensorAdapter(RpiSensor[] sensors){
        mItems = sensors;
        mSensorValues = new SparseArray<>(mItems.length);
    }

    @Override
    public int getItemCount() {
        return mItems.length;
    }

    public RpiSensor getItem(int position){
        return mItems[position];
    }

    public void setSensorValue(RpiSensor sensor, Float value){
        Float current = getSensorValue(sensor);
        if((current == null && value == null) || (current != null && !value.equals(current))){
            return;
        }
        mSensorValues.put(sensor.id, value);
        for(int i=0; i< mItems.length; i++){
            if(sensor.equals(mItems[i])){
                notifyItemChanged(i);
            }
        }
    }

    protected View inflateView(ViewGroup parent, @LayoutRes int layoutResId){
        return LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false);
    }

    public Float getSensorValue(RpiSensor sensor){
        return mSensorValues.get(sensor.id);
    }

    protected float getSensorDisplayPercent(RpiSensor sensor){
        return sensor.getDisplayPercent(getSensorValue(sensor));
    }

    protected CharSequence getSensorDisplayValue(RpiSensor sensor){
        return sensor.getDisplayText(getSensorValue(sensor));
    }
}
