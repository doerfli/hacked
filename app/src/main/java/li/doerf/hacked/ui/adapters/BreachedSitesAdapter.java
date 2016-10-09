package li.doerf.hacked.ui.adapters;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

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
        CardView cardView = (CardView) holder.getView();

        final SQLiteDatabase db = HackedSQLiteHelper.getInstance(getContext()).getReadableDatabase();
        final BreachedSite site = BreachedSite.create(db, aCursor);

        TextView nameView = (TextView) cardView.findViewById(R.id.site_name);
        nameView.setText(site.getName());

        TextView pwnCountView = (TextView) cardView.findViewById(R.id.pwn_count);
        pwnCountView.setText( String.format("%,d %s", site.getPwnCount(), getContext().getString(R.string.accounts)));
    }
}
