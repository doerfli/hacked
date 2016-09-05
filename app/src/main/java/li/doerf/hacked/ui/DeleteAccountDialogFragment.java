package li.doerf.hacked.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import li.doerf.hacked.R;
import li.doerf.hacked.db.tables.Account;

/**
 * Created by moo on 06/09/16.
 */
public class DeleteAccountDialogFragment extends DialogFragment {
    private final String LOGTAG = getClass().getSimpleName();
    private Account myAccount;
    private SQLiteDatabase myDb;
    private AccountDeletedListener myListener;

    public interface AccountDeletedListener {
        void accountDeleted(Account aNumber);
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            myListener = (AccountDeletedListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
//            throw new ClassCastException(activity.toString()
//                    + " must implement NumberAddedListener");
            Log.w(LOGTAG, "activity does not implement NumberAddedListener. no notification");
        }
    }

    public void setAccountAndDb(Account account, SQLiteDatabase db) {
        myAccount = account;
        myDb = db;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dialog_account_delete_msg)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        myAccount.delete(myDb);
                        myListener.accountDeleted(myAccount);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        DeleteAccountDialogFragment.this.getDialog().cancel();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
