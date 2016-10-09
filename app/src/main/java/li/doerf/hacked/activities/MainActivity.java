package li.doerf.hacked.activities;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
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
import li.doerf.hacked.ui.fragments.AccountListFragment;
import li.doerf.hacked.utils.IServiceRunningListener;
import li.doerf.hacked.utils.ServiceRunningNotifier;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, IServiceRunningListener {
    private final String LOGTAG = getClass().getSimpleName();

    private AccountListFragment myContentFragment;
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
                myContentFragment.checkForBreaches(null);
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
        getMenuInflater().inflate(R.menu.main, menu);
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

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

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
                        switch (anEvent) {
                            case STARTED:
                                myContentFragment.setSyncActive(true);

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
                                myContentFragment.setSyncActive(false);

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
        );
    }
}
