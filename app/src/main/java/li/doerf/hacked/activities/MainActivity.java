package li.doerf.hacked.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import li.doerf.hacked.CustomEvent;
import li.doerf.hacked.HackedApplication;
import li.doerf.hacked.R;
import li.doerf.hacked.ui.fragments.AccountListFragment;
import li.doerf.hacked.ui.fragments.BreachListType;
import li.doerf.hacked.ui.fragments.BreachedSitesListFragment;
import li.doerf.hacked.ui.fragments.PasswordFragment;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
//    private final String LOGTAG = getClass().getSimpleName();

    private Fragment myContentFragment;
    private Toolbar myToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext()) != ConnectionResult.RESULT_SUCCESS.getErrorCode() ) {
            GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this);
        }

        setContentView(R.layout.activity_main);
        myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        myContentFragment = AccountListFragment.create();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, myContentFragment)
                .commit();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            if ( myContentFragment instanceof AccountListFragment ) {
                ((AccountListFragment) myContentFragment).checkForBreaches(null);
                ((HackedApplication) getApplication()).trackCustomEvent(CustomEvent.CHECK_FOR_BREACHES);
            } else if ( myContentFragment instanceof BreachedSitesListFragment ) {
                ((BreachedSitesListFragment)myContentFragment).reloadBreachedSites();
                ((HackedApplication) getApplication()).trackCustomEvent(CustomEvent.RELOAD_BREACHED_SITES);
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, myToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        setTitle(getString(R.string.nd_accounts));
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
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

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
////        int id = item.getItemId();
//
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_privacypolicy) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://doerfli.github.io/hacked/privacy"));
            startActivity(browserIntent);

            return true;
        }

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
        } else if (id == R.id.action_pwned_password) {
            myContentFragment = PasswordFragment.newInstance();
            myToolbar.setTitle(getString(R.string.nd_pwned_passwords));
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, myContentFragment)
                    .addToBackStack("pwned_passwords")
                    .commit();
        } else if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
