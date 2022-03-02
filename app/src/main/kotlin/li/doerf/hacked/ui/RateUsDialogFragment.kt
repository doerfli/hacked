package li.doerf.hacked.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import li.doerf.hacked.CustomEvent
import li.doerf.hacked.R
import li.doerf.hacked.util.Analytics
import li.doerf.hacked.util.AppReview
import li.doerf.hacked.util.RatingHelper

class RateUsDialogFragment() : DialogFragment() {
    private val LOGTAG = javaClass.simpleName
    private lateinit var appReview: AppReview

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.rating_dialog_title))
                .setMessage(getString(R.string.rating_dialog_message))
                .setPositiveButton(getString(R.string.rating_dialog_positive)) { _: DialogInterface?, _: Int -> handleClickPositive() }
                .setNeutralButton(getString(R.string.rating_dialog_neutral)) { _: DialogInterface?, _: Int -> handleClickNeutral() }
                .setNegativeButton(getString(R.string.rating_dialog_negative)) { _: DialogInterface?, _: Int -> handleClickNegative() }.create()
    }

    fun setAppReview(appRevie: AppReview) {
        this.appReview = appRevie
    }

    private fun handleClickPositive() {
        if (this::appReview.isInitialized) {
            appReview.showReview()
        } else {
            handleClickNeutral();
        }
    }

    private fun handleClickNeutral() {
        val settings = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val editor = settings.edit()
        editor.putInt(RatingHelper.PREF_KEY_RATING_COUNTER, 0)
        editor.apply()
        Log.i(LOGTAG, "setting: reset rating counter")
        Analytics.trackCustomEvent(CustomEvent.RATE_LATER)
    }

    private fun handleClickNegative() {
        val settings = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val editor = settings.edit()
        editor.putBoolean(RatingHelper.PREF_KEY_RATING_NEVER, true)
        editor.apply()
        Log.i(LOGTAG, "setting: never rate")
        Analytics.trackCustomEvent(CustomEvent.RATE_NEVER)
    }
}