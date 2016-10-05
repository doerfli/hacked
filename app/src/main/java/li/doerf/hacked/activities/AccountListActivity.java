package li.doerf.hacked.activities;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
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

public class AccountListActivity extends AppCompatActivity implements IServiceRunningListener {
    private final String LOGTAG = getClass().getSimpleName();

    private FloatingActionButton myFloatingActionCheckButton;
    private ObjectAnimator myFabAnimation;
    private static boolean myIsActive = false;
    private AccountListFragment myContentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        myContentFragment = AccountListFragment.create();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, myContentFragment)
                .commit();

        myFloatingActionCheckButton = (FloatingActionButton) findViewById(R.id.button_check);
        myFloatingActionCheckButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myContentFragment.checkForBreaches(null);
            }
        });
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_activity_account_list, menu);
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

        return super.onOptionsItemSelected(item);
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
