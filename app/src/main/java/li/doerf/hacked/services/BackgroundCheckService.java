package li.doerf.hacked.services;

import android.util.Log;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.remote.haveibeenpwned.HIBPAccountChecker;
import li.doerf.hacked.remote.haveibeenpwned.IProgressUpdater;

/**
 * Created by moo on 08.03.18.
 */

public class BackgroundCheckService extends JobService {
    private static final String TAG = "BackgroundCheckService";

    @Override
    public boolean onStartJob(JobParameters job) {
        HIBPAccountChecker checker = new HIBPAccountChecker(getApplicationContext(), new IProgressUpdater() {
            @Override
            public void updateProgress(Account account) {
            Log.d(TAG, "checked account " + account.getName());
            // nothing to update when running in background
            }
        });

        Boolean foundNewBreaches = checker.check(null);
        if ( foundNewBreaches ) {
            CheckServiceHelper helper = new CheckServiceHelper(getApplicationContext());
            helper.showNotification();
        }

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }
}
