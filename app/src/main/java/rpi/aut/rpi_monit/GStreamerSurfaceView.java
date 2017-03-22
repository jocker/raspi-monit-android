package rpi.aut.rpi_monit;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

// A simple SurfaceView whose width and height can be set from the outside
public class GStreamerSurfaceView extends SurfaceView {
    private static final float ASPECT_HEIGHT = 9f, ASPECT_WIDTH = 16f;

    // Mandatory constructors, they do not do much
    public GStreamerSurfaceView(Context context, AttributeSet attrs,
                                int defStyle) {
        super(context, attrs, defStyle);
    }

    public GStreamerSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GStreamerSurfaceView (Context context) {
        super(context);
    }


    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec){
        int originalWidth = MeasureSpec.getSize(widthMeasureSpec);

        int originalHeight = MeasureSpec.getSize(heightMeasureSpec);

        int calculatedHeight = (int)(originalWidth * ASPECT_HEIGHT / ASPECT_WIDTH +0.5);

        int finalWidth, finalHeight;

        if (calculatedHeight > originalHeight){
            finalWidth = (int)(originalHeight * ASPECT_WIDTH / ASPECT_HEIGHT+0.5);
            finalHeight = originalHeight;
        }
        else{
            finalWidth = originalWidth;
            finalHeight = calculatedHeight;
        }

        super.onMeasure(
                MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY));
    }

}
