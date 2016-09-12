package li.doerf.hacked.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Created by moo on 12/09/16.
 */
public class AppVersion {
    private static final String LOGTAG = "AppVersion";

    public static String getAppVersion( Context aContext) {
        try {
            PackageInfo packageInfo = aContext.getPackageManager().getPackageInfo(aContext.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(LOGTAG, "exception while retrieving version", e);
        }
        return "Unknown";
    }
}
