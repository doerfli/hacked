package li.doerf.hacked.ui.fragments;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import li.doerf.hacked.HackedApplication;
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

        ((HackedApplication) getActivity().getApplication()).trackView("Fragment~BreachDetails");
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
