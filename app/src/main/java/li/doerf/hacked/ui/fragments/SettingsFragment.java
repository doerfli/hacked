package li.doerf.hacked.ui.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;

import li.doerf.hacked.HackedApplication;
import li.doerf.hacked.R;
import li.doerf.hacked.utils.SynchronizationHelper;


/**
 * Created by moo on 01/12/15.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final String LOGTAG = getClass().getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        ((HackedApplication) getActivity().getApplication()).trackView("Fragment~Settings");
    }

    @Override
    public void onPause() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(LOGTAG, "preference changed: " + key);
        if ( getString(R.string.pref_key_sync_enable).equals( key) ||
                getString( R.string.pref_key_sync_interval).equals( key)
                ) {
            boolean enabled = SynchronizationHelper.scheduleSync(getActivity().getApplicationContext());

            if ( enabled) {
                ((HackedApplication) getActivity().getApplication()).trackEvent("BackgroundSyncEnable");
            } else {
                ((HackedApplication) getActivity().getApplication()).trackEvent("BackgroundSyncDisable");
            }
        }
    }

}
