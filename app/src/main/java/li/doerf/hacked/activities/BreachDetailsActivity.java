package li.doerf.hacked.activities;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;

import li.doerf.hacked.R;
import li.doerf.hacked.db.DatasetChangeListener;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.db.tables.Breach;
import li.doerf.hacked.ui.adapters.BreachesAdapter;

public class BreachDetailsActivity extends AppCompatActivity implements DatasetChangeListener {

    public static final String EXTRA_ACCOUNT_ID = "AccountId";
    private SQLiteDatabase myReadbableDb;
    private Account myAccount;
    private List<Breach> myBreaches;
    private BreachesAdapter myBreachesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breach_details);
        myReadbableDb = HackedSQLiteHelper.getInstance(getApplicationContext()).getReadableDatabase();
        long accountId = getIntent().getLongExtra(EXTRA_ACCOUNT_ID, -999);
        myAccount = Account.findById(myReadbableDb, accountId);
        myBreaches = Breach.findAllByAccount(myReadbableDb, myAccount);

        setTitle(myAccount.getName());

        if ( myBreaches.size() == 0 ) {
            CardView noBreachFound = (CardView) findViewById(R.id.no_breach_found);
            noBreachFound.setVisibility(View.VISIBLE);
        }

        myBreachesAdapter = new BreachesAdapter(this, myBreaches);

        RecyclerView breachesList = (RecyclerView) findViewById(R.id.breaches_list);
        breachesList.setHasFixedSize(true);
        LinearLayoutManager lm = new LinearLayoutManager(getApplicationContext());
        breachesList.setLayoutManager(lm);
        breachesList.setAdapter(myBreachesAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Breach.registerDatasetChangedListener(this, Breach.class);
    }

    @Override
    protected void onPause() {
        Breach.unregisterDatasetChangedListener(this, Breach.class);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        myReadbableDb = null;
        super.onDestroy();
    }

    @Override
    public void datasetChanged() {
        myBreaches = Breach.findAllByAccount(myReadbableDb, myAccount);
        myBreachesAdapter.changeList(myBreaches);
    }
}
