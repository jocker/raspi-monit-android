package rpi.aut.rpi_monit.lib;


import android.annotation.SuppressLint;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.subjects.PublishSubject;

public class RxView {

    public static final int
            ATTACHED = 1,
            DETACHED = 2,
            FOCUS_GAIN = 3,
            FOCUS_LOST = 4;

    // the main purpose of these methods is to allow attaching multiple listeners(like click listeners) for the same view
    // and also to correctly remove all the listeners/subscriptions once the view is detached from the window

    // all source observables are completed whenever a view is destroyed, at which point all subscribers are unsubscribed

    public static <T extends View> Observable<T> onClick(T ...views){
        if(views.length == 1){
            if(views[0] == null){
                return Observable.empty();
            }
            return getEmitter(views[0], ClickListener.class)
                    .compose(filterDetachedAndMapView(views[0]));
        }else{
            return Observable.fromArray(views).flatMap(t -> {
                return onClick(t);
            });

        }

    }

    private static final Set<Integer> sSuspendedViewEvents = new HashSet<>();


    public static boolean eventsSuspendedFor(View view){
        return false;
    }



    public static <T extends View> Observable<T> onAttach(T view){
        if(ViewCompat.isAttachedToWindow(view)){
            return Observable.just(view);
        }

        Observable<Integer> source = getEmitter(view, AttachStateListener.class).cast(Integer.class);
        return source.take(1).filter(attachState -> attachState == ATTACHED).map(state -> view);
    }

    public static <T extends View> Observable<Boolean> onAttachedStateChanged(T view){
        if(ViewCompat.isAttachedToWindow(view)){
            return Observable.just(true);
        }
        Observable<Integer> source = getEmitter(view, AttachStateListener.class).cast(Integer.class);
        return source.map(state -> state == ATTACHED);
    }

    public static <V extends View> Observable<V> onDetach(V view){
        final WeakReference<V> viewRef = new WeakReference<>(view);
        return getEmitter(view, AttachStateListener.class)
                .filter(flag -> (int)flag == DETACHED)
                .take(1).map(obj -> viewRef.get())
                .filter(v -> v != null);
    }

    public static <V extends View> Observable<V> onGlobalLayout(V view){
        return buildListener(view, GlobalLayoutListener.class);
    }

    public static <V extends View> Observable<V> onLayout(V view){
        return buildListener(view, LayoutChangedListener.class);
    }

    public static <V extends View> Observable<V> onPreDraw(V view) {
        return buildListener(view, PreDrawListener.class);
    }

    public static <V extends View> Observable<V> onFocusChanged(V view) {
        Observable<V> s = buildListener(view, FocusChangedListener.class);
        return s;
    }

    public static <T extends View> Observable<T> onMeasured(T view){
        if(view.getHeight() >0 && view.getWidth() > 0){
            return Observable.just(view);
        }
        return onGlobalLayout(view).take(1).map(t -> {
            return view;
        });
    }

    public static <T extends EditText> Observable<T> onTextChanged(T view){
        return watchTextChanged(view).filter(i -> i.type == TextChangedListener.ON_CHANGE).map(i -> view);
    }

    public static <T extends TextView> Observable<TextChangeInfo> watchTextChanged(T view){
        return getEmitter(view, TextChangedListener.class).takeUntil(onDetach(view));
    }

    public static <V extends CompoundButton> Observable<V> onCheckedChanged(V view){
        return buildListener(view, CheckedChangedLister.class);
    }




    public static <T extends AdapterView> Observable<Integer> onItemSelected(T view){
        return getEmitter(view, ItemSelectedListener.class).cast(Integer.class).takeUntil(onDetach(view));
    }

    public static Observable<Integer> onItemSelected(AutoCompleteTextView view){
        return RxView.getEmitter(view, ItemSelectedListener.class).cast(Integer.class).takeUntil(onDetach(view));
    }


    private static <T extends View> ObservableTransformer<Object, T> filterDetachedAndMapView(final T view){
        Observable<T> detachObs = onDetach(view);
        final WeakReference<T> viewRef = new WeakReference<>(view);
        return observable -> observable.takeUntil(detachObs)
                .map( obj -> viewRef.get())
                .filter(obj -> obj != null);
    }

    private static <V extends View, Q, T extends ViewListener<Q, V>> Observable<V> buildListener(V view, Class<T> listenerClass){
        return getEmitter(view, listenerClass).compose(filterDetachedAndMapView(view));
    }

    private static <Q, V extends View, T extends ViewListener<Q, V>> Observable<Q> getEmitter(V view, Class<T> listenerClass){
        return ViewListenerHolder.get(view).getObservable(listenerClass);
    }

    private static class ViewListenerHolder<V extends View>{

        private final static SparseArray<ViewListenerHolder> sListeners = new SparseArray<>();

        @SuppressWarnings("unchecked")
        public static <V extends View> ViewListenerHolder<V> get(V view){
            int key = System.identityHashCode(view);
            ViewListenerHolder holder = sListeners.get(key);
            if(holder == null){
                final ViewListenerHolder holderRef = holder = new ViewListenerHolder(view);
                sListeners.put(key, holder);
                onDetach(view).subscribe(v -> {
                    holderRef.detach();
                    sListeners.remove(key);
                });

            }
            return (ViewListenerHolder<V>)holder;
        }


        private final HashMap mListeners = new HashMap<>();
        private WeakReference<V> mViewRef;

        public ViewListenerHolder(V view){
            mViewRef = new WeakReference<>(view);

        }

        @SuppressWarnings("unchecked")
        private <Q, T extends ViewListener<Q, ?>> T get(Class<?> observerClass, boolean createIfMissing){
            T instance = (T)mListeners.get(observerClass);
            if(createIfMissing && (instance == null)){
                try{
                    instance = (T)observerClass.newInstance();
                }catch(IllegalAccessException e){
                    throw new RuntimeException(e);
                }catch(InstantiationException e1){
                    throw new RuntimeException(e1);
                }
                mListeners.put(observerClass, instance);
            }else if(createIfMissing){
                return null;
            }
            return instance;
        }

        public <Q, T extends ViewListener<Q, V>> Observable<Q> getObservable(Class<?> listenerClass){
            T instance = get(listenerClass, false);
            V view = Utils.getReferencedValue(mViewRef);
            if(view == null){
                return Observable.empty();
            }
            if(instance == null){
                instance = get(listenerClass, true);
                instance.setAttachedView(view);
            }
            return instance.asObservable();
        }

        private void detach(){

            for(Object klass :mListeners.keySet()){
                ViewListener instance = (ViewListener)mListeners.get(klass);
                instance.getSignalPipe().onComplete();
                View view = mViewRef.get();
                if(view != null){
                    instance.detachFrom(view);
                }

            }
            mViewRef = new WeakReference<>(null);
            mListeners.clear();
        }

        protected V getView(){
            return Utils.getReferencedValue(mViewRef);
        }

    }


    private static abstract class ViewListener<SignalType, V extends View> {
        private PublishSubject<SignalType> mSignalPipe;
        private WeakReference<V> mAttachedViewRef;

        public final void setAttachedView(V view){
            V prevView = getAttachedView();
            if(prevView != null && prevView != view){
                detachFrom(prevView);
                getSignalPipe().onComplete();
            }
            if(view != null){
                mAttachedViewRef = new WeakReference<>(view);
                attachTo(view);
            }

        }

        public abstract void attachTo(V view);
        public abstract void detachFrom(V view);

        protected void sendSignal(SignalType signal){
            View view = getAttachedView();
            if(view != null && RxView.eventsSuspendedFor(view)){
                return;
            }
            getSignalPipe().onNext(signal);
        }

        protected PublishSubject<SignalType> getSignalPipe(){
            if(mSignalPipe == null){
                mSignalPipe = PublishSubject.create();
            }
            return mSignalPipe;
        }

        protected Observable<SignalType> asObservable(){
            return getSignalPipe();
        }

        protected V getAttachedView(){
            return Utils.getReferencedValue(mAttachedViewRef);
        }
    }

    private static abstract class ViewTreeListener<SignalType, V extends View> extends ViewListener<SignalType, V> {

        @Override
        public void attachTo(V view){
            ViewTreeObserver tree = view.getViewTreeObserver();
            if(tree.isAlive()){
                attachTo(tree);
            }else{
                getSignalPipe().onComplete();
            }
        }

        @Override
        public void detachFrom(V view){
            ViewTreeObserver tree = view.getViewTreeObserver();
            if(tree.isAlive()){
                detachFrom(tree);
            }else{
                getSignalPipe().onComplete();
            }
        }

        public abstract void attachTo(ViewTreeObserver viewTree);
        public abstract void detachFrom(ViewTreeObserver viewTree);


    }

    private static final class AttachStateListener<V extends View> extends ViewListener<Integer, V> implements View.OnAttachStateChangeListener{
        private boolean mIsAttached;

        public AttachStateListener(){}

        @Override
        public void onViewAttachedToWindow(View v) {
            if(eventsSuspendedFor(v)){
                return;
            }
            if(!mIsAttached){
                mIsAttached = true;
                sendSignal(ATTACHED);
            }
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            if(eventsSuspendedFor(v)){
                return;
            }
            if(mIsAttached){
                mIsAttached = false;
                sendSignal(DETACHED);
                getSignalPipe().onComplete();
            }
        }

        @Override
        public void attachTo(View view) {
            view.addOnAttachStateChangeListener(this);
        }

        @Override
        public void detachFrom(View view) {
            view.removeOnAttachStateChangeListener(this);
        }

    }


    private static final class ClickListener<V extends View> extends ViewListener<View, V> implements View.OnClickListener{
        public ClickListener(){}

        @Override
        public void onClick(View v) {
            sendSignal(v);
        }

        @Override
        public void attachTo(View view) {
            view.setClickable(true);
            view.setOnClickListener(this);
        }

        @Override
        public void detachFrom(View view) {
            view.setOnClickListener(null);
        }
    }



    private static final class GlobalLayoutListener<V extends View> extends ViewTreeListener<V, V> implements ViewTreeObserver.OnGlobalLayoutListener{
        public GlobalLayoutListener(){}

        @Override
        public void onGlobalLayout() {
            sendSignal(getAttachedView());
        }

        @Override
        public void attachTo(ViewTreeObserver viewTree) {
            viewTree.addOnGlobalLayoutListener(this);
        }

        @SuppressWarnings("deprecation")
        @SuppressLint("NewApi")
        @Override
        public void detachFrom(ViewTreeObserver viewTree) {
            if (Build.VERSION.SDK_INT < 16) {
                viewTree.removeGlobalOnLayoutListener(this);
            } else {
                viewTree.removeOnGlobalLayoutListener(this);
            }
        }

    }

    private static final class PreDrawListener<V extends View> extends ViewTreeListener<V, V> implements ViewTreeObserver.OnPreDrawListener{
        public PreDrawListener(){}

        @Override
        public boolean onPreDraw() {
            sendSignal(getAttachedView());
            return true;
        }

        @Override
        public void attachTo(ViewTreeObserver viewTree) {
            viewTree.addOnPreDrawListener(this);
        }

        @Override
        public void detachFrom(ViewTreeObserver viewTree) {
            viewTree.removeOnPreDrawListener(this);
        }

    }


    private static final class FocusChangedListener<V extends View> extends ViewListener<Integer, V> implements View.OnFocusChangeListener{
        public FocusChangedListener(){}

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if(ViewCompat.isAttachedToWindow(v)){
                sendSignal(hasFocus ? FOCUS_GAIN : FOCUS_LOST);
            }
        }

        @Override
        public void attachTo(View view) {
            view.setOnFocusChangeListener(this);
        }

        @Override
        public void detachFrom(View view) {
            view.setOnFocusChangeListener(null);
        }
    }

    public static class TextChangeInfo{
        public CharSequence text;
        public int start, count, after, before, type;
        public Editable editable;
        public TextView editTextView;
    }

    private static TextChangeInfo sGlobalTextInfo;

    public static final class TextChangedListener extends ViewListener<TextChangeInfo, TextView> implements TextWatcher {

        public static final int BEFORE_CHANGE = 1, AFTER_CHANGE = 2, ON_CHANGE = 3;
        private WeakReference<TextView> mViewRef;
        public TextChangedListener(){}



        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            TextChangeInfo info = getTextInfo(BEFORE_CHANGE);
            info.text = s;
            info.start = start;
            info.count = count;
            info.after = after;
            sendSignal(info);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

            TextChangeInfo info = getTextInfo(ON_CHANGE);
            info.text = s;
            info.start = start;
            info.before = before;
            info.count = count;

            sendSignal(info);

        }

        @Override
        public void afterTextChanged(Editable s) {
            TextChangeInfo info = getTextInfo(AFTER_CHANGE);
            info.editable = s;
            sendSignal(info);

        }

        private TextChangeInfo getTextInfo(int signalType){
            if(sGlobalTextInfo == null){
                sGlobalTextInfo = new TextChangeInfo();
            }
            sGlobalTextInfo.text = null;
            sGlobalTextInfo.start = sGlobalTextInfo.count = sGlobalTextInfo.after = sGlobalTextInfo.before = 0;
            sGlobalTextInfo.type = signalType;
            sGlobalTextInfo.editTextView = Utils.getReferencedValue(mViewRef);
            return sGlobalTextInfo;
        }


        @Override
        public void attachTo(TextView view) {
            view.addTextChangedListener(this);
        }

        @Override
        public void detachFrom(TextView view) {
            view.removeTextChangedListener(this);
        }
    }



    private static final class ItemSelectedListener<V extends View> extends ViewListener<Integer, V> implements AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener{

        public ItemSelectedListener(){}

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            sendSignal(position);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }

        // How ridiculous is this? same method definition for different components, but NO common interface
        // Although the AutocompleteTextField allows setting an onItemSelected listener, it will never call it (known android bug)
        // As a walkaround - this listener will trigger the notification when one item is clicked for
        // AutoComplete fields, and when one item is selected for AdapterViews
        @Override
        public void attachTo(View view) {
            if(view instanceof AdapterView){
                ((AdapterView) view).setOnItemSelectedListener(this);
            }else if( view instanceof AutoCompleteTextView){
                ((AutoCompleteTextView) view).setOnItemClickListener(this);
            }
        }

        @Override
        public void detachFrom(View view) {
            if(view instanceof AdapterView){
                ((AdapterView) view).setOnItemSelectedListener(null);
            }else if( view instanceof AutoCompleteTextView){
                ((AutoCompleteTextView) view).setOnItemClickListener(null);
            }
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            sendSignal(position);
        }
    }


    private static class LayoutChangedListener<V extends View> extends ViewListener<V, V> implements View.OnLayoutChangeListener{
        public LayoutChangedListener(){}

        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            sendSignal(getAttachedView());
        }

        @Override
        public void attachTo(View view) {
            view.addOnLayoutChangeListener(this);
        }

        @Override
        public void detachFrom(View view) {
            view.removeOnLayoutChangeListener(this);
        }
    }

    private static class CheckedChangedLister<V extends CompoundButton> extends ViewListener<Boolean, V> implements CompoundButton.OnCheckedChangeListener{

        public CheckedChangedLister(){}

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            sendSignal(isChecked);
        }

        @Override
        public void attachTo(CompoundButton view) {
            view.setOnCheckedChangeListener(this);
        }

        @Override
        public void detachFrom(CompoundButton view) {
            view.setOnCheckedChangeListener(null);
        }
    }





}