package li.doerf.hacked.ui.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import li.doerf.hacked.HackedApplication;
import li.doerf.hacked.R;
import li.doerf.hacked.db.AppDatabase;
import li.doerf.hacked.db.daos.AccountDao;
import li.doerf.hacked.db.entities.Account;
import li.doerf.hacked.ui.HibpInfo;
import li.doerf.hacked.ui.adapters.BreachesAdapter;
import li.doerf.hacked.ui.viewmodels.BreachViewModel;
import li.doerf.hacked.utils.BackgroundTaskHelper;

/**
 * Created by moo on 06/10/16.
 */
public class BreachDetailsFragment extends Fragment {
    private final String LOGTAG = getClass().getSimpleName();
//    private Account myAccount;
    private BreachesAdapter myBreachesAdapter;
    private long myAccountId;
    private BreachViewModel myViewModel;

    public static BreachDetailsFragment create(long accountId) {
        BreachDetailsFragment fragment = new BreachDetailsFragment();
        fragment.myAccountId = accountId;
        return fragment;
    }


    @SuppressLint("CheckResult")
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        AccountDao accountDao = AppDatabase.get(context).getAccountDao();
        myViewModel = ViewModelProviders.of(this).get(BreachViewModel.class);
        myBreachesAdapter = new BreachesAdapter(getContext(), new ArrayList<>());

        new BackgroundTaskHelper<Account>().runInBackgroundAndConsumeOnMain(() -> accountDao.findById(myAccountId), account -> {
            getActivity().setTitle(account.getName());
            myViewModel.getBreachList(
                    account.getId()).observe(
                    BreachDetailsFragment.this, accounts -> myBreachesAdapter.addItems(accounts));
        });
    }

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_breach_details, container, false);

        // TODO do something about this
//        if ( myBreachesAdapter.getItemCount() == 0 ) {
//            CardView noBreachFound = (CardView) view.findViewById(R.id.no_breach_found);
//            noBreachFound.setVisibility(View.VISIBLE);
//            TextView hibpInfo = (TextView) view.findViewById(R.id.hibp_info);
//            hibpInfo.setVisibility(View.GONE);
//            return view;
//        }

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
        ((HackedApplication) getActivity().getApplication()).trackView("Fragment~BreachDetails");
    }

}
