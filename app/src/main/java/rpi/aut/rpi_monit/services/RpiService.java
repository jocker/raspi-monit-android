package rpi.aut.rpi_monit.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.View;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.socket.client.IO;
import io.socket.client.Socket;
import rpi.aut.rpi_monit.AppConfig;
import rpi.aut.rpi_monit.http.ApiResponse;
import rpi.aut.rpi_monit.http.HttpClient;
import rpi.aut.rpi_monit.lib.Action1;
import rpi.aut.rpi_monit.lib.RpiSensor;
import rpi.aut.rpi_monit.lib.Utils;
import rpi.aut.rpi_monit.timeseries.TimeSeriesData;
import rpi.aut.rpi_monit.timeseries.TimeSeriesPoint;
import timber.log.Timber;


public class RpiService extends Service {

    private static final String SESSION_ID = UUID.randomUUID().toString();

    private static final String SUCCESS = "success";

    public static Observable<RpiService.ServiceBinder> getRpiService(View view){
        return Utils.getHostActivity(view).getServiceConnection(RpiService.class).cast(RpiService.ServiceBinder.class);
    }

    private ServiceBinder mServiceBinder;

    private final ReentrantLock mLock = new ReentrantLock();
    private final Condition mCondition = mLock.newCondition();
    private boolean mIsConnecting = false;
    private volatile Socket mConnection;
    private boolean mIsStarted;

    private final PublishSubject<TimeSeriesPoint> mSensorPointPipe = PublishSubject.create();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if(!mIsStarted){
            mIsStarted = true;
            startService(intent);
        }
        if(mServiceBinder == null){
            mServiceBinder = new ServiceBinder(this);
        }
        return mServiceBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private Observable<Socket> onConnected(){
        try{
            mLock.lock();
            if(mConnection != null){
                return Observable.just(mConnection);
            }
        }finally {
            mLock.unlock();
        }
        return Observable.<Socket>fromPublisher(s -> {
            try{
                mLock.lock();

                final Action1<Socket> resolve = socket -> {
                    try{
                        mLock.lock();
                        if(socket != null){
                            s.onNext(socket);
                        }
                        mConnection = socket;
                        mIsConnecting = false;
                        s.onComplete();
                        mCondition.signalAll();
                    }finally {
                        mLock.unlock();
                    }
                };

                if(mConnection != null){
                    resolve.call(mConnection);
                    return;
                }
                while (mIsConnecting){
                    try{
                        mCondition.await();
                    }catch (InterruptedException e){

                    }
                }
                if(mConnection != null){
                    resolve.call(mConnection);
                    return;
                }
                mIsConnecting = true;

                try{
                    final Socket socket = IO.socket(AppConfig.getRemoteUri().toString());
                    socket.once(Socket.EVENT_CONNECT, args -> {
                        Timber.e("RpiService EVENT_CONNECT =>"+ Arrays.toString(args));
                        resolve.call(socket);
                    }).once(Socket.EVENT_CONNECT_ERROR, args -> {
                        Timber.e("RpiService EVENT_CONNECT_ERROR =>"+ Arrays.toString(args));
                        resolve.call(null);
                        socket.close();
                    }).once(Socket.EVENT_CONNECT_TIMEOUT, args -> {
                        Timber.e("RpiService EVENT_CONNECT_TIMEOUT =>"+ Arrays.toString(args));
                        resolve.call(null);
                        socket.close();
                    }).once(Socket.EVENT_DISCONNECT, args -> {
                        Timber.e("RpiService","EVENT_DISCONNECT =>"+ Arrays.toString(args));
                        resolve.call(null);
                    });
                    socket.connect();

                }catch (URISyntaxException e){
                    throw new RuntimeException(e);
                }

            }finally {
                mLock.unlock();
            }
        });
    }

    public static class ServiceBinder extends Binder{

        private final RpiService mService;

        ServiceBinder(RpiService service){
            super();
            mService = service;
        }

        public Observable<Boolean> setCameraX(int value){
            return sendCameraCmd("camera:x", value);
        }

        public Observable<Boolean> setCameraY(int value){
            return sendCameraCmd("camera:y", value);
        }

        public Observable<Boolean> setCameraIrBrightness(int value){
            return sendCameraCmd("camera:ir", value);
        }

        public Observable<CameraSettings> getCameraSettings(){
            return sendCmd("camera:settings", null).map(jsonObject -> {
                if(jsonObject.has(SUCCESS) && jsonObject.get(SUCCESS).getAsBoolean()){
                    return CameraSettings.fromJson(jsonObject);
                }
                return null;
            });
        }

        private Observable<Boolean> sendCameraCmd(String cmd, int value){
            JsonObject obj = new JsonObject();
            obj.addProperty("cmd", cmd);
            obj.addProperty("value", value);
            return sendCmd(cmd, obj).map(jsonObject -> {
                if(jsonObject.isJsonObject() && jsonObject.has(SUCCESS)){
                    return jsonObject.get(SUCCESS).getAsBoolean();
                }
                return false;
            });
        }

        private Observable<JsonObject> sendCmd(String cmd, JsonObject obj){
            if(obj == null){
                obj = new JsonObject();
            }
            obj.addProperty("cmd", cmd );
            obj.addProperty("session_id", SESSION_ID);
            obj.addProperty("created_at", System.currentTimeMillis());

            final Object[] args = new Object[]{ obj };

            return mService.onConnected().flatMapMaybe(socket -> {
                return Maybe.create(e -> {
                    socket.emit("cmd", args, receivedArgs -> {
                        if(!e.isDisposed()){
                            if(receivedArgs != null && receivedArgs.length == 1 && receivedArgs[0] instanceof String){
                                JsonObject parsedArgs = Utils.fromJson(String.valueOf(receivedArgs[0]), JsonObject.class);
                                e.onSuccess(parsedArgs);
                            }
                            e.onComplete();
                        }
                    });

                });
            });
        }

        /*private static void x(){

            Observable.timer(1, TimeUnit.MILLISECONDS).map(aLong -> {
                try{
                    Uri uri = AppConfig.getRemoteUri();
                    uri.getHost();
                    InetAddress in = InetAddress.getByName(uri.getHost());
                    final long now = System.currentTimeMillis();
                    try {
                        if (in.isReachable(100)) {
                            final long ellapsed = System.currentTimeMillis()-now;
                            Log.e("aaaa","aaaaaa");
                            Log.e("aaaa","aaaaaa");
                            Log.e("aaaa","aaaaaa");
                            Log.e("aaaa","aaaaaa");
                        } else {
                            Log.e("aaaa","aaaaaa");
                            Log.e("aaaa","aaaaaa");
                            Log.e("aaaa","aaaaaa");
                            Log.e("aaaa","aaaaaa");
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        Log.e("aaaa","aaaaaa");
                    }
                    Log.e("aaaa","aaaa");
                    Log.e("aaaa","aaaa");
                    Log.e("aaaa","aaaa");
                    Log.e("aaaa","aaaa");
                }catch (UnknownHostException e){

                }

                return 1;

            }).subscribe();



        }*/


        public Observable<TimeSeriesPoint> onSensorPoint(RpiSensor sensor){
            return mService.onConnected().flatMap(socket -> {
                return mService.mSensorPointPipe.filter(timeSeriesPoint -> {
                    return sensor == null || timeSeriesPoint.rpiSensor == sensor;
                });
            });
        }

        public Observable<TimeSeriesPoint> onSensorPoint(){
            return this.onSensorPoint(null);
        }


        public Observable<ApiResponse<TimeSeriesData>> loadSensorData(RpiSensor sensor, Long since){
            HttpClient.RequestBuilder requestBuilder = HttpClient.get("series", sensor.rawType);
            if(since != null){
                requestBuilder.appendQueryParameter("since", String.valueOf(since));
            }

            Observable<ApiResponse<JsonElement>> source = requestBuilder.buildAsObservable(JsonElement.class);
            return source.map(objectApiResponse -> {
                if(!objectApiResponse.isSuccessful()){
                    return ApiResponse.failure(objectApiResponse);
                }
                return ApiResponse.success(objectApiResponse.request, objectApiResponse.response, TimeSeriesData.parse(objectApiResponse.body));
            });

        }

        public SensorDataProvider getSensorDataProvider(RpiSensor sensor){
            return new SensorDataProvider() {
                @Override
                public Observable<ApiResponse<TimeSeriesData>> load() {
                    return loadSensorData(sensor, null);
                }

                @Override
                public Observable<TimeSeriesPoint> onNewEntry() {
                    return onSensorPoint(sensor);

                }
            };
        }

    }



    public interface SensorDataProvider{
        Observable<ApiResponse<TimeSeriesData>> load();
        Observable<TimeSeriesPoint> onNewEntry();
    }


    public static class CameraSettings{

        public static CameraSettings fromJson(JsonObject json){
            int x = 0, y = 0, ir = 0;
            if(json != null && json.has("values")){
                JsonObject source = json.get("values").getAsJsonObject();
                if(source.has("x")){
                    x = source.get("x").getAsInt();
                }
                if(source.has("y")){
                    y = source.get("y").getAsInt();
                }
                if(source.has("ir")){
                    ir = source.get("ir").getAsInt();
                }
            }

            return new CameraSettings(x, y, ir);

        }

        public final int x, y, ir;

        private CameraSettings(int x, int y, int ir){
            this.x = x;
            this.y = y;
            this.ir = ir;
        }
    }
}
