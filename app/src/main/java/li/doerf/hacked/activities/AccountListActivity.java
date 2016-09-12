package li.doerf.hacked.activities;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import li.doerf.hacked.R;
import li.doerf.hacked.db.DatasetChangeListener;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.services.HaveIBeenPwnedCheckService;
import li.doerf.hacked.ui.AddAccountDialogFragment;
import li.doerf.hacked.ui.adapters.AccountsAdapter;
import li.doerf.hacked.utils.ConnectivityHelper;
import li.doerf.hacked.utils.IServiceRunningListener;
import li.doerf.hacked.utils.ServiceRunningNotifier;
import li.doerf.hacked.utils.SynchronizationHelper;

public class AccountListActivity extends AppCompatActivity implements DatasetChangeListener, IServiceRunningListener {
    private final String LOGTAG = getClass().getSimpleName();

    private SQLiteDatabase myReadbableDb;
    private AccountsAdapter myAccountsAdapter;
    private Cursor myCursor;
    private FloatingActionButton myFloatingActionButton;
    private AnimatorSet myFabAnimation;
    private boolean mySyncActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        myFloatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        myFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkForBreaches(view);
            }
        });

        myReadbableDb = HackedSQLiteHelper.getInstance(getApplicationContext()).getReadableDatabase();
        myAccountsAdapter = new AccountsAdapter(this, null, getSupportFragmentManager());

        RecyclerView accountsList = (RecyclerView) findViewById(R.id.accounts_list);
        accountsList.setHasFixedSize(true);
        LinearLayoutManager lm = new LinearLayoutManager(getApplicationContext());
        accountsList.setLayoutManager(lm);
        accountsList.setAdapter(myAccountsAdapter);

        showInitialSetupAccount();
        showInitialSetupCheck();
        showInitialHelp();
    }

    private void showInitialSetupAccount() {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean initialSetupAccountDone = settings.getBoolean(getString(R.string.pref_initial_setup_account_done), false);
        if ( ! initialSetupAccountDone ) {
            final CardView initialAccount = (CardView) findViewById(R.id.initial_account);
            initialAccount.setVisibility(View.VISIBLE);
            Button addB = (Button) findViewById(R.id.button_add_initial_account);
            addB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText accountET = (EditText) findViewById(R.id.account);
                    String accountName = accountET.getText().toString().trim();

                    if ( accountName.equals("") ) {
                        Toast.makeText(getApplicationContext(), getString(R.string.toast_please_enter_account), Toast.LENGTH_LONG).show();
                        return;
                    }

                    Account account = Account.create( accountName);
                    SQLiteDatabase db = HackedSQLiteHelper.getInstance(getApplicationContext()).getWritableDatabase();

                    if ( account.exists(db) ) {
                        Log.w(LOGTAG, "account already exists");
                        Toast.makeText(getApplicationContext(), getString(R.string.toast_account_exists), Toast.LENGTH_LONG).show();
                        return;
                    }

                    db.beginTransaction();
                    account.insert(db);
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    account.notifyObservers();

                    initialAccount.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_account_added), Toast.LENGTH_LONG).show();
                    checkForBreaches(initialAccount);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(getString(R.string.pref_initial_setup_account_done), true);
                    editor.apply();
                    InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    mgr.hideSoftInputFromWindow(accountET.getWindowToken(), 0);
                }
            });
        }
    }

    private void showInitialSetupCheck() {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean initialSetupCheckDone = settings.getBoolean(getString(R.string.pref_initial_setup_check_done), false);
        if ( ! initialSetupCheckDone ) {
            final CardView initialSetupCheck = (CardView) findViewById(R.id.initial_setup_check);
            initialSetupCheck.setVisibility(View.VISIBLE);

            Button noB = (Button) findViewById(R.id.initial_setup_check_no);
            noB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    initialSetupCheck.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_check_not_enabled), Toast.LENGTH_LONG).show();

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(getString(R.string.pref_initial_setup_check_done), true);
                    editor.apply();
                }
            });

            Button yesB = (Button) findViewById(R.id.initial_setup_check_yes);
            yesB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    initialSetupCheck.setVisibility(View.GONE);

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(getString(R.string.pref_key_sync_enable), true);
                    editor.putBoolean(getString(R.string.pref_initial_setup_check_done), true);
                    editor.apply();

                    SynchronizationHelper.scheduleSync(getApplicationContext());
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_check_enabled), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void showInitialHelp() {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean initialHelpDismissed = settings.getBoolean(getString(R.string.pref_initial_help_dismissed), false);
        if ( ! initialHelpDismissed ) {
            final CardView initialHelp = (CardView) findViewById(R.id.initial_help);
            initialHelp.setVisibility(View.VISIBLE);
            Button dismissB = (Button) findViewById(R.id.button_dismiss_help);
            dismissB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    initialHelp.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_dont_show_initial_help_again), Toast.LENGTH_SHORT).show();

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(getString(R.string.pref_initial_help_dismissed), true);
                    editor.apply();
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_account_list, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        if (id == R.id.action_check) {
            checkForBreaches(this.findViewById(android.R.id.content));
            return true;
        }

        if (id == R.id.action_add_account) {
            AddAccountDialogFragment newFragment = new AddAccountDialogFragment();
            newFragment.show(getSupportFragmentManager(), "addaccount");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Account.registerDatasetChangedListener(this, Account.class);
        ServiceRunningNotifier.registerServiceRunningListener(this);
        refreshList();
    }

    @Override
    protected void onPause() {
        Account.unregisterDatasetChangedListener(this, Account.class);
        ServiceRunningNotifier.unregisterServiceRunningListener(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if ( myCursor != null ) {
            myCursor.close();
        }
        myReadbableDb = null;
        super.onDestroy();
    }

    public void refreshList() {
        myCursor = Account.listAll(myReadbableDb);
        myAccountsAdapter.swapCursor(myCursor);

        if ( myCursor.getCount() > 0 ) {
            myFloatingActionButton.setVisibility(View.VISIBLE);
        } else {
            myFloatingActionButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void datasetChanged() {
        refreshList();
    }

    private void checkForBreaches(View view) {
        if ( ! ConnectivityHelper.isConnected( getApplicationContext()) ) {
            Log.i(LOGTAG, "no network");
            Toast.makeText(getApplicationContext(), getString(R.string.toast_error_no_network), Toast.LENGTH_SHORT).show();
            return;
        }

        if ( mySyncActive) {
            Log.i(LOGTAG, "check already in progress");
            Toast.makeText(getApplicationContext(), getString(R.string.toast_check_in_progress), Toast.LENGTH_SHORT).show();
            return;
        }

        Intent i = new Intent(getBaseContext(), HaveIBeenPwnedCheckService.class);
        startService(i);

        int expectedDuration = (int) Math.ceil(myAccountsAdapter.getItemCount() * 2.5);
        Snackbar.make(view, getString(R.string.snackbar_checking_account, expectedDuration), Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    @Override
    public void notifyListener(final Event anEvent) {
        new Handler(Looper.getMainLooper()).post(
                new Runnable() {
                    @Override
                    public void run() {
                        switch (anEvent) {
                            case STARTED:
                                mySyncActive = true;

                                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                                    Log.w(LOGTAG, "showing no animation on api < 21");
                                    return;
                                }

                                if (myFabAnimation == null) {
                                    Log.d(LOGTAG, "animation starting");
                                    myFabAnimation = (AnimatorSet) AnimatorInflater.loadAnimator(getApplicationContext(),
                                            R.animator.rotate_right_repeated);
                                    myFabAnimation.setTarget(myFloatingActionButton);
                                    myFabAnimation.start();
                                } else {
                                    Log.d(LOGTAG, "animation already active");
                                }
                                break;

                            case STOPPED:
                                mySyncActive = false;

                                if (myFabAnimation != null) {
                                    Log.d(LOGTAG, "animation stopping");
                                    myFabAnimation.cancel();
                                    myFabAnimation = null;
                                    myFloatingActionButton.setRotation(0);
                                }
                                break;
                        }
                    }
                }
        );
    }
}
