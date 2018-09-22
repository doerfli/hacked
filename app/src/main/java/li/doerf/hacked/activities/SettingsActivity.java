package li.doerf.hacked.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import li.doerf.hacked.HackedApplication;
import li.doerf.hacked.ui.fragments.SettingsFragment;

/**
 * Created by moo on 01/12/15.
 */
public class SettingsActivity extends AppCompatActivity {
    private final String LOGTAG = getClass().getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HackedApplication application = (HackedApplication) getApplication();

        SettingsFragment settingsFragment = new SettingsFragment();
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
