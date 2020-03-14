package li.doerf.hacked.util

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import li.doerf.hacked.ui.RateUsDialogFragment

class RatingHelper(private val context: Context) {
    private val LOGTAG = javaClass.simpleName

    private suspend fun showRateUsDialog() {
        withContext(Dispatchers.Main) {
            val dialog = RateUsDialogFragment()
            dialog.show((context as FragmentActivity).supportFragmentManager, "rateus")
        }
    }

    private fun showNoRatingDialog(): Boolean {
        if (hadConnectionFailureWithinLast10Days()) {
            Log.d(LOGTAG, "has had connection failure within last 10 days")
            return true
        }
        if (hasRated()) {
            Log.d(LOGTAG, "has already reated us")
            return true
        }
        if (hasNeverRating()) {
            Log.d(LOGTAG, "has chosen rating never")
            return true
        }
        return false
    }

    private fun hadConnectionFailureWithinLast10Days(): Boolean {
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        val lastConnectionFailure = settings.getLong(PREF_KEY_LAST_ACCESS_DENIED_FAILURE, 0)
        val tenDays = 1000 * 60 * 60 * 24 * 10.toLong()
        return System.currentTimeMillis() < lastConnectionFailure + tenDays
    }

    suspend fun showRateUsDialogDelayed() {
        if (showNoRatingDialog()) return
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        var ratingCount = settings.getInt(PREF_KEY_RATING_COUNTER, 0)
        if (ratingCount < RATING_DIALOG_COUNTER_THRESHOLD) {
            Log.d(LOGTAG, "counter below threshold. incrementing counter")
            val editor = settings.edit()
            editor.putInt(PREF_KEY_RATING_COUNTER, ++ratingCount)
            editor.apply()
            return
        }
        showRateUsDialog()
    }

    private fun hasRated(): Boolean {
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        return settings.getBoolean(PREF_KEY_HAS_RATED_US, false)
    }

    private fun hasNeverRating(): Boolean {
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        return settings.getBoolean(PREF_KEY_RATING_NEVER, false)
    }

    fun setRatingCounterBelowthreshold() {
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = settings.edit()
        editor.putInt(PREF_KEY_RATING_COUNTER, RATING_DIALOG_COUNTER_THRESHOLD)
        editor.apply()
        Log.i(LOGTAG, "setting: rating counter to below threshold")
    }

    companion object {
        const val PREF_KEY_HAS_RATED_US = "PREF_KEY_HAS_RATED_US"
        const val PREF_KEY_RATING_COUNTER = "PREF_KEY_RATING_COUNTER"
        const val PREF_KEY_RATING_NEVER = "PREF_KEY_RATING_NEVER"
        const val PREF_KEY_LAST_ACCESS_DENIED_FAILURE = "PREF_KEY_LAST_CONNECTION_FAILURE"
        const val RATING_DIALOG_COUNTER_THRESHOLD = 7
    }

}