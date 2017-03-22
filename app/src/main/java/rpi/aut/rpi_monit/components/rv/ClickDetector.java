package rpi.aut.rpi_monit.components.rv;

import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.LinkedHashSet;
import java.util.Set;

import rpi.aut.rpi_monit.lib.RxView;

public class ClickDetector implements RecyclerView.OnItemTouchListener {

    private static final int TYPE_CLICK = 1, TYPE_LONG_CLICK = 2;

    private final GestureDetector mGestureDetector;
    private SparseArray<Set<GestureCallback>> mCallbacks;

    public static ClickDetector create(RecyclerView recyclerView){
        return new ClickDetector(recyclerView);
    }

    public ClickDetector(RecyclerView recyclerView){
        final WeakReference<RecyclerView> ref = new WeakReference<>(recyclerView);
        mGestureDetector = new GestureDetector(recyclerView.getContext(),new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                RecyclerView recyclerView = ref.get();
                if(recyclerView != null){
                    View child = recyclerView.findChildViewUnder(e.getX(),e.getY());
                    runCallbacks(TYPE_LONG_CLICK, recyclerView.getChildAdapterPosition(child), child, e);
                }

            }
        });

        recyclerView.addOnItemTouchListener(this);

        RxView.onDetach(recyclerView).subscribe(v -> {
            mCallbacks = null;
        });
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        View child=rv.findChildViewUnder(e.getX(),e.getY());
        if(child!=null && mGestureDetector.onTouchEvent(e)){
            runCallbacks(TYPE_CLICK, rv.getChildAdapterPosition(child), child, e);
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    public void onClick(GestureCallback callback){
        addCallback(TYPE_CLICK, callback);
    }

    public void onLongClick(GestureCallback callback){
        addCallback(TYPE_LONG_CLICK, callback);
    }

    private void addCallback(int type, GestureCallback callback){
        if(mCallbacks == null){
            mCallbacks = new SparseArray<>();
        }
        if(mCallbacks.indexOfKey(type) < 0){
            mCallbacks.put(type, new LinkedHashSet<>());
        }
        mCallbacks.get(type).add(callback);
    }

    private boolean runCallbacks(int type, int position, View view, MotionEvent event){
        if(mCallbacks != null && mCallbacks.indexOfKey(type) >=0){
            for(GestureCallback callback: mCallbacks.get(type)){
                if(callback.call(position, view, event)){
                   return true;
                }
            }
        }
        return false;
    }

    public interface GestureCallback {
        boolean call(int position, View view, MotionEvent event);
    }
}
