package rpi.aut.rpi_monit;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private final String PIPELINE = "tcpclientsrc host=192.168.1.142 port=5000 ! gdpdepay ! rtph264depay ! avdec_h264 ! videoconvert ! autovideosink sync=false";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//
//
//        StreamPlayer player = StreamPlayer.create((SurfaceView) findViewById(R.id.videoSurface), PIPELINE);
//        player.play();
    }


}
