package li.doerf.hacked.ui;

import android.app.Activity;
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
import android.widget.EditText;

import li.doerf.hacked.R;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.Account;

/**
 * The fragment used to add new numbers.
 *
 * Created by moo on 30/11/15.
 */
public class AddAccountDialogFragment extends DialogFragment {
    private final String LOGTAG = getClass().getSimpleName();
    private String myName;
    private AccountAddedListener myListener;

    public interface AccountAddedListener {
        void accountAdded(Account aNumber);
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            myListener = (AccountAddedListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
//            throw new ClassCastException(activity.toString()
//                    + " must implement NumberAddedListener");
            Log.w(LOGTAG, "activity does not implement NumberAddedListener. no notification");
        }
    }

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
                        addPhoneNumberToDb(myName);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        myName = null;
                        AddAccountDialogFragment.this.getDialog().cancel();
                    }
                });

        final AlertDialog dialog = builder.create();
        return dialog;
    }


    private void addPhoneNumberToDb(String name) {
        if (name == null || name.trim().equals("")) {
            name = "<Not set>";
        }

        SQLiteDatabase db = HackedSQLiteHelper.getInstance(getContext()).getWritableDatabase();
        Account account = Account.create( name);
        account.insert(db);
        myListener.accountAdded(account);
    }
}
