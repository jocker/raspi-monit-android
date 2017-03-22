package rpi.aut.rpi_monit.lib;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatDrawableManager;

public class DrawableUtils {

    public static Drawable getTintedDrawable(Context context, @DrawableRes int resId, int tintColor){
        return setTint(getDrawable(context, resId), tintColor);
    }

    public static Drawable getDrawable(Context context, @DrawableRes int resId){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getResources().getDrawable(resId, context.getTheme());
        }
        return AppCompatDrawableManager.get().getDrawable(context, resId);
    }

    public static <T extends Drawable> Drawable setTint(T dr, int color){
        if(color != Color.TRANSPARENT && dr != null){
            Drawable res = dr.mutate();

            if(dr instanceof VectorDrawableCompat){
                ((VectorDrawableCompat)res).setTint(color);
            }else {
                DrawableCompat.setTint(res, color);
            }
            return res;
        }
        return dr;

    }

    public static <T extends Drawable> Drawable setTintList(T dr, ColorStateList colors, int defaultColor){
        if(colors != null && dr != null){
            if(dr instanceof VectorDrawableCompat){
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                    return setTint(dr, defaultColor);
                }

            }else {
                DrawableCompat.setTintList(dr, colors);
            }
        }
        return dr;

    }

    public static Drawable newRoundRectDrawable(float radius, int strokeWidth, int color) {
        ShapeDrawable backgroundDrawable = new ShapeDrawable(new RoundRectShape(new float[]{radius, radius, radius, radius, radius, radius, radius, radius}, null, null));
        final Paint paint = backgroundDrawable.getPaint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        if(strokeWidth > 0){
            Drawable[] layers = {backgroundDrawable};
            LayerDrawable drawable = new LayerDrawable(layers);
            int halfStrokeWidth = (int) (strokeWidth / 2f);
            drawable.setLayerInset(0, halfStrokeWidth, halfStrokeWidth, halfStrokeWidth, halfStrokeWidth);
            return drawable;
        }
        return backgroundDrawable;
    }
}