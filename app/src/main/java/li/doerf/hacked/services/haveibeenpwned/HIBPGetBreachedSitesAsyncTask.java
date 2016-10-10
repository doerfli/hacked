package li.doerf.hacked.services.haveibeenpwned;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.List;

import li.doerf.hacked.R;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.BreachedSite;
import li.doerf.hacked.remote.BreachedAccount;
import li.doerf.hacked.remote.HaveIBeenPwned;
import li.doerf.hacked.ui.fragments.BreachedSitesListFragment;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by moo on 09/10/16.
 */
public class HIBPGetBreachedSitesAsyncTask extends AsyncTask<Void,Void,Void> {
    private final String LOGTAG = getClass().getSimpleName();
    private final Context myContext;
    private final BreachedSitesListFragment myUiFragment;

    public HIBPGetBreachedSitesAsyncTask(BreachedSitesListFragment uiFragment) {
        myContext = uiFragment.getContext();
        myUiFragment = uiFragment;
    }

    @Override
    protected Void doInBackground(Void[] params) {
        SQLiteDatabase db = HackedSQLiteHelper.getInstance(myContext).getWritableDatabase();

        BreachedSite.deleteAll(db);

        Log.d(LOGTAG, "retrieving breached sites");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://haveibeenpwned.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        long i = 0;
        publishProgress();

        try {
            HaveIBeenPwned service = retrofit.create(HaveIBeenPwned.class);
            Call<List<BreachedAccount>> breachedSitesCall = service.getBreachedSites();
            Response<List<BreachedAccount>> response = breachedSitesCall.execute();
            List<BreachedAccount> breachedSites = response.body();

            for (BreachedAccount ba : breachedSites) {
                Log.d(LOGTAG, "breached site: " + ba.getName());
                BreachedSite site = BreachedSite.create(
                        ba.getName(),
                        ba.getTitle(),
                        ba.getDomain(),
                        DateTime.parse(ba.getBreachDate()),
                        DateTime.parse(ba.getAddedDate()),
                        ba.getPwnCount(),
                        ba.getDescription(),
                        ba.getDataClasses(),
                        ba.getIsVerified()
                );
                site.insert(db);

                if ( i++ % 10 == 0 ) {
                    publishProgress();
                }
            }

        } catch ( IOException e) {
            Log.e(LOGTAG, "caught IOException while getting breached sites", e);
            Toast.makeText(myContext, myContext.getString(R.string.error_download_data), Toast.LENGTH_LONG).show();
        } finally {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(myContext);
            settings.edit().putLong(myContext.getString(R.string.PREF_KEY_LAST_SYNC_HIBP_TOP20), System.currentTimeMillis()).apply();
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
        myUiFragment.refreshList();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        myUiFragment.refreshList();
        myUiFragment.refreshComplete();
    }
}
