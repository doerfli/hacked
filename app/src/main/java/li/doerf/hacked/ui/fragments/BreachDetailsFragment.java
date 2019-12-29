package li.doerf.hacked.ui.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

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
//    private final String LOGTAG = getClass().getSimpleName();
    private BreachesAdapter myBreachesAdapter;
    private long myAccountId;
    private BreachViewModel myViewModel;

    public static BreachDetailsFragment create(long accountId) {
        BreachDetailsFragment fragment = new BreachDetailsFragment();
        fragment.myAccountId = accountId;
        return fragment;
    }

//    @Override
//    public void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        myAccountId = AccountDetailsFragmentArgs.fromBundle(getArguments()).getAccountId();
//    }

    @SuppressLint("CheckResult")
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        myViewModel = ViewModelProviders.of(this).get(BreachViewModel.class);
        myBreachesAdapter = new BreachesAdapter(getContext(), new ArrayList<>());
    }

    public View onCreateView(@NonNull  LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_breach_details, container, false);
        CardView noBreachFound = view.findViewById(R.id.no_breach_found);
        CardView breachHelp = view.findViewById(R.id.breach_help);

        AccountDao accountDao = AppDatabase.get(getContext()).getAccountDao();
        new BackgroundTaskHelper<List<Account>>().runInBackgroundAndConsumeOnMain(() -> accountDao.findById(myAccountId), accounts -> {
            for(Account account : accounts) {
                getActivity().setTitle(account.getName());
                myViewModel.getBreachList(
                        account.getId()).observe(
                        BreachDetailsFragment.this, breaches -> {
                            myBreachesAdapter.addItems(breaches);
                            if (myBreachesAdapter.getItemCount() == 0) {
                                noBreachFound.setVisibility(View.VISIBLE);
                                breachHelp.setVisibility(View.GONE);
                            } else {
                                noBreachFound.setVisibility(View.GONE);
                                breachHelp.setVisibility(View.VISIBLE);
                            }
                        });
            }
        });

        AppCompatTextView whatNow = view.findViewById(R.id.what_now);
        whatNow.setOnClickListener((event) -> {
            Group breachHelpText = view.findViewById(R.id.breach_help_text);
            if (breachHelpText.getVisibility() == View.GONE) {
                breachHelpText.setVisibility(View.VISIBLE);
            } else {
                breachHelpText.setVisibility(View.GONE);
            }
        });

        String link1 = "<a href=\"https://lastpass.com\">LastPass</a>";
        String link2 = "<a href=\"https://1password.com\">1Password</a>";
        String link3 = "<a href=\"https://dashlane.com\">Dashlane</a>";
        String text = getString(R.string.breach_details_first_text, link1, link2, link3);
        AppCompatTextView textOne = view.findViewById(R.id.t1);
        textOne.setMovementMethod(LinkMovementMethod.getInstance());
        textOne.setText(Html.fromHtml(text));

        RecyclerView breachesList = view.findViewById(R.id.breaches_list);
        breachesList.setHasFixedSize(true);
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        breachesList.setLayoutManager(lm);
        breachesList.setAdapter(myBreachesAdapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(breachesList.getContext(),
                lm.getOrientation());
        breachesList.addItemDecoration(dividerItemDecoration);

        HibpInfo.prepare( getContext(), view.findViewById(R.id.hibp_info), breachesList);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HackedApplication) getActivity().getApplication()).trackView("Fragment~BreachDetails");
    }

}
