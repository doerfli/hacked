package li.doerf.hacked.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import li.doerf.hacked.R;

/**
 * Created by moo on 09/09/16.
 */
public class ConnectivityHelper {
    private static final String LOGTAG = "ConnectivityHelper";

    public static boolean isConnected( Context aContext) {
        ConnectivityManager cm =
                (ConnectivityManager) aContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    public static boolean isWifiNetwork( Context aContext) {
        ConnectivityManager cm =
                (ConnectivityManager) aContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if ( ! isConnected ) {
            return false;
        }

        return activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }

    /**
     * Checks if its currently allowed to access the network according to the settings.
     *
     * @param aContext
     *
     * @return
     */
    public static boolean isAllowedToAccessNetwork(Context aContext) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(aContext);
        boolean runCheckOnCellular = settings.getBoolean(aContext.getString(R.string.pref_key_sync_via_cellular), false);
        if ( ! runCheckOnCellular && ! ConnectivityHelper.isWifiNetwork( aContext)) {
            Log.d(LOGTAG, "no wifi available. try next time");
            return false;
        }
        return true;
    }
}
