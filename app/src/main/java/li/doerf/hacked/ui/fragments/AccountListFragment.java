package li.doerf.hacked.ui.fragments;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import li.doerf.hacked.HackedApplication;
import li.doerf.hacked.R;
import li.doerf.hacked.db.AppDatabase;
import li.doerf.hacked.db.daos.AccountDao;
import li.doerf.hacked.db.entities.Account;
import li.doerf.hacked.remote.haveibeenpwned.HIBPAccountCheckerWorker;
import li.doerf.hacked.ui.AddAccountDialogFragment;
import li.doerf.hacked.ui.adapters.AccountsAdapter;
import li.doerf.hacked.ui.viewmodels.AccountViewModel;
import li.doerf.hacked.utils.BackgroundTaskHelper;
import li.doerf.hacked.utils.RatingHelper;

/**
 * Created by moo on 05/10/16.
 */
public class AccountListFragment extends Fragment {
    private final String LOGTAG = getClass().getSimpleName();
    private AccountsAdapter myAccountsAdapter;
    private View myFragmentRootView;
    private LocalBroadcastReceiver myBroadcastReceiver;

    public static AccountListFragment create() {
        return new AccountListFragment();
    }

    public AccountListFragment() {
        super();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        myAccountsAdapter = new AccountsAdapter(getContext(), new ArrayList<>(), getFragmentManager());
        setHasOptionsMenu(true);

        AccountViewModel myViewModel = ViewModelProviders.of(this).get(AccountViewModel.class);
        myViewModel.getAccountList().observe(AccountListFragment.this, accounts -> myAccountsAdapter.addItems(accounts));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        myFragmentRootView =  inflater.inflate(R.layout.fragment_account_list, container, false);

        RecyclerView accountsList = myFragmentRootView.findViewById(R.id.accounts_list);
        accountsList.setHasFixedSize(true);
        LinearLayoutManager lm = new LinearLayoutManager(getContext());
        accountsList.setLayoutManager(lm);
        accountsList.setAdapter(myAccountsAdapter);
        
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(accountsList.getContext(),
                lm.getOrientation());
        accountsList.addItemDecoration(dividerItemDecoration);

        showInitialSetupAccount(myFragmentRootView);
        showInitialHelp(myFragmentRootView);

        return myFragmentRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((HackedApplication) getActivity().getApplication()).trackView("Fragment~AccountList");
        registerReceiver();
        new RatingHelper(getContext()).showRateUsDialogDelayed();
    }

    @Override
    public void onPause() {
        unregisterReceiver();
        super.onPause();
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

        if (id == R.id.action_rateus) {
            new RatingHelper(getContext()).showRateUsDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showInitialSetupAccount(final View aRootView) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean initialSetupAccountDone = settings.getBoolean(getString(R.string.pref_initial_setup_account_done), false);
        if ( ! initialSetupAccountDone ) {
            final CardView initialAccount = aRootView.findViewById(R.id.initial_account);
            initialAccount.setVisibility(View.VISIBLE);
            Button addB = aRootView.findViewById(R.id.button_add_initial_account);
            addB.setOnClickListener(v -> {
                EditText accountET = aRootView.findViewById(R.id.account);
                String accountName = accountET.getText().toString().trim();

                ((HackedApplication) getActivity().getApplication()).trackEvent("AddInitialAccount");

                if ( accountName.equals("") ) {
                    Toast.makeText(getContext(), getString(R.string.toast_please_enter_account), Toast.LENGTH_LONG).show();
                    return;
                }

                AccountDao accountDao = AppDatabase.get(getContext()).getAccountDao();
                Account newAcc = new Account();
                newAcc.setName(accountName);
                newAcc.setNumBreaches(0);
                newAcc.setNumAcknowledgedBreaches(0);
                insertFirstAccount(accountET, accountDao, newAcc, initialAccount, settings);
            });
        }
    }

    @SuppressLint("CheckResult")
    private void insertFirstAccount(EditText accountET, AccountDao accountDao, Account newAcc, CardView initialAccount, SharedPreferences settings) {
        new BackgroundTaskHelper<List<Long>>().runInBackgroundAndConsumeOnMain(
                () -> accountDao.insert(newAcc),
                ids -> {
                    newAcc.setId(ids.get(0));
                    initialAccount.setVisibility(View.GONE);
                    Toast.makeText(getContext(), getString(R.string.toast_account_added), Toast.LENGTH_LONG).show();
                    checkForBreaches(newAcc);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(getString(R.string.pref_initial_setup_account_done), true);
                    editor.apply();
                    InputMethodManager mgr = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (mgr != null) {
                        mgr.hideSoftInputFromWindow(accountET.getWindowToken(), 0);
                    }
                }
        );
    }

    private void showInitialHelp(View aRootView) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean initialHelpDismissed = settings.getBoolean(getString(R.string.pref_initial_help_dismissed), false);
        if ( ! initialHelpDismissed ) {
            final CardView initialHelp = aRootView.findViewById(R.id.initial_help);
            initialHelp.setVisibility(View.VISIBLE);
            Button dismissB = aRootView.findViewById(R.id.button_dismiss_help);
            dismissB.setOnClickListener(v -> {
                initialHelp.setVisibility(View.GONE);
                Toast.makeText(getContext(), getString(R.string.toast_dont_show_initial_help_again), Toast.LENGTH_SHORT).show();

                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean(getString(R.string.pref_initial_help_dismissed), true);
                editor.apply();

                ((HackedApplication) getActivity().getApplication()).trackEvent("InitialHelpDismiss");
            });
        }
    }

    public void checkForBreaches(Account account) {

        OneTimeWorkRequest checker =
                new OneTimeWorkRequest.Builder(HIBPAccountCheckerWorker.class)
                        .build();
        WorkManager.getInstance().enqueue(checker);

        if ( account == null && isAdded() ) { // only show this message when checking for more then one account
            Snackbar.make(myFragmentRootView, getString(R.string.snackbar_checking_account), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter(HIBPAccountCheckerWorker.BROADCAST_ACTION_ACCOUNT_CHECK_FINISHED);
        myBroadcastReceiver = new LocalBroadcastReceiver();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(myBroadcastReceiver, intentFilter);
    }

    private void unregisterReceiver() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(myBroadcastReceiver);
        myBroadcastReceiver = null;
    }

    private class LocalBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOGTAG, "received local broadcast message");
        }
    }
}
