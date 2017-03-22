package rpi.aut.rpi_monit.drawables;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import rpi.aut.rpi_monit.lib.Utils;

public class PercentMarkerDrawable extends Drawable {

    public interface Direction{
        int TOP = 1,
        LEFT = 2,
        RIGHT = 3,
        BOTTOM = 4;
    }


    private static final String TEXT = "100";
    private static final float RATIO = 0.6f;

    private final Paint mPaint;

    private int mIntrinsicWidth, mIntrinsicHeight;
    private int mProgress;

    private Path mPath;
    private final int mPadding, mTextWidth, mTextHeight;

    private final int mArrowDirection;

    public PercentMarkerDrawable(int direction){

        mArrowDirection = direction;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.WHITE);
        mPaint.setTextSize(Utils.spToPx(14));
        mPaint.setFakeBoldText(true);
        mPadding = Utils.dpToPx(5);
        mTextWidth = (int)(mPaint.measureText(TEXT)+0.5);
        mTextHeight = (int)(mPaint.descent()-mPaint.ascent()+0.5);
    }

    public void setProgress(@IntRange(from=0, to=100) int progress){
        if(mProgress != progress){
            mProgress = progress;
            invalidateSelf();
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        mPaint.setColor(Color.WHITE);
        canvas.drawPath(getPath(), mPaint);
        String mText = "100";
        mPaint.setColor(Color.BLACK);
        canvas.drawText(mText, 0, mText.length(), mPadding, getIntrinsicHeight()-mPadding-mPaint.descent(), mPaint);

    }

    private Path getPath(){
        if(mPath == null){
            mPath = new Path();
            int rectWidth = 2*mPadding+mTextWidth;
            int rectHeight = 2*mPadding+mTextHeight;

            int rx = Utils.dpToPx(2);

            final float radii[] = new float[8];
            switch (mArrowDirection){
                case Direction.RIGHT:
                    radii[0] = radii[1] = radii[6] = radii[7] = rx;
                    break;
                case Direction.LEFT:
                    radii[2] = radii[3] = radii[4] = radii[5] = rx;
                    break;
                case Direction.TOP:
                    radii[4] = radii[5] = radii[6] = radii[7] = rx;
                    break;
                case Direction.BOTTOM:
                    radii[0] = radii[1] = radii[2] = radii[3] = rx;
                    break;
            }

            switch (mArrowDirection){
                case Direction.RIGHT:
                    mPath.addRoundRect(0,0,rectWidth, rectHeight, radii, Path.Direction.CW);

                    mPath.moveTo(rectWidth, 0);
                    mPath.rLineTo(getIntrinsicWidth()-rectWidth, getIntrinsicHeight()/2);
                    mPath.rLineTo(rectWidth-getIntrinsicWidth(), getIntrinsicHeight()/2);
                    break;
                case Direction.LEFT:
                    mPath.addRoundRect(getIntrinsicWidth()-rectWidth,0,rectWidth, rectHeight, radii, Path.Direction.CW);
//
//                    mPath.moveTo(getIntrinsicWidth()-rectWidth, 0);
//                    mPath.rLineTo(rectWidth-getIntrinsicHeight(), getIntrinsicHeight()/2);
//                    mPath.rLineTo(getIntrinsicWidth()-rectWidth, getIntrinsicHeight());
                    break;
                case Direction.TOP:
                    mPath.addRoundRect(0,getIntrinsicHeight()-rectHeight,rectWidth, getIntrinsicHeight(), radii, Path.Direction.CW);

                    mPath.moveTo(0, getIntrinsicHeight()-rectHeight);
                    mPath.rLineTo(getIntrinsicWidth()/2, rectHeight-getIntrinsicHeight());
                    mPath.rLineTo(getIntrinsicWidth()/2, getIntrinsicHeight()-rectHeight);
                    break;
                case Direction.BOTTOM:
                    mPath.addRoundRect(0,0,rectWidth, rectHeight, radii, Path.Direction.CW);

                    mPath.moveTo(0, rectHeight);
                    mPath.rLineTo(getIntrinsicWidth()/2, getIntrinsicHeight());
                    mPath.rLineTo(getIntrinsicWidth(), rectHeight-getIntrinsicHeight());
                    break;

            }


        }
        return mPath;
    }


    @Override
    public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {

    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    public int getIntrinsicWidth() {
        if(mIntrinsicWidth == 0){
            switch (mArrowDirection){
                case Direction.TOP:
                case Direction.BOTTOM:
                    mIntrinsicWidth = mPadding*2+mTextWidth;
                    break;
                case Direction.LEFT:
                case Direction.RIGHT:
                    mIntrinsicWidth = (int)(mTextWidth+2*mPadding+getIntrinsicHeight()*RATIO+0.5);
            }

        }
        return mIntrinsicWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        if(mIntrinsicHeight == 0){
            switch (mArrowDirection){
                case Direction.TOP:
                case Direction.BOTTOM:
                    mIntrinsicHeight = (int)(mPadding*2+mTextHeight+getIntrinsicWidth()*RATIO+0.5);
                    break;
                case Direction.LEFT:
                case Direction.RIGHT:
                    mIntrinsicHeight = mPadding*2+mTextHeight;
                    break;
            }
        }
        return mIntrinsicHeight;
    }
}
