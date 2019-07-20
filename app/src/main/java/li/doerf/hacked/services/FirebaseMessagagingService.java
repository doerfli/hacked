package li.doerf.hacked.services;

import android.util.Log;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import li.doerf.hacked.HackedApplication;
import li.doerf.hacked.remote.haveibeenpwned.HIBPAccountResponseWorker;

public class FirebaseMessagagingService extends FirebaseMessagingService {

    private static final String TAG = "FirebaseMessagagingServ";

    @Override
    public void onNewToken(String token) {
        ((HackedApplication)getApplication()).setDeviceToken(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "received message: " + remoteMessage.getData().toString());
        if (remoteMessage.getData().containsKey("type")) {
            if (remoteMessage.getData().get("type").equals("hibp-response")) {
                Log.d(TAG, "processing hibp-response");
                processHibpResponse(remoteMessage.getData().get("account"), remoteMessage.getData().get("response"));
            }
        } else if (remoteMessage.getNotification() != null) {
            // TODO v3 show notification
        }
    }

    private void processHibpResponse(String account, String responseStr) {
        List<Map<String, Object>> response = new Gson().fromJson(responseStr, List.class);
        List<String> breaches = new ArrayList<>();
        for(Map<String, Object> e : response) {
            breaches.add((String) e.get("Name"));
        }

        Data inputData = new Data.Builder()
                .putStringArray(HIBPAccountResponseWorker.KEY_BREACHES, breaches.toArray(new String[]{}))
                .putString(HIBPAccountResponseWorker.KEY_ACCOUNT, account)
                .build();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest workerRequest = new OneTimeWorkRequest.Builder(HIBPAccountResponseWorker.class)
                .setInputData(inputData)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance().enqueue(workerRequest);
    }
}
