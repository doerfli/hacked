package li.doerf.hacked;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import li.doerf.hacked.utils.SynchronizationHelper;

/**
 * Created by moo on 25.05.17.
 */

public class HackedApplication extends Application {
    private static final String TAG = "HackedApplication";
    private static final String PREF_KEY_MIGRATE_BACKGROUND_SERVICE_TO_FIREBASE_SCHEDULER_DONE = "PREF_KEY_MIGRATE_BACKGROUND_SERVICE_TO_FIREBASE_SCHEDULER_DONE";
    private static GoogleAnalytics sAnalytics;
    private static Tracker sTracker;

    @Override
    public void onCreate() {
        super.onCreate();

        sAnalytics = GoogleAnalytics.getInstance(this);
        migrateBackgroundCheckService();
    }

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    private synchronized Tracker getDefaultTracker() {
        // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
        if (sTracker == null) {
            sTracker = sAnalytics.newTracker(R.xml.global_tracker);
        }

        return sTracker;
    }

    public synchronized void trackView(String name) {
        if ( runsInTestlab() ) return;
//        Log.i(LOGTAG, "Tracking view: " + name);
        getDefaultTracker().setScreenName(name);
        getDefaultTracker().send(new HitBuilders.ScreenViewBuilder().build());
    }

    public synchronized void trackEvent(String name) {
        if ( runsInTestlab() ) return;
//        Log.i(, "Tracking event: " + name);
        getDefaultTracker().send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction(name)
                .build());
    }

    private boolean runsInTestlab() {
        String testLabSetting = Settings.System.getString(getContentResolver(), "firebase.test.lab");
        return "true".equals(testLabSetting);
    }

    private void migrateBackgroundCheckService() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean done = settings.getBoolean(PREF_KEY_MIGRATE_BACKGROUND_SERVICE_TO_FIREBASE_SCHEDULER_DONE, false);

        if (done) {
            return;
        }

        Log.i(TAG, "migrating background check service to firebase");

        SynchronizationHelper.scheduleSync(getApplicationContext());

        // update preference
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREF_KEY_MIGRATE_BACKGROUND_SERVICE_TO_FIREBASE_SCHEDULER_DONE, true);
        editor.apply();
        Log.d(TAG, "done");
    }

}
