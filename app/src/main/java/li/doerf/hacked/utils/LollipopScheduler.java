package li.doerf.hacked.utils;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import li.doerf.hacked.services.LollipopCheckService;
import li.doerf.hacked.utils.IScheduler;

/**
 * Created by moo on 26.03.17.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LollipopScheduler implements IScheduler {
    private static final String LOGTAG = "LollipopScheduler";
    private static final int JOB_ID = 42;
    private final Context myContext;

    public LollipopScheduler(Context aContext) {
        myContext = aContext;
    }

    @Override
    public void scheduleCheckService(long interval) {
        Log.d(LOGTAG, "scheduling");
        ComponentName serviceComponent = new ComponentName(myContext, LollipopCheckService.class);

        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, serviceComponent)
            .setPeriodic(interval)
            .setPersisted(true);

        JobScheduler jobScheduler = (JobScheduler) myContext.getSystemService
                (Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());
        Log.i(LOGTAG, "scheduled service to run every " + interval + " ms");
    }

    @Override
    public void cancelCheckService() {
        JobScheduler jobScheduler = (JobScheduler) myContext.getSystemService
                (Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(JOB_ID);
        Log.i(LOGTAG, "unscheduled service");
    }
}
