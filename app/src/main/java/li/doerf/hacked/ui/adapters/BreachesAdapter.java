package li.doerf.hacked.ui.adapters;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import li.doerf.hacked.HackedApplication;
import li.doerf.hacked.R;
import li.doerf.hacked.db.AppDatabase;
import li.doerf.hacked.db.daos.AccountDao;
import li.doerf.hacked.db.daos.BreachDao;
import li.doerf.hacked.db.entities.Account;
import li.doerf.hacked.db.entities.Breach;

/**
 * Created by moo on 07/09/16.
 */
public class BreachesAdapter extends RecyclerView.Adapter<RecyclerViewHolder> {
    private final String LOGTAG = getClass().getSimpleName();
    private final Context myContext;
    private final BreachDao myBreachDao;
    private List<Breach> myBreachList;

    public BreachesAdapter(Context aContext, List<Breach> aList) {
        myContext = aContext;
        myBreachList = aList;
        myBreachDao = AppDatabase.get(aContext).getBreachDao();
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        CardView itemLayout = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_breach, parent, false);
        return new RecyclerViewHolder(itemLayout);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        Breach breach = myBreachList.get(position);
        final CardView cardView = (CardView) holder.getView();
        final long breachId = breach.getId();

        TextView title = (TextView) cardView.findViewById(R.id.title);
        title.setText(breach.getTitle());

        TextView domain = (TextView) cardView.findViewById(R.id.domain);
        domain.setText(breach.getDomain());

        DateTimeFormatter dtfOut = DateTimeFormat.forPattern("yyyy/MM/dd");
        TextView breachDate = (TextView) cardView.findViewById(R.id.breach_date);
        breachDate.setText(dtfOut.print(breach.getBreachDate()));

        TextView compromisedData = (TextView) cardView.findViewById(R.id.compromised_data);
        compromisedData.setText(breach.getDataClasses());

        TextView description = (TextView) cardView.findViewById(R.id.description);
        description.setText(Html.fromHtml(breach.getDescription()).toString());

        View statusIndicator = cardView.findViewById(R.id.status_indicator);
        Button acknowledge = (Button) cardView.findViewById(R.id.acknowledge);

        if ( ! breach.getAcknowledged() ) {
            statusIndicator.setBackgroundColor(getContext().getResources().getColor(R.color.account_status_breached));
            acknowledge.setVisibility(View.VISIBLE);
            acknowledge.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Breach breach = myBreachDao.findById(breachId);
                    if ( breach == null ) {
                        return;
                    }

                    breach.setAcknowledged(true);
                    // TODO process in own thread
                    myBreachDao.update(breach);

                    ((HackedApplication) getContext().getApplicationContext()).trackEvent("BreachAcknowledged");
                    Snackbar.make(cardView, getContext().getString(R.string.breach_acknowledged), Snackbar.LENGTH_SHORT).show();

                    updateAccountIsHacked(breach.getAccount());

                    notifyDataSetChanged();
                }
            });
        } else {
            statusIndicator.setBackgroundColor(getContext().getResources().getColor(R.color.account_status_only_acknowledged));
            acknowledge.setVisibility(View.GONE);
        }

        TextView unverified = cardView.findViewById(R.id.unverified);

        if ( breach.getVerified() ) {
            unverified.setVisibility(View.GONE);
        } else {
            unverified.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return myBreachList.size();
    }

    // TODO move to other class
    private void updateAccountIsHacked(Long accountId) {
        AccountDao accountDao = AppDatabase.get(getContext()).getAccountDao();
        Account account = accountDao.findById(accountId);

        if ( ! account.getHacked() ) {
            return;
        }

        if ( myBreachDao.countUnacknowledged(accountId) > 0 ) {
            return;
        }

        account.setHacked(false);
        // TODO process in own thread
        accountDao.update(account);
    }

    public void addItems(List<Breach> list) {
        myBreachList = list;
        notifyDataSetChanged();
    }

    private Context getContext() {
        return myContext;
    }

}
