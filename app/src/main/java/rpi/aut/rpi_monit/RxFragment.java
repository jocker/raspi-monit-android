package rpi.aut.rpi_monit;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observable;
import rpi.aut.rpi_monit.lib.BitMask;
import rpi.aut.rpi_monit.lib.Func0;

public class RxFragment extends Fragment {

    private final String PARAM_INSTANCE_ID = "fragment_instance_id";

    private static final AtomicInteger sInstanceCounter = new AtomicInteger();
    private Integer mInstanceId;

    // http://developer.android.com/images/fragment_lifecycle.png
    public interface RxLifecycle {
        int BEFORE                  = 1<<0;
        int AFTER                   = 1<<1;

        int ATTACH                  = 1<<2;
        int CREATE                  = 1<<3;
        int RESTORE_INSTANCE_STATE  = 1<<4;
        int CREATE_VIEW             = 1<<5;
        int ACTIVITY_CREATED        = 1<<6;
        int START                   = 1<<7;


        int RESUME                  = 1<<8;
        int PAUSE                   = 1<<9;
        int SAVE_INSTANCE_STATE     = 1<<10;
        int STOP                    = 1<<11;
        int DESTROY_VIEW            = 1<<12;
        int DESTROY                 = 1<<13;
        int DETACH                  = 1<<14;
    }



    private final rpi.aut.rpi_monit.lib.RxLifecycle mLifecycle = new rpi.aut.rpi_monit.lib.RxLifecycle(RxLifecycle.BEFORE, RxLifecycle.AFTER, RxLifecycle.DETACH);

    @Override
    public void onAttach(Activity activity){
        mLifecycle.revertTo(0);
        mLifecycle.signalLifecycleEvent(RxLifecycle.ATTACH, () ->  super.onAttach(activity) );
    }

    @Override
    public void onCreate(final Bundle savedInstance){

        mLifecycle.signalLifecycleEvent(RxLifecycle.CREATE, () -> {
            super.onCreate(savedInstance);
            if (savedInstance != null) {
                int instanceId = savedInstance.getInt(PARAM_INSTANCE_ID, -1);
                if (instanceId >= 0) {
                    mInstanceId = instanceId;
                }
                mLifecycle.signalLifecycleEvent(RxLifecycle.RESTORE_INSTANCE_STATE, () -> {
                });
            }
        });
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final @Nullable ViewGroup container, final @Nullable Bundle savedInstanceState){
        mLifecycle.revertTo(RxLifecycle.CREATE_VIEW);
        setHasView(() ->
                super.onCreateView(inflater, container, savedInstanceState)
        );
        return null;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstance){
        super.onViewCreated(view, savedInstance);
        setHasView(() ->  {super.onViewCreated(view, savedInstance); return null;});

    }

    private <T> void setHasView(Func0<T> action){
        if (mLifecycle.getHistory().highest() != RxLifecycle.CREATE_VIEW){
            // this means that the fragment didn't call super from onCreateView
            mLifecycle.revertTo(RxLifecycle.CREATE_VIEW);
            mLifecycle.signalLifecycleEvent(RxLifecycle.CREATE_VIEW, action );
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstance){
        mLifecycle.signalLifecycleEvent(RxLifecycle.ACTIVITY_CREATED, () -> super.onActivityCreated(savedInstance));
    }

    @Override
    public void onStart(){
        mLifecycle.revertTo(RxLifecycle.START);
        mLifecycle.signalLifecycleEvent(RxLifecycle.START, super::onStart );
    }


    @Override
    public void onResume(){
        mLifecycle.signalLifecycleEvent(RxLifecycle.RESUME, super::onResume );
    }

    @Override
    public void onPause(){
        mLifecycle.signalLifecycleEvent(RxLifecycle.PAUSE, super::onPause );
    }

    @Override
    public void onStop(){
        mLifecycle.signalLifecycleEvent(RxLifecycle.STOP, super::onStop);
    }

    @Override
    public void onDestroyView(){
        mLifecycle.signalLifecycleEvent(RxLifecycle.DESTROY_VIEW, super::onDestroyView );
    }

    @Override
    public void onSaveInstanceState(final Bundle outState){
        if(mInstanceId != null){
            outState.putInt(PARAM_INSTANCE_ID, getInstanceId());
        }
        mLifecycle.signalLifecycleEvent(RxLifecycle.SAVE_INSTANCE_STATE, () -> super.onSaveInstanceState(outState));
    }

    @Override
    public void onDestroy() {
        mLifecycle.signalLifecycleEvent(RxLifecycle.DESTROY, super::onDestroy );
    }

    @Override
    public void onDetach(){
        mLifecycle.signalLifecycleEvent(RxLifecycle.DETACH, super::onDetach);
    }


    public final Observable<Integer> onLifecycleTearReached(int flag){
        flag = Integer.highestOneBit(flag);
        if(getLifecycleState().contains(flag) && !getLifecycleState().contains(RxLifecycle.DESTROY)){
            return Observable.just(flag);
        }
        return onLifecycleEvent(flag);
    }

    public final Observable<Integer> onLifecycleEvent(int eventFlags){
        return mLifecycle.getLifecyclePipe(eventFlags).take(1);
    }

    // Use this method for filtering any observable that should be tied to the fragment lifecycle
    // Do NOT use anything else because it will be a pain to update the code in order to support screen orientation changes
    // this observable will be kept inside a retained fragment of the parent activity
    public final Observable<Integer> onLifecycleDestroy(){
        if(getLifecycleState().contains(RxLifecycle.DESTROY)){
            return Observable.just(RxLifecycle.DESTROY);
        }
        return onLifecycleEvent(RxLifecycle.DESTROY);
    }

    public final BitMask.Sequence getLifecycleState(){
        return mLifecycle.getHistory();
    }

    private int getInstanceId(){
        if(mInstanceId == null){
            mInstanceId = sInstanceCounter.incrementAndGet();
        }
        return mInstanceId;
    }

    public boolean isPermanentlyDestroyed(){
        return getLifecycleState().contains(RxFragment.RxLifecycle.DESTROY) && !getLifecycleState().contains(RxFragment.RxLifecycle.SAVE_INSTANCE_STATE);
    }

    public final Observable<Integer> onLifecycleStateChanged(int watchedFlags){
        return mLifecycle.getLifecyclePipe(watchedFlags).map(flags ->
                BitMask.removeFlags(flags, RxLifecycle.AFTER, RxLifecycle.BEFORE)
        ).takeUntil(onLifecycleDestroy()).distinctUntilChanged();
    }



}