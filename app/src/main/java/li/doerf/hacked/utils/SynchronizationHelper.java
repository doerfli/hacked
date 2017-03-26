package li.doerf.hacked.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import li.doerf.hacked.R;

/**
 * Created by moo on 08/09/16.
 */
public class SynchronizationHelper {
    private static final String LOGTAG = "SynchronizationHelper";

    public static void scheduleSync(Context aContext) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(aContext);
        disableSync(aContext);
        if (settings.getBoolean(aContext.getString(R.string.pref_key_sync_enable), false)) {
            enableSync(aContext);
        }
    }

    private static void enableSync(Context aContext) {
        Log.i(LOGTAG, "scheduling synchronization");
//        long interval = AlarmManager.INTERVAL_HALF_HOUR;
        long interval = 15000; // testing

        if ( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP ) {
            IScheduler scheduler = new LollipopScheduler(aContext);
            scheduler.scheduleCheckService(interval);
        } else{
            IScheduler scheduler = new JellyBeanScheduler(aContext);
            scheduler.scheduleCheckService(interval);
        }
    }

    private static void disableSync(Context aContext) {
        Log.i(LOGTAG, "unscheduling synchronization");
        if ( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP ) {
            IScheduler scheduler = new LollipopScheduler(aContext);
            scheduler.cancelCheckService();
        } else{
            IScheduler scheduler = new JellyBeanScheduler(aContext);
            scheduler.cancelCheckService();
        }
    }
}
