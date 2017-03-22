package rpi.aut.rpi_monit.components;

import android.content.Context;
import android.util.AttributeSet;

import com.github.lzyzsd.circleprogress.DonutProgress;

public class ScalableProgressBar extends DonutProgress {
    public ScalableProgressBar(Context context) {
        super(context);
    }

    public ScalableProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScalableProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = getMeasuredWidth();
        setMeasuredDimension(size, size);
        int finishedStrokeWidth = size/18;
        int unfinishedStrokeWidth = finishedStrokeWidth/3;
        setFinishedStrokeWidth(finishedStrokeWidth);
        setUnfinishedStrokeWidth(unfinishedStrokeWidth);
        setTextSize(size/5);
    }
}
