package li.doerf.hacked.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import li.doerf.hacked.R;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.utils.ConnectivityHelper;

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
        final View view = inflater.inflate(R.layout.dialog_add_account, null);

        builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        myName = ((EditText) view.findViewById(R.id.account)).getText().toString();
                        addAccount(myName);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        myName = null;
                        AddAccountDialogFragment.this.getDialog().cancel();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }


    private void addAccount(String name) {
        if (name == null || name.trim().equals("")) {
            Toast.makeText(getContext(), getString(R.string.toast_enter_valid_name), Toast.LENGTH_LONG).show();
            Log.w(LOGTAG, "account name not valid");
            return;
        }

        Account account = Account.create( name.trim());
        SQLiteDatabase db = HackedSQLiteHelper.getInstance(getContext()).getWritableDatabase();

        if ( account.exists(db) ) {
            Toast.makeText(getContext(), getString(R.string.toast_account_exists), Toast.LENGTH_LONG).show();
            Log.w(LOGTAG, "account already exists");
            return;
        }

        db.beginTransaction();
        account.insert(db);
        db.setTransactionSuccessful();
        db.endTransaction();
        account.notifyObservers();

        if ( ! ConnectivityHelper.isConnected( getContext()) ) {
            Log.i(LOGTAG, "no network");
            return;
        }

//TODO        new HIBPCheckAccountAsyncTask(getContext()).execute( account.getId());
    }
}
