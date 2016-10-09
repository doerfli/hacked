package li.doerf.hacked.activities;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import li.doerf.hacked.R;
import li.doerf.hacked.services.haveibeenpwned.GetBreachedSitesAsyncTask;
import li.doerf.hacked.ui.fragments.AccountListFragment;
import li.doerf.hacked.ui.fragments.BreachListType;
import li.doerf.hacked.ui.fragments.BreachedSitesListFragment;
import li.doerf.hacked.utils.IServiceRunningListener;
import li.doerf.hacked.utils.ServiceRunningNotifier;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, IServiceRunningListener {
    private final String LOGTAG = getClass().getSimpleName();

    private Fragment myContentFragment;
    private FloatingActionButton myFloatingActionCheckButton;
    private ObjectAnimator myFabAnimation;
    private static boolean myIsActive;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        myContentFragment = AccountListFragment.create();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, myContentFragment)
                .commit();

        myFloatingActionCheckButton = (FloatingActionButton) findViewById(R.id.fab);
        myFloatingActionCheckButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( myContentFragment instanceof AccountListFragment ) {
                    ((AccountListFragment) myContentFragment).checkForBreaches(null);
                } else if ( myContentFragment instanceof BreachedSitesListFragment ) {
                    new GetBreachedSitesAsyncTask( (BreachedSitesListFragment) myContentFragment).execute();
                }
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ServiceRunningNotifier.registerServiceRunningListener(this);
        myIsActive = true;
    }

    @Override
    protected void onPause() {
        ServiceRunningNotifier.unregisterServiceRunningListener(this);
        myIsActive = false;
        super.onPause();
    }

    @Override
    public void onDestroy() {
        myIsActive = false; // just to be sure
        super.onDestroy();
    }

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
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, myContentFragment)
                    .addToBackStack("account_list")
                    .commit();
        } else if (id == R.id.action_top_breached_sites) {
            myContentFragment = BreachedSitesListFragment.create(BreachListType.Top20);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, myContentFragment)
                    .addToBackStack("top20_breached_sites")
                    .commit();
        } else if (id == R.id.action_most_recent_breaches) {
            myContentFragment = BreachedSitesListFragment.create(BreachListType.MostRecent);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, myContentFragment)
                    .addToBackStack("most_recent_breached_sites")
                    .commit();
        } else if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public static boolean isActive() {
        return myIsActive;
    }

    @Override
    public void notifyListener(final Event anEvent) {
        new Handler(Looper.getMainLooper()).post(
                new Runnable() {
                    @Override
                    public void run() {
                        if ( myContentFragment instanceof AccountListFragment ) {
                            AccountListFragment fragment = (AccountListFragment) myContentFragment;
                            switch (anEvent) {
                                case STARTED:
                                    fragment.setSyncActive(true);

                                    if (myFabAnimation == null) {
                                        Log.d(LOGTAG, "animation starting");
                                        myFabAnimation = (ObjectAnimator) AnimatorInflater.loadAnimator(getApplicationContext(),
                                                R.animator.rotate_right_repeated);
                                        myFabAnimation.setTarget(myFloatingActionCheckButton);
                                        myFabAnimation.start();
                                    } else {
                                        Log.d(LOGTAG, "animation already active");
                                    }
                                    break;

                                case STOPPED:
                                    fragment.setSyncActive(false);

                                    if (myFabAnimation != null) {
                                        Log.d(LOGTAG, "animation stopping");
                                        myFabAnimation.removeAllListeners();
                                        myFabAnimation.end();
                                        myFabAnimation.cancel();
                                        myFabAnimation = null;
                                        myFloatingActionCheckButton.clearAnimation();
                                        myFloatingActionCheckButton.setRotation(0);
                                    }
                                    break;
                            }
                        }
                    }
                }
        );
    }
}
