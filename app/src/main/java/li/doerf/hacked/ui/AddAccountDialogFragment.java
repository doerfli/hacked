package li.doerf.hacked.ui;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import li.doerf.hacked.HackedApplication;
import li.doerf.hacked.R;
import li.doerf.hacked.db.AppDatabase;
import li.doerf.hacked.db.daos.AccountDao;
import li.doerf.hacked.db.entities.Account;
import li.doerf.hacked.remote.haveibeenpwned.HIBPAccountCheckerWorker;

import static android.content.ContentValues.TAG;

/**
 * The fragment used to add new numbers.
 *
 * Created by moo on 30/11/15.
 */
public class AddAccountDialogFragment extends DialogFragment {
    private final String LOGTAG = getClass().getSimpleName();
    private String myName;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        @SuppressLint("InflateParams")
        final View view = inflater.inflate(R.layout.dialog_add_account, null);

        builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.add, (dialog, id) -> {
                    myName = ((EditText) view.findViewById(R.id.account)).getText().toString();
                    addAccount(myName);
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> {
                    myName = null;
                    AddAccountDialogFragment.this.getDialog().cancel();
                });

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        return dialog;
    }


    @SuppressLint("CheckResult")
    private void addAccount(String aName) {
        if (aName == null || aName.trim().equals("")) {
            Toast.makeText(getContext(), getString(R.string.toast_enter_valid_name), Toast.LENGTH_LONG).show();
            Log.w(LOGTAG, "account name not valid");
            return;
        }

        final String name = aName.trim();

        final AccountDao accountDao = AppDatabase.get(getContext()).getAccountDao();

        //noinspection ResultOfMethodCallIgnored
        getAccountCount(name, accountDao)
                .subscribe( count -> {
                    if (count > 0 ) {
                        return;
                    }

                    final Account account = new Account();
                    account.setName(name);
                    account.setNumBreaches(0);
                    account.setNumAcknowledgedBreaches(0);
                    insertAccount(accountDao, account, getActivity().getApplication());
                });
    }

    private Single<Integer> getAccountCount(String name, AccountDao accountDao) {
        return Single.fromCallable(() -> accountDao.countByName(name))
                .subscribeOn(Schedulers.io());
    }

    @SuppressLint("CheckResult")
    private void insertAccount(AccountDao accountDao, Account account, Application application) {
        //noinspection ResultOfMethodCallIgnored
        Single.fromCallable(() -> accountDao.insert(account))
                .subscribeOn(Schedulers.io())
                .subscribe(ids -> {
                    ((HackedApplication) application).trackEvent("AddAccount");

                    Data inputData = new Data.Builder()
                            .putLong(HIBPAccountCheckerWorker.KEY_ID, ids.get(0))
                            .build();
                    Constraints constraints = new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.UNMETERED)
                            .build();

                    OneTimeWorkRequest checker =
                            new OneTimeWorkRequest.Builder(HIBPAccountCheckerWorker.class)
                                    .setInputData(inputData)
                                    .setConstraints(constraints)
                                    .build();
                    WorkManager.getInstance().enqueue(checker);
                } ,throwable -> Log.e(TAG, "Error msg", throwable));
    }


}
