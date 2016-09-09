package li.doerf.hacked.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

public class AccountListActivity extends AppCompatActivity implements DatasetChangeListener {

    private SQLiteDatabase myReadbableDb;
    private AccountsAdapter myAccountsAdapter;
    private Cursor myCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
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

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean initialHelpDismissed = settings.getBoolean(getString(R.string.pref_initial_help_dismissed), false);

        if ( ! initialHelpDismissed ) {
            final CardView initialHelp = (CardView) findViewById(R.id.initial_help);
            initialHelp.setVisibility(View.VISIBLE);
            Button dismissB = (Button) findViewById(R.id.button_dismiss_help);
            dismissB.requestFocus();
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

        boolean initialAccountDone = settings.getBoolean(getString(R.string.pref_initial_account_setup), false);
        if ( ! initialAccountDone ) {
            final CardView initialAccount = (CardView) findViewById(R.id.initial_account);
            initialAccount.setVisibility(View.VISIBLE);
            Button addB = (Button) findViewById(R.id.button_add_initial_account);
            addB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText accountET = (EditText) findViewById(R.id.account);
                    String accountName = accountET.getText().toString().trim();

                    if ( accountName == "" ) {
                        Toast.makeText(getApplicationContext(), getString(R.string.toast_please_enter_account), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    initialAccount.setVisibility(View.GONE);

                    SQLiteDatabase db = HackedSQLiteHelper.getInstance(getApplicationContext()).getWritableDatabase();
                    db.beginTransaction();
                    Account account = Account.create( accountName);
                    account.insert(db);
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    account.notifyObservers();

                    Toast.makeText(getApplicationContext(), getString(R.string.toast_account_added), Toast.LENGTH_LONG).show();
                    checkForBreaches(initialAccount);

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(getString(R.string.pref_initial_account_setup), true);
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
        refreshList();
    }

    @Override
    protected void onPause() {
        Account.unregisterDatasetChangedListener(this, Account.class);
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
    }

    @Override
    public void datasetChanged() {
        refreshList();
    }

    private void checkForBreaches(View view) {
        if ( ! ConnectivityHelper.isConnected( getApplicationContext()) ) {
            Toast.makeText(getApplicationContext(), getString(R.string.toast_error_no_network), Toast.LENGTH_LONG).show();
            return;
        }

        Intent i = new Intent(getBaseContext(), HaveIBeenPwnedCheckService.class);
        startService(i);
        Snackbar.make(view, getString(R.string.snackbar_checking_account), Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }
}
