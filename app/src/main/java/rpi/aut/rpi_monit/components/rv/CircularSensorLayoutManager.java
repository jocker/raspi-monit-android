package rpi.aut.rpi_monit.components.rv;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class CircularSensorLayoutManager extends RecyclerView.LayoutManager{

    final float mAngleRange = 360;
    final float mAngleOffset = 0;

    private TouchDelegateGroup mTouchDelegateGroup;


    public CircularSensorLayoutManager(){
        super();
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onAttachedToWindow(RecyclerView view) {
        view.setItemAnimator(null);
        view.setHasFixedSize(true);
        super.onAttachedToWindow(view);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        removeAllViews();
        if(mTouchDelegateGroup != null){
            mTouchDelegateGroup.clearTouchDelegates();
        }


        float startAngle = mAngleOffset;

        final int width = getWidth();
        final int height = getHeight();

        final int minDimen = width > height ? height : width;
        final float radius = minDimen/2 - minDimen/12;

        int childCount = state.getItemCount(), radiiChildCount = childCount-1;

        final float angle = mAngleRange/radiiChildCount;

        for(int pos=0; pos< childCount; pos++){
            View view = recycler.getViewForPosition(pos);
            view.measure(View.MeasureSpec.makeMeasureSpec(minDimen, View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED);
            int viewWidth = view.getMeasuredWidth(), viewHeight = view.getMeasuredHeight();

            addView(view);
            final float centerAngle = startAngle + angle/2f;
            final int x;
            final int y;
            final float scale;

            if(pos == radiiChildCount){
                x = width/2;
                y = height/2;
                scale = 1f/2;
            }else{
                scale = 1f/6;
                x = (int) (radius * Math.cos(Math.toRadians(centerAngle))) + width/2;
                y = (int) (radius * Math.sin(Math.toRadians(centerAngle))) + height/2;
            }
            view.setScaleX(scale);
            view.setScaleY(scale);



            final int left = x - viewWidth/2;
            final int top = y - viewHeight/2;
            final int right = x + viewWidth/2;
            final int bottom = y + viewHeight/2;
            if(mTouchDelegateGroup != null){
                if(pos == radiiChildCount){
                    view.setTouchDelegate(mTouchDelegateGroup);
                }else{
                    mTouchDelegateGroup.addTouchDelegate(new TouchDelegate(new Rect(left, top, right, bottom), view));
                }
            }



            view.measure(View.MeasureSpec.makeMeasureSpec(minDimen, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(minDimen, View.MeasureSpec.EXACTLY));

            layoutDecorated(view,left, top, right, bottom);

            startAngle += angle;

        }

    }


    public static class TouchDelegateGroup extends TouchDelegate {
        private final ArrayList<TouchDelegate> mTouchDelegates = new ArrayList<>();
        private TouchDelegate mCurrentTouchDelegate;
        private boolean mEnabled = true;

        public TouchDelegateGroup(View uselessHackyView) {
            super(new Rect(), uselessHackyView);
        }

        public void addTouchDelegate(TouchDelegate touchDelegate) {
            mTouchDelegates.add(touchDelegate);
        }

        public void removeTouchDelegate(TouchDelegate touchDelegate) {
            mTouchDelegates.remove(touchDelegate);
            if (mCurrentTouchDelegate == touchDelegate) {
                mCurrentTouchDelegate = null;
            }
        }

        public void clearTouchDelegates() {
            mTouchDelegates.clear();
            mCurrentTouchDelegate = null;
        }

        @Override
        public boolean onTouchEvent(@NonNull MotionEvent event) {
            if (!mEnabled) return false;

            TouchDelegate delegate = null;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    for (int i = 0; i < mTouchDelegates.size(); i++) {
                        TouchDelegate touchDelegate = mTouchDelegates.get(i);
                        if (touchDelegate.onTouchEvent(event)) {
                            mCurrentTouchDelegate = touchDelegate;
                            return true;
                        }
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    delegate = mCurrentTouchDelegate;
                    break;

                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    delegate = mCurrentTouchDelegate;
                    mCurrentTouchDelegate = null;
                    break;
            }

            return delegate != null && delegate.onTouchEvent(event);
        }

        public void setEnabled(boolean enabled) {
            mEnabled = enabled;
        }
    }

}
