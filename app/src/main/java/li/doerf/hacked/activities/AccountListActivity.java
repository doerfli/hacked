package li.doerf.hacked.activities;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import li.doerf.hacked.R;
import li.doerf.hacked.ui.fragments.AccountListFragment;

public class AccountListActivity extends AppCompatActivity {
    private final String LOGTAG = getClass().getSimpleName();

    private FloatingActionButton myFloatingActionCheckButton;
    private ObjectAnimator myFabAnimation;
    private boolean mySyncActive;
    private static boolean myIsActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, AccountListFragment.create())
                .commit();

        // TODO
//        myFloatingActionCheckButton = (FloatingActionButton) findViewById(R.id.button_check);
//        myFloatingActionCheckButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                checkForBreaches(view, null);
//            }
//        });
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
        // TODO
//        Account.registerDatasetChangedListener(this, Account.class);
//        ServiceRunningNotifier.registerServiceRunningListener(this);
        // TODO
//        refreshList();
        myIsActive = true;
    }

    @Override
    protected void onPause() {
        // TODO
//        Account.unregisterDatasetChangedListener(this, Account.class);
//        ServiceRunningNotifier.unregisterServiceRunningListener(this);
        myIsActive = false;
        super.onPause();
    }

    @Override
    public void onDestroy() {
        // TODO
//        if ( myCursor != null ) {
//            myCursor.close();
//        }
//        myReadbableDb = null;
        myIsActive = false; // just to be sure
        super.onDestroy();
    }

    public static boolean isActive() {
        return myIsActive;
    }
}
