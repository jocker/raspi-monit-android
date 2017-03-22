package rpi.aut.rpi_monit.components;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import rpi.aut.rpi_monit.drawables.CircularProgressDrawable;

public class CircularProgressView extends AppCompatTextView {

    private CircularProgressDrawable mBackground;

    public CircularProgressView(Context context) {
        this(context, null);
    }

    public CircularProgressView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public CircularProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mBackground = new CircularProgressDrawable();
        setBackground(mBackground);
    }

    public void setProgressWidth(float size){
        mBackground.setProgressWidth(size);
    }

    public void setProgressColor(int color){
        mBackground.setProgressColor(color);
    }

    public void setTrackWidth(float size){
        mBackground.setTrackWidth(size);

    }

    public void setTrackColor(int color){
        mBackground.setTrackColor(color);
    }

    public void setStartAngle(@IntRange(from = 0, to = 360) int angle){
        mBackground.setStartAngle(angle);
    }

    public void setProgress(@FloatRange(fromInclusive = true, toInclusive = true, from = 0f, to = 1f) float progress) {
        mBackground.setProgress(progress);
    }

    public void setFillColor(int color){
        mBackground.setFillColor(color);
    }

    public void setIcon(Drawable icon){
        mBackground.setInnerDrawable(icon);
    }
}
