package li.doerf.hacked.ui.fragments

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import li.doerf.hacked.BuildConfig
import li.doerf.hacked.CustomEvent
import li.doerf.hacked.R
import li.doerf.hacked.util.Analytics
import li.doerf.hacked.util.FcmTokenManager
import li.doerf.hacked.util.ThemeMode
import li.doerf.hacked.utils.SynchronizationHelper

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val logTag = javaClass.simpleName

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<Preference>("version")?.summary =
            String.format("%s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)

        findPreference<Preference>("device")?.summary =
            String.format("%s %s / API %s", Build.MANUFACTURER, Build.MODEL, Build.VERSION.SDK_INT)

        findPreference<Preference>("clean_token")?.setOnPreferenceClickListener {
            FcmTokenManager.cleanToken(requireContext())
            true
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        Analytics.trackView("Fragment~Settings")
    }

    override fun onPause() {
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        android.util.Log.d(logTag, "preference changed: $key")
        when (key) {
            getString(R.string.pref_key_sync_enable),
            getString(R.string.pref_key_sync_interval),
            getString(R.string.pref_key_sync_via_cellular) -> {
                val enabled = SynchronizationHelper.scheduleSync(requireActivity().applicationContext)
                if (enabled) {
                    Analytics.trackCustomEvent(CustomEvent.BACKGROUND_SYNC_ENABLED)
                } else {
                    Analytics.trackCustomEvent(CustomEvent.BACKGROUND_SYNC_DISABLED)
                }
            }
            ThemeMode.PREF_KEY -> ThemeMode.apply(requireActivity().applicationContext)
        }
    }
}
