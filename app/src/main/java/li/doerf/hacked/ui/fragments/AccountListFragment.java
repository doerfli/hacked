package li.doerf.hacked.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import li.doerf.hacked.HackedApplication;
import li.doerf.hacked.R;
import li.doerf.hacked.db.entities.Account;
import li.doerf.hacked.remote.haveibeenpwned.HIBPCheckAccountAsyncTask;
import li.doerf.hacked.ui.AddAccountDialogFragment;
import li.doerf.hacked.ui.adapters.AccountsAdapter;
import li.doerf.hacked.ui.viewmodels.AccountViewModel;
import li.doerf.hacked.utils.AccountHelper;
import li.doerf.hacked.utils.ConnectivityHelper;
import li.doerf.hacked.utils.SynchronizationHelper;

/**
 * Created by moo on 05/10/16.
 */
public class AccountListFragment extends Fragment {
    private final String LOGTAG = getClass().getSimpleName();
    private AccountsAdapter myAccountsAdapter;
    private View myFragmentRootView;
    private SwipeRefreshLayout mySwipeRefreshLayout;
    private AccountViewModel myViewModel;

    public static AccountListFragment create() {
        AccountListFragment f = new AccountListFragment();
        return f;
    }

    public AccountListFragment() {
        super();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        myAccountsAdapter = new AccountsAdapter(getContext(), new ArrayList<Account>(), getFragmentManager());
        setHasOptionsMenu(true);

        myViewModel = ViewModelProviders.of(this).get(AccountViewModel.class);
        myViewModel.getAccountList().observe(AccountListFragment.this, new Observer<List<Account>>() {
            @Override
            public void onChanged(List<Account> accounts) {
                myAccountsAdapter.addItems(accounts);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        myFragmentRootView =  inflater.inflate(R.layout.fragment_account_list, container, false);

        RecyclerView accountsList = (RecyclerView) myFragmentRootView.findViewById(R.id.accounts_list);
        accountsList.setHasFixedSize(true);
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        accountsList.setLayoutManager(lm);
        accountsList.setAdapter(myAccountsAdapter);
        
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(accountsList.getContext(),
                lm.getOrientation());
        accountsList.addItemDecoration(dividerItemDecoration);

        mySwipeRefreshLayout = (SwipeRefreshLayout) myFragmentRootView.findViewById(R.id.swipe_refresh_layout);
        mySwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                checkForBreaches(null);
                ((HackedApplication) getActivity().getApplication()).trackEvent("CheckForBreaches");
            }
        });

        showInitialSetupAccount(myFragmentRootView);
        showInitialSetupCheck(myFragmentRootView);
        showInitialHelp(myFragmentRootView);

        return myFragmentRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HackedApplication) getActivity().getApplication()).trackView("Fragment~AccountList");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_fragment_account_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_check) {
            checkForBreaches(null);
            return true;
        }

        if (id == R.id.action_add_account) {
            AddAccountDialogFragment newFragment = new AddAccountDialogFragment();
            newFragment.show(getFragmentManager(), "addaccount");

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showInitialSetupAccount(final View aRootView) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean initialSetupAccountDone = settings.getBoolean(getString(R.string.pref_initial_setup_account_done), false);
        if ( ! initialSetupAccountDone ) {
            final CardView initialAccount = (CardView) aRootView.findViewById(R.id.initial_account);
            initialAccount.setVisibility(View.VISIBLE);
            Button addB = (Button) aRootView.findViewById(R.id.button_add_initial_account);
            addB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText accountET = (EditText) aRootView.findViewById(R.id.account);
                    String accountName = accountET.getText().toString().trim();

                    ((HackedApplication) getActivity().getApplication()).trackEvent("AddInitialAccount");

                    if ( accountName.equals("") ) {
                        Toast.makeText(getContext(), getString(R.string.toast_please_enter_account), Toast.LENGTH_LONG).show();
                        return;
                    }

                    Account account = AccountHelper.createAccount(getContext(), accountName);

                    initialAccount.setVisibility(View.GONE);
                    Toast.makeText(getContext(), getString(R.string.toast_account_added), Toast.LENGTH_LONG).show();
                    checkForBreaches(account);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(getString(R.string.pref_initial_setup_account_done), true);
                    editor.apply();
                    InputMethodManager mgr = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    mgr.hideSoftInputFromWindow(accountET.getWindowToken(), 0);
                }
            });
        }
    }

    private void showInitialSetupCheck(View aRootView) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean initialSetupCheckDone = settings.getBoolean(getString(R.string.pref_initial_setup_check_done), false);
        if ( ! initialSetupCheckDone ) {
            final CardView initialSetupCheck = (CardView) aRootView.findViewById(R.id.initial_setup_check);
            initialSetupCheck.setVisibility(View.VISIBLE);

            Button noB = (Button) aRootView.findViewById(R.id.initial_setup_check_no);
            noB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    initialSetupCheck.setVisibility(View.GONE);
                    Toast.makeText(getContext(), getString(R.string.toast_check_not_enabled), Toast.LENGTH_LONG).show();

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(getString(R.string.pref_initial_setup_check_done), true);
                    editor.apply();

                    ((HackedApplication) getActivity().getApplication()).trackEvent("InitialSyncDisable");
                }
            });

            Button yesB = (Button) aRootView.findViewById(R.id.initial_setup_check_yes);
            yesB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    initialSetupCheck.setVisibility(View.GONE);

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(getString(R.string.pref_key_sync_enable), true);
                    editor.putBoolean(getString(R.string.pref_initial_setup_check_done), true);
                    editor.apply();

                    ((HackedApplication) getActivity().getApplication()).trackEvent("InitialSyncEnable");

                    SynchronizationHelper.scheduleSync(getContext());
                    Toast.makeText(getContext(), getString(R.string.toast_check_enabled), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void showInitialHelp(View aRootView) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean initialHelpDismissed = settings.getBoolean(getString(R.string.pref_initial_help_dismissed), false);
        if ( ! initialHelpDismissed ) {
            final CardView initialHelp = (CardView) aRootView.findViewById(R.id.initial_help);
            initialHelp.setVisibility(View.VISIBLE);
            Button dismissB = (Button) aRootView.findViewById(R.id.button_dismiss_help);
            dismissB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    initialHelp.setVisibility(View.GONE);
                    Toast.makeText(getContext(), getString(R.string.toast_dont_show_initial_help_again), Toast.LENGTH_SHORT).show();

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(getString(R.string.pref_initial_help_dismissed), true);
                    editor.apply();

                    ((HackedApplication) getActivity().getApplication()).trackEvent("InitialHelpDismiss");
                }
            });
        }
    }

    public void checkForBreaches(Account account) {
        if ( ! ConnectivityHelper.isConnected( getContext()) ) {
            Log.i(LOGTAG, "no network");
            Toast.makeText(getContext(), getString(R.string.toast_error_no_network), Toast.LENGTH_SHORT).show();
            refreshComplete();
            return;
        }

        // only do this when checking more than one account (possible timing issue)
        if ( account == null && HIBPCheckAccountAsyncTask.isRunning()) {
            Log.i(LOGTAG, "check already in progress");
            Toast.makeText(getContext(), getString(R.string.toast_check_in_progress), Toast.LENGTH_SHORT).show();
            refreshComplete();
            return;
        }

        new HIBPCheckAccountAsyncTask(getContext(), this).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, account != null ? account.getId() : null );

        mySwipeRefreshLayout.setRefreshing(true);

        if ( account == null ) { // only show this message when checking for more then one account
            int expectedDuration = (int) Math.ceil(myAccountsAdapter.getItemCount() * 2.5);
            Snackbar.make(myFragmentRootView, getString(R.string.snackbar_checking_account, expectedDuration), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    /**
     * Indicate that the refresh is complete to stop refresh animation.
     */
    public void refreshComplete() {
        mySwipeRefreshLayout.setRefreshing(false);
    }

}
