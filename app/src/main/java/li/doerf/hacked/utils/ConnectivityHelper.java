package li.doerf.hacked.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by moo on 09/09/16.
 */
public class ConnectivityHelper {
    private final String LOGTAG = getClass().getSimpleName();

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
}
