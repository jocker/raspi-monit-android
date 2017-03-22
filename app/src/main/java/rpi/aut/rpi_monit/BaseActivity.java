package rpi.aut.rpi_monit;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.subjects.AsyncSubject;
import rpi.aut.rpi_monit.lib.RxActivity;

public class BaseActivity extends RxActivity {

    private Map<Class, BoundServiceConnection> mServices = new HashMap<>();

    public  <T extends IBinder> Observable<T> getServiceConnection(Class<? extends Service> typeOfService){
        BoundServiceConnection<T> conn = (BoundServiceConnection<T>)mServices.get(typeOfService);
        if(conn == null || conn.isDestroyed()){
            conn = new BoundServiceConnection<>(this, typeOfService);
            mServices.put(typeOfService, conn);
        }
        return conn.getConnection().takeUntil(onLifecycleEvent(RxLifecycle.PAUSE)).take(1);
    }

    public void removeServiceConnection(Class<? extends Service> typeOfService){
        BoundServiceConnection conn = mServices.get(typeOfService);
        if(conn != null){
            conn.disconnect();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        for(BoundServiceConnection conn: mServices.values()){
            conn.disconnect();
        }
        mServices.clear();
    }

    private static class BoundServiceConnection<T extends IBinder>{
        private WeakReference<BaseActivity> mActivity;
        private boolean mBound = false;
        private T mBinder;
        private boolean mDestroyed = false;
        private AsyncSubject<T> mConnectionPipe;

        private ServiceConnection mConnection = new ServiceConnection() {

            @Override
            @SuppressWarnings("unchecked")
            public void onServiceConnected(ComponentName className, IBinder service) {
                mBound = true;
                mBinder = (T)service;
                if(mConnectionPipe != null){
                    mConnectionPipe.onNext(mBinder);
                    mConnectionPipe.onComplete();
                    mConnectionPipe = null;
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mBound = false;
                mBinder = null;
                if(mConnectionPipe != null){
                    mConnectionPipe.onComplete();
                }
                mDestroyed = true;
            }
        };

        private BoundServiceConnection(BaseActivity activity, Class<? extends Service> typeOfService){
            mActivity = new WeakReference<>(activity);
            Intent intent = new Intent(activity, typeOfService);
            activity.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

        private Observable<T> getConnection(){
            if(mBinder != null){
                return Observable.just(mBinder);
            }else if(mConnectionPipe == null){
                mConnectionPipe = AsyncSubject.create();
            }
            return mConnectionPipe;
        }

        public void disconnect(){
            if (mBound) {
                BaseActivity activity = mActivity.get();
                if(activity != null){
                    activity.unbindService(mConnection);
                    mBound = false;
                }
            }
            mDestroyed = true;
        }

        public boolean isDestroyed(){
            return mDestroyed;
        }
    }
}
