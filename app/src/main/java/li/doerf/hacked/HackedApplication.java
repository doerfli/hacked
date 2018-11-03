package li.doerf.hacked;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.List;

import androidx.multidex.MultiDexApplication;
import li.doerf.hacked.db.AppDatabase;
import li.doerf.hacked.db.daos.AccountDao;
import li.doerf.hacked.db.daos.BreachDao;
import li.doerf.hacked.db.entities.Account;
import li.doerf.hacked.db.entities.Breach;
import li.doerf.hacked.utils.BackgroundTaskHelper;
import li.doerf.hacked.utils.SynchronizationHelper;

/**
 * Created by moo on 25.05.17.
 */

public class HackedApplication extends MultiDexApplication {
    private static final String TAG = "HackedApplication";
    private static final String PREF_KEY_MIGRATE_BACKGROUND_SERVICE_TO_FIREBASE_SCHEDULER_DONE = "PREF_KEY_MIGRATE_BACKGROUND_SERVICE_TO_FIREBASE_SCHEDULER_DONE";
    private static GoogleAnalytics sAnalytics;
    private static Tracker sTracker;

    @Override
    public void onCreate() {
        super.onCreate();

        sAnalytics = GoogleAnalytics.getInstance(this);
        migrateBackgroundCheckService();
        migrateNumBreaches();
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

    public void migrateBackgroundCheckService() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean done = settings.getBoolean(PREF_KEY_MIGRATE_BACKGROUND_SERVICE_TO_FIREBASE_SCHEDULER_DONE, false);

        if (done) {
            return;
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(getString(R.string.pref_key_sync_enable), true);
        editor.apply();

        Log.i(TAG, "migrating background check service to firebase");

        SynchronizationHelper.scheduleSync(getApplicationContext());

        // update preference
        editor.putBoolean(PREF_KEY_MIGRATE_BACKGROUND_SERVICE_TO_FIREBASE_SCHEDULER_DONE, true);
        editor.apply();
        Log.d(TAG, "done");
    }

    private void migrateNumBreaches() {
        Log.i(TAG, "checking if accounts with numBreaches null must be migrated");
        AccountDao accountDao = AppDatabase.get(getApplicationContext()).getAccountDao();
        BreachDao breachDao = AppDatabase.get(getApplicationContext()).getBreachDao();

        new BackgroundTaskHelper<Boolean>().runInBackgroundAndConsumeOnMain(
                () -> {
                    List<Account> accounts = accountDao.getAllWithNumBreachesNull();
                    Log.d(TAG, accounts.size() + " accounts to migrate");

                    for (Account account : accounts) {
                        List<Breach> breaches = breachDao.findByAccount(account.getId());
                        Long numUnAck = breachDao.countUnacknowledged(account.getId());
                        Long numAck = breaches.size() - numUnAck;
                        account.setNumBreaches(breaches.size());
                        account.setNumAcknowledgedBreaches(numAck.intValue());
                        accountDao.update(account);
                        Log.i(TAG, "updated account: numBreaches=" + account.getNumBreaches() + " numAcknowledgedBreaches=" + account.getNumAcknowledgedBreaches());
                    }

                    return true;
                },
                (b) -> {
                    // do nothing
                });
    }

}
