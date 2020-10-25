package li.doerf.hacked.util

import android.app.Activity
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import li.doerf.hacked.CustomEvent
import li.doerf.hacked.HackedApplication
import li.doerf.hacked.ui.RateUsDialogFragment

class RatingHelper(private val activity: Activity) : AppReview {
    private suspend fun showRateUsDialog() {
        // since the show is delayed (and app could be closed now) this needs to be checked here
        if (!activity.isFinishing && !activity.isDestroyed) {
            val dialog = RateUsDialogFragment(this)
            withContext(Dispatchers.Main) {
                val fragmentManager = (activity as FragmentActivity).supportFragmentManager
                if (! fragmentManager.isDestroyed && !fragmentManager.isStateSaved) {
                    dialog.show(fragmentManager, "rateus")
                }
            }
        }
    }

    override fun showReview() {
        val manager = ReviewManagerFactory.create(activity.applicationContext)
//        val manager = FakeReviewManager(activity.applicationContext)
        val request = manager.requestReviewFlow()
        Log.d(LOGTAG, "requesting review flow")
        request.addOnCompleteListener { requestr ->
            if (requestr.isSuccessful) {
                Log.d(LOGTAG, "review flow request successful")
                // We got the ReviewInfo object
                val reviewInfo = requestr.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                Log.d(LOGTAG, "launching review flow")
                flow.addOnCompleteListener { _ ->
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown. Thus, no
                    // matter the result, we continue our app flow.
                    Log.i(LOGTAG, "review finished")
                    saveSettingRated()
                }
            } else {
                Log.e(LOGTAG, "unable to request review flow", request.exception)
            }
        }
    }

    private fun saveSettingRated() {
        val settings = PreferenceManager.getDefaultSharedPreferences(activity.applicationContext)
        val editor = settings.edit()
        editor.putBoolean(PREF_KEY_HAS_RATED_US, true)
        editor.apply()
        (activity.application as HackedApplication).trackCustomEvent(CustomEvent.RATE_NOW)
        Log.i(LOGTAG, "setting: rated us - true")
    }

    private fun showNoRatingDialog(): Boolean {
        if (hadConnectionFailureWithinLast10Days()) {
            Log.i(LOGTAG, "has had connection failure within last 10 days")
            return true
        }
        if (hasRated()) {
            Log.i(LOGTAG, "has already rated us")
            return true
        }
        if (hasNeverRating()) {
            Log.i(LOGTAG, "has chosen rating never")
            return true
        }
        return false
    }

    private fun hadConnectionFailureWithinLast10Days(): Boolean {
        val settings = PreferenceManager.getDefaultSharedPreferences(activity.applicationContext)
        val lastConnectionFailure = settings.getLong(PREF_KEY_LAST_ACCESS_DENIED_FAILURE, 0)
        val tenDays = 1000 * 60 * 60 * 24 * 10.toLong()
        return System.currentTimeMillis() < lastConnectionFailure + tenDays
    }

    suspend fun showRateUsDialogDelayed() {
        if (showNoRatingDialog()) return
        val settings = PreferenceManager.getDefaultSharedPreferences(activity.applicationContext)
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
        val settings = PreferenceManager.getDefaultSharedPreferences(activity.applicationContext)
        return settings.getBoolean(PREF_KEY_HAS_RATED_US, false)
    }

    private fun hasNeverRating(): Boolean {
        val settings = PreferenceManager.getDefaultSharedPreferences(activity.applicationContext)
        return settings.getBoolean(PREF_KEY_RATING_NEVER, false)
    }

    fun setRatingCounterBelowthreshold() {
        val settings = PreferenceManager.getDefaultSharedPreferences(activity.applicationContext)
        val editor = settings.edit()
        editor.putInt(PREF_KEY_RATING_COUNTER, RATING_DIALOG_COUNTER_THRESHOLD)
        editor.apply()
        Log.i(LOGTAG, "setting: rating counter to below threshold")
    }

    companion object {
        const val LOGTAG = "RatingHelper"
        const val PREF_KEY_HAS_RATED_US = "PREF_KEY_HAS_RATED_US"
        const val PREF_KEY_RATING_COUNTER = "PREF_KEY_RATING_COUNTER"
        const val PREF_KEY_RATING_NEVER = "PREF_KEY_RATING_NEVER"
        const val PREF_KEY_LAST_ACCESS_DENIED_FAILURE = "PREF_KEY_LAST_CONNECTION_FAILURE"
        const val RATING_DIALOG_COUNTER_THRESHOLD = 7
    }

}