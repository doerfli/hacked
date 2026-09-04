package li.doerf.hacked.ui.viewmodels

import android.app.Application
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import li.doerf.hacked.CustomEvent
import li.doerf.hacked.R
import li.doerf.hacked.util.Analytics
import li.doerf.hacked.util.FcmTokenManager
import li.doerf.hacked.util.ThemeMode
import li.doerf.hacked.utils.SynchronizationHelper

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)

    private val keySyncEnable = application.getString(R.string.pref_key_sync_enable)
    private val keySyncCellular = application.getString(R.string.pref_key_sync_via_cellular)
    private val keySyncInterval = application.getString(R.string.pref_key_sync_interval)
    private val defaultInterval = application.getString(R.string.pref_sync_interval_default)

    private val _themeMode = MutableStateFlow(prefs.getString(ThemeMode.PREF_KEY, ThemeMode.SYSTEM) ?: ThemeMode.SYSTEM)
    val themeMode: StateFlow<String> = _themeMode

    private val _syncEnabled = MutableStateFlow(prefs.getBoolean(keySyncEnable, true))
    val syncEnabled: StateFlow<Boolean> = _syncEnabled

    private val _syncViaCellular = MutableStateFlow(prefs.getBoolean(keySyncCellular, false))
    val syncViaCellular: StateFlow<Boolean> = _syncViaCellular

    private val _syncInterval = MutableStateFlow(prefs.getString(keySyncInterval, defaultInterval) ?: defaultInterval)
    val syncInterval: StateFlow<String> = _syncInterval

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit { putString(ThemeMode.PREF_KEY, mode) }
            withContext(Dispatchers.Main) { ThemeMode.apply(getApplication()) }
        }
    }

    fun setSyncEnabled(enabled: Boolean) {
        _syncEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit { putBoolean(keySyncEnable, enabled) }
            onSyncPrefChanged()
        }
    }

    fun setSyncViaCellular(enabled: Boolean) {
        _syncViaCellular.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit { putBoolean(keySyncCellular, enabled) }
            onSyncPrefChanged()
        }
    }

    fun setSyncInterval(interval: String) {
        _syncInterval.value = interval
        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit { putString(keySyncInterval, interval) }
            onSyncPrefChanged()
        }
    }

    fun cleanToken(onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            FcmTokenManager.cleanToken(getApplication())
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    private fun onSyncPrefChanged() {
        val enabled = SynchronizationHelper.scheduleSync(getApplication<Application>().applicationContext)
        Analytics.trackCustomEvent(if (enabled) CustomEvent.BACKGROUND_SYNC_ENABLED else CustomEvent.BACKGROUND_SYNC_DISABLED)
    }
}
