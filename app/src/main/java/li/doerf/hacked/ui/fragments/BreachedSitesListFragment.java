package li.doerf.hacked.ui.fragments;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import li.doerf.hacked.R;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.BreachedSite;
import li.doerf.hacked.ui.adapters.BreachedSitesAdapter;

/**
 * Created by moo on 09/10/16.
 */
public class BreachedSitesListFragment extends Fragment {
    private final String LOGTAG = getClass().getSimpleName();
    private String myOrderBy;
    private SQLiteDatabase myReadbableDb;
    private BreachedSitesAdapter myBreachedSitesAdapter;
    private View myFragmentRootView;
    private Cursor myCursor;

    public static BreachedSitesListFragment create(String orderby) {
        BreachedSitesListFragment fragment = new BreachedSitesListFragment();
        fragment.setOrderBy(orderby);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        myReadbableDb = HackedSQLiteHelper.getInstance(getContext()).getReadableDatabase();
        myBreachedSitesAdapter = new BreachedSitesAdapter(getContext(), null);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        myFragmentRootView =  inflater.inflate(R.layout.fragment_breached_sites_list, container, false);

        RecyclerView accountsList = (RecyclerView) myFragmentRootView.findViewById(R.id.breached_sites_list);
        accountsList.setHasFixedSize(true);
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        accountsList.setLayoutManager(lm);
        accountsList.setAdapter(myBreachedSitesAdapter);

        return myFragmentRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    @Override
    public void onDetach() {
        if ( myCursor != null ) {
            myCursor.close();
        }
        myReadbableDb = null;

        super.onDetach();
    }

    public void setOrderBy(String orderBy) {
        myOrderBy = orderBy;
    }

    public void refreshList() {
        myCursor = BreachedSite.listAll(myReadbableDb, myOrderBy);
        if ( ! myCursor.isClosed() ) {
            Cursor old = null;
            try {
                old = myBreachedSitesAdapter.swapCursor(myCursor);
            } finally {
                if ( old != null ) {
                    old.close();
                }
            }
        } else {
            Log.w(LOGTAG, "cursor closed");
            myBreachedSitesAdapter.swapCursor(null);
        }
    }
}
