package li.doerf.hacked.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import li.doerf.hacked.R;
import li.doerf.hacked.activities.NavActivity;
import li.doerf.hacked.remote.hibp.HIBPAccountResponseWorker;
import li.doerf.hacked.utils.NotificationHelper;
import li.doerf.hacked.utils.OreoNotificationHelper;

public class FirebaseMessagagingService extends FirebaseMessagingService {

    private static final String TAG = "FirebaseMessagagingServ";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "received message: " + remoteMessage.getData().toString());
        if (remoteMessage.getData().containsKey("type")) {
            if (remoteMessage.getData().get("type").equals("hibp-response")) {
                Log.d(TAG, "processing hibp-response");
                processHibpResponse(remoteMessage.getData().get("account"), remoteMessage.getData().get("response"));
            }
        } else if (remoteMessage.getNotification() != null) {
            showNotification(remoteMessage.getNotification());
        }
    }

    private void processHibpResponse(String account, String responseStr) {
        Log.d(TAG, "processHibpResponse for account: " + account);
        List<String> breaches = new ArrayList<>();
        if (responseStr != null && !responseStr.isEmpty()) {
            try {
                List<Map<String, Object>> response = new Gson().fromJson(responseStr, List.class);
                if (response != null) {
                    for (Map<String, Object> e : response) {
                        if (e != null && e.containsKey("Name")) {
                            breaches.add((String) e.get("Name"));
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "error parsing responseStr: " + responseStr, e);
            }
        }

        Data inputData = new Data.Builder()
                .putStringArray(HIBPAccountResponseWorker.KEY_BREACHES, breaches.toArray(new String[0]))
                .putString(HIBPAccountResponseWorker.KEY_ACCOUNT, account)
                .build();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest workerRequest = new OneTimeWorkRequest.Builder(HIBPAccountResponseWorker.class)
                .setInputData(inputData)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork("hibp-response", ExistingWorkPolicy.APPEND_OR_REPLACE, workerRequest);
        Log.d(TAG, "enqueued hibp-response worker for account: " + account);
    }

    private void showNotification(RemoteMessage.Notification remoteNotification) {
        if ( android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            OreoNotificationHelper onh = new OreoNotificationHelper(getApplicationContext());
            onh.createGeneralNotificationChannel();
        }

        androidx.core.app.NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext(), OreoNotificationHelper.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(remoteNotification.getTitle())
                        .setContentText(remoteNotification.getBody())
                        .setChannelId(OreoNotificationHelper.CHANNEL_ID_GENERAL)
                        .setOnlyAlertOnce(true)
                        .setAutoCancel(true);

        // TODO navigate directly to breaches
        Intent showBreachDetails = new Intent(getApplicationContext(), NavActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        getApplicationContext(),
                        0,
                        showBreachDetails,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        Notification notification = mBuilder.build();
        NotificationHelper.notify(getApplicationContext(), notification);
    }
}
