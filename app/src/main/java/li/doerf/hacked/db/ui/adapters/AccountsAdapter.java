package li.doerf.hacked.db.ui.adapters;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import li.doerf.hacked.R;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.Account;

public class AccountsAdapter extends RecyclerViewCursorAdapter<RecyclerViewHolder> {
    private final String LOGTAG = getClass().getSimpleName();
//    private final AdapterModelChangedListener myItemsChangedListener;

    public AccountsAdapter(Context aContext, Cursor aCursor){ // , AdapterModelChangedListener anItemsChangesListener) {
        super(aContext, aCursor);
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

        SQLiteDatabase db = HackedSQLiteHelper.getInstance(getContext()).getReadableDatabase();
        Account account = Account.create(db, aCursor);

        TextView numberView = (TextView) cardView.findViewById(R.id.name);
        numberView.setText(account.getName());
    }

//    public interface AdapterModelChangedListener {
//        void itemsAdded();
//        void itemsDeleted();
//    }
}
