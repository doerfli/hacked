package li.doerf.hacked;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDexApplication;

import com.google.android.gms.security.ProviderInstaller;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.List;

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

public class HackedApplication extends MultiDexApplication implements LifecycleObserver, DefaultLifecycleObserver {
    private static final String TAG = "HackedApplication";
    private static final String PREF_KEY_MIGRATE_BACKGROUND_SERVICE_TO_WORKMANAGER_DONE = "PREF_KEY_MIGRATE_BACKGROUND_SERVICE_TO_WORKMANAGER_DONE";
    private FirebaseAnalytics firebaseAnalytics;

    @Override
    public void onCreate() {
        super.onCreate();
        enableTls12ForAndroidApi19AndBelow();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        migrateBackgroundCheckService();
        migrateNumBreaches();
    }

    private void enableTls12ForAndroidApi19AndBelow() {
        ProviderInstaller.installIfNeededAsync(this, new ProviderInstaller.ProviderInstallListener() {
            @Override
            public void onProviderInstalled() {
            }
            @Override
            public void onProviderInstallFailed(int i, Intent intent) {
                Log.i(TAG, "Provider install failed (" + i + ") : SSL Problems may occurs");
            }
        });
    }

    public synchronized void trackView(String name) {
        if ( runsInTestlab() ) return;
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, name);
        bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "View");
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle);
    }

    public synchronized void trackCustomEvent(CustomEvent eventName) {
        if ( runsInTestlab() ) return;
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Function");
        firebaseAnalytics.logEvent(eventName.name(), bundle);
    }

    private boolean runsInTestlab() {
        String testLabSetting = Settings.System.getString(getContentResolver(), "firebase.test.lab");
        return "true".equals(testLabSetting);
    }

    public void migrateBackgroundCheckService() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean done = settings.getBoolean(PREF_KEY_MIGRATE_BACKGROUND_SERVICE_TO_WORKMANAGER_DONE, false);

        if (done) {
            return;
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(getString(R.string.pref_key_sync_enable), true);
        editor.apply();

        Log.i(TAG, "migrating background check service to workmanager");

        SynchronizationHelper.scheduleSync(getApplicationContext());

        // update preference
        editor.putBoolean(PREF_KEY_MIGRATE_BACKGROUND_SERVICE_TO_WORKMANAGER_DONE, true);
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

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        Log.d(TAG, "application opened");
        Bundle bundle = new Bundle();
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle);
    }

}
