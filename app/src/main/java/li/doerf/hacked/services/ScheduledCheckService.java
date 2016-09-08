package li.doerf.hacked.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import li.doerf.hacked.R;
import li.doerf.hacked.utils.ConnectivityHelper;

/**
 * Created by moo on 08/09/16.
 */
public class ScheduledCheckService extends IntentService {
    private final String LOGTAG = getClass().getSimpleName();


    public ScheduledCheckService() {
        super("ScheduledCheckService");
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

        boolean runCheckOnCellular = settings.getBoolean(getString(R.string.pref_key_sync_via_cellular), false);
        if ( ! runCheckOnCellular && ! ConnectivityHelper.isWifiNetwork( getApplicationContext())) {
            Log.d(LOGTAG, "no wifi available. try next time");
            return;
        }

        Log.i(LOGTAG, "check interval expired. run check now");
        Intent i = new Intent(this, HaveIBeenPwnedCheckService.class);
        startService(i);

        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(getString(R.string.PREF_KEY_LAST_SYNC_TIMESTAMP), System.currentTimeMillis());
        editor.apply();
    }

    private int getCurrentInterval(SharedPreferences aSettings) {
        String intervalString = aSettings.getString(getString(R.string.pref_key_sync_interval), "everyday");

        switch ( intervalString) {
            case "everyday":
                return 1000 * 60 * 60 * 24;
//                return 1000 * 30; // for testing

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
