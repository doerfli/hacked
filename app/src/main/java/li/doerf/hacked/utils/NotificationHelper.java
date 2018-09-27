package li.doerf.hacked.utils;

import android.app.Notification;
import android.content.Context;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

import androidx.core.app.NotificationManagerCompat;

/**
 * Created by moo on 04/12/15.
 */
public class NotificationHelper {
    private final static String LOGTAG = "NotificationHelper";
    private final static AtomicInteger notifyId = new AtomicInteger();

    private static int getNotificationId() {
        return notifyId.incrementAndGet();
    }

    public static void notify(Context aContext, Notification aNotification) {
        // Sets an ID for the notification
        int notificationId = NotificationHelper.getNotificationId();
        notify(aContext, aNotification, notificationId);
    }

    private static void notify(Context aContext, Notification aNotification, int aNotificationId) {
        // Gets an instance of the NotificationManager service
//        NotificationManager notificationManager =
//                (NotificationManager) aContext.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(aContext);
        // Builds the notification and issues it.
        notificationManager.notify(aNotificationId, aNotification);
        Log.d(LOGTAG, "notification build and issued: " + aNotificationId);
    }

    public static void cancelAll(Context aContext) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(aContext);
        notificationManager.cancelAll();
    }
}
