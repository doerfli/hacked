package li.doerf.hacked.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.Serializable;
import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import li.doerf.hacked.HackedApplication;
import li.doerf.hacked.R;
import li.doerf.hacked.remote.haveibeenpwned.HIBPGetBreachedSitesAsyncTask;
import li.doerf.hacked.ui.HibpInfo;
import li.doerf.hacked.ui.adapters.BreachedSitesAdapter;
import li.doerf.hacked.ui.viewmodels.BreachedSitesViewModel;

/**
 * Created by moo on 09/10/16.
 */
public class BreachedSitesListFragment extends Fragment {
    private static final String KEY_BREACH_LIST_TYPE = "BreachListType";
//    private final String LOGTAG = getClass().getSimpleName();
    private BreachListType myBreachListType;
    private BreachedSitesAdapter myBreachedSitesAdapter;
    private SwipeRefreshLayout mySwipeRefreshLayout;


    public static BreachedSitesListFragment create(BreachListType aType) {
        BreachedSitesListFragment fragment = new BreachedSitesListFragment();
        fragment.setMyBreachListType(aType);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        myBreachedSitesAdapter = new BreachedSitesAdapter(getContext(), new ArrayList<>());
        setHasOptionsMenu(true);

        BreachedSitesViewModel myViewModel = ViewModelProviders.of(this).get(BreachedSitesViewModel.class);
        switch ( myBreachListType){
            case Top20:
                myViewModel.getBreachesSitesTop20().observe(BreachedSitesListFragment.this, sites -> myBreachedSitesAdapter.addItems(sites));
                break;
            case MostRecent:
                myViewModel.getBreachesSitesMostRecent().observe(BreachedSitesListFragment.this, sites -> myBreachedSitesAdapter.addItems(sites));
                break;
            case All:
            default:
                myViewModel.getBreachesSites().observe(BreachedSitesListFragment.this, sites -> myBreachedSitesAdapter.addItems(sites));
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_breached_sites_list, container, false);

        RecyclerView breachedSites = view.findViewById(R.id.breached_sites_list);
        breachedSites.setHasFixedSize(true);
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        breachedSites.setLayoutManager(lm);
        breachedSites.setAdapter(myBreachedSitesAdapter);

        HibpInfo.prepare( getContext(), view.findViewById(R.id.hibp_info), breachedSites);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(breachedSites.getContext(),
                lm.getOrientation());
        breachedSites.addItemDecoration(dividerItemDecoration);

        mySwipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        mySwipeRefreshLayout.setOnRefreshListener(() -> reloadBreachedSites());

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
        if ( myBreachedSitesAdapter.getItemCount() == 0 || lastUpdateBeforeOneHour()) {
            reloadBreachedSites();
        }
        if ( myBreachListType == null ) {
            myBreachListType = BreachListType.MostRecent;
        }

        ((HackedApplication) getActivity().getApplication()).trackView("Fragment~BreachedSites" + myBreachListType.name());
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

    private void setMyBreachListType(BreachListType myBreachListType) {
        this.myBreachListType = myBreachListType;
    }

    public void reloadBreachedSites() {
        if ( ! mySwipeRefreshLayout.isRefreshing() ) {
            mySwipeRefreshLayout.setRefreshing(true);
        }
        new HIBPGetBreachedSitesAsyncTask(this).execute();
    }

    /**
     * Indicate that the refresh is complete to stop refresh animation.
     */
    public void refreshComplete() {
        mySwipeRefreshLayout.setRefreshing(false);
    }
}
