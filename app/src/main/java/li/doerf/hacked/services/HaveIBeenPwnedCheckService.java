package li.doerf.hacked.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import li.doerf.hacked.R;
import li.doerf.hacked.activities.AccountListActivity;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.db.tables.Breach;
import li.doerf.hacked.remote.BreachedAccount;
import li.doerf.hacked.remote.HaveIBeenPwned;
import li.doerf.hacked.utils.IServiceRunningListener;
import li.doerf.hacked.utils.NotificationHelper;
import li.doerf.hacked.utils.ServiceRunningNotifier;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class HaveIBeenPwnedCheckService extends IntentService {
    public static final String EXTRA_IDS = "EXTRA_IDS";
    private final String LOGTAG = getClass().getSimpleName();
    private static long noReqBefore = 0;

    public HaveIBeenPwnedCheckService() {
        super("HaveIBeenPwnedCheckService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOGTAG, "onHandleIntent");

        try {
            ServiceRunningNotifier.notifyServiceRunningListeners(IServiceRunningListener.Event.STARTED);

            long[] ids = intent.getLongArrayExtra(EXTRA_IDS);

            doCheck(ids != null ? toLongArray(ids) : null);
        } finally {
            ServiceRunningNotifier.notifyServiceRunningListeners(IServiceRunningListener.Event.STOPPED);
        }

    }

    private Long[] toLongArray(long[] longs) {
        Long[] r = new Long[longs.length];

        for ( int i = 0; i < longs.length; i++) {
            r[i] = longs[i];
        }

        return r;
    }

    private void doCheck(Long[] ids) {
        Log.d(LOGTAG, "starting check for breaches");
        Context context = getBaseContext();
        SQLiteDatabase db = HackedSQLiteHelper.getInstance(context).getReadableDatabase();

        Cursor c = null;
        List<Account> newBreachedAccounts = new ArrayList<>();

        try {
            if ( ids == null ) {
                Log.d(LOGTAG, "all ids");
                c = Account.listAll(db);
            } else {
                Log.d(LOGTAG, "only " + ids.length + " ids");
                c = Account.findCursorByIds(db, ids);
            }

            while (c.moveToNext()) {
                Account account = Account.create(db, c);
                Log.d(LOGTAG, "Checking for account: " + account.getName());

                boolean isNewBreachFound = false;

                try {
                    db.beginTransaction();
                    Response<List<BreachedAccount>> response = retrieveBreaches(account);

                    if (response.isSuccessful()) {
                        List<BreachedAccount> breachedAccounts = response.body();
                        isNewBreachFound = processBreachedAccounts(db, account, breachedAccounts);
                    } else {
                        if (response.code() == 404) {
                            Log.i(LOGTAG, "no breach found: " + account.getName());
                        } else {
                            Log.w(LOGTAG, "unexpected response code: " + response.code());
                        }
                    }

                    account.setLastChecked(DateTime.now());
                    if (isNewBreachFound) {
                        account.setHacked(true);
                        newBreachedAccounts.add(account);
                    }
                    account.update(db);
                    db.setTransactionSuccessful();
                } catch (IOException e) {
                    Log.e(LOGTAG, "caughtIOException while contacting www.haveibeenpwned.com - " + e.getMessage(), e);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), getString(R.string.toast_error_error_during_check), Toast.LENGTH_LONG).show();
                        }
                    });
                    break;
                } finally {
                    db.endTransaction();
                    account.notifyObservers();
                }
            }

            if ( newBreachedAccounts.size() > 0 ) {
                showNotification(newBreachedAccounts);
            }
        } finally {
            Log.d(LOGTAG, "finished checking for breaches");
            if ( c != null ) c.close();
        }

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
            isNewBreachFound = true;
        }

        return isNewBreachFound;
    }

    @NonNull
    private Response<List<BreachedAccount>> retrieveBreaches(Account account) throws IOException {
        Log.d(LOGTAG, "retrieving breaches: " + account.getName());
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

    private void showNotification(List<Account> newBreachedAccounts) {
        List<String> names = FluentIterable.from(newBreachedAccounts).transform(new Function<Account,String>() {
            @Override
            public String apply(Account input) {
                return input.getName();
            }
        }).toList();

        android.support.v4.app.NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(getApplicationContext().getString(R.string.notification_new_breach_found))
                        .setContentText(Joiner.on(", ").join(names));

        Intent showBreachDetails = new Intent(getApplicationContext(), AccountListActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        getApplicationContext(),
                        0,
                        showBreachDetails,
                        PendingIntent.FLAG_ONE_SHOT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        Notification notification = mBuilder.build();
        notification.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;
        NotificationHelper.notify(getApplicationContext(), notification);
    }
}
