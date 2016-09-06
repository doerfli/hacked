package li.doerf.hacked.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


import org.joda.time.DateTime;
import org.xml.sax.DTDHandler;

import java.io.IOException;
import java.util.List;

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

    public HaveIBeenPwnedCheckService() {
        super("HaveIBeenPwnedCheckService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOGTAG, "onHandleIntent");
        doCheck();
    }

    private void doCheck() {
        Context context = getBaseContext();
        SQLiteDatabase db = HackedSQLiteHelper.getInstance(context).getReadableDatabase();

        Cursor c = Account.listAll(db);
        int waitForNextRequest = 1500;

        while( c.moveToNext()) {
            Account account = Account.create( db, c);
            Log.d(LOGTAG, "Checking for account: " + account.getName());

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://haveibeenpwned.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            HaveIBeenPwned service = retrofit.create(HaveIBeenPwned.class);
            Call<List<BreachedAccount>> breachedAccountsList = service.listBreachedAccounts( account.getName());

            if ( waitForNextRequest > 0 ) {
                try {
                    Log.d(LOGTAG, "waiting for " + waitForNextRequest + " before next request");
                    Thread.sleep( waitForNextRequest);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                Response<List<BreachedAccount>> response = breachedAccountsList.execute();

                if ( response.isSuccessful()) {
                    for (BreachedAccount ba : response.body()) {
                        Breach existing = Breach.findByAccountAndName(db, account, ba.getName());

                        if ( existing != null ) {
                            // TODO handle existing (change account?)
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
                        // TODO update account
                    }
                } else {
                    if ( response.code() == 404 ) {
                        Log.i(LOGTAG, "no breach found: " + account.getName());
                        // TODO update account checked
                    } else {
                        Log.w(LOGTAG, "unexpected response code: " + response.code());
                    }
                }

//                Log.d(LOGTAG, response.headers().toString());
                String retryAfter = response.headers().get("Retry-After");
                if ( retryAfter != null ) {
                    waitForNextRequest = Integer.parseInt(retryAfter) * 1000;
                } else {
                    waitForNextRequest = 1500;
                }
            } catch (IOException e) {
                Log.e(LOGTAG, "caughtIOException while contacting www.haveibeenpwned.com", e);
            }
        }

        Log.i(LOGTAG, "finished checking");
    }
}
