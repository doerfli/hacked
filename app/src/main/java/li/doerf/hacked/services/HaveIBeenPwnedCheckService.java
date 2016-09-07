package li.doerf.hacked.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.db.tables.Breach;
import li.doerf.hacked.remote.BreachedAccount;
import li.doerf.hacked.remote.HaveIBeenPwned;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class HaveIBeenPwnedCheckService extends IntentService {
    private final String LOGTAG = getClass().getSimpleName();
    private static long noReqBefore = 0;

    public HaveIBeenPwnedCheckService() {
        super("HaveIBeenPwnedCheckService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOGTAG, "onHandleIntent");
        doCheck();
    }

    private void doCheck() {
        Log.d(LOGTAG, "starting check for braches");
        Context context = getBaseContext();
        SQLiteDatabase db = HackedSQLiteHelper.getInstance(context).getReadableDatabase();

        Cursor c = null;

        try {
            c = Account.listAll(db);

            while (c.moveToNext()) {
                Account account = Account.create(db, c);
                Log.d(LOGTAG, "Checking for account: " + account.getName());

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl("https://haveibeenpwned.com/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

                HaveIBeenPwned service = retrofit.create(HaveIBeenPwned.class);
                Call<List<BreachedAccount>> breachedAccountsList = service.listBreachedAccounts(account.getName());
                long timeDelta = noReqBefore - System.currentTimeMillis();

                if (timeDelta > 0) {
                    try {
                        Log.d(LOGTAG, "waiting for " + timeDelta + "ms before next request");
                        Thread.sleep(timeDelta);
                    } catch (InterruptedException e) {
                        Log.e(LOGTAG, "caught InterruptedException while waiting for next request slot", e);
                    }
                }

                boolean isNewBreachFound = false;

                try {

                    Response<List<BreachedAccount>> response = breachedAccountsList.execute();

                    // check for next request timeout
                    String retryAfter = response.headers().get("Retry-After");
                    Random random = new Random();
                    if (retryAfter != null) {
                        noReqBefore = System.currentTimeMillis() + (Integer.parseInt(retryAfter) * 1000) + random.nextInt(100);
                    } else {
                        noReqBefore = System.currentTimeMillis() + (1500 + random.nextInt(100));
                    }

                    db.beginTransaction();

                    if (response.isSuccessful()) {
                        for (BreachedAccount ba : response.body()) {
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
                                    ba.getDataClass(),
                                    ba.getIsVerified(),
                                    false
                            );
                            breach.insert(db);
                            Log.i(LOGTAG, "breach inserted into db");
                            isNewBreachFound = true;
                        }
                    } else {
                        if (response.code() == 404) {
                            Log.i(LOGTAG, "no breach found: " + account.getName());
                        } else {
                            Log.w(LOGTAG, "unexpected response code: " + response.code());
                        }
                    }

                    account.setLastChecked(DateTime.now());
                    if (isNewBreachFound) {
                        account.setHacked(isNewBreachFound);
                    }
                    account.update(db);
                    db.setTransactionSuccessful();
                } catch (IOException e) {
                    Log.e(LOGTAG, "caughtIOException while contacting www.haveibeenpwned.com", e);
                } finally {
                    db.endTransaction();
                    account.notifyObservers();
                }
            }
        } finally {
            Log.d(LOGTAG, "finished checking for breaches");
            if ( c != null ) c.close();
        }

    }
}
