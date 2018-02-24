package li.doerf.hacked;

import android.app.Application;
import android.provider.Settings;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by moo on 25.05.17.
 */

public class HackedApplication extends Application {

    private static GoogleAnalytics sAnalytics;
    private static Tracker sTracker;

    @Override
    public void onCreate() {
        super.onCreate();

        sAnalytics = GoogleAnalytics.getInstance(this);
    }

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */
    private synchronized Tracker getDefaultTracker() {
        // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
        if (sTracker == null) {
            sTracker = sAnalytics.newTracker(R.xml.global_tracker);
        }

        return sTracker;
    }

    public synchronized void trackView(String name) {
        if ( runsInTestlab() ) return;
//        Log.i(LOGTAG, "Tracking view: " + name);
        getDefaultTracker().setScreenName(name);
        getDefaultTracker().send(new HitBuilders.ScreenViewBuilder().build());
    }

    public synchronized void trackEvent(String name) {
        if ( runsInTestlab() ) return;
//        Log.i(, "Tracking event: " + name);
        getDefaultTracker().send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction(name)
                .build());
    }

    private boolean runsInTestlab() {
        String testLabSetting = Settings.System.getString(getContentResolver(), "firebase.test.lab");
        return "true" == testLabSetting;
    }

}
