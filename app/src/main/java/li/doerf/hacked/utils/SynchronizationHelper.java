package li.doerf.hacked.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import li.doerf.hacked.HackedApplication;
import li.doerf.hacked.R;
import li.doerf.hacked.remote.haveibeenpwned.HIBPAccountCheckerWorker;

/**
 * Created by moo on 08/09/16.
 */
public class SynchronizationHelper {
    private static final String LOGTAG = "SynchronizationHelper";
    private static final String JOB_TAG = "hacked-background-check-job";

    public static boolean scheduleSync(Context aContext) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(aContext);
        disableSync();
        boolean enabled = false;

        if (settings.getBoolean(aContext.getString(R.string.pref_key_sync_enable), false)) {
            enableSync(aContext);
            enabled = true;
        }

        return enabled;
    }

    private static void enableSync(Context aContext) {
        Log.d(LOGTAG, "scheduling synchronization");

        int currentIntervalHours = getCurrentIntervalHours(aContext);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(aContext);
        boolean runCheckOnCellular = settings.getBoolean(aContext.getString(R.string.pref_key_sync_via_cellular), false);
        NetworkType networkType = runCheckOnCellular ? NetworkType.METERED : NetworkType.UNMETERED;

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build();

        Data inputData = new Data.Builder()
                .putString(HIBPAccountCheckerWorker.KEY_DEVICE_TOKEN, ((HackedApplication) aContext.getApplicationContext()).getDeviceToken())
                .build();

        PeriodicWorkRequest.Builder checkWorker =
                new PeriodicWorkRequest.Builder(HIBPAccountCheckerWorker.class, currentIntervalHours,
                        TimeUnit.HOURS)
                .addTag(JOB_TAG)
                .setInputData(inputData)
                .setConstraints(constraints);

        PeriodicWorkRequest photoCheckWork = checkWorker.build();
        WorkManager.getInstance().enqueue(photoCheckWork);

        Log.i(LOGTAG, "scheduled job");
    }

    private static void disableSync() {
        Log.d(LOGTAG, "unscheduling synchronization");
        WorkManager.getInstance().cancelAllWorkByTag(JOB_TAG);
        Log.i(LOGTAG, "unscheduled sync job");
    }

    /**
     * Get the current check interval in hours
     * @param aContext the context
     * @return the current check interval in hours
     */
    private static int getCurrentIntervalHours(Context aContext) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(aContext);
        String intervalString = settings.getString(aContext.getString(R.string.pref_key_sync_interval), "everyday");

        switch ( intervalString) {
            case "everyday":
                return 24;

            case "everytwodays":
                return 24 * 2;

            case "everythreedays":
                return 24 * 3;

            case "everyweek":
                return 24 * 7;

            default:
                return 24;
        }
    }

}
