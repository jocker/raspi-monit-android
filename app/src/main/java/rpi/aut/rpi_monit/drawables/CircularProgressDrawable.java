package rpi.aut.rpi_monit.drawables;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class CircularProgressDrawable extends Drawable {

    private int mStartAngle = -90;

    private float mProgress;

    private final Paint mProgressPaint, mTrackPaint, mFillPaint;

    private final RectF mStrokeRect = new RectF();

    private Drawable mInnerDrawable;

    private static class DrawableCallback implements Callback{
        private final Drawable mParent;
        private Drawable mChild;

        public void setDelegateDrawable(Drawable child){
            if(mChild != null && mChild.getCallback() == this){
                mChild.setCallback(null);
            }
            mChild = child;
            if(child != null){
                mChild.setCallback(this);
            }
        }

        public DrawableCallback(Drawable dr){
            mParent = dr;
        }

        @Override
        public void invalidateDrawable(@NonNull Drawable who) {
            if(mParent.getCallback() != null){
                mParent.getCallback().invalidateDrawable(mParent);
            }
        }

        @Override
        public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
            if(mParent.getCallback() != null){
                mParent.getCallback().scheduleDrawable(mParent, what, when);
            }
        }

        @Override
        public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
            if(mParent.getCallback() != null){
                mParent.getCallback().unscheduleDrawable(mParent, what);
            }
        }
    }

    private final DrawableCallback mInnerDrawableCallback;

    public CircularProgressDrawable(){

        mTrackPaint = initPaint(Paint.Style.STROKE);

        mProgressPaint = initPaint(Paint.Style.STROKE);

        mFillPaint = initPaint(Paint.Style.FILL);

        mInnerDrawableCallback = new DrawableCallback(this);

    }

    private Paint initPaint(Paint.Style paintStyle){
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(paintStyle);
        p.setColor(Color.TRANSPARENT);
        return p;
    }

    public void setProgressWidth(float size){
        if(mProgressPaint.getStrokeWidth() != size){
            mProgressPaint.setStrokeWidth(size);
            invalidateSelf();
        }
    }

    public void setProgressColor(int color){
        if(mProgressPaint.getColor() != color){
            mProgressPaint.setColor(color);
            invalidateSelf();
        }
    }

    public void setTrackWidth(float size){
        if(mTrackPaint.getStrokeWidth() != size){
            mTrackPaint.setStrokeWidth(size);
            invalidateSelf();
        }

    }

    public void setTrackColor(int color){
        if(mTrackPaint.getColor() != color){
            mTrackPaint.setColor(color);
            invalidateSelf();
        }
    }

    public void setStartAngle(int angle){
        angle = angle/360;
        if(mStartAngle != angle){
            mStartAngle = angle;
            invalidateSelf();
        }
    }

    public void setFillColor(int color){
        if(mFillPaint.getColor() != color){
            mFillPaint.setColor(color);
        }
    }

    public void setProgress(@FloatRange(fromInclusive = true, toInclusive = true, from = 0f, to = 1f) float progress) {
        if(mProgress != progress){
            mProgress = progress;
            invalidateSelf();
        }
    }

    public void setInnerDrawable(Drawable dr){
        mInnerDrawable = dr;
        mInnerDrawableCallback.setDelegateDrawable(dr);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {


        final Rect bounds = getBounds();
        final int size = Math.min(bounds.width(), bounds.height());

        final float progressStrokeWidth =  mProgressPaint.getStrokeWidth(), trackStrokeWidth = mTrackPaint.getStrokeWidth();

        int strokeInset = (int)(Math.max(progressStrokeWidth, trackStrokeWidth)/2+0.5);

        final float progressAngle = 360f*mProgress;


        mStrokeRect.set(strokeInset, strokeInset, size-strokeInset, size-strokeInset);

        float innerCircleRadius = (size - Math.min(progressStrokeWidth, trackStrokeWidth) + Math.abs(progressStrokeWidth - trackStrokeWidth)) / 2.0F;
        if(mFillPaint.getColor() != Color.TRANSPARENT){
            canvas.drawCircle((float)size / 2.0F, (float)size / 2.0F, innerCircleRadius, mFillPaint);
        }

        canvas.drawArc(mStrokeRect, mStartAngle + progressAngle, 360.0F - progressAngle, false, mTrackPaint);
        canvas.drawArc(mStrokeRect, mStartAngle, progressAngle, false, mProgressPaint);

        if(mInnerDrawable != null){
            int width = mInnerDrawable.getBounds().width(), height = mInnerDrawable.getBounds().height();
            if(width == 0 || height == 0){
                width = mInnerDrawable.getIntrinsicWidth();
                height = mInnerDrawable.getIntrinsicHeight();
                mInnerDrawable.setBounds(0,0,width, height);
            }
            int savePoint = canvas.save();
            int left = bounds.left+(bounds.width()-width)/2, top = bounds.top+(bounds.height()-height)/2;
            canvas.translate(left, top);
            mInnerDrawable.draw(canvas);
            canvas.restoreToCount(savePoint);
        }

    }

    @Override
    public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
        mProgressPaint.setAlpha(alpha);
        mTrackPaint.setAlpha(alpha);
        mFillPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mProgressPaint.setColorFilter(colorFilter);
        mTrackPaint.setColorFilter(colorFilter);
        mFillPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

}
