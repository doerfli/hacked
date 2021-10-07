package li.doerf.hacked;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDexApplication;

import com.google.firebase.analytics.FirebaseAnalytics;

import io.reactivex.processors.PublishProcessor;
import li.doerf.hacked.util.Analytics;
import li.doerf.hacked.util.NavEvent;

/**
 * Created by moo on 25.05.17.
 */

public class HackedApplication extends MultiDexApplication implements LifecycleObserver, DefaultLifecycleObserver {
    private static final String TAG = "HackedApplication";
    private final PublishProcessor<NavEvent> navEvents = PublishProcessor.create();

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


}
