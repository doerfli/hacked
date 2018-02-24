package li.doerf.hacked.ui.adapters;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

import li.doerf.hacked.HackedApplication;
import li.doerf.hacked.R;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.db.tables.Breach;

/**
 * Created by moo on 07/09/16.
 */
public class BreachesAdapter extends RecyclerViewListAdapter<RecyclerViewHolder, Breach> {
    private final String LOGTAG = getClass().getSimpleName();

    public BreachesAdapter(Context aContext, List<Breach> aList) {
        super(aContext, aList);
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        CardView itemLayout = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_breach, parent, false);
        return new RecyclerViewHolder(itemLayout);
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, final Breach aBreach) {
        final CardView cardView = (CardView) holder.getView();
        final long breachId = aBreach.getId();

        TextView title = (TextView) cardView.findViewById(R.id.title);
        title.setText(aBreach.getTitle());

        TextView domain = (TextView) cardView.findViewById(R.id.domain);
        domain.setText(aBreach.getDomain());

        DateTimeFormatter dtfOut = DateTimeFormat.forPattern("yyyy/MM/dd");
        TextView breachDate = (TextView) cardView.findViewById(R.id.breach_date);
        breachDate.setText(dtfOut.print(aBreach.getBreachDate()));

        TextView compromisedData = (TextView) cardView.findViewById(R.id.compromised_data);
        compromisedData.setText(aBreach.getDataClasses());

        TextView description = (TextView) cardView.findViewById(R.id.description);
        description.setText(Html.fromHtml(aBreach.getDescription()).toString());

        View statusIndicator = cardView.findViewById(R.id.status_indicator);
        Button acknowledge = (Button) cardView.findViewById(R.id.acknowledge);

        if ( ! aBreach.getIsAcknowledged() ) {
            statusIndicator.setBackgroundColor(getContext().getResources().getColor(R.color.account_status_breached));
            acknowledge.setVisibility(View.VISIBLE);
            acknowledge.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SQLiteDatabase db = HackedSQLiteHelper.getInstance(getContext()).getWritableDatabase();
                    Breach breach = Breach.findById(db, breachId);
                    if ( breach == null ) {
                        return;
                    }

                    db.beginTransaction();
                    breach.setIsAcknowledged(true);
                    breach.update(db);
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    ((HackedApplication) getContext().getApplicationContext()).trackEvent("BreachAcknowledged");
                    Snackbar.make(cardView, getContext().getString(R.string.breach_acknowledged), Snackbar.LENGTH_SHORT).show();
                    breach.notifyObservers();

                    Account account = Account.findById(db, breach.getAccount().getId());
                    if ( account == null ) {
                        return;
                    }
                    account.updateIsHacked(db);
                    notifyDataSetChanged();
                }
            });
        } else {
            statusIndicator.setBackgroundColor(getContext().getResources().getColor(R.color.account_status_only_acknowledged));
            acknowledge.setVisibility(View.GONE);
        }

        TextView unverified = cardView.findViewById(R.id.unverified);

        if ( aBreach.getIsVerified() ) {
            unverified.setVisibility(View.GONE);
        } else {
            unverified.setVisibility(View.VISIBLE);
        }
    }

}
