package li.doerf.hacked.remote.haveibeenpwned;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import li.doerf.hacked.R;
import li.doerf.hacked.activities.MainActivity;
import li.doerf.hacked.db.HackedSQLiteHelper;
import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.db.tables.Breach;
import li.doerf.hacked.ui.fragments.AccountListFragment;
import li.doerf.hacked.utils.NotificationHelper;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class HIBPCheckAccountAsyncTask extends AsyncTask<Long,Account,Boolean> {
    private final static String NOTIFICATION_GROUP_KEY_BREACHES = "group_key_breachs";

    private final String LOGTAG = getClass().getSimpleName();
    private final Context myContext;
    private static long noReqBefore = 0;
    private static boolean running;
    private final AccountListFragment myFragment;
    private boolean updateLastCheckTimestamp = false;

    public HIBPCheckAccountAsyncTask(Context aContext, AccountListFragment aFragment) {
        myContext = aContext;
        myFragment = aFragment;
    }

    public static boolean isRunning() {
        return running;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        running = true;
    }

    @Override
    protected Boolean doInBackground(Long... accountids) {
        boolean foundNewBreaches = false;
        if ( accountids.length > 0 ) {
            for (Long id : accountids) {
                foundNewBreaches = doCheck(id);
            }
        } else {
            updateLastCheckTimestamp = true;
            foundNewBreaches = doCheck(null);
        }

        return foundNewBreaches;
    }

    @Override
    protected void onProgressUpdate(Account... accounts) {
        super.onProgressUpdate(accounts);
        if ( accounts.length > 0 ) {
            accounts[0].notifyObservers();
        }
    }

    @Override
    protected void onPostExecute(Boolean aFoundNewBreach) {
        super.onPostExecute(aFoundNewBreach);
        running = false;
        if ( myFragment != null ) {
            myFragment.refreshComplete();
        }
        if ( aFoundNewBreach ) {
            showNotification();
        }

        if ( updateLastCheckTimestamp ) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(myContext);
            SharedPreferences.Editor editor = settings.edit();
            long ts = System.currentTimeMillis();
            editor.putLong(myContext.getString(R.string.PREF_KEY_LAST_SYNC_TIMESTAMP), ts);
            editor.apply();
            Log.i(LOGTAG, "updated last checked timestamp: " + ts);
        }
    }

    private Boolean doCheck(Long id) {
        Log.d(LOGTAG, "starting check for breaches");
        SQLiteDatabase db = HackedSQLiteHelper.getInstance(myContext).getWritableDatabase();
        HIBPAccountChecker checker = new HIBPAccountChecker( myContext);
        boolean newBreachFound = false;

        Cursor c = null;

        try {
            if ( id == null ) {
                Log.d(LOGTAG, "all ids");
                c = Account.listAll(db);
            } else {
                Log.d(LOGTAG, "only id " + id);
                c = Account.findCursorById(db, id);
            }

            while (c.moveToNext()) {
                Account account = Account.create(db, c);
                Log.d(LOGTAG, "Checking for account: " + account.getName());

                try {
                    List<BreachedAccount> breachedAccounts = checker.check(account.getName());
                    newBreachFound |= processBreachedAccounts( db, account, breachedAccounts);
                } finally {
                    publishProgress();
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

    private void showNotification() {
        if ( AccountListFragment.isFragmentShown() ) {
            Log.d(LOGTAG, "AccountListFragment active, no notification shown");
            return;
        }

        String title = myContext.getString(R.string.notification_title_new_breaches_found);
        android.support.v4.app.NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(myContext)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setGroup(NOTIFICATION_GROUP_KEY_BREACHES);

        Intent showBreachDetails = new Intent(myContext, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        myContext,
                        0,
                        showBreachDetails,
                        PendingIntent.FLAG_ONE_SHOT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        Notification notification = mBuilder.build();
        notification.flags |= Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;
        NotificationHelper.notify(myContext, notification);
    }
}
