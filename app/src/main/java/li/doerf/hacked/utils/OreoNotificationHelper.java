package li.doerf.hacked.utils;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import li.doerf.hacked.R;

/**
 * Created by moo on 08.10.17.
 */
@TargetApi(Build.VERSION_CODES.O)
public class OreoNotificationHelper {

    private final Context myContext;
    public static final String CHANNEL_ID = "my_channel_01";
    public static final String CHANNEL_ID_GENERAL = "my_channel_02";

    public OreoNotificationHelper(Context aContext) {
        myContext = aContext;
    }

    public void createNotificationChannel() {
        NotificationManager mNotificationManager =
                (NotificationManager) myContext.getSystemService(Context.NOTIFICATION_SERVICE);
// The id of the channel.
// The user-visible name of the channel.
        CharSequence name = myContext.getString(R.string.channel_name);
// The user-visible description of the channel.
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
// Configure the notification channel.
//        mChannel.setDescription(description);
        mChannel.enableLights(true);
// Sets the notification light color for notifications posted to this
// channel, if the device supports this feature.
        mChannel.setLightColor(Color.RED);
        if (mNotificationManager == null ) { return; }
        mNotificationManager.createNotificationChannel(mChannel);
    }

    public void createGeneralNotificationChannel() {
        NotificationManager mNotificationManager =
                (NotificationManager) myContext.getSystemService(Context.NOTIFICATION_SERVICE);
// The id of the channel.
// The user-visible name of the channel.
        CharSequence name = myContext.getString(R.string.channel_name_general);
// The user-visible description of the channel.
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID_GENERAL, name, importance);
// Configure the notification channel.
//        mChannel.setDescription(description);
        mChannel.enableLights(true);
// Sets the notification light color for notifications posted to this
// channel, if the device supports this feature.
        mChannel.setLightColor(Color.RED);
        if (mNotificationManager == null ) { return; }
        mNotificationManager.createNotificationChannel(mChannel);
    }
}
