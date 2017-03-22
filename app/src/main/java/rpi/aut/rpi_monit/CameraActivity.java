package rpi.aut.rpi_monit;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import rpi.aut.rpi_monit.components.CircularProgressView;
import rpi.aut.rpi_monit.components.TickBarView;
import rpi.aut.rpi_monit.components.rv.SensorAdapter;
import rpi.aut.rpi_monit.lib.DrawableUtils;
import rpi.aut.rpi_monit.lib.RpiSensor;
import rpi.aut.rpi_monit.lib.RxView;
import rpi.aut.rpi_monit.lib.Utils;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class CameraActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);


        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.

        View bottomSheet = findViewById(R.id.bottom_sheet);
        BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // React to state change
                Log.e("aaaaa","aaaaa");
                Log.e("aaaaa","aaaaa");
                Log.e("aaaaa","aaaaa");
                Log.e("aaaaa","aaaaa");
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // React to dragging events
                Log.e("aaaaa","aaaaa "+slideOffset);
            }
        });


        RecyclerView sensorNav = (RecyclerView)findViewById(R.id.sensorButtons);
        sensorNav.setAdapter(new SensorButtonAdapter());
        sensorNav.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));




        RxView.onMeasured(getWindow().getDecorView()).subscribe(view -> {
            TickBarView cameraY = (TickBarView)findViewById(R.id.barCameraY);
            cameraY.setProgress(0.2f, false);
            ViewGroup.LayoutParams lp = cameraY.getLayoutParams();
            lp.height = getWindow().getDecorView().getHeight()/2;
            cameraY.setLayoutParams(lp);

            TickBarView cameraX = (TickBarView)findViewById(R.id.barCameraX);
            cameraX.setProgress(0.2f, false);
            lp = cameraX.getLayoutParams();
            lp.width = getWindow().getDecorView().getWidth()/3*2;
            cameraX.setLayoutParams(lp);

            TickBarView cameraLight = (TickBarView)findViewById(R.id.barCameraLight);
            cameraLight.setProgress(0.2f, false);
            lp = cameraLight.getLayoutParams();
            lp.height = getWindow().getDecorView().getHeight()/2;
            cameraLight.setLayoutParams(lp);
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private static class SensorButtonAdapter extends SensorAdapter{

        public SensorButtonAdapter() {
            super(new RpiSensor[]{
                    RpiSensor.Light,
                    RpiSensor.Sound,
                    null,
                    RpiSensor.Temperature,
                    RpiSensor.Humidity
            });
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(inflateView(parent, R.layout.rv_sensor_button), this);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            RpiSensor sensor = getItem(position);
            CircularProgressView view = (CircularProgressView)holder.itemView;
            Context context = view.getContext();


            if(sensor == null){
                view.setFillColor(Color.WHITE);
                view.setIcon(DrawableUtils.getTintedDrawable(context, R.drawable.ic_settings, Utils.getThemeColor(context, android.R.attr.colorPrimaryDark)));
            }else{
                view.setIcon(sensor.getIcon(context));
                view.setProgress(getSensorDisplayPercent(sensor)/100);
                view.setProgress(0.4f);
                view.setTrackWidth(Utils.dpToPx(2));
                view.setProgressWidth(Utils.dpToPx(3));
                view.setTrackColor(Utils.getThemeColor(context, android.R.attr.colorPrimary));
                view.setProgressColor(sensor.getColor(context));
                view.setAlpha(0.7f);
            }
        }

    }
}
