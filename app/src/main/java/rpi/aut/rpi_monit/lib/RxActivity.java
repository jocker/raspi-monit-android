package rpi.aut.rpi_monit.lib;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import io.reactivex.Observable;

public class RxActivity extends AppCompatActivity {

    // http://developer.android.com/images/activity_lifecycle.png
    public interface RxLifecycle {
        int BEFORE                  = 1<<0;
        int AFTER                   = 1<<1;

        int CREATE                  = 1<<2;
        int START                   = 1<<3;
        int RESTORE_INSTANCE_STATE  = 1<<4;
        int RESUME                  = 1<<5;
        int PAUSE                   = 1<<6;
        int SAVE_INSTANCE_STATE     = 1<<7;
        int DESTROY                 = 1<<8;
    }

    private final rpi.aut.rpi_monit.lib.RxLifecycle mLifecycle = new rpi.aut.rpi_monit.lib.RxLifecycle(RxLifecycle.BEFORE, RxLifecycle.AFTER, RxLifecycle.DESTROY);

    @Override
    protected void onCreate(Bundle savedInstance){
        mLifecycle.revertTo(0);
        mLifecycle.signalLifecycleEvent(RxLifecycle.CREATE, () -> {
            super.onCreate(savedInstance);
        });
    }

    @Override
    protected void onStart(){
        mLifecycle.signalLifecycleEvent(RxLifecycle.START, () -> super.onStart());
    }

    @Override
    protected void onResume(){
        mLifecycle.revertTo(RxLifecycle.RESUME);
        mLifecycle.signalLifecycleEvent(RxLifecycle.RESUME, () -> super.onResume());
    }

    @Override
    protected void onPause(){
        mLifecycle.signalLifecycleEvent(RxLifecycle.PAUSE, () -> super.onPause());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mLifecycle.signalLifecycleEvent(RxLifecycle.SAVE_INSTANCE_STATE, () -> super.onSaveInstanceState(outState));
    }

    @Override
    protected void onDestroy(){
        mLifecycle.signalLifecycleEvent(RxLifecycle.DESTROY, () -> super.onDestroy() );
    }

    @Override
    public void onRestoreInstanceState(final Bundle savedInstanceState){
        mLifecycle.signalLifecycleEvent(RxLifecycle.RESTORE_INSTANCE_STATE, () -> super.onRestoreInstanceState(savedInstanceState));
    }


    public final Observable<Integer> onLifecycleEvent(int eventFlags){
        return mLifecycle.getLifecyclePipe(eventFlags);
    }

    public final Observable<Integer> onLifecycleDestroy(){
        if(isDead()){
            return Observable.just(RxLifecycle.DESTROY);
        }
        return onLifecycleEvent(RxLifecycle.DESTROY);
    }

    public final BitMask.Sequence getLifecycleState(){
        return mLifecycle.getHistory();
    }

    public boolean isAlive(){
        return getLifecycleState().eq(RxLifecycle.RESUME);
    }

    public boolean isDead(){
        return getLifecycleState().contains(RxLifecycle.DESTROY);
    }

    public boolean isSaved(){
        return getLifecycleState().contains(RxLifecycle.SAVE_INSTANCE_STATE);
    }

    public Observable<Integer> whenLifecycleIs(final int state){
        if(getLifecycleState().eq(state)){
            return Observable.just(state);
        }
        return onLifecycleEvent(state)
                .map(newState -> newState &~ (RxLifecycle.BEFORE | RxLifecycle.AFTER))
                .take(1)
                .filter(newState -> newState == state);
    }

    public final Observable<Integer> onLifecycleTearReached(int flag){
        flag = Integer.highestOneBit(flag);
        if(getLifecycleState().contains(flag) && !getLifecycleState().contains(RxActivity.RxLifecycle.DESTROY)){
            return Observable.just(flag);
        }
        return onLifecycleEvent(flag);
    }



}
