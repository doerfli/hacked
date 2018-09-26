package li.doerf.hacked.remote.haveibeenpwned;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.common.base.Joiner;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.List;

import li.doerf.hacked.R;
import li.doerf.hacked.db.AppDatabase;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.daos.BreachedSiteDao;
import li.doerf.hacked.db.entities.BreachedSite;
import li.doerf.hacked.ui.fragments.BreachedSitesListFragment;
import li.doerf.hacked.utils.ConnectivityHelper;
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
    private final BreachedSiteDao myBreachedSiteDao;

    public HIBPGetBreachedSitesAsyncTask(BreachedSitesListFragment uiFragment) {
        myContext = uiFragment.getContext();
        myUiFragment = uiFragment;
        myBreachedSiteDao = AppDatabase.get(uiFragment.getContext()).getBrachedSiteDao();
    }

    @Override
    protected Void doInBackground(Void[] params) {
        SQLiteDatabase db = HackedSQLiteHelper.getInstance(myContext).getWritableDatabase();

        if ( ! ConnectivityHelper.isConnected(myContext) ) {
            Log.w(LOGTAG, "no network available");
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(myContext, myContext.getString(R.string.toast_error_no_network), Toast.LENGTH_LONG).show();
                }
            });
            return null;
        }

        List<BreachedSite> allOldSites = myBreachedSiteDao.getAll();
        if (!allOldSites.isEmpty()) {
            Log.d(LOGTAG, "deleting old sites");
            myBreachedSiteDao.delete(allOldSites.toArray(new BreachedSite[0]));
        }

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

                BreachedSite site = new BreachedSite();
                site.setName(ba.getName());
                site.setTitle(ba.getTitle());
                site.setDomain(ba.getDomain());
                site.setBreachDate(DateTime.parse(ba.getBreachDate()).getMillis());
                site.setAddedDate(DateTime.parse(ba.getAddedDate()).getMillis());
                site.setPwnCount(ba.getPwnCount());
                site.setDescription(ba.getDescription());
                site.setDataClasses(ba.getDataClasses() != null ? Joiner.on(", ").join(ba.getDataClasses()) : "");
                site.setVerified(ba.getIsVerified());

                myBreachedSiteDao.insert(site);
            }

        } catch ( IOException e) {
            Log.e(LOGTAG, "caught IOException while getting breached sites", e);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(myContext, myContext.getString(R.string.error_download_data), Toast.LENGTH_LONG).show();
                }
            });
        } finally {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(myContext);
            settings.edit().putLong(myContext.getString(R.string.PREF_KEY_LAST_SYNC_HIBP_TOP20), System.currentTimeMillis()).apply();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        myUiFragment.refreshComplete();
    }
}
