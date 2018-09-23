package li.doerf.hacked.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.collect.Collections2;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Collection;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import li.doerf.hacked.R;
import li.doerf.hacked.activities.BreachDetailsActivity;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.entities.Account;
import li.doerf.hacked.db.tables.Breach;
import li.doerf.hacked.ui.DeleteAccountDialogFragment;
import li.doerf.hacked.utils.NotificationHelper;

public class AccountsAdapter extends RecyclerView.Adapter<RecyclerViewHolder> {
    private final String LOGTAG = getClass().getSimpleName();
    private final FragmentManager mySupportFragmentManager;
    private List<Account> myAccountList;

    private final Context myContext;

    public AccountsAdapter(Context aContext, List<Account> accountList, FragmentManager supportFragmentManager){
        mySupportFragmentManager = supportFragmentManager;
        myAccountList = accountList;
        myContext = aContext;
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        CardView itemLayout = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_account, parent, false);
        return new RecyclerViewHolder(itemLayout);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        CardView cardView = (CardView) holder.getView();

        final SQLiteDatabase db = HackedSQLiteHelper.getInstance(getContext()).getReadableDatabase();
        final Account account = myAccountList.get(position);
        final Collection<Breach> breaches = Breach.findAllByAccount(db, account);
        int numBreaches = breaches.size();
        int numAcknowledgedBreaches = Collections2.filter(breaches, new com.google.common.base.Predicate<Breach>() {
            @Override
            public boolean apply(@Nullable Breach input) {
                if ( input == null ) { return false; }
                return input.getIsAcknowledged();
            }
        }).size();
        TextView nameView = (TextView) cardView.findViewById(R.id.name);
        nameView.setText(account.getName());


        TextView lastCheckedView = (TextView) cardView.findViewById(R.id.last_checked);
        if ( account.getLastChecked() != null ) {
            DateTimeFormatter dtfOut = DateTimeFormat.forPattern("yyyy/MM/dd HH:mm");
            lastCheckedView.setText(dtfOut.print(account.getLastChecked()));
        } else {
            lastCheckedView.setText("-");
        }

        // set breach status text
        TextView breachStatus = (TextView) cardView.findViewById(R.id.breach_state);
        if ( account.getLastChecked() == null ) {
            breachStatus.setText("-");
        } else if ( numBreaches == 0 ) {
            breachStatus.setText(getContext().getString(R.string.status_no_breach_found));
        } else {
            if ( numAcknowledgedBreaches == 0 ) {
                breachStatus.setText(getContext().getString(R.string.status_breaches_found, numBreaches));
            } else {
                breachStatus.setText(getContext().getString(R.string.status_breaches_found_acknowledged, numBreaches, numAcknowledgedBreaches));
            }
        }

        View statusIndicator = cardView.findViewById(R.id.status_indicator);
        // set color of card
        if ( account.getHacked()) {
            statusIndicator.setBackgroundColor(getContext().getResources().getColor(R.color.account_status_breached));
        } else if ( ! account.getHacked() && account.getLastChecked() == null ) {
            statusIndicator.setBackgroundColor(getContext().getResources().getColor(R.color.account_status_unknown));
        } else {
            if ( numBreaches == 0 ) {
                statusIndicator.setBackgroundColor(getContext().getResources().getColor(R.color.account_status_ok));
            } else {
                statusIndicator.setBackgroundColor(getContext().getResources().getColor(R.color.account_status_only_acknowledged));
            }
        }

        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent showBreachDetails = new Intent(getContext(), BreachDetailsActivity.class);
                showBreachDetails.putExtra(BreachDetailsActivity.EXTRA_ACCOUNT_ID, account.getId());
                getContext().startActivity(showBreachDetails);
                NotificationHelper.cancelAll(getContext());
            }
        });

        cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                DeleteAccountDialogFragment d = new DeleteAccountDialogFragment();
                d.setAccount(account);
                d.show(mySupportFragmentManager, "deleteAccount");
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return myAccountList.size();
    }

    public void addItems(List<Account> accountList) {
        myAccountList = accountList;
        notifyDataSetChanged();
    }

    public Context getContext() {
        return myContext;
    }
}
