package li.doerf.hacked.remote.haveibeenpwned;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.joda.time.DateTime;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import li.doerf.hacked.R;
import li.doerf.hacked.activities.MainActivity;
import li.doerf.hacked.db.AppDatabase;
import li.doerf.hacked.db.daos.AccountDao;
import li.doerf.hacked.db.daos.BreachDao;
import li.doerf.hacked.db.entities.Account;
import li.doerf.hacked.db.entities.Breach;
import li.doerf.hacked.utils.AccountHelper;
import li.doerf.hacked.utils.NotificationHelper;
import li.doerf.hacked.utils.OreoNotificationHelper;
import li.doerf.hacked.utils.RatingHelper;
import li.doerf.hacked.utils.StringHelper;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static li.doerf.hacked.R.id.account;

/**
 * Created by moo on 26.03.17.
 */

public class HIBPAccountCheckerWorker extends Worker {
    public static final String BROADCAST_ACTION_ACCOUNT_CHECK_FINISHED = "li.doerf.hacked.BROADCAST_ACTION_ACCOUNT_CHECK_FINISHED";
    public static final String KEY_ID = "ID";
    private static final String NOTIFICATION_GROUP_KEY_BREACHES = "group_key_breachs";
    private static long noReqBefore = 0;
    private final String LOGTAG = getClass().getSimpleName();

    private final WeakReference<Context> myContext;
    private final AccountDao myAccountDao;
    private final BreachDao myBreachDao;
    private boolean updateLastCheckTimestamp;

    public HIBPAccountCheckerWorker(@NonNull Context aContext, @NonNull WorkerParameters params) {
        super(aContext, params);
        myContext = new WeakReference<>(aContext);
        myAccountDao = AppDatabase.get(aContext).getAccountDao();
        myBreachDao = AppDatabase.get(aContext).getBreachDao();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(LOGTAG, "doWork");
        long id = getInputData().getLong(KEY_ID, -1);
        updateLastCheckTimestamp = id < 0;
        Boolean foundNewBreach = false;

        try {
            foundNewBreach = check(id);
            return Result.success();
        } catch (IOException e) {
            Log.e(LOGTAG, "caught exception during check", e);
            return Result.failure();
        } finally {
            doPostCheckActions(foundNewBreach);
        }
    }

    private synchronized Boolean check(Long id) throws IOException {
        Log.d(LOGTAG, "starting check for breaches");
        boolean newBreachFound = false;

        List<Account> accountsToCheck = new ArrayList<>();

        if ( id < 0) {
            accountsToCheck.addAll(myAccountDao.getAll());
        } else {
            Log.d(LOGTAG, "only id " + id);
            accountsToCheck.addAll(myAccountDao.findById(id));
        }

        for (Account account : accountsToCheck) {
            Log.d(LOGTAG, "Checking for account: " + account.getName());
            List<BreachedAccount> breachedAccounts = doCheck(account.getName());
            newBreachFound |= processBreachedAccounts( account, breachedAccounts);
        }

        Log.d(LOGTAG, "finished checking for breaches");

        return newBreachFound;
    }

    private boolean processBreachedAccounts(Account account, List<BreachedAccount> breachedAccounts) {
        boolean isNewBreachFound = false;

        for (BreachedAccount ba : breachedAccounts) {
            Breach existing = myBreachDao.findByAccountAndName(account.getId(), ba.getName());

            if (existing != null) {
                Log.d(LOGTAG, "breach already existing: " + ba.getName());
                continue;
            }

            Log.d(LOGTAG, "new breach: " + ba.getName());
            Breach newBreach = new Breach();
            newBreach.setAccount(account.getId());
            newBreach.setName(ba.getName());
            newBreach.setTitle(ba.getTitle());
            newBreach.setDomain(ba.getDomain());
            newBreach.setBreachDate(DateTime.parse(ba.getBreachDate()).getMillis());
            newBreach.setAddedDate(DateTime.parse(ba.getAddedDate()).getMillis());
            newBreach.setPwnCount(ba.getPwnCount());
            newBreach.setDescription(ba.getDescription());
            newBreach.setDataClasses(ba.getDataClasses() != null ? StringHelper.join(ba.getDataClasses(), ", ") : "");
            newBreach.setVerified(ba.getIsVerified());
            newBreach.setAcknowledged(false);
            myBreachDao.insert(newBreach);
            Log.i(LOGTAG, "breach inserted into db");
            isNewBreachFound = true;
        }

        account.setLastChecked(DateTime.now());
        if (isNewBreachFound && ! account.getHacked() ) {
            account.setHacked(true);
        }
        if (isNewBreachFound) {
            new AccountHelper(myContext.get()).updateBreachCounts(account);
        }
        myAccountDao.update(account);

        return isNewBreachFound;
    }

    private List<BreachedAccount> doCheck(String anAccount) throws IOException {
        try {
            Response<List<BreachedAccount>> response = retrieveBreaches(anAccount);

            String contentTypeHeader = response.headers().get("content-type");

            if (contentTypeHeader != null && ! contentTypeHeader.startsWith("application/json") ) {
//                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(myContext.get(), myContext.get().getString(R.string.toast_error_error_during_check), Toast.LENGTH_LONG).show());
                throw new AccessNotAllowedException("content-type in response was not application/json");
            }

            if (response.isSuccessful() ) {
                return response.body();
            } else {
                if (response.code() == 404) {
                    Log.i(LOGTAG, "no breach found: " + account);
                    return new ArrayList<>();
                } else {
                    Log.w(LOGTAG, "unexpected response code: " + response.code());
                }
            }
        } catch (IOException e) {
            Log.e(LOGTAG, "caughtIOException while contacting www.haveibeenpwned.com - " + e.getMessage(), e);
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(myContext.get(), myContext.get().getString(R.string.toast_error_error_during_check), Toast.LENGTH_LONG).show());
            throw e;
        } catch (AccessNotAllowedException e) {
            Log.e(LOGTAG, "caughtIOException while contacting www.haveibeenpwned.com - " + e.getMessage(), e);
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(myContext.get(), myContext.get().getString(R.string.toast_error_error_during_check), Toast.LENGTH_LONG).show());
            PreferenceManager.getDefaultSharedPreferences(myContext.get())
                    .edit()
                    .putLong(RatingHelper.PREF_KEY_LAST_ACCESS_DENIED_FAILURE, System.currentTimeMillis())
                    .apply();
            throw new IOException(e);
        }
        return new ArrayList<>();
    }

    @NonNull
    private synchronized Response<List<BreachedAccount>> retrieveBreaches(String account) throws IOException {
        Log.d(LOGTAG, "retrieving breaches: " + account);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Log.v("OkHttp", message));
        // set your desired log level
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(logging);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://haveibeenpwned.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
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

    private void doPostCheckActions(Boolean foundNewBreach) {
        Intent localIntent = new Intent(BROADCAST_ACTION_ACCOUNT_CHECK_FINISHED);
        LocalBroadcastManager.getInstance(myContext.get()).sendBroadcast(localIntent);
        Log.d(LOGTAG, "broadcast finish sent");

        if ( updateLastCheckTimestamp ) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(myContext.get());
            SharedPreferences.Editor editor = settings.edit();
            long ts = System.currentTimeMillis();
            editor.putLong(myContext.get().getString(R.string.PREF_KEY_LAST_SYNC_TIMESTAMP), ts);
            editor.apply();
            Log.i(LOGTAG, "updated last checked timestamp: " + ts);
        }

        if ( foundNewBreach) {
            showNotification();
        }
    }

    private void showNotification() {
        if ( android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            OreoNotificationHelper onh = new OreoNotificationHelper(myContext.get());
            onh.createNotificationChannel();
        }

        String title = myContext.get().getString(R.string.notification_title_new_breaches_found);
        androidx.core.app.NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(myContext.get(), OreoNotificationHelper.CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(myContext.get().getString(R.string.notification_text_click_to_open))
                        .setChannelId(OreoNotificationHelper.CHANNEL_ID)
                        .setOnlyAlertOnce(true)
                        .setGroup(NOTIFICATION_GROUP_KEY_BREACHES);

        Intent showBreachDetails = new Intent(myContext.get(), MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        myContext.get(),
                        0,
                        showBreachDetails,
                        PendingIntent.FLAG_ONE_SHOT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        Notification notification = mBuilder.build();
        NotificationHelper.notify(myContext.get(), notification);
    }

}
