package li.doerf.hacked.services;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import li.doerf.hacked.R;
import li.doerf.hacked.activities.MainActivity;
import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.remote.haveibeenpwned.HIBPAccountChecker;
import li.doerf.hacked.remote.haveibeenpwned.HIBPCheckAccountAsyncTask;
import li.doerf.hacked.remote.haveibeenpwned.IProgressUpdater;
import li.doerf.hacked.ui.fragments.AccountListFragment;
import li.doerf.hacked.utils.ConnectivityHelper;
import li.doerf.hacked.utils.NotificationHelper;

/**
 * Created by moo on 08/09/16.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class JellyBeanCheckService extends IntentService {
    private final String LOGTAG = getClass().getSimpleName();
    private CheckServiceHelper myHelper;

    public JellyBeanCheckService() {
        super("JellyBeanCheckService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        myHelper = new CheckServiceHelper(getApplicationContext());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOGTAG, "onHandleIntent");

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        long lastSync = settings.getLong(getString(R.string.PREF_KEY_LAST_SYNC_TIMESTAMP), 0);
        int currentInterval = myHelper.getCurrentInterval(settings);

        // check if time has come to run the service
        if ( System.currentTimeMillis() < lastSync + currentInterval ) {
            return;
        }

        if ( ! ConnectivityHelper.isAllowedToAccessNetwork( getApplicationContext()) ) {
            return;
        }

        Log.i(LOGTAG, "check interval expired. run check now");

        HIBPAccountChecker checker = new HIBPAccountChecker(getApplicationContext(), new IProgressUpdater() {
            @Override
            public void updateProgress(Account account) {
                Log.d(LOGTAG, "checked account " + account.getName());
                // nothing to update when running in background
            }
        });

        Boolean foundNewBreaches = checker.check(null);

        SharedPreferences.Editor editor = settings.edit();
        long ts = System.currentTimeMillis();
        editor.putLong(getApplicationContext().getString(R.string.PREF_KEY_LAST_SYNC_TIMESTAMP), ts);
        editor.apply();
        Log.i(LOGTAG, "updated last checked timestamp: " + ts);

        if ( foundNewBreaches ) {
            myHelper.showNotification();
        }
    }
}
