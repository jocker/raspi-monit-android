package rpi.aut.rpi_monit.lib;

import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.ColorInt;

import java.util.Random;

public class ColorUtils {

    private static final Paint sFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    //factor between 0f and 1f
    public static int lighten(@ColorInt int argb, float factor){
        return adjustBrightness(argb, 1 + factor);
    }

    //factor between 0f and 1f
    public static int darken(@ColorInt int argb, float factor){
        return adjustBrightness(argb, 1 - factor);
    }


    public static int whiten(@ColorInt int argb, float factor){
        if(argb == Color.TRANSPARENT){
            return argb;
        }
        return android.support.v4.graphics.ColorUtils.compositeColors(ColorUtils.addTransparency(argb, 1 - factor), Color.WHITE);
    }

    public static int blend(@ColorInt int argb, float whitenFactor, int bledColor){
        return android.support.v4.graphics.ColorUtils.compositeColors(whiten(argb, whitenFactor), bledColor);
    }

    public static String printColor(@ColorInt int color){
        return String.format("#%X", color);
    }

    public static int adjustBrightness(@ColorInt int argb, float factor){
        float[] hsv = new float[3];
        Color.colorToHSV(argb, hsv);

        hsv[2] = Math.min(hsv[2] * factor, 1f);

        return Color.HSVToColor(Color.alpha(argb), hsv);
    }

    public static int addAlpha(@ColorInt int argb, float factor){
        if(argb == Color.TRANSPARENT){
            return argb;
        }
        return Color.argb(
                (int) (Color.alpha(argb) * factor + 0.5f),
                Color.red(argb),
                Color.green(argb),
                Color.blue(argb)
        );
    }

    public static int addTransparency(@ColorInt int argb, float factor){
        if(argb == Color.TRANSPARENT){
            return argb;
        }
        return Color.argb(
                (int) (255 * factor),
                Color.red(argb),
                Color.green(argb),
                Color.blue(argb)
        );
    }



    public static int random(){
        Random rnd = new Random();
        return Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
    }

    public static Paint randomPaint(){
        sFillPaint.setColor(random());
        return sFillPaint;
    }

    public static Paint getFillPaint(@ColorInt int color){
        sFillPaint.setColor(color);
        sFillPaint.setTypeface(null);
        sFillPaint.setStyle(Paint.Style.FILL);
        return sFillPaint;
    }
}