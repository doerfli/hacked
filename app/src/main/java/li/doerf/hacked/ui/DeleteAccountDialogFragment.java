package li.doerf.hacked.ui;

import android.app.Dialog;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import li.doerf.hacked.HackedApplication;
import li.doerf.hacked.R;
import li.doerf.hacked.db.AppDatabase;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.daos.AccountDao;
import li.doerf.hacked.db.daos.BreachDao;
import li.doerf.hacked.db.entities.Account;
import li.doerf.hacked.db.entities.Breach;

/**
 * Created by moo on 06/09/16.
 */
public class DeleteAccountDialogFragment extends DialogFragment {
    private final String LOGTAG = getClass().getSimpleName();
    private Account myAccount;
    private SQLiteDatabase myDb;

    public void setAccount(Account account) {
        myAccount = account;
        myDb = HackedSQLiteHelper.getInstance(getContext()).getWritableDatabase();
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.dialog_account_delete_msg, myAccount.getName()))
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    // TODO this probably needs to go into another thread
                    AccountDao accountDao = AppDatabase.get(getContext()).getAccountDao();
                    BreachDao breachDao = AppDatabase.get(getContext()).getBreachDao();
                    List<Breach> breaches = breachDao.findByAccount(myAccount.getId());
                    try {
                        myDb.beginTransaction();
                        for (Breach b : breaches) {
                            breachDao.delete(b);
                        }
                        accountDao.delete(myAccount);
                        myDb.setTransactionSuccessful();
                    } finally {
                        myDb.endTransaction();
                        ((HackedApplication) getActivity().getApplication()).trackEvent("DeleteAccount");
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> DeleteAccountDialogFragment.this.getDialog().cancel());
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
