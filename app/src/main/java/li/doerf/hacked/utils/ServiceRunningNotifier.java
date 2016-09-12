package li.doerf.hacked.utils;

import android.util.Log;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by moo on 12/09/16.
 */
public class ServiceRunningNotifier {
    private static final String LOGTAG = "ServiceRunningNotifier";
    private static List<IServiceRunningListener> myServiceRunningListeners;

    static {
        myServiceRunningListeners = Lists.newArrayList();
    }

    public static void registerServiceRunningListener(IServiceRunningListener aListener) {
        myServiceRunningListeners.add(aListener);
//        Log.d(LOGTAG, "listener added. size " + myServiceRunningListeners.size());
    }

    public static void unregisterServiceRunningListener(IServiceRunningListener aListener) {
        myServiceRunningListeners.remove(aListener);
//        Log.d(LOGTAG, "listener removed. size " + myServiceRunningListeners.size());
    }

    public static void notifyServiceRunningListeners( IServiceRunningListener.Event anEvent) {
        Log.d(LOGTAG, "notifying listeners: " + anEvent + " count " + myServiceRunningListeners.size());
        for ( IServiceRunningListener l : myServiceRunningListeners) {
//            Log.d(LOGTAG, "notifying listener: " + l);
            l.notifyListener( anEvent);
        }
    }
}
