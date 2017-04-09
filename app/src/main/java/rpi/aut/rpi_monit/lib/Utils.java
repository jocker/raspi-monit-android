package rpi.aut.rpi_monit.lib;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.v4.view.ViewCompat;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import rpi.aut.rpi_monit.BaseActivity;
import rpi.aut.rpi_monit.RpiMonitApp;

public class Utils {

    public static GsonBuilder newGsonBuilder(){
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .disableHtmlEscaping()

                .addSerializationExclusionStrategy(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                        final Expose expose = fieldAttributes.getAnnotation(Expose.class);
                        return expose != null && !expose.serialize();
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> aClass) {
                        return false;
                    }
                })
                .addDeserializationExclusionStrategy(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                        final Expose expose = fieldAttributes.getAnnotation(Expose.class);
                        return expose != null && !expose.deserialize();
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> aClass) {
                        return false;
                    }
                })
                .registerTypeAdapter(JsonElement.class, new JsonDeserializer<JsonElement>(){

                    @Override
                    public JsonElement deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        return json;
                    }
                })
                .registerTypeAdapter(JsonObject.class, new JsonDeserializer<JsonElement>(){

                    @Override
                    public JsonObject deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        return json != null && json.isJsonObject() ? json.getAsJsonObject() : null;
                    }
                });
    }


    private static volatile Gson sGson;

    public static Gson getDefaultJsonConverter(){
        if(sGson == null){
            synchronized (Utils.class){
                if(sGson == null){
                    sGson = newGsonBuilder().create();
                }
            }
        }
        return sGson;
    }

    public static String toJson(Object something){
        return getDefaultJsonConverter().toJson(something);
    }

    public static <T> T fromJson(String raw, Type typeOfT){
        return getDefaultJsonConverter().fromJson(raw, typeOfT);
    }


    public static Context getGlobalContext(){
        return RpiMonitApp.getAppContext();
    }

    public static <T> T getReferencedValue(WeakReference<T> ref){
        return ref == null ? null : ref.get();
    }

    public static int dpToPx(int dp){
        return toPx(TypedValue.COMPLEX_UNIT_DIP, dp);
    }

    public static int spToPx(int sp){
        return toPx(TypedValue.COMPLEX_UNIT_SP, sp);
    }

    public static int toPx(int typedValueUnit, int typedValue){
        return (int)(TypedValue.applyDimension(typedValueUnit, typedValue, Resources.getSystem().getDisplayMetrics()) +0.5);
    }

    private static final Rect TEMP_RECT = new Rect();
    public static Rect emptyRect(){
        TEMP_RECT.setEmpty();
        return TEMP_RECT;
    }

    private static final SparseArray<Point> sScreenSizes = new SparseArray<>();
    public static Point getWindowSize(){
        Context ctx = getGlobalContext();
        int orientation = ctx.getResources().getConfiguration().orientation;
        if(sScreenSizes.get(orientation) == null){
            Point p = new Point();
            ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(p);
            sScreenSizes.put(orientation, p);
        }
        return sScreenSizes.get(orientation);
    }

    public static boolean isViewVisibleOnScreen(View view){
        if(view == null){
            return false;
        }
        if(!ViewCompat.isAttachedToWindow(view)){
            return false;
        }
        if(!view.isShown()){
            return false;
        }
        Rect r = emptyRect();
        view.getGlobalVisibleRect(r);

        if(r.width() <=0 || r.height() <=0){
            return false;
        }
        Point windowSize = getWindowSize();
        if(r.left >= windowSize.x || r.right <=0 || r.bottom <=0 || r.top >= windowSize.y){
            return false;
        }
        return true;

    }

    private static AtomicInteger sNextGeneratedViewId;
    public static int generateViewId() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return View.generateViewId();
        }

        if(sNextGeneratedViewId == null){
            sNextGeneratedViewId = new AtomicInteger(1);
        }

        for (;;) { // this is the com.syncdb.library.internal implementation for api > 17
            final int result = sNextGeneratedViewId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedViewId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    public static int getViewId(View view){
        int id = view.getId();
        if(id == View.NO_ID){
            view.setId(generateViewId());
            return getViewId(view);
        }
        return id;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static void setViewBackground(View view, Drawable drawable){
        if(view == null){
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(drawable);
        } else {
            view.setBackgroundDrawable(drawable);
        }
    }

    public static BaseActivity getHostActivity(View view){
        if(view == null){
            return null;
        }
        Context context = view.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof BaseActivity) {
                return (BaseActivity)context;
            }
            context = ((ContextWrapper)context).getBaseContext();
        }
        return null;
    }

    public static int getDimension(@DimenRes int resId){
        return getGlobalContext().getResources().getDimensionPixelSize(resId);
    }

    public static int getColor(@ColorRes int colorResId){
        return getColor(getGlobalContext(), colorResId);
    }

    public static int getColor(Context context,  @ColorRes int colorResId){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            context.getResources().getColor(colorResId, null);
        }
        return context.getResources().getColor(colorResId);
    }

    public static String getCacheDir(){
        Context context = getGlobalContext();
        File dir = context.getExternalCacheDir();
        if(dir == null){
            dir = context.getCacheDir();
        }
        if(dir == null){
            dir = android.os.Environment.getExternalStorageDirectory();
        }
        if(dir != null){
            return dir.getPath();
        }
        return null;
    }

    public static boolean isAppDebuggable(){
        return hasApplicationFlag(ApplicationInfo.FLAG_DEBUGGABLE);
    }

    public static boolean hasApplicationFlag(int flag){
        return (getGlobalContext().getApplicationInfo().flags & flag) == flag;
    }

    public static boolean inMainThread(){
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    private static final Handler sMainThreadHandler = new Handler(Looper.getMainLooper());

    public static void runInMainThread(Runnable r){
        if(r != null){
            if(inMainThread()){
                r.run();
            }else{
                sMainThreadHandler.post(r);
            }
        }
    }

    public static void runInMainThread( long delayMs, Runnable r){
        if(r == null){
            return;
        }
        if(delayMs <= 0){
            runInMainThread(r);
            return;
        }
        sMainThreadHandler.postDelayed(r, delayMs);
    }

    public static void cancelMainThreadCallback(Runnable r){
        sMainThreadHandler.removeCallbacks(r);
    }

    public static String md5Digest(final String s) {

        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(s.getBytes(),0,s.length());
            return new BigInteger(1, m.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }


    public static boolean isAndroidEmulator(){
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    public static <T extends Parcelable> T cloneParcelable(T object, Parcelable.Creator<T> creator){
        Parcel p  = Parcel.obtain();
        try{
            object.writeToParcel(p, 0);
            p.setDataPosition(0);
            return creator.createFromParcel(p);
        }finally {
            p.recycle();
        }

    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static boolean isLollipop(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }


    public static int random(int lower, int upper){
        return ThreadLocalRandom.current().nextInt(lower, upper);
    }


    public static int getThemeColor(Context context, int themeResId){
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(themeResId, typedValue, true);
        return typedValue.data;
    }

    public static double mapValue(double src, final double srcMin, final double srcMax, final double destMin, final double destMax){
        if(src > srcMax){
            src = srcMax;
        }
        if(src < srcMin){
            src = srcMin;
        }

        return (src-srcMin)/(srcMax-srcMin)*(destMax-destMin)+destMin;
    }



}
