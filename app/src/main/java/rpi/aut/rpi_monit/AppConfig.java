package rpi.aut.rpi_monit;

import android.net.Uri;

public class AppConfig {

    private static final Uri sRemoteUri = Uri.parse("http://192.168.100.155:3000");

    public static Uri getRemoteUri(){
        return sRemoteUri;
    }
}
