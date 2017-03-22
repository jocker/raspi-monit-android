package rpi.aut.rpi_monit.lib;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

public class RxLifecycle {
    private final int mBeforeFlag, mAfterFlag, mCompletedFlag;
    private final BitMask.Sequence mHistory = new BitMask.Sequence(0);
    private BehaviorSubject<Integer> mLifecyclePipe = BehaviorSubject.create();
    private boolean mIsCompleted;

    public RxLifecycle(int beforeFlag, int afterFlag, int completedFlag){
        mBeforeFlag = beforeFlag;
        mAfterFlag = afterFlag;
        mCompletedFlag = completedFlag;
    }

    public void signalLifecycleEvent(int flag, Runnable action){
        mHistory.add(flag);

        mLifecyclePipe.onNext(mBeforeFlag | flag);
        action.run();
        mLifecyclePipe.onNext(mAfterFlag | flag);
        if(flag == mCompletedFlag){
            mLifecyclePipe.onComplete();
            mIsCompleted = true;
        }
    }

    public <T> T signalLifecycleEvent(int flag, Func0<T> action){
        mHistory.add(flag);

        mLifecyclePipe.onNext(mBeforeFlag | flag);
        T result = action.call();
        mLifecyclePipe.onNext(mAfterFlag | flag);
        if(flag == mCompletedFlag){
            mLifecyclePipe.onComplete();
        }
        return result;
    }

    public Observable<Integer> getLifecyclePipe(int eventFlags){
        if((eventFlags & (mBeforeFlag | mAfterFlag)) == 0){
            eventFlags |= mAfterFlag;
        }
        final int watchedFlags = eventFlags;
        return mLifecyclePipe.filter(newStateFlag -> {
            return (watchedFlags & newStateFlag) == newStateFlag;
        });


    }

    public BitMask.Sequence getHistory(){
        return mHistory;
    }

    public void revertTo(int flag){
        if(flag == 0 && mIsCompleted){
            mIsCompleted = false;
            mLifecyclePipe = BehaviorSubject.create();
        }
        mHistory.revertTo(flag);
    }


}