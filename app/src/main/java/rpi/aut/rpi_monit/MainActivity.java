package rpi.aut.rpi_monit;

import android.os.Bundle;
import android.os.PowerManager;
import android.util.SparseIntArray;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import rpi.aut.rpi_monit.components.TickBarView;
import rpi.aut.rpi_monit.lib.RxView;


public class MainActivity extends BaseActivity {

    private final List<StreamPlayer> mPlayers = new ArrayList<>();
    private PowerManager.WakeLock mPowerLock;

    private final TickBarView.OnProgressChangedListener mProgressListener = new TickBarView.OnProgressChangedListener() {

        final SparseIntArray mPositions = new SparseIntArray();

        @Override
        public void onStartDrag(TickBarView view) {

        }

        @Override
        public void onEndDrag(TickBarView view) {

        }

        @Override
        public void onProgressChanged(TickBarView view, float progress, boolean fromUser) {
            if(fromUser){
                final int id = view.getId();
                final int absProgress = (int)Math.ceil(progress*1000);
                if(mPositions.get(id, -1) == absProgress){
                    return;
                }
                mPositions.put(id, absProgress);
                invokeWs(service -> {
                    switch (id) {
                        case R.id.barCameraY:
                            service.setCameraY(1000 - absProgress).subscribe();
                            break;
                        case R.id.barCameraX:
                            service.setCameraX(absProgress).subscribe();
                            break;
                        case R.id.barCameraIrLight:
                            service.setCameraIrBrightness(1000 -absProgress).subscribe();
                            break;
                    }
                });
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera);


        StreamPlayer player = StreamPlayer.createAudioPlayer(this);
        if(player != null){
            mPlayers.add(player);
            player.play();
        }


        player = StreamPlayer.createVideoPlayer((SurfaceView) findViewById(R.id.videoSurface));
        if(player != null){
            mPlayers.add(player);
            player.play();
        }

        getWsService().flatMap(serviceBinder -> {
            return serviceBinder.getCameraSettings();
        }).takeUntil(onLifecycleDestroy()).observeOn(AndroidSchedulers.mainThread()).subscribe(cameraSettings -> {
            ((TickBarView)findViewById(R.id.barCameraX)).setProgress((100-cameraSettings.x)/100f);
            ((TickBarView)findViewById(R.id.barCameraY)).setProgress(cameraSettings.y/100f);
            ((TickBarView)findViewById(R.id.barCameraIrLight)).setProgress(cameraSettings.ir/100f);
        });

        RxView.onMeasured(getWindow().getDecorView()).subscribe(view -> {
            TickBarView cameraY = (TickBarView)findViewById(R.id.barCameraY);
            cameraY.setProgressListener(mProgressListener);
            ViewGroup.LayoutParams lp = cameraY.getLayoutParams();
            lp.height = getWindow().getDecorView().getHeight()/2;
            cameraY.setLayoutParams(lp);

            TickBarView cameraX = (TickBarView)findViewById(R.id.barCameraX);
            cameraX.setProgressListener(mProgressListener);
            lp = cameraX.getLayoutParams();
            lp.width = getWindow().getDecorView().getWidth()/3*2;
            cameraX.setLayoutParams(lp);

            TickBarView cameraLight = (TickBarView)findViewById(R.id.barCameraIrLight);
            cameraLight.setProgressListener(mProgressListener);
            lp = cameraLight.getLayoutParams();
            lp.height = getWindow().getDecorView().getHeight()/2;
            cameraLight.setLayoutParams(lp);
        });



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

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        for(StreamPlayer player:mPlayers){
//            player.destroy();
//        }
//        mPlayers.clear();
    }
}
