package li.doerf.hacked.services.haveibeenpwned;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.List;

import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.BreachedSite;
import li.doerf.hacked.remote.BreachedAccount;
import li.doerf.hacked.remote.HaveIBeenPwned;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by moo on 09/10/16.
 */
public class GetBreachedSitesAsyncTask extends AsyncTask {
    private final String LOGTAG = getClass().getSimpleName();
    private final Context myContext;

    public GetBreachedSitesAsyncTask(Context aContext) {
        myContext = aContext;
    }

    @Override
    protected Object doInBackground(Object[] params) {
        SQLiteDatabase db = HackedSQLiteHelper.getInstance(myContext).getWritableDatabase();

        BreachedSite.deleteAll(db);

        Log.d(LOGTAG, "retrieving breached sites");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://haveibeenpwned.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

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
            }

        } catch ( IOException e) {
            Log.e(LOGTAG, "caught IOException while getting breached sites", e);
            // TODO handle this
        }

        return null;
    }
}
