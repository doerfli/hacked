package li.doerf.hacked;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.ui.AddAccountDialogFragment;
import li.doerf.hacked.ui.DeleteAccountDialogFragment;
import li.doerf.hacked.ui.adapters.AccountsAdapter;
import li.doerf.hacked.services.HaveIBeenPwnedCheckService;

public class AccountListActivity extends AppCompatActivity implements AddAccountDialogFragment.AccountAddedListener, DeleteAccountDialogFragment.AccountDeletedListener {

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
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                AddAccountDialogFragment newFragment = new AddAccountDialogFragment();
                newFragment.show(getSupportFragmentManager(), "addaccount");
            }
        });

        myReadbableDb = HackedSQLiteHelper.getInstance(getApplicationContext()).getReadableDatabase();
        myAccountsAdapter = new AccountsAdapter(this, null, getSupportFragmentManager());

        RecyclerView accountsList = (RecyclerView) findViewById(R.id.accounts_list);
        accountsList.setHasFixedSize(true);
        LinearLayoutManager lm = new LinearLayoutManager(getApplicationContext());
        accountsList.setLayoutManager(lm);
        accountsList.setAdapter(myAccountsAdapter);
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
            return true;
        }

        if (id == R.id.action_check) {
            Intent i = new Intent(getBaseContext(), HaveIBeenPwnedCheckService.class);
            startService(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshList();
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
    public void accountAdded(Account aNumber) {
        refreshList();
    }

    @Override
    public void accountDeleted(Account aNumber) {
        refreshList();
    }
}
