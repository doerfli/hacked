package li.doerf.hacked.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import li.doerf.hacked.utils.SynchronizationHelper;


/**
 * Checks and sets notification of reboot if required.
 *
 * Created by moo on 04/12/15.
 */
public class BootupCompleteReceiver extends BroadcastReceiver {
    private final String LOGTAG = getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(LOGTAG, "checking app status after ACTION_BOOT_COMPLETED");
            SynchronizationHelper.scheduleSync(context);
        } else if (intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
            Log.d(LOGTAG, "checking app status after ACTION_PACKAGE_REPLACED");
            SynchronizationHelper.scheduleSync(context);
        }

    }
}
