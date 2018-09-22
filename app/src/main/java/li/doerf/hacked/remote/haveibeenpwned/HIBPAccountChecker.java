package li.doerf.hacked.remote.haveibeenpwned;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.common.collect.Lists;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import androidx.annotation.NonNull;
import li.doerf.hacked.R;
import li.doerf.hacked.db.HackedSQLiteHelper;
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
    private final IProgressUpdater myProgressUpdater;
    private boolean abort = false;

    public HIBPAccountChecker(Context aContext, IProgressUpdater aProgressUpdates) {
        myContext = aContext;
        myProgressUpdater = aProgressUpdates;
    }

    public Boolean check(Long id) {
        Log.d(LOGTAG, "starting check for breaches");
        SQLiteDatabase db = HackedSQLiteHelper.getInstance(myContext).getWritableDatabase();
        boolean newBreachFound = false;
        abort = false;

        Cursor c = null;

        try {
            if ( id == null ) {
                Log.d(LOGTAG, "all ids");
                c = Account.listAll(db);
            } else {
                Log.d(LOGTAG, "only id " + id);
                c = Account.findCursorById(db, id);
            }

            while (c.moveToNext() && ! abort) {
                Account account = Account.create(db, c);
                Log.d(LOGTAG, "Checking for account: " + account.getName());

                try {
                    List<BreachedAccount> breachedAccounts = doCheck(account.getName());
                    newBreachFound |= processBreachedAccounts( db, account, breachedAccounts);
                } finally {
                    myProgressUpdater.updateProgress( account);
                }
            }
        } finally {
            Log.d(LOGTAG, "finished checking for breaches");
            if ( c != null ) c.close();
        }

        return newBreachFound;
    }

    private boolean processBreachedAccounts(SQLiteDatabase db, Account account, List<BreachedAccount> breachedAccounts) {
        boolean isNewBreachFound = false;

        for (BreachedAccount ba : breachedAccounts) {
            Breach existing = Breach.findByAccountAndName(db, account, ba.getName());

            if (existing != null) {
                Log.d(LOGTAG, "breach already existing: " + ba.getName());
                continue;
            }

            Log.d(LOGTAG, "new breach: " + ba.getName());
            Breach breach = Breach.create(
                    account,
                    ba.getName(),
                    ba.getTitle(),
                    ba.getDomain(),
                    DateTime.parse(ba.getBreachDate()),
                    DateTime.parse(ba.getAddedDate()),
                    ba.getPwnCount(),
                    ba.getDescription(),
                    ba.getDataClasses(),
                    ba.getIsVerified(),
                    false
            );
            breach.insert(db);
            Log.i(LOGTAG, "breach inserted into db");
            isNewBreachFound |= true;
        }

        account.setLastChecked(DateTime.now());
        if (isNewBreachFound && ! account.isHacked() ) {
            account.setHacked(true);
        }
        account.update(db);

        return isNewBreachFound;
    }
        
    private List<BreachedAccount> doCheck(String anAccount) {
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
        return Lists.newArrayList();
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

    public void abort() {
        abort = true;
    }
}
