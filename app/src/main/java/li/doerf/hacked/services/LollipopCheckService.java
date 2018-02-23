package li.doerf.hacked.services;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import li.doerf.hacked.R;
import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.remote.haveibeenpwned.HIBPAccountChecker;
import li.doerf.hacked.remote.haveibeenpwned.IProgressUpdater;
import li.doerf.hacked.utils.ConnectivityHelper;

/**
 * Created by moo on 26.03.17.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LollipopCheckService extends JobService {
    private static final String LOGTAG = "LollipopCheckService";
    private final static String NOTIFICATION_GROUP_KEY_BREACHES = "group_key_breachs";
    private JobParameters myParams;
    HIBPAccountChecker checker;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(LOGTAG, "onStartJob");
        myParams = jobParameters;
        final CheckServiceHelper helper = new CheckServiceHelper(getApplicationContext());

        new Thread( new Runnable() {
            @Override
            public void run() {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                long lastSync = settings.getLong(getString(R.string.PREF_KEY_LAST_SYNC_TIMESTAMP), 0);
                int currentInterval = helper.getCurrentInterval(settings);

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
                    helper.showNotification();
                }

                jobFinished(myParams, false);
            }
        }).start();

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.i(LOGTAG, "onStopJob");
        if ( checker != null ) {
            checker.abort();
        }
        return false;
    }
}
