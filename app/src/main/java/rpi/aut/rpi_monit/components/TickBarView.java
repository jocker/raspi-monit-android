package rpi.aut.rpi_monit.components;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import rpi.aut.rpi_monit.R;
import rpi.aut.rpi_monit.lib.DrawableUtils;
import rpi.aut.rpi_monit.lib.RxView;
import rpi.aut.rpi_monit.lib.Utils;

public class TickBarView extends View {

    private static final int
            STATE_INACTIVE = 0,
            STATE_ACTIVE = 1;

    public interface Orientation{
        int
                LEFT = 1,
                TOP = 2,
                RIGHT = -1,
                BOTTOM = -2;
    }

    private interface MarkerType{
        int CONTRAST = 1,
                MOVE_H = 2,
                MOVE_V = 3;
    }

    private BackgroundDrawable mDrawable;
    private int mOrientation;

    private int[] mMargins = new int[4];
    private int[] mSize = new int[2];
    private boolean mMarginsSet = false;

    private Pair<Integer, Integer> mStartDragPoint;
    private final float mSpeedFactor = 0.5f;
    private float mProgress = 0f, mDragProgress = 0;
    private int mState = STATE_INACTIVE;
    private ValueAnimator mStateAnimator;

    private boolean mAnimateTickbars = false;

    private OnProgressChangedListener mListener;


    public TickBarView(Context context) {
        this(context, null);
    }

    public TickBarView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TickBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray styles = context.obtainStyledAttributes(attrs, R.styleable.TickBarView, defStyleAttr, 0);

        mOrientation = styles.getInt(R.styleable.TickBarView_barOrientation, Orientation.BOTTOM);
        int markerType = styles.getInt(R.styleable.TickBarView_barMarker, 0);
        Drawable marker = null;
        switch (markerType){
            case MarkerType.CONTRAST:
                marker = DrawableUtils.getTintedDrawable(getContext(), R.drawable.ic_brightness, Color.WHITE);
                marker.setBounds(0,0,marker.getIntrinsicWidth()-Utils.dpToPx(8), marker.getIntrinsicHeight()-Utils.dpToPx(8));
                break;
            case MarkerType.MOVE_H:
                marker = DrawableUtils.getTintedDrawable(getContext(), R.drawable.ic_move_horizontal, Color.WHITE);
                marker.setBounds(0,0,marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
                break;
            case MarkerType.MOVE_V:
                marker = DrawableUtils.getTintedDrawable(getContext(), R.drawable.ic_move_vertical, Color.WHITE);
                marker.setBounds(0,0,marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
                break;
        }

        styles.recycle();

        mDrawable = new BackgroundDrawable(Utils.dpToPx(14),mOrientation);

        mDrawable.setMarkerDrawable(marker);

        mDrawable.setCallback(this);
        setWillNotDraw(false);
        setClickable(true);

        RxView.onClick(this).subscribe(tickBarView -> {
            int newState = mState == STATE_ACTIVE ? STATE_INACTIVE : STATE_ACTIVE;
            setState(newState);
        });


    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return super.verifyDrawable(who) || who == mDrawable;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if(mAnimateTickbars){
            mAnimateTickbars = false;
            float startValue = mStateAnimator != null && mStateAnimator.isRunning() ? (float)mStateAnimator.getAnimatedValue() : mState == STATE_ACTIVE ? 0 : 1 , endValue = 0;

            if(mStateAnimator != null){
                mStateAnimator.cancel();
                mStateAnimator = null;
            }

            switch (mState){
                case STATE_ACTIVE:
                    endValue = 1;

                    break;
                case STATE_INACTIVE:
                    endValue = 0;
                    break;
            }



            int duration = (int)(Math.abs(startValue-endValue)*200);
            ValueAnimator animator = ValueAnimator.ofFloat(startValue, endValue);
            animator.setDuration(duration);
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mStateAnimator = (ValueAnimator)animation;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if(mStateAnimator == animation){
                        mStateAnimator = null;
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    if(mStateAnimator == animation){
                        mStateAnimator = null;
                    }
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });

            animator.addUpdateListener(animation -> {
                float percent = (float)animation.getAnimatedValue();
                mDrawable.setTickSizePercent(percent);
            });

            animator.start();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        storeLayoutParams();
        int width = getMeasuredWidth(), height = getMeasuredHeight();
        if(mState == STATE_ACTIVE){
            mDrawable.setBounds(
                    mMargins[0],
                    mMargins[1],
                    width-mMargins[2],
                    height-mMargins[3]
            );
        }else{
            mDrawable.setBounds(0,0,width, height);
        }



    }

    private void storeLayoutParams(){
        if(!mMarginsSet && getLayoutParams() instanceof ViewGroup.MarginLayoutParams){
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)getLayoutParams();
            mMargins[0] = lp.leftMargin;
            mMargins[1] = lp.topMargin;
            mMargins[2] = lp.rightMargin;
            mMargins[3] = lp.bottomMargin;
            mMarginsSet = true;

            mSize[0] = lp.width;
            mSize[1] = lp.height;
        }
    }


    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        //canvas.drawColor(ColorUtils.addAlpha(Color.RED, 0.1f));
        mDrawable.draw(canvas);
    }


    public void setState(int state){
        storeLayoutParams();

        if(mState == state){
            return;
        }

        mState = state;

        if(getLayoutParams() instanceof ViewGroup.MarginLayoutParams){
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)getLayoutParams();
            switch (state){
                case STATE_INACTIVE:

                    mDrawable.setMarkerVisible(true);
                    mDrawable.setIndicatorVisible(false);


                    if(getParent() instanceof ViewGroup){
                        boolean resetZPosition = false;
                        ViewGroup parent = (ViewGroup)getParent();
                        for(int i=0;i< parent.getChildCount();i++){
                            if(resetZPosition){
                                parent.getChildAt(i).bringToFront();
                            }else{
                                resetZPosition = parent.getChildAt(i) == this;
                            }
                        }
                    }

                    lp.width = mSize[0];
                    lp.height = mSize[1];
                    lp.leftMargin = mMargins[0];
                    lp.topMargin = mMargins[1];
                    lp.rightMargin = mMargins[2];
                    lp.bottomMargin = mMargins[3];
                    break;
                case STATE_ACTIVE:
                    bringToFront();
                    mDrawable.setMarkerVisible(false);
                    mDrawable.setIndicatorVisible(true);

                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    lp.leftMargin = lp.topMargin = lp.rightMargin = lp.bottomMargin = 0;
                    break;
            }

            mAnimateTickbars = true;
            super.setLayoutParams(lp);
            requestLayout();

        }

    }

    public boolean isActive(){
        return mState == STATE_ACTIVE;
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        mMarginsSet = false;
        super.setLayoutParams(params);
    }

    private Pair<Integer,Integer> mDownPoint;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if ( !isEnabled()) {
            return super.onTouchEvent(event);
        }

        boolean handled = false;

        if(mState == STATE_INACTIVE){
            return super.onTouchEvent(event);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownPoint = new Pair<>(Math.round(event.getX()), Math.round(event.getY()));
                Log.e("down", "down");
                Log.e("down", "down");
                handled = false;
                break;

            case MotionEvent.ACTION_MOVE:

                if(isDragging()){
                    trackTouchEvent(event);
                }else if(mDownPoint != null){

                    if(isDragging()){
                        trackTouchEvent(event);
                    }else{
                        int distance = Utils.dpToPx(1);
                        if(Math.abs(mDownPoint.first-event.getX()) > distance || Math.abs(mDownPoint.second-event.getY()) > distance){
                            setIsDragging(event, true);
                            mStartDragPoint = mDownPoint;
                            mDownPoint = null;
                            trackTouchEvent(event);
                        }
                    }

                    handled = isDragging();
                }

                break;
            case MotionEvent.ACTION_UP:
                if (isDragging()) {
                    trackTouchEvent(event);
                    setIsDragging(event, false);
                    handled = true;
                }
                mDownPoint = null;
                break;

            case MotionEvent.ACTION_CANCEL:
                if (isDragging()) {
                    setIsDragging(event, false);
                    handled = true;
                }
                break;
        }

        if(handled){
            return true;
        }

        return super.onTouchEvent(event);
    }

    private void setIsDragging(MotionEvent event, boolean isDragging){
        if(isDragging() != isDragging){
            setPressed(isDragging);
            if(isDragging){
                mDragProgress = mProgress;
                mStartDragPoint = new Pair<>(Math.round(event.getX()), Math.round(event.getY()));
            }else{
                setProgress(mProgress, true);
                mDownPoint = null;
                mStartDragPoint = null;
            }
        }
    }

    private boolean isDragging(){
        return mStartDragPoint != null;
    }

    public void setProgress(float progress, boolean fromUser){
        if(mProgress != progress){
            mProgress = progress;
            mDrawable.setProgress(progress);
            if(mListener != null){
                mListener.onProgressChanged(this, progress, fromUser);
            }
        }
    }

    private void trackTouchEvent(MotionEvent event) {

        int distance = 0, totalDistance = 0;
        float percent = 0f;

        if(mStartDragPoint == null){
            return;
        }

        switch (mOrientation){
            case Orientation.TOP:
            case Orientation.BOTTOM:
                distance = Math.round(event.getX())-mStartDragPoint.first;
                totalDistance = getWidth();

                break;
            case Orientation.LEFT:
            case Orientation.RIGHT:
                distance = Math.round(event.getY())-mStartDragPoint.second;
                totalDistance = getHeight();
                break;
        }

        if(totalDistance > 0){
            percent = (distance*mSpeedFactor)/totalDistance;
            percent += mDragProgress;
            percent = Math.max(0, Math.min(1, percent));
            setProgress(percent, true);
            Log.e("progress", String.format("distance:%s total:%s percent:%s", distance, totalDistance, percent));
        }

    }






    private static class BackgroundDrawable extends Drawable{



        private static void fillIndicatorPath(Path path, int baseSize, int orientation, int[] outSize){
            int width = 0, height = 0;
            switch (orientation){
                case Orientation.RIGHT:
                    width = baseSize;
                    height = baseSize;
                    path.lineTo(width, height/2);
                    path.rLineTo(-width, height/2);
                    break;
                case Orientation.LEFT:
                    width = baseSize;
                    height = baseSize;
                    path.moveTo(width, 0);
                    path.rLineTo(-width, height/2);
                    path.rLineTo(width, height/2);
                    break;
                case Orientation.TOP:
                    width = baseSize;
                    height = baseSize;
                    path.moveTo(0,height);
                    path.rLineTo(width/2, -height);
                    path.rLineTo(width/2, height);
                    break;
                case Orientation.BOTTOM:
                    width = baseSize;
                    height = baseSize;
                    path.lineTo(width/2, height);
                    path.rLineTo(width/2, -height);
                    break;

            }

            outSize[0] = width;
            outSize[1] = height;
        }


        public static void fillHorizontalTickPath(Path path, int width, int height, int tickSize, int numTicks, int numSubTicks, float tickSizePercent){
            path.reset();
            float tickSpace = (width-tickSize*(numTicks+1)*1f)/numTicks, subDivisionWidth = numSubTicks > 1 ? tickSpace/numSubTicks : 0;
            int left = 0;

            float tickHeight = mapPercent(tickSize, height, tickSizePercent), tickTop = (height-tickHeight)/2;

            int subTickTop = (height-tickSize)/2;

            for(int i=0;i<=numTicks;i++){

                if(tickSizePercent == 1){
                    path.addRect(left, 0, left+tickSize, height, Path.Direction.CW);
                }else {
                    path.addRoundRect(left, tickTop, left+tickSize, tickTop+tickHeight, tickSize/2f, tickSize/2f, Path.Direction.CW);
                }

                left += tickSize;
                if(i< numTicks){
                    for(int j=1; j< numSubTicks; j++){
                        path.addRoundRect(left+j*subDivisionWidth, subTickTop, left+j*subDivisionWidth+tickSize, subTickTop+tickSize, tickSize/2, tickSize, Path.Direction.CW);
                    }
                    left += tickSpace;
                }
            }

        }

        public static void fillVerticalTickPath(Path path, int width, int height, int tickSize, int numTicks, int numSubTicks, float tickSizePercent){
            path.reset();
            float tickSpace = (height-tickSize*(numTicks+1)*1f)/numTicks, subDivisionWidth = numSubTicks > 1 ? tickSpace/numSubTicks : 0;
            float top = 0;

            float tickWidth =  mapPercent(tickSize, width, tickSizePercent), tickLeft = (width-tickWidth)/2;

            float subTickLeft = (width-tickSize)/2;

            for(int i=0;i<=numTicks;i++){

                if(tickSizePercent == 1){
                    path.addRect(tickLeft, top, width, top+tickSize, Path.Direction.CW);
                }else{
                    path.addRoundRect(tickLeft, top, tickLeft+tickWidth, top+tickSize, tickWidth/2f, tickWidth/2f, Path.Direction.CW);
                }

                top+= tickSize;
                if(i< numTicks){
                    for(int j=1; j< numSubTicks; j++){
                        path.addRoundRect(subTickLeft, top+j*subDivisionWidth, subTickLeft+tickSize, top+j*subDivisionWidth+tickSize, tickWidth/2f, tickWidth/2f, Path.Direction.CW);
                    }
                    top+= tickSpace;
                }

            }

        }


        private final int mTrackSize, mOrientation;


        private int mMarkerWidth, mMarkerHeight, mIndicatorWidth, mIndicatorHeight, mTrackHPadding, mTrackVPadding;

        private Path mTrackPath, mIndicatorPath;
        private Paint mFillPaint, mStrokePaint;
        private Drawable mMarkerDrawable;

        private final int[] mPathSize = new int[2];

        private float mProgress = 0f;

        private final int mTickSize;

        private float mTickSizePercent = 0f;

        private boolean mMarkerVisible = true, mIndicatorVisible = false;

        public BackgroundDrawable(int size, int orientation){
            mTrackSize = size;
            mOrientation = orientation;

            mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mFillPaint.setStyle(Paint.Style.FILL);
            mFillPaint.setColor(Color.WHITE);
            mFillPaint.setTextSize(Utils.spToPx(14));

            mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mStrokePaint.setStyle(Paint.Style.STROKE);
            mStrokePaint.setColor(Color.BLACK);
            mStrokePaint.setShadowLayer(2, 0,0,Color.BLACK);

            mIndicatorWidth = mPathSize[0];
            mIndicatorHeight = mPathSize[1];

            mTickSize = Utils.dpToPx(3);
        }

        public void setMarkerVisible(boolean visible){
            if(mMarkerVisible != visible){
                mMarkerVisible = visible;
                invalidateSelf();
            }
        }

        public void setIndicatorVisible(boolean visible){
            if(mIndicatorVisible != visible){
                mIndicatorVisible = visible;
                invalidateSelf();
            }
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);

            if(mTrackPath != null){
                mTrackPath.reset();
            }
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            //canvas.drawRect(getBounds(), ColorUtils.getFillPaint(ColorUtils.addAlpha(Color.RED, 0.1f)));


            drawTrack(canvas);
            drawMarker(canvas);
            drawIndicator(canvas);

        }

        private void drawTrack(Canvas canvas){
            int left = 0, top = 0;

            Rect bounds = getBounds();

            Path path = getTrackPath();

            switch (mOrientation){
                case Orientation.RIGHT:
                    top = bounds.top+mTrackVPadding;
                    left = bounds.right-mTrackSize-mTrackHPadding;
                    break;
                case Orientation.LEFT:
                    top = bounds.top+mTrackVPadding;
                    left = bounds.left+mTrackHPadding;
                    break;
                case Orientation.TOP:
                    top = bounds.top+mTrackVPadding;
                    left = bounds.left+mTrackHPadding;
                    break;
                case Orientation.BOTTOM:
                    top = bounds.bottom-mTrackVPadding-mTrackSize;
                    left = bounds.left+mTrackHPadding;
            }

            int savePoint = canvas.save();
            canvas.translate(left, top);
            canvas.drawPath(path, mFillPaint);
            canvas.drawPath(path, mStrokePaint);

            canvas.restoreToCount(savePoint);

        }

        private void drawIndicator(Canvas canvas){

            if(!mIndicatorVisible){
                return;
            }

            int left = 0, top = 0;

            Rect bounds = getBounds();

            Path path = getIndicatorPath();

            switch (mOrientation){
                case Orientation.RIGHT:
                    left = bounds.right-mTrackHPadding-mTrackSize-mIndicatorWidth;
                    top =mapPercent(
                            bounds.top+mTrackVPadding-(mIndicatorHeight-mTickSize)/2,
                            bounds.bottom-mTrackVPadding-(mIndicatorHeight+mTickSize)/2,
                            mProgress);
                    break;
                case Orientation.LEFT:
                    left = bounds.left+mTrackHPadding+mTrackSize;
                    top =mapPercent(
                            bounds.top+mTrackVPadding-(mIndicatorHeight-mTickSize)/2,
                            bounds.bottom-mTrackVPadding-(mIndicatorHeight+mTickSize)/2,
                            mProgress);
                    break;
                case Orientation.TOP:
                    top = bounds.top+mTrackVPadding+mTrackSize;
                    left = mapPercent(
                            bounds.left+mTrackHPadding-(mIndicatorWidth-mTickSize)/2,
                            bounds.right-mTrackHPadding-(mIndicatorWidth)/2-mTickSize,
                            mProgress
                    );
                    break;
                case Orientation.BOTTOM:
                    top = bounds.bottom-mTrackSize-mTrackHPadding-mIndicatorHeight;
                    left = mapPercent(
                            bounds.left+mTrackHPadding-(mIndicatorWidth-mTickSize)/2,
                            bounds.right-mTrackHPadding-(mIndicatorWidth)/2-mTickSize,
                            mProgress
                    );
            }

            int savePoint = canvas.save();
            canvas.translate(left, top);
            canvas.drawPath(path, mFillPaint);
            canvas.drawPath(path, mStrokePaint);

            canvas.restoreToCount(savePoint);
        }

        private void drawMarker(Canvas canvas){

            if(!mMarkerVisible){
                return;
            }

            if(mMarkerDrawable == null){
                return;
            }

            int left = 0, top = 0;

            Rect bounds = getBounds();

            switch (mOrientation){
                case Orientation.RIGHT:
                    left = bounds.right-mMarkerWidth;
                    top = mapPercent(bounds.top, bounds.bottom-mMarkerHeight, mProgress);
                    break;
                case Orientation.LEFT:
                    left = bounds.left;
                    top = mapPercent(bounds.top, bounds.bottom-mMarkerHeight, mProgress);
                    break;
                case Orientation.TOP:
                    top = 0;
                    left = mapPercent(bounds.left, bounds.right - bounds.left-mMarkerWidth-mTickSize, mProgress);
                    break;
                case Orientation.BOTTOM:
                    top = bounds.bottom - mMarkerHeight;
                    left = mapPercent(bounds.left, bounds.right - bounds.left-mMarkerWidth, mProgress);
                    break;
            }

            int savePoint = canvas.save();
            canvas.translate(left, top);
            mMarkerDrawable.draw(canvas);
            canvas.restoreToCount(savePoint);
        }

        public void setProgress(@FloatRange(from = 0, to = 1) float progress){
            if(mProgress != progress){
                mProgress = progress;
                invalidateSelf();
            }
        }

        public void setMarkerDrawable(Drawable marker){
            mMarkerDrawable = marker;

            if(mTrackPath != null){
                mTrackPath.reset();
            }

            int width = 0, height = 0;
            if(marker != null){
                width = marker.getBounds().height();
                height = marker.getBounds().width();
            }
            if(width != mMarkerWidth || height != mMarkerHeight){
                if(mTrackPath != null){
                    mTrackPath.reset();
                }
                mMarkerWidth = width;
                mMarkerHeight = height;
            }
        }

        private Path getIndicatorPath(){
            if(mIndicatorPath == null){
                mIndicatorPath = new Path();
                fillIndicatorPath(mIndicatorPath, Utils.dpToPx(14), mOrientation, mPathSize);
                mIndicatorWidth = mPathSize[0];
                mIndicatorHeight = mPathSize[1];
            }
            return mIndicatorPath;
        }

        private Path getTrackPath(){
            if(mTrackPath == null){
                mTrackPath = new Path();
            }
            if(mTrackPath.isEmpty()){
                int hPadding = 0, vPadding = 0;
                getIndicatorPath(); // making sure we have correctly set mIndicatorWidth/mIndicatorHeight

                switch (mOrientation){
                    case Orientation.BOTTOM:
                    case Orientation.TOP:
                        hPadding = Math.max(mIndicatorWidth/2, mMarkerWidth/2);
                        vPadding = Math.max(0, mMarkerHeight- mTrackSize)/2;
                        fillHorizontalTickPath(mTrackPath, getBounds().width()-2*hPadding, mTrackSize, mTickSize, 10, 2, mTickSizePercent);
                        break;
                    case Orientation.LEFT:
                    case Orientation.RIGHT:
                        vPadding = (Math.max(mMarkerHeight, 0)-mTickSize)/2;
                        hPadding = Math.max(0, mMarkerWidth-mTrackSize)/2;
                        fillVerticalTickPath(mTrackPath, mTrackSize, getBounds().height()-2*vPadding, mTickSize, 10, 2, mTickSizePercent);
                        break;
                }

                mTrackVPadding = vPadding;
                mTrackHPadding = hPadding;
            }

            return mTrackPath;

        }

        @Override
        public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
            mStrokePaint.setAlpha(alpha);
            mFillPaint.setAlpha(alpha);
            if(mMarkerDrawable != null){
                mMarkerDrawable.setAlpha(alpha);
            }
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {

        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }

        public void setTickSizePercent(float percent){
            if(mTickSizePercent != percent){
                mTickSizePercent = percent;
                if(mTrackPath != null){
                    mTrackPath.reset();
                    invalidateSelf();
                }

            }
        }

        public float getTickSizePercent(){
            return mTickSizePercent;
        }

    }


    private static int mapPercent(int min, int max, float percent){
        return(int)( min+(max-min)*percent+0.5);
    }



    public interface OnProgressChangedListener{
        void onStartDrag(TickBarView view);
        void onEndDrag(TickBarView view);
        void onProgressChanged(TickBarView view, float progress, boolean fromUser);
    }


}
