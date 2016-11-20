package li.doerf.hacked.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import li.doerf.hacked.R;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.remote.haveibeenpwned.HIBPCheckAccountAsyncTask;
import li.doerf.hacked.ui.fragments.AccountListFragment;
import li.doerf.hacked.ui.fragments.BreachListType;
import li.doerf.hacked.ui.fragments.BreachedSitesListFragment;
import li.doerf.hacked.utils.ConnectivityHelper;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final int IMPORT_ACCOUNTS = 321;
    private final String LOGTAG = getClass().getSimpleName();

    private Fragment myContentFragment;
    private Toolbar myToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        myContentFragment = AccountListFragment.create();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, myContentFragment)
                .commit();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( myContentFragment instanceof AccountListFragment ) {
                    ((AccountListFragment) myContentFragment).checkForBreaches(null);
                } else if ( myContentFragment instanceof BreachedSitesListFragment ) {
                    ((BreachedSitesListFragment)myContentFragment).reloadBreachedSites();
                }
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, myToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        setTitle(getString(R.string.nd_accounts));
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//    }

//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.action_accounts_list) {
            myContentFragment = AccountListFragment.create();
            myToolbar.setTitle(getString(R.string.nd_accounts));
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, myContentFragment)
                    .addToBackStack("account_list")
                    .commit();
        } else if (id == R.id.action_top_breached_sites) {
            myContentFragment = BreachedSitesListFragment.create(BreachListType.Top20);
            myToolbar.setTitle(getString(R.string.nd_top_20_breached_websites));
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, myContentFragment)
                    .addToBackStack("top20_breached_sites")
                    .commit();
        } else if (id == R.id.action_most_recent_breaches) {
            myContentFragment = BreachedSitesListFragment.create(BreachListType.MostRecent);
            myToolbar.setTitle(getString(R.string.nd_most_recent_breaches));
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, myContentFragment)
                    .addToBackStack("most_recent_breached_sites")
                    .commit();
        } else if (id == R.id.action_all_breaches) {
            myContentFragment = BreachedSitesListFragment.create(BreachListType.All);
            myToolbar.setTitle(getString(R.string.nd_all_breaches));
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, myContentFragment)
                    .addToBackStack("all_breached_sites")
                    .commit();
        } else if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        } else if ( id == R.id.action_import ) {
            Intent intent = new Intent();
            intent.setAction("li.doerf.hacked.searchaccounts");
            PackageManager packageManager = getPackageManager();
            List activities = packageManager.queryIntentActivities(intent,
                    PackageManager.MATCH_DEFAULT_ONLY);
            // check if search app is installed
            boolean isIntentSafe = activities.size() > 0;
            if ( ! isIntentSafe ) {
                // TODO show dialog with link to play store to install app
                Toast.makeText(getApplicationContext(), "no application found", Toast.LENGTH_LONG).show();
            } else {
                startActivityForResult(intent, IMPORT_ACCOUNTS);
            }

            return true;
        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMPORT_ACCOUNTS) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                SQLiteDatabase db = HackedSQLiteHelper.getInstance(getApplicationContext()).getWritableDatabase();
                ArrayList<String> accounts = data.getStringArrayListExtra("accounts");

                for ( String acc : accounts ) {
                    Account account = Account.create( acc.trim());

                    if ( account.exists(db) ) {
                        Log.w(LOGTAG, "account already exists. ignoring");
                        continue;
                    }

                    db.beginTransaction();
                    account.insert(db);
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    account.notifyObservers();

                    if ( ConnectivityHelper.isConnected( getApplicationContext()) ) {
                        new HIBPCheckAccountAsyncTask(getApplicationContext(), null).execute( account.getId());
                    } else {
                        Log.w(LOGTAG, "no network, cannot sync");
                    }
                }
            }
        }
    }
}
