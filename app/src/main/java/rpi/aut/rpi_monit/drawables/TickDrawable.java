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

public class TickDrawable extends Drawable {

    public static void fillHorizontalTickPath(Path path, int width, int height, int tickWidth, int numTicks, int numSubTicks){
        path.reset();
        int tickSpace = (width-tickWidth)/numTicks, subDivisionWidth = numSubTicks > 1 ? tickSpace/numSubTicks : 0;
        int left = 0;
        for(int i=0;i<=numTicks;i++){
            path.addRect(left, 0, left+tickWidth, height, Path.Direction.CW);
            if(i< numSubTicks){
                for(int j=1; j< numSubTicks; j++){
                    path.addCircle(left+j*subDivisionWidth, height/2, tickWidth , Path.Direction.CW);
                }
            }
            left+= tickSpace;
        }
    }

    public static void fillVerticalTickPath(Path path, int width, int height, int tickWidth, int numTicks, int numSubTicks){
        path.reset();
        int tickSpace = (height-tickWidth)/numTicks, subDivisionWidth = numSubTicks > 1 ? tickSpace/numSubTicks : 0;
        int top = 0;
        for(int i=0;i<=numTicks;i++){
            path.addRect(0, top, width, top+tickWidth, Path.Direction.CW);
            if(i< numSubTicks){
                for(int j=1; j< numSubTicks; j++){
                    path.addCircle(width/2, top+j*subDivisionWidth, tickWidth, Path.Direction.CW);
                }
            }
            top+= tickSpace;
        }
    }

    public static void fillMarkerPath(Paint textPaint, CharSequence text, Path path){
        int textWidth = (int)(textPaint.measureText(text, 0, text.length())+0.5), textHeight = (int)(textPaint.descent()-textPaint.ascent()+0.5);
        int padding = Utils.dpToPx(2);

    }


    private final Paint mPaint, mStrokePaint;
    private int mSubdivisions = 2, mDivisions = 10;
    private Path mShapePath;

    public TickDrawable(){
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.WHITE);
        mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mStrokePaint.setColor(Color.BLACK);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeWidth(Utils.dpToPx(2));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if(mShapePath != null){
            canvas.drawPath(mShapePath, mStrokePaint);
            canvas.drawPath(mShapePath, mPaint);
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        if(mShapePath == null){
            mShapePath = new Path();
        }else{
            mShapePath.reset();
        }
        int tickWidth = Utils.dpToPx(2);
        int tickSpace = (bounds.width()-tickWidth)/mDivisions, subDivisionWidth = tickSpace/mSubdivisions;
        int left = 0;
        for(int i=0;i<=mDivisions;i++){
            mShapePath.addRect(left, 0, left+tickWidth, bounds.height(), Path.Direction.CW);
            if(i< mDivisions){
                for(int j=1; j< mSubdivisions; j++){
                    mShapePath.addCircle(left+j*subDivisionWidth, bounds.height()/2, tickWidth , Path.Direction.CW);
                }
            }
            left+= tickSpace;
        }
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
}
