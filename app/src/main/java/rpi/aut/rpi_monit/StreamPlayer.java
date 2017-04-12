package rpi.aut.rpi_monit;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.freedesktop.gstreamer.GStreamer;

import java.lang.ref.WeakReference;

import rpi.aut.rpi_monit.lib.Utils;

public class StreamPlayer implements SurfaceHolder.Callback  {

    private static final String VIDEO_PIPELINE = "tcpclientsrc host=192.168.1.124 port=5000 ! gdpdepay ! rtph264depay ! avdec_h264 ! videoconvert ! autovideosink sync=false";
    private static final String AUDIO_PIPELINE = "udpsrc port=5001 caps=\"application/x-rtp\" ! queue ! rtppcmudepay ! mulawdec ! audioconvert ! autoaudiosink sync=false";
    //private static final String AUDIO_PIPELINE = "audiotestsrc ! audioconvert ! audioresample ! autoaudiosink";


    private native void nativeInit(String pipelineString);     // Initialize native code, build pipeline, etc
    private native void nativeFinalize(); // Destroy pipeline and shutdown native code
    private native void nativePlay();     // Set pipeline to PLAYING
    private native void nativePause();    // Set pipeline to PAUSED
    private static native boolean nativeClassInit(); // Initialize native class: cache Method IDs for callbacks
    private native void nativeSurfaceInit(Object surface);
    private native void nativeSurfaceFinalize();

    private long native_custom_data;      // Native code will use this to keep private data

    private boolean mIsPlaying = true;   // Whether the user asked to go to PLAYING

    public static StreamPlayer createAudioPlayer(Context context){
        return create(context, AUDIO_PIPELINE);
    }

    public static StreamPlayer createVideoPlayer(SurfaceView surface){
        StreamPlayer player = create(surface.getContext(), VIDEO_PIPELINE);
        if(player != null){
            player.setSurface(surface);
        }
        return player;
    }

    private static StreamPlayer create(Context context, String rawPipeline){
        try {
            GStreamer.init(context);
            return new StreamPlayer(context, rawPipeline);
        } catch (Exception e) {
            Log.e("StreamPlayer", e.getMessage(), e);
           return null;
        }
    }

    private StreamPlayer(Context context, String rawPipeline){
        nativeInit(rawPipeline);
    }


    private WeakReference<SurfaceView> mSurfaceRef;
    public void setSurface(SurfaceView surface){
        SurfaceView prev = Utils.getReferencedValue(mSurfaceRef);
        if(prev != null && prev.getHolder() != null){
            prev.getHolder().removeCallback(this);
        }
        mSurfaceRef = new WeakReference<>(surface);
        if(surface != null && surface.getHolder() != null){
            surface.getHolder().addCallback(this);
        }

    }

    // Called from native code. This sets the content of the TextView from the UI thread.
    private void setMessage(final String message) {
        Log.e("StreamPlayer", "SET MESSAGE "+message);
    }

    // Called from native code. Native code calls this once it has created its pipeline and
    // the main loop is running, so it is ready to accept commands.
    private void onGStreamerInitialized () {
        Log.i ("GStreamer", "Gst initialized. Restoring state, playing:" + mIsPlaying);
        // Restore previous playing state
        if (mIsPlaying) {
            nativePlay();
        } else {
            nativePause();
        }

    }

    public void play(){
        if(!mIsPlaying){
            mIsPlaying = true;
            nativePlay();
        }
    }

    public void pause(){
        if(mIsPlaying){
            mIsPlaying = false;
            nativePause();
        }
    }


    static {
        System.loadLibrary("gstreamer_android");
        System.loadLibrary("gst_stream_player");
        nativeClassInit();
    }

    public void destroy(){
        nativeFinalize();
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e("GStreamer", "Surface created: " + holder.getSurface());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e("GStreamer", "Surface changed to format " + format + " width "
                + width + " height " + height);
        nativeSurfaceInit (holder.getSurface());
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e("GStreamer", "Surface destroyed");
        nativeSurfaceFinalize ();
    }

    private void onMediaSizeChanged (int width, int height) {
        Log.e ("GStreamer", "Media size changed to " + width + "x" + height);
    }

    private void onStreamEnded() {
        Log.e ("GStreamer", "Stream Ended");
    }

    private void onError(String cause){
        Log.e ("GStreamer", "ERROR "+cause);
    }
}
