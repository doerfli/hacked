package li.doerf.hacked.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import li.doerf.hacked.HackedApplication;
import li.doerf.hacked.R;
import li.doerf.hacked.ui.fragments.BreachDetailsFragment;

public class BreachDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_ACCOUNT_ID = "AccountId";
    private BreachDetailsFragment myContentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breach_details);
        long accountId = getIntent().getLongExtra(EXTRA_ACCOUNT_ID, -999);

        HackedApplication application = (HackedApplication) getApplication();

        myContentFragment = BreachDetailsFragment.create(accountId);
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
