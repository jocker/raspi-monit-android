package rpi.aut.rpi_monit.charts;

import android.content.Context;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;

import java.lang.ref.WeakReference;

import io.reactivex.android.schedulers.AndroidSchedulers;
import rpi.aut.rpi_monit.BaseActivity;
import rpi.aut.rpi_monit.lib.RpiSensor;
import rpi.aut.rpi_monit.lib.RxView;
import rpi.aut.rpi_monit.lib.Utils;
import rpi.aut.rpi_monit.services.RpiService;
import rpi.aut.rpi_monit.timeseries.TimeSeries;
import rpi.aut.rpi_monit.timeseries.TimeSeriesData;
import rpi.aut.rpi_monit.timeseries.TimeSeriesPoint;

public abstract class BaseChart {

    private TimeSeries mSeries;
    private int mViewportSize;
    private RpiService.SensorDataProvider mDataProvider;


    private WeakReference<LineChart> chartRef;
    private final RpiSensor mSensor;
    public BaseChart(RpiSensor sensor, LineChart chart) {
        mSensor = sensor;
        if(chart != null){
            setChart(chart);
        }
    }

    public void setDataProvider(RpiService.SensorDataProvider provider){
        mDataProvider = provider;
        provider.load().takeUntil(RxView.onDetach(getChart())).observeOn(AndroidSchedulers.mainThread()).subscribe(resp -> {
            if(resp.isSuccessful()){
                add(resp.body);
                provider.onNewEntry().takeUntil(RxView.onDetach(getChart())).observeOn(AndroidSchedulers.mainThread()).subscribe(timeSeriesPoint -> {
                    add(timeSeriesPoint);
                });
            }
        });
    }

    public final void setChart(LineChart chart) {
        if(chartRef == null && chart != null){
            chartRef = new WeakReference<>(chart);
            onAttached(chart);
            RxView.onAttach(chart).take(1).flatMap(v -> {
                return ((BaseActivity)Utils.getHostActivity(chart)).getServiceConnection(RpiService.class).map(iBinder -> {
                    return ((RpiService.ServiceBinder)iBinder).getSensorDataProvider(mSensor);
                });
            }).takeUntil(RxView.onDetach(chart)).observeOn(AndroidSchedulers.mainThread()).subscribe(dataProvider -> {
                setDataProvider(dataProvider);
            });


        }
    }

    protected abstract void onAttached(LineChart chart);

    protected LineChart getChart() {
        return Utils.getReferencedValue(chartRef);
    }

    protected void add(TimeSeriesPoint point){
        if(getChart() == null){
            Log.wtf("WS", "chart missing");
            return;
        }
        if(getChart().getData() != null && mSeries != null){
            int insertIndex = mSeries.add(point.timestamp, point.value);
            onDataInserted(insertIndex);
        }
    }

    protected void add(TimeSeriesData data) {

        if(mViewportSize == 0){
            mViewportSize = data.size;
            getChart().setVisibleXRangeMaximum(mViewportSize);
        }

        if (data == null || data.isEmpty()) {
            return;
        }

        if(mSeries == null){
            mSeries = new TimeSeries(0);
        }

        int insertPos = mSeries.add(data);
        if(insertPos < 0){
            return;
        }

        if(insertPos == 0){
            setData(getChart(), mSeries);
        }else{
            onDataInserted(insertPos);
        }

    }

    private void onDataInserted(int insertPos){
        if(insertPos >= 0){

            if(mSeries.getCount() > 2*mViewportSize){
                mSeries.trimToSize(mViewportSize);
                setData(getChart(), mSeries);
            }else{
                for(int i=insertPos;i<mSeries.getCount(); i++){
                    addDataPoint(getChart(), mSeries, i, mSeries.getValue(i));
                }
            }
        }

    }

    public int getViewportSize(){
        return mViewportSize;
    }

    protected TimeSeries getSeries() {
        return mSeries;
    }

    protected abstract void setData(LineChart chart, TimeSeries series);

    protected abstract void addDataPoint(LineChart chart, TimeSeries series, int position, int value);

    protected Context getContext(){
        if(getChart() == null){
            return null;
        }
        return getChart().getContext();
    }


    public int getPrimaryColor(){
        return mSensor.getColor(getContext());
    }

    public int getPrimaryColorLight(){
        return mSensor.getLightColor(getContext());
    }

}