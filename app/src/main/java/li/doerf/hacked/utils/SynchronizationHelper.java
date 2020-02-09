package li.doerf.hacked.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import li.doerf.hacked.R;
import li.doerf.hacked.remote.haveibeenpwned.HIBPAccountCheckerWorker;

/**
 * Created by moo on 08/09/16.
 */
public class SynchronizationHelper {
    private static final String LOGTAG = "SynchronizationHelper";
    private static final String JOB_TAG = "hacked-background-check-job";

    public static void setupInitialSync(Context aContext) {
        boolean isScheduled = false;
        try {
            List<WorkInfo> workinfos = WorkManager.getInstance(aContext).getWorkInfosByTag(LOGTAG).get();
            for (WorkInfo wi : workinfos) {
                WorkInfo.State state = wi.getState();
                if( state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED) {
                    Log.d(LOGTAG, "found scheduled work");
                    isScheduled = true;
                }
            }
        } catch (ExecutionException e) {
            Log.e(LOGTAG, "caught ExecutionException while checking status", e);
        } catch (InterruptedException e) {
            Log.e(LOGTAG, "caught InterruptedException while checking status", e);
        }
        if (!isScheduled) {
            Log.i(LOGTAG, "no background work scheduled - need to schedule new background worker");
            scheduleSync(aContext);
        }
    }


    public static boolean scheduleSync(Context aContext) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(aContext);
        disableSync(aContext);
        boolean enabled = false;

        if (settings.getBoolean(aContext.getString(R.string.pref_key_sync_enable), true)) {
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

        PeriodicWorkRequest.Builder checkWorker =
                new PeriodicWorkRequest.Builder(HIBPAccountCheckerWorker.class, currentIntervalHours,
                        TimeUnit.HOURS)
                .addTag(JOB_TAG)
                .setConstraints(constraints);

        PeriodicWorkRequest photoCheckWork = checkWorker.build();
        WorkManager.getInstance(aContext).enqueueUniquePeriodicWork(JOB_TAG, ExistingPeriodicWorkPolicy.KEEP, photoCheckWork);

        Log.i(LOGTAG, "scheduled job");
    }

    private static void disableSync(Context aContext) {
        Log.d(LOGTAG, "unscheduling synchronization");
        WorkManager.getInstance(aContext).cancelAllWorkByTag(JOB_TAG);
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
