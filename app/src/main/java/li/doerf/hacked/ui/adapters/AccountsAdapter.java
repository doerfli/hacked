package li.doerf.hacked.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Collection;

import li.doerf.hacked.BreachDetailsActivity;
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
                .inflate(R.layout.card_account, parent, false);
        return new RecyclerViewHolder(itemLayout);
    }

    @Override
    public void onBindViewHolder(final RecyclerViewHolder holder, Cursor aCursor) {
        CardView cardView = (CardView) holder.getView();

        final SQLiteDatabase db = HackedSQLiteHelper.getInstance(getContext()).getReadableDatabase();
        final Account account = Account.create(db, aCursor);
        final Collection<Breach> breaches = Breach.findAllByAccount(db, account);

        TextView nameView = (TextView) cardView.findViewById(R.id.name);
        nameView.setText(account.getName());

        TextView lastCheckedView = (TextView) cardView.findViewById(R.id.last_checked);
        if ( account.getLastChecked() != null ) {
            DateTimeFormatter dtfOut = DateTimeFormat.forPattern("yyyy/MM/dd HH:mm");
            lastCheckedView.setText(account.getLastChecked().toString( dtfOut.print(account.getLastChecked())));
        } else {
            lastCheckedView.setText("-");
        }

        TextView breachStatus = (TextView) cardView.findViewById(R.id.breach_state);
        if ( account.isHacked()) {
            breachStatus.setText(getContext().getString(R.string.status_breaches_found, breaches.size()));
            cardView.setCardBackgroundColor(getContext().getResources().getColor(R.color.account_status_breached));
        } else if ( ! account.isHacked() && account.getLastChecked() == null ) {
            breachStatus.setText("-");
            cardView.setCardBackgroundColor(getContext().getResources().getColor(R.color.account_status_unknown));
        } else {
            breachStatus.setText(getContext().getString(R.string.status_no_breach_found));
            cardView.setCardBackgroundColor(getContext().getResources().getColor(R.color.account_status_ok));
        }

        if ( breaches.size() > 0 ) {
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent showBreachDetails = new Intent(getContext(), BreachDetailsActivity.class);
                    showBreachDetails.putExtra(BreachDetailsActivity.EXTRA_ACCOUNT_ID, account.getId());
                    getContext().startActivity(showBreachDetails);
                }
            });
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
