package li.doerf.hacked.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import li.doerf.hacked.HackedApplication;
import li.doerf.hacked.R;
import li.doerf.hacked.ui.fragments.BreachDetailsFragment;

public class BreachDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_ACCOUNT_ID = "AccountId";
    private BreachDetailsFragment myContentFragment;
    private Tracker myTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breach_details);
        long accountId = getIntent().getLongExtra(EXTRA_ACCOUNT_ID, -999);

        HackedApplication application = (HackedApplication) getApplication();
        myTracker = application.getDefaultTracker();

        myContentFragment = BreachDetailsFragment.create(accountId, myTracker);
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, myContentFragment)
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
