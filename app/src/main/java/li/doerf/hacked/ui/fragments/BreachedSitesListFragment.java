package li.doerf.hacked.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.Serializable;

import li.doerf.hacked.R;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.BreachedSite;
import li.doerf.hacked.services.haveibeenpwned.HIBPGetBreachedSitesAsyncTask;
import li.doerf.hacked.ui.adapters.BreachedSitesAdapter;

/**
 * Created by moo on 09/10/16.
 */
public class BreachedSitesListFragment extends Fragment {
    private static final String KEY_BREACH_LIST_TYPE = "BreachListType";
    private final String LOGTAG = getClass().getSimpleName();
    private SQLiteDatabase myReadbableDb;
    private BreachedSitesAdapter myBreachedSitesAdapter;
    private Cursor myCursor;
    private BreachListType myBreachListType;

    public static BreachedSitesListFragment create(BreachListType aType) {
        BreachedSitesListFragment fragment = new BreachedSitesListFragment();
        fragment.setMyBreachListType(aType);
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
        View view = inflater.inflate(R.layout.fragment_breached_sites_list, container, false);

        RecyclerView accountsList = (RecyclerView) view.findViewById(R.id.breached_sites_list);
        accountsList.setHasFixedSize(true);
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        accountsList.setLayoutManager(lm);
        accountsList.setAdapter(myBreachedSitesAdapter);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if ( savedInstanceState != null ) {
            Serializable blt = savedInstanceState.getSerializable(KEY_BREACH_LIST_TYPE);
            if ( blt != null ) {
                myBreachListType = (BreachListType) blt;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
        if ( myBreachedSitesAdapter.getCursor().getCount() == 0 || lastUpdateBeforeOneHour()) {
            reloadBreachedSites();
        }
        if ( myBreachListType == null ) {
            myBreachListType = BreachListType.MostRecent;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_BREACH_LIST_TYPE, myBreachListType);
    }

    private boolean lastUpdateBeforeOneHour() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        long lastsync = settings.getLong(getString(R.string.PREF_KEY_LAST_SYNC_HIBP_TOP20), 0);
        long now = System.currentTimeMillis();
        return lastsync < ( now - 60 * 60 * 1000 );
    }

    @Override
    public void onDetach() {
        if ( myCursor != null ) {
            myCursor.close();
        }
        myReadbableDb = null;

        super.onDetach();
    }

    public void refreshList() {
        Log.d(LOGTAG, "refreshing list");

        switch ( myBreachListType){
            case Top20:
                myCursor = BreachedSite.listTop20(myReadbableDb);
            break;

            case MostRecent:
                myCursor = BreachedSite.listMostRecent(myReadbableDb);
            break;

            case All:
            default:
                myCursor = BreachedSite.listAll(myReadbableDb);
        }
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

    public void setMyBreachListType(BreachListType myBreachListType) {
        this.myBreachListType = myBreachListType;
    }

    public void reloadBreachedSites() {
        new HIBPGetBreachedSitesAsyncTask(this).execute();
    }
}
