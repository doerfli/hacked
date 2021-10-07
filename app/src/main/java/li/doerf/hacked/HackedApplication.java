package li.doerf.hacked;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDexApplication;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.concurrent.ExecutionException;

import io.reactivex.processors.PublishProcessor;
import li.doerf.hacked.util.Analytics;
import li.doerf.hacked.util.NavEvent;

/**
 * Created by moo on 25.05.17.
 */

public class HackedApplication extends MultiDexApplication implements LifecycleObserver, DefaultLifecycleObserver {
    private static final String TAG = "HackedApplication";
    private static String token;
    private PublishProcessor<NavEvent> navEvents = PublishProcessor.create();

    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        Log.d(TAG, "application opened");
        Analytics.Companion.getInstance().logEvent(FirebaseAnalytics.Event.APP_OPEN, new Bundle());
    }

    public PublishProcessor<NavEvent> getNavEvents() {
        return navEvents;
    }

    public String getDeviceToken() {
        if (token != null) {
            Log.d(TAG, "already got token:  " + token);
            return token;
        }

        Log.d(TAG, "retrieving fcm token");
        Task<String> taskTokenRetrieveTask = FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener((OnCompleteListener<String>) task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    token = task.getResult();
                });
        try {
            return Tasks.await(taskTokenRetrieveTask);
        } catch (ExecutionException e) {
            Log.e(TAG, "caught ExecutionException", e);
        } catch (InterruptedException e) {
            Log.e(TAG, "caught InterruptedException", e);
        }

        return null;
    }
}
