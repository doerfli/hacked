package li.doerf.hacked.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;
import li.doerf.hacked.ui.RateUsDialogFragment;

public class RatingHelper {
    public static final String PREF_KEY_HAS_RATED_US = "PREF_KEY_HAS_RATED_US";
    public static final String PREF_KEY_RATING_COUNTER = "PREF_KEY_RATING_COUNTER";
    public static final String PREF_KEY_RATING_NEVER = "PREF_KEY_RATING_NEVER";
    public static final int RATING_DIALOG_COUNTER_THRESHOLD = 7;
    private final String LOGTAG = getClass().getSimpleName();
    private final Context myContext;

    public RatingHelper(Context context) {
        myContext = context;
    }

    public void showRateUsDialog() {
        RateUsDialogFragment dialog = new RateUsDialogFragment();
        dialog.show(((FragmentActivity) myContext).getSupportFragmentManager(), "rateus");
    }

    private boolean showNoRatingDialog() {
        if (hasRated()) {
            Log.d(LOGTAG, "has already reated us");
            return true;
        }
        if (hasNeverRating()) {
            Log.d(LOGTAG, "has chosen rating never");
            return true;
        }

        return false;
    }

    public void showRateUsDialogDelayed() {
        if (showNoRatingDialog()) return;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(myContext);
        int ratingCount = settings.getInt(PREF_KEY_RATING_COUNTER, 0);
        if (ratingCount < RATING_DIALOG_COUNTER_THRESHOLD) {
            Log.d(LOGTAG, "counter below threshold. incrementing counter");
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(PREF_KEY_RATING_COUNTER, ++ratingCount);
            editor.apply();
            return;
        }

        showRateUsDialog();
    }


    private boolean hasRated() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(myContext);
        return settings.getBoolean(PREF_KEY_HAS_RATED_US, false);
    }

    private boolean hasNeverRating() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(myContext);
        return settings.getBoolean(PREF_KEY_RATING_NEVER, false);
    }

    public void setRatingCounterBelowthreshold() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(myContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(RatingHelper.PREF_KEY_RATING_COUNTER, RATING_DIALOG_COUNTER_THRESHOLD);
        editor.apply();
        Log.i(LOGTAG, "setting: rating counter to below threshold");
    }

}
