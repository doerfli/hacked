package li.doerf.hacked.ui.adapters;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import li.doerf.hacked.R;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.db.tables.Breach;
import li.doerf.hacked.ui.DeleteAccountDialogFragment;

public class AccountsAdapter extends RecyclerViewCursorAdapter<RecyclerViewHolder> {
    private final String LOGTAG = getClass().getSimpleName();
    private final FragmentManager mySupportFragmentManager;
//    private final AdapterModelChangedListener myItemsChangedListener;

    public AccountsAdapter(Context aContext, Cursor aCursor, FragmentManager supportFragmentManager){ // , AdapterModelChangedListener anItemsChangesListener) {
        super(aContext, aCursor);
        mySupportFragmentManager = supportFragmentManager;
//        myItemsChangedListener = anItemsChangesListener;
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        CardView itemLayout = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.account_card, parent, false);
        return new RecyclerViewHolder(itemLayout);
    }

    @Override
    public void onBindViewHolder(final RecyclerViewHolder holder, Cursor aCursor) {
        CardView cardView = (CardView) holder.getView();

        final SQLiteDatabase db = HackedSQLiteHelper.getInstance(getContext()).getReadableDatabase();
        final Account account = Account.create(db, aCursor);
//        Cursor breachesCursor = Breach.findByAccount(db, account);
//        int num = breachesCursor.getCount();
//        breachesCursor.close();

        TextView numberView = (TextView) cardView.findViewById(R.id.name);
        numberView.setText(account.getName());

        if ( account.isHacked() ) {
            numberView.setText(numberView.getText().toString().concat(" breached"));
        }

        cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                DeleteAccountDialogFragment d = new DeleteAccountDialogFragment();
                d.setAccountAndDb(account, db);
                d.show(mySupportFragmentManager, "deleteAccount");
                return true;
            }
        });
    }
}
