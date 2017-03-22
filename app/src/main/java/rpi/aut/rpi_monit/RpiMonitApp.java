package rpi.aut.rpi_monit;

import android.app.Application;
import android.content.Context;

public class RpiMonitApp extends Application {

    public static Context getAppContext(){
        return sAppContext;
    }

    private static Context sAppContext;

    @Override
    public void onCreate() {
        super.onCreate();
        sAppContext = this;
    }
}
