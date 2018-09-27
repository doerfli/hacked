package li.doerf.hacked.ui.adapters;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import li.doerf.hacked.R;
import li.doerf.hacked.db.entities.BreachedSite;

public class BreachedSitesAdapter extends RecyclerView.Adapter<RecyclerViewHolder> {
//    private final String LOGTAG = getClass().getSimpleName();
    private final Context myContext;
    private List<BreachedSite> myBreachedSites;

    public BreachedSitesAdapter(Context aContext, List<BreachedSite> breachedSites){
        myBreachedSites = breachedSites;
        myContext = aContext;
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        CardView itemLayout = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_breached_site, parent, false);
        return new RecyclerViewHolder(itemLayout);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        final BreachedSite site = myBreachedSites.get(position);

        final CardView cardView = (CardView) holder.getView();

        TextView nameView = cardView.findViewById(R.id.site_name);
        nameView.setText(site.getName());

        TextView unconfirmed = cardView.findViewById(R.id.unconfirmed);

        if ( site.getVerified()) {
            nameView.setTextColor(getContext().getResources().getColor(android.R.color.primary_text_light));
            unconfirmed.setVisibility(View.GONE);
        } else {
            nameView.setTextColor(getContext().getResources().getColor(R.color.account_status_unknown));
            unconfirmed.setVisibility(View.VISIBLE);
        }

        TextView pwnCountView = cardView.findViewById(R.id.pwn_count);
        pwnCountView.setText( String.format(getContext().getResources().getConfiguration().locale, "%,d %s", site.getPwnCount(), getContext().getString(R.string.accounts)));

        final RelativeLayout details = cardView.findViewById(R.id.breach_details);
        final ImageView arrowDown = cardView.findViewById(R.id.arrow_down);
        final ImageView arrowUp = cardView.findViewById(R.id.arrow_up);

        cardView.setOnClickListener(view -> {
            if ( details.getVisibility() == View.VISIBLE ) {
                details.setVisibility(View.GONE);
                arrowDown.setVisibility(View.VISIBLE);
                arrowUp.setVisibility(View.GONE);
            } else {
                details.setVisibility(View.VISIBLE);
                arrowDown.setVisibility(View.GONE);
                arrowUp.setVisibility(View.VISIBLE);

                TextView domain = cardView.findViewById(R.id.domain);
                domain.setText(site.getDomain());

                DateTimeFormatter dtfOut = DateTimeFormat.forPattern("yyyy/MM/dd");
                TextView breachDate = cardView.findViewById(R.id.breach_date);
                breachDate.setText(dtfOut.print(site.getBreachDate()));

                TextView compromisedData = cardView.findViewById(R.id.compromised_data);
                compromisedData.setText(site.getDataClasses());

                TextView description = cardView.findViewById(R.id.description);
                description.setText(Html.fromHtml(site.getDescription()).toString());
            }
        });
    }

    @Override
    public int getItemCount() {
        return myBreachedSites.size();
    }

    public void addItems(List<BreachedSite> list) {
        myBreachedSites = list;
        notifyDataSetChanged();
    }

    public Context getContext() {
        return myContext;
    }
}
