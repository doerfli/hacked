package li.doerf.hacked;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDexApplication;

import com.google.android.gms.security.ProviderInstaller;
import com.google.firebase.analytics.FirebaseAnalytics;

import li.doerf.hacked.utils.SynchronizationHelper;

/**
 * Created by moo on 25.05.17.
 */

public class HackedApplication extends MultiDexApplication implements LifecycleObserver, DefaultLifecycleObserver {
    private static final String TAG = "HackedApplication";
    private FirebaseAnalytics firebaseAnalytics;

    @Override
    public void onCreate() {
        super.onCreate();
        enableTls12ForAndroidApi19AndBelow();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }

    private void enableTls12ForAndroidApi19AndBelow() {
        ProviderInstaller.installIfNeededAsync(this, new ProviderInstaller.ProviderInstallListener() {
            @Override
            public void onProviderInstalled() {
            }
            @Override
            public void onProviderInstallFailed(int i, Intent intent) {
                Log.i(TAG, "Provider install failed (" + i + ") : SSL Problems may occurs");
            }
        });
    }

    public synchronized void trackView(String name) {
        if ( runsInTestlab() ) return;
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, name);
        bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "View");
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle);
    }

    public synchronized void trackCustomEvent(CustomEvent eventName) {
        if ( runsInTestlab() ) return;
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Function");
        firebaseAnalytics.logEvent(eventName.name(), bundle);
    }

    private boolean runsInTestlab() {
        String testLabSetting = Settings.System.getString(getContentResolver(), "firebase.test.lab");
        return "true".equals(testLabSetting);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        Log.d(TAG, "application opened");
        Bundle bundle = new Bundle();
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle);
        scheduleBackgroundSync();
    }

    private void scheduleBackgroundSync() {
        SynchronizationHelper.setupInitialSync(getApplicationContext());
    }

}
