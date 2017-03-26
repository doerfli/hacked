package li.doerf.hacked.utils;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import li.doerf.hacked.services.JellyBeanCheckService;
import li.doerf.hacked.utils.IScheduler;

/**
 * Created by moo on 26.03.17.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class JellyBeanScheduler implements IScheduler {
    private static final String LOGTAG = "JellyBeanScheduler";
    private final Context myContext;

    public JellyBeanScheduler(Context aContext) {
        myContext = aContext;
    }

    @Override
    public void scheduleCheckService(long interval) {
        Log.d(LOGTAG, "scheduling");
        PendingIntent checkerService = getPendingIntent(myContext);

        AlarmManager alarmManager = (AlarmManager) myContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                0,
                interval,
                checkerService);
        Log.i(LOGTAG, "scheduled service to run every " + interval + " ms");
    }

    @Override
    public void cancelCheckService() {
        AlarmManager alarmManager = (AlarmManager) myContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getPendingIntent(myContext));
        Log.i(LOGTAG, "cancelled pending intent");
    }

    private static PendingIntent getPendingIntent(Context aContext) {
        Intent i = new Intent(aContext, JellyBeanCheckService.class);
        return PendingIntent.getService(aContext, 0, i, 0);
    }
}
