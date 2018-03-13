package li.doerf.hacked.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import li.doerf.hacked.HackedApplication;


/**
 * Checks and sets notification of reboot if required.
 *
 * Created by moo on 04/12/15.
 */
public class PackageReplacedReceiver extends BroadcastReceiver {
    private final String LOGTAG = getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
            Log.d(LOGTAG, "checking app status after ACTION_PACKAGE_REPLACED");
            ((HackedApplication) context.getApplicationContext()).migrateBackgroundCheckService();
            Log.d(LOGTAG, "blubb");
        }
    }
}
