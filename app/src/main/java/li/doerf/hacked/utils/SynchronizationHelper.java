package li.doerf.hacked.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

import li.doerf.hacked.R;
import li.doerf.hacked.services.BackgroundCheckService;

/**
 * Created by moo on 08/09/16.
 */
public class SynchronizationHelper {
    private static final String LOGTAG = "SynchronizationHelper";
    public static final String JOB_TAG = "hacked-background-check-job";

    public static boolean scheduleSync(Context aContext) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(aContext);
        disableSync(aContext);
        boolean enabled = false;

        if (settings.getBoolean(aContext.getString(R.string.pref_key_sync_enable), false)) {
            enableSync(aContext);
            enabled = true;
        }

        return enabled;
    }

    private static void enableSync(Context aContext) {
        Log.d(LOGTAG, "scheduling synchronization");

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(aContext);
        int currentInterval = getCurrentInterval(aContext, settings);

        // Create a new dispatcher using the Google Play driver.
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(aContext));

        Job.Builder jb = dispatcher.newJobBuilder()
                .setService(BackgroundCheckService.class)
                .setTag(JOB_TAG)
                .setRecurring(true)
                .setLifetime(Lifetime.FOREVER)
                .setTrigger(Trigger.executionWindow(currentInterval, currentInterval + 60 * 60))
                .setReplaceCurrent(true)
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL);

        boolean runCheckOnCellular = settings.getBoolean(aContext.getString(R.string.pref_key_sync_via_cellular), false);

        if ( runCheckOnCellular ) {
            Log.d(LOGTAG, "schedule for any network availability");
            jb = jb.setConstraints(Constraint.ON_ANY_NETWORK);
        } else {
            Log.d(LOGTAG, "schedule for unmetered network availability");
            jb = jb.setConstraints(Constraint.ON_UNMETERED_NETWORK);
        }

        Job myJob = jb.build();

        dispatcher.mustSchedule(myJob);
        Log.i(LOGTAG, "scheduled job");
    }

    private static void disableSync(Context aContext) {
        Log.d(LOGTAG, "unscheduling synchronization");

        // Create a new dispatcher using the Google Play driver.
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(aContext));
        dispatcher.cancel(JOB_TAG);
        Log.i(LOGTAG, "unscheduled sync job");
    }

    private static int getCurrentInterval(Context aContext, SharedPreferences aSettings) {
        String intervalString = aSettings.getString(aContext.getString(R.string.pref_key_sync_interval), "everyday");

        switch ( intervalString) {
            case "everyday":
                return 60 * 60 * 24;
//                return 1000 * 30; // for testing

            case "everytwodays":
                return 60 * 60 * 24 * 2;

            case "everythreedays":
                return 60 * 60 * 24 * 3;

            case "everyweek":
                return 60 * 60 * 24 * 7;

            default:
                return 60 * 60 * 24;
        }
    }

}
