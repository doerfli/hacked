package li.doerf.hacked.services;

import android.util.Log;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

/**
 * Created by moo on 08.03.18.
 */

public class BackgroundCheckService extends JobService {
    private static final String TAG = "BackgroundCheckService";

    @Override
    public boolean onStartJob(final JobParameters job) {
        Log.d(TAG, "bg job start");

        // TODO make this work again
//        Runnable runnable = () -> {
//            try {
//                HIBPAccountCheckerWorker checker = new HIBPAccountCheckerWorker(getApplicationContext(), account -> {
//                    Log.d(TAG, "checked account " + account.getName());
//                    // nothing to update when running in background
//                });
//
//                Boolean foundNewBreaches = checker.check(null);
//                if (foundNewBreaches) {
//                    CheckServiceHelper helper = new CheckServiceHelper(getApplicationContext());
//                    helper.showNotification();
//                }
//            } finally {
//                jobFinished(job, false);
//            }
//        };
//        new Thread(runnable).start();

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }
}
