package li.doerf.hacked.ui.fragments;


import android.os.Bundle;
import android.preference.PreferenceFragment;

import li.doerf.hacked.R;


/**
 * Created by moo on 01/12/15.
 */
public class SettingsFragment extends PreferenceFragment {
    private final String LOGTAG = getClass().getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

}
