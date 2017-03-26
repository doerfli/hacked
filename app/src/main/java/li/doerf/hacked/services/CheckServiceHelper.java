package li.doerf.hacked.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import li.doerf.hacked.R;
import li.doerf.hacked.activities.MainActivity;
import li.doerf.hacked.ui.fragments.AccountListFragment;
import li.doerf.hacked.utils.NotificationHelper;

/**
 * Created by moo on 26.03.17.
 */

public class CheckServiceHelper {
//    private static final String LOGTAG = "CheckServiceHelper";
    private static final String NOTIFICATION_GROUP_KEY_BREACHES = "group_key_breachs";
    private final Context myContext;

    CheckServiceHelper(Context aContext) {
        myContext = aContext;
    }

    void showNotification() {
        String title = myContext.getString(R.string.notification_title_new_breaches_found);
        android.support.v4.app.NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(myContext)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(myContext.getString(R.string.notification_text_click_to_open))
                        .setGroup(NOTIFICATION_GROUP_KEY_BREACHES);

        Intent showBreachDetails = new Intent(myContext, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        myContext,
                        0,
                        showBreachDetails,
                        PendingIntent.FLAG_ONE_SHOT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        Notification notification = mBuilder.build();
        notification.flags |= Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;
        NotificationHelper.notify(myContext, notification);
    }

    int getCurrentInterval(SharedPreferences aSettings) {
        String intervalString = aSettings.getString(myContext.getString(R.string.pref_key_sync_interval), "everyday");

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
