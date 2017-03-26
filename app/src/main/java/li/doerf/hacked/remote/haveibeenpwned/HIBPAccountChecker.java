package li.doerf.hacked.remote.haveibeenpwned;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.common.collect.Lists;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import li.doerf.hacked.R;
import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.db.tables.Breach;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static li.doerf.hacked.R.id.account;

/**
 * Created by moo on 26.03.17.
 */

public class HIBPAccountChecker {
    private final String LOGTAG = getClass().getSimpleName();
    private static long noReqBefore = 0;

    private final Context myContext;

    public HIBPAccountChecker(Context aContext) {
        myContext = aContext;
    }
        
    public List<BreachedAccount> check(String anAccount) {
        try {
            Response<List<BreachedAccount>> response = retrieveBreaches(anAccount);

            if (response.isSuccessful()) {
                List<BreachedAccount> breachedAccounts = response.body();
                return breachedAccounts;
            } else {
                if (response.code() == 404) {
                    Log.i(LOGTAG, "no breach found: " + account);
                    return Lists.newArrayList();
                } else {
                    Log.w(LOGTAG, "unexpected response code: " + response.code());
                }
            }

        } catch (IOException e) {
            Log.e(LOGTAG, "caughtIOException while contacting www.haveibeenpwned.com - " + e.getMessage(), e);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(myContext, myContext.getString(R.string.toast_error_error_during_check), Toast.LENGTH_LONG).show();
                }
            });
        }
        return null;
    }

    @NonNull
    private synchronized Response<List<BreachedAccount>> retrieveBreaches(String account) throws IOException {
        Log.d(LOGTAG, "retrieving breaches: " + account);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://haveibeenpwned.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        HaveIBeenPwned service = retrofit.create(HaveIBeenPwned.class);
        Call<List<BreachedAccount>> breachedAccountsList = service.listBreachedAccounts(account);
        long timeDelta = noReqBefore - System.currentTimeMillis();

        if (timeDelta > 0) {
            try {
                Log.d(LOGTAG, "waiting for " + timeDelta + "ms before next request");
                Thread.sleep(timeDelta);
            } catch (InterruptedException e) {
                Log.e(LOGTAG, "caught InterruptedException while waiting for next request slot", e);
            }
        }

        Response<List<BreachedAccount>> response = breachedAccountsList.execute();

        // check next request timeout
        String retryAfter = response.headers().get("Retry-After");
        Random random = new Random();
        if (retryAfter != null) {
            noReqBefore = System.currentTimeMillis() + (Integer.parseInt(retryAfter) * 1000) + random.nextInt(100);
        } else {
            noReqBefore = System.currentTimeMillis() + (1500 + random.nextInt(100));
        }

        return response;
    }
    
}
