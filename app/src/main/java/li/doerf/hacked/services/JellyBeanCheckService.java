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
    private final static String NOTIFICATION_GROUP_KEY_BREACHES = "group_key_breachs";

    public JellyBeanCheckService() {
        super("JellyBeanCheckService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOGTAG, "onHandleIntent");

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        long lastSync = settings.getLong(getString(R.string.PREF_KEY_LAST_SYNC_TIMESTAMP), 0);
        int currentInterval = getCurrentInterval(settings);

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
            showNotification();
        }
    }

    private void showNotification() {
        if ( AccountListFragment.isFragmentShown() ) {
            Log.d(LOGTAG, "AccountListFragment active, no notification shown");
            return;
        }

        String title = getApplicationContext().getString(R.string.notification_title_new_breaches_found);
        android.support.v4.app.NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(getApplicationContext().getString(R.string.notification_text_click_to_open))
                        .setGroup(NOTIFICATION_GROUP_KEY_BREACHES);

        Intent showBreachDetails = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        getApplicationContext(),
                        0,
                        showBreachDetails,
                        PendingIntent.FLAG_ONE_SHOT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        Notification notification = mBuilder.build();
        notification.flags |= Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;
        NotificationHelper.notify(getApplicationContext(), notification);
    }

    private int getCurrentInterval(SharedPreferences aSettings) {
        String intervalString = aSettings.getString(getString(R.string.pref_key_sync_interval), "everyday");

        switch ( intervalString) {
            case "everyday":
//                return 1000 * 60 * 60 * 24;
                return 1000 * 30; // for testing

            case "everytwodays":
                return 1000 * 60 * 60 * 24 * 2;

            case "everythreedays":
                return 1000 * 60 * 60 * 24 * 3;

            case "everyweek":
                return 1000 * 60 * 60 * 24 * 7;

            default:
                return 1000 * 60 * 60 * 24;
        }
    }
}
