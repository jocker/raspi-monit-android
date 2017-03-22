package rpi.aut.rpi_monit.charts;

import android.graphics.Color;
import android.support.annotation.StyleRes;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;

import rpi.aut.rpi_monit.lib.RpiSensor;
import rpi.aut.rpi_monit.lib.Utils;
import rpi.aut.rpi_monit.timeseries.TimeSeries;

public class WaveFormChart extends BaseChart {

    public WaveFormChart(RpiSensor sensor, LineChart chart) {
        super(sensor, chart);
    }

    @Override
    protected void onAttached(LineChart chart){
        if(chart != null){
            chart.setBackgroundColor(getPrimaryColor());
            chart.setDrawGridBackground(true);

            chart.setDrawBorders(false);


            // no description text
            chart.getDescription().setEnabled(false);
            chart.setPadding(0,0,0,0);
            // if disabled, scaling can be done on x- and y-axis separately
            chart.setPinchZoom(false);
            chart.setGridBackgroundColor(Color.TRANSPARENT);
            chart.setBorderColor(Color.TRANSPARENT);

            chart.getLegend().setEnabled(false);

            XAxis xAxis = chart.getXAxis();
            xAxis.setEnabled(false);

            YAxis leftAxis = chart.getAxisLeft();
            leftAxis.setAxisMaximum(50);
            leftAxis.setAxisMinimum(-50);
            leftAxis.setDrawAxisLine(false);
            leftAxis.setDrawZeroLine(false);
            leftAxis.setDrawGridLines(false);
            leftAxis.setDrawLabels(false);


            chart.setViewPortOffsets(-40f, 0f, 0f, 0f);

            chart.getAxisRight().setEnabled(false);
        }
        super.setChart(chart);
    }

    public void setTheme(@StyleRes int themeId){
    }

    @Override
    protected void setData(LineChart chart, TimeSeries series) {

        final int entryCount = series.getCount();

        List<Entry> topValues = new ArrayList<>(entryCount), bottomValues = new ArrayList<>(entryCount);
        final Entry[] placeholder = new Entry[2];
        for(int i=0;i<entryCount; i++){
            createEntryPoint(i, series.getValue(i), placeholder);
            topValues.add(placeholder[0]);
            bottomValues.add(placeholder[1]);
        }

        if (chart.getData() != null && chart.getData().getDataSetCount() > 0) {
            ((LineDataSet)chart.getData().getDataSetByIndex(0)).setValues(topValues);
            ((LineDataSet)chart.getData().getDataSetByIndex(1)).setValues(bottomValues);

            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
        }else{

            YAxis leftAxis = chart.getAxisLeft();
            leftAxis.setAxisMaximum(50);
            leftAxis.setAxisMinimum(-50);

            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(createSet(topValues, true));
            dataSets.add(createSet(bottomValues, false));

            LineData data = new LineData(dataSets);
            data.setDrawValues(false);
            chart.setData(data);



        }

        chart.setVisibleXRangeMaximum(getViewportSize());
        chart.moveViewToX(chart.getData().getEntryCount());


    }

    private void createEntryPoint(float timestamp, int value, Entry[] dest){
        int bottomValue = value/2, topValue = value-bottomValue;
        dest[0] = new Entry(timestamp, topValue);
        dest[1] = new Entry(timestamp, -bottomValue);
    }

    @Override
    protected void addDataPoint(LineChart chart, TimeSeries series, int position, int value) {

        LineData data = chart.getData();

        if (data != null) {

            ILineDataSet topSet = data.getDataSetByIndex(0);
            ILineDataSet bottomSet = data.getDataSetByIndex(1);

            Entry[] placeholder = new Entry[2];
            createEntryPoint(topSet.getEntryCount(), value, placeholder);

            topSet.addEntry(placeholder[0]);
            bottomSet.addEntry(placeholder[1]);

            data.notifyDataChanged();

            chart.notifyDataSetChanged();

            chart.setVisibleXRangeMaximum(getViewportSize());

            chart.moveViewToX(data.getEntryCount());



        }
    }


    private LineDataSet createSet(List<Entry> entries, boolean isTop){
        LineDataSet set = new LineDataSet(entries, isTop ? "Top" : "Bottom");

        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(getPrimaryColorLight());
        //set.setHighLightColor(getPrimaryColorLight());
        set.setDrawCircles(false);
        set.setLineWidth(0.11f);
        set.setCircleRadius(3f);
        set.setFillAlpha(255);
        set.setDrawFilled(true);

        set.setFillColor(Utils.getThemeColor(getChart().getContext(), android.R.attr.colorPrimaryDark));
        set.setDrawCircleHole(false);
        set.setFillFormatter((dataSet, dataProvider) -> {
            LineChart chart = getChart();
            if(chart != null){
                if(isTop){
                    return chart.getAxisLeft().getAxisMaximum();
                }
                return chart.getAxisLeft().getAxisMinimum();
            }
            return 0;

        });

        return set;
    }
}
