package li.doerf.hacked.ui;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import li.doerf.hacked.R;
import li.doerf.hacked.utils.RatingHelper;

public class RateUsDialogFragment extends DialogFragment {

    private final String LOGTAG = getClass().getSimpleName();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.rating_dialog_title))
                .setMessage(getString(R.string.rating_dialog_message))
                .setPositiveButton(getString(R.string.rating_dialog_positive), (dialog, which) -> handleClickPositive())
                .setNeutralButton(getString(R.string.rating_dialog_neutral), (dialog, which) -> handleClickNeutral())
                .setNegativeButton(getString(R.string.rating_dialog_negative), (dialog, which) -> handleClickNegative()).create();
    }

    private void handleClickPositive() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(
                Uri.parse(
                "https://play.google.com/store/apps/details?id=li.doerf.hacked"));
        intent.setPackage("com.android.vending");
        startActivity(intent);
        saveSettingRated();
    }

    private void saveSettingRated() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(RatingHelper.PREF_KEY_HAS_RATED_US, true);
        editor.apply();
        Log.i(LOGTAG, "setting: rated us - true");
    }

    private void handleClickNeutral() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(RatingHelper.PREF_KEY_RATING_COUNTER, 0);
        editor.apply();
        Log.i(LOGTAG, "setting: reset rating counter");
    }

    private void handleClickNegative() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(RatingHelper.PREF_KEY_RATING_NEVER, true);
        editor.apply();
        Log.i(LOGTAG, "setting: never rate");
    }
}
