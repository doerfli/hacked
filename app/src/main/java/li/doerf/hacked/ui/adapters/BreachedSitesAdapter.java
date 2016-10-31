package li.doerf.hacked.ui.adapters;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import li.doerf.hacked.R;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.BreachedSite;

public class BreachedSitesAdapter extends RecyclerViewCursorAdapter<RecyclerViewHolder> {
    private final String LOGTAG = getClass().getSimpleName();

    public BreachedSitesAdapter(Context aContext, Cursor aCursor){
        super(aContext, aCursor);
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        CardView itemLayout = (CardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_breached_site, parent, false);
        return new RecyclerViewHolder(itemLayout);
    }

    @Override
    public void onBindViewHolder(final RecyclerViewHolder holder, Cursor aCursor) {
        final CardView cardView = (CardView) holder.getView();

        final SQLiteDatabase db = HackedSQLiteHelper.getInstance(getContext()).getReadableDatabase();
        final BreachedSite site = BreachedSite.create(db, aCursor);

        TextView nameView = (TextView) cardView.findViewById(R.id.site_name);
        nameView.setText(site.getName());

        TextView pwnCountView = (TextView) cardView.findViewById(R.id.pwn_count);
        pwnCountView.setText( String.format(getContext().getResources().getConfiguration().locale, "%,d %s", site.getPwnCount(), getContext().getString(R.string.accounts)));

        final RelativeLayout details = (RelativeLayout) cardView.findViewById(R.id.breach_details);
        final ImageView arrowDown = (ImageView) cardView.findViewById(R.id.arrow_down);
        final ImageView arrowUp = (ImageView) cardView.findViewById(R.id.arrow_up);

        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( details.getVisibility() == View.VISIBLE ) {
                    details.setVisibility(View.GONE);
                    arrowDown.setVisibility(View.VISIBLE);
                    arrowUp.setVisibility(View.GONE);
                } else {
                    details.setVisibility(View.VISIBLE);
                    arrowDown.setVisibility(View.GONE);
                    arrowUp.setVisibility(View.VISIBLE);

                    TextView domain = (TextView) cardView.findViewById(R.id.domain);
                    domain.setText(site.getDomain());

                    DateTimeFormatter dtfOut = DateTimeFormat.forPattern("yyyy/MM/dd");
                    TextView breachDate = (TextView) cardView.findViewById(R.id.breach_date);
                    breachDate.setText(dtfOut.print(site.getBreachDate()));

                    TextView compromisedData = (TextView) cardView.findViewById(R.id.compromised_data);
                    compromisedData.setText(site.getDataClasses());

                    TextView description = (TextView) cardView.findViewById(R.id.description);
                    description.setText(Html.fromHtml(site.getDescription()).toString());

                }
            }
        });
    }
}
