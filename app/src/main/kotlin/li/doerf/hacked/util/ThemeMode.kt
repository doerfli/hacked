package li.doerf.hacked.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager

object ThemeMode {
    const val PREF_KEY = "PREF_KEY_THEME_MODE"
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"

    @JvmStatic
    fun apply(context: Context) {
        val mode = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_KEY, SYSTEM)
        AppCompatDelegate.setDefaultNightMode(toNightMode(mode))
    }

    private fun toNightMode(mode: String?): Int = when (mode) {
        LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        DARK -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
}
