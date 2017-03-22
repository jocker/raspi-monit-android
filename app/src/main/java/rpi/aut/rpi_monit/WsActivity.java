package rpi.aut.rpi_monit;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.PowerManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;

import java.util.Arrays;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import rpi.aut.rpi_monit.charts.WaveFormChart;
import rpi.aut.rpi_monit.components.ScalableProgressBar;
import rpi.aut.rpi_monit.components.rv.CircularSensorLayoutManager;
import rpi.aut.rpi_monit.lib.Action1;
import rpi.aut.rpi_monit.lib.RpiSensor;
import rpi.aut.rpi_monit.lib.RxView;
import rpi.aut.rpi_monit.lib.Utils;
import rpi.aut.rpi_monit.services.RpiService;


public class WsActivity extends BaseActivity {

    int mChartColor, mBackgroundColor;

    private PowerManager.WakeLock mPowerLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(isLandscape()){
            hideSystemUI();
        }
        super.onCreate(savedInstanceState);

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true);
        mChartColor = typedValue.data;

        mBackgroundColor = Color.RED;



        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        setContentView(R.layout.activity_ws);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        if(toolbar != null){
            toolbar.inflateMenu(R.menu.actions);
        }



        WaveFormChart chart = new WaveFormChart(RpiSensor.Sound, (LineChart)findViewById(R.id.chart));


        RecyclerView rv = (RecyclerView)findViewById(R.id.recycler);
        rv.setAdapter(new RpiSensorAdapter());
        rv.setLayoutManager(new CircularSensorLayoutManager());


        //((SimpleItemAnimator) rv.getItemAnimator()).setSupportsChangeAnimations(false);

    }


    @Override
    protected void onResume() {
        if(mPowerLock == null){
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            mPowerLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,  "MyWakelockTag");
        }

        mPowerLock.acquire();

        super.onResume();
    }

    @Override
    protected void onPause() {
        if(mPowerLock != null){
            mPowerLock.release();
        }
        super.onPause();
    }

    private void invokeWs(Action1<RpiService.ServiceBinder> action){
        getServiceConnection(RpiService.class).observeOn(AndroidSchedulers.mainThread()).subscribe(iBinder -> {
            action.call((RpiService.ServiceBinder)iBinder);
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(isLandscape()){
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    // This snippet shows the system bars. It does this by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private boolean isLandscape(){
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }




    public static class ViewHolder extends RecyclerView.ViewHolder{
        private final RpiSensorAdapter mAdapter;
        public ViewHolder(View itemView, RpiSensorAdapter adapter) {
            super(itemView);
            mAdapter = adapter;
            itemView.setOnClickListener(v -> {
                mAdapter.onItemClick(getAdapterPosition());
            });
        }
    }


    public static class RpiSensorAdapter extends RecyclerView.Adapter<ViewHolder>{

        private final List<RpiSensor> mItems;
        private final SparseArray<Integer> mSensorValues = new SparseArray<>();

        public RpiSensorAdapter(){
            mItems = Arrays.asList(RpiSensor.ALL);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View itemView = inflater.inflate(R.layout.rv_progress, parent, false);
            return new ViewHolder(itemView, this);
        }

        public RpiSensor getItem(int position){
            return mItems.get(position);
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);

            RpiService.getRpiService(recyclerView).flatMap(serviceBinder -> {
                return serviceBinder.onSensorPoint();
            }).takeUntil(RxView.onDetach(recyclerView)).observeOn(AndroidSchedulers.mainThread()).subscribe(timeSeriesPoint -> {
                setSensorValue(timeSeriesPoint.rpiSensor, timeSeriesPoint.value);
            });

        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ScalableProgressBar progressView = (ScalableProgressBar)holder.itemView.findViewById(R.id.progress);
            RpiSensor sensor = getItem(position);
            progressView.setUnfinishedStrokeColor(sensor.getLightColor(progressView.getContext()));
            progressView.setFinishedStrokeColor(sensor.getColor(progressView.getContext()));
            progressView.setTextColor(Utils.getColor(progressView.getContext(), R.color.gray));
            progressView.setInnerBackgroundColor(Utils.getColor(progressView.getContext(), R.color.backgroundDark));



            Integer sensorValue = getSensorValue(sensor);
            progressView.setMax(100);



            TextView label = (TextView)holder.itemView.findViewById(R.id.txtLabel);

            /*label.setTextSize(progressView.getTextSize()*0.8f);
            label.setText(sensor.getDisplayName(progressView.getContext()));*/


        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        public void onItemClick(int position){
            setActiveItem(mItems.get(position));
        }

        public boolean setActiveItem(RpiSensor sensor){
            int index = mItems.size()-1;
            if(mItems.get(index) != sensor){
                mItems.set(index, sensor);
                notifySensorChanged(sensor);
                return true;
            }
            return false;
        }

        public RpiSensor getActiveItem(){
            return mItems.get(mItems.size()-1);
        }

        public void setSensorValue(RpiSensor sensor, Integer value){
            Integer prevValue = mSensorValues.get(sensor.id);
            if(prevValue == null || prevValue != value){
                mSensorValues.put(sensor.id, value);
                notifySensorChanged(sensor);
            }
        }

        public Integer getSensorValue(RpiSensor sensor){
            return mSensorValues.get(sensor.id);
        }

        public void onSaveInstanceState(Parcel out){
            out.writeInt(mItems.size());
            for(RpiSensor sensor: mItems){
                out.writeInt(sensor.id);
            }
            out.writeInt(mSensorValues.size());
            for(int i=0; i< mSensorValues.size(); i++){
                out.writeInt(mSensorValues.keyAt(i));
                out.writeInt(mSensorValues.valueAt(i));
            }
        }

        public void onRestoreInstanceState(Parcel in){
            int size = in.readInt();
            mItems.clear();
            for(int i=0;i<size; i++){
                mItems.add(i, RpiSensor.byId(i));
            }
            mSensorValues.clear();
            size = in.readInt();
            for(int i=0;i<size;i++){
                int key = in.readInt();
                int value = in.readInt();
                mSensorValues.put(key, value);
            }
            notifyDataSetChanged();
        }

        private void notifySensorChanged(RpiSensor sensor){
            for(int i=0; i< mItems.size(); i++){
                if(mItems.get(i) == sensor){
                    notifyItemChanged(i);
                }
            }
        }


    }





}
