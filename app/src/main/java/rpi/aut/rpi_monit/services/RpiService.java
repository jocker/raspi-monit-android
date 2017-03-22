package rpi.aut.rpi_monit.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.google.gson.JsonElement;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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


public class RpiService extends Service {

    public static Observable<RpiService.ServiceBinder> getRpiService(View view){
        return Utils.getHostActivity(view).getServiceConnection(RpiService.class).cast(RpiService.ServiceBinder.class);
    }

    //private static final String SOCKET_URI = "http://192.168.1.142:3000";

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
                    Socket socket = IO.socket(AppConfig.getRemoteUri().toString());
                    socket.once(Socket.EVENT_CONNECT, args -> {
                        Log.e("RpiService","EVENT_CONNECT =>"+ Arrays.toString(args));
                        resolve.call(socket);
                    }).once(Socket.EVENT_CONNECT_ERROR, args -> {
                        Log.e("RpiService","EVENT_CONNECT_ERROR =>"+ Arrays.toString(args));
                        resolve.call(null);
                    }).once(Socket.EVENT_CONNECT_TIMEOUT, args -> {
                        Log.e("RpiService","EVENT_CONNECT_TIMEOUT =>"+ Arrays.toString(args));
                        resolve.call(null);
                    }).once(Socket.EVENT_DISCONNECT, args -> {
                        Log.e("RpiService","EVENT_DISCONNECT =>"+ Arrays.toString(args));
                        resolve.call(null);
                    }).on("sensor_data", args -> {
                        if(args != null && args.length == 1 && args[0] instanceof JSONObject){
                            TimeSeriesPoint sensorEvent = TimeSeriesPoint.create((JSONObject)args[0]);
                            if(sensorEvent != null){
                                mSensorPointPipe.onNext(sensorEvent);
                            }
                        }

                    });
                    socket.connect();

                }catch (URISyntaxException e){
                    throw new RuntimeException(e);
                }

            }finally {
                mLock.unlock();
            }
        }).doOnSubscribe(disposable -> {
            Log.e("aaaaa","aaaaaa");
            Log.e("aaaaa","aaaaaa");
            Log.e("aaaaa","aaaaaa");
            Log.e("aaaaa","aaaaaa");
        });
    }

    public static class ServiceBinder extends Binder{

        private final RpiService mService;

        ServiceBinder(RpiService service){
            super();
            mService = service;
        }


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


        public Observable<Object> broadcast(String eventName, Object... args){
            return mService.onConnected().flatMapMaybe(socket -> {
                return Maybe.create(e -> {
                    socket.emit(eventName, args, receivedArgs -> {
                        if(!e.isDisposed()){
                            e.onSuccess(receivedArgs[0]);
                            e.onComplete();
                        }
                    });

                });
            });
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

        public Observable<Boolean> setCameraX(int value){
            return invokeCameraCmd("x", obj -> {
                obj.put("percent", value);
            });
        }

        public Observable<Boolean> setCameraY(int value){
            return invokeCameraCmd("y", obj -> {
                obj.put("percent", value);
            });
        }

        private Observable<Boolean> invokeCameraCmd(String type, Action1<Map<String, Object>> action){
            Map<String, Object> params = new HashMap<>();
            action.call(params);
            params.put("type", type);
            final long sentAt = System.currentTimeMillis();
            return broadcast("camera:cmd", Utils.toJson(params)).map(o -> {
                Log.e("camera:cmd", "RECEIVED "+String.valueOf(o)+" AFTER "+ (System.currentTimeMillis()-sentAt));
                return true;
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
}
