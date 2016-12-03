package li.doerf.hacked.ui.fragments;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import li.doerf.hacked.R;
import li.doerf.hacked.db.DatasetChangeListener;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.db.tables.Breach;
import li.doerf.hacked.ui.HibpInfo;
import li.doerf.hacked.ui.adapters.BreachesAdapter;

/**
 * Created by moo on 06/10/16.
 */
public class BreachDetailsFragment extends Fragment implements DatasetChangeListener {
    private final String LOGTAG = getClass().getSimpleName();
    private SQLiteDatabase myReadbableDb;
    private Account myAccount;
    private List<Breach> myBreaches;
    private BreachesAdapter myBreachesAdapter;
    private long myAccountId;


    public static BreachDetailsFragment create(long accountId) {
        BreachDetailsFragment fragment = new BreachDetailsFragment();
        fragment.setMyAccountId(accountId);
        return fragment;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        myReadbableDb = HackedSQLiteHelper.getInstance(getContext()).getWritableDatabase();
        myAccount = Account.findById(myReadbableDb, myAccountId);
        myBreaches = Breach.findAllByAccount(myReadbableDb, myAccount);
        myBreachesAdapter = new BreachesAdapter(getContext(), myBreaches);
    }

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_breach_details, container, false);

        getActivity().setTitle(myAccount.getName());

        if ( myBreaches.size() == 0 ) {
            CardView noBreachFound = (CardView) view.findViewById(R.id.no_breach_found);
            noBreachFound.setVisibility(View.VISIBLE);
            TextView hibpInfo = (TextView) view.findViewById(R.id.hibp_info);
            hibpInfo.setVisibility(View.GONE);
            return view;
        }

        RecyclerView breachesList = (RecyclerView) view.findViewById(R.id.breaches_list);
        breachesList.setHasFixedSize(true);
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        breachesList.setLayoutManager(lm);
        breachesList.setAdapter(myBreachesAdapter);

        HibpInfo.prepare( getContext(), (TextView) view.findViewById(R.id.hibp_info), breachesList);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(breachesList.getContext(),
                lm.getOrientation());
        breachesList.addItemDecoration(dividerItemDecoration);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Breach.registerDatasetChangedListener(this, Breach.class);
    }

    @Override
    public void onPause() {
        Breach.unregisterDatasetChangedListener(this, Breach.class);
        super.onPause();
    }

    @Override
    public void onDetach() {
        myReadbableDb = null;
        super.onDetach();
    }

    @Override
    public void datasetChanged() {
        myBreaches = Breach.findAllByAccount(myReadbableDb, myAccount);
        if ( myBreachesAdapter != null )
            myBreachesAdapter.changeList(myBreaches);
    }

    public void setMyAccountId(long myAccountId) {
        this.myAccountId = myAccountId;
    }
}
