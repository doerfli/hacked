package li.doerf.hacked.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.analytics.Tracker;

import li.doerf.hacked.HackedApplication;
import li.doerf.hacked.ui.fragments.SettingsFragment;

/**
 * Created by moo on 01/12/15.
 */
public class SettingsActivity extends AppCompatActivity {
    private final String LOGTAG = getClass().getSimpleName();
    private Tracker myTracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HackedApplication application = (HackedApplication) getApplication();
        myTracker = application.getDefaultTracker();

        SettingsFragment settingsFragment = new SettingsFragment();
        settingsFragment.setTracker(myTracker);
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, settingsFragment)
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();


    }
}
