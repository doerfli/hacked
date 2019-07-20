package li.doerf.hacked.remote.haveibeenpwned;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.joda.time.DateTime;

import java.io.IOException;
import java.lang.ref.WeakReference;

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
import li.doerf.hacked.utils.StringHelper;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HIBPAccountResponseWorker extends Worker {

    private static final String TAG = "HIBPAccountResponseWork";
    private static final String NOTIFICATION_GROUP_KEY_BREACHES = "group_key_breachs";
    public static final String KEY_ACCOUNT = "Account";
    public static final String KEY_BREACHES = "Breaches";
    private final WeakReference<Context> myContext;
    private final AccountDao myAccountDao;
    private final BreachDao myBreachDao;

    public HIBPAccountResponseWorker(@NonNull Context aContext, @NonNull WorkerParameters workerParams) {
        super(aContext, workerParams);
        myContext = new WeakReference<>(aContext);
        myAccountDao = AppDatabase.get(aContext).getAccountDao();
        myBreachDao = AppDatabase.get(aContext).getBreachDao();
    }

    @NonNull
    @Override
    public Result doWork() {
        String[] breaches = getInputData().getStringArray(KEY_BREACHES);
        String accountName = getInputData().getString(KEY_ACCOUNT);
        Account account = myAccountDao.findByName(accountName).get(0);

        try {
            boolean foundNewBreach  = false;

            for (String breachName : breaches) {
                Log.d(TAG, breachName);
                foundNewBreach |= handleBreach(account, breachName);
            }

            account.setLastChecked(DateTime.now());
            if (foundNewBreach && ! account.getHacked() ) {
                account.setHacked(true);
            }
            if (foundNewBreach) {
                new AccountHelper(myContext.get()).updateBreachCounts(account);
            }
            myAccountDao.update(account);


            // TODO v3 check if breach already exists
            // TODO v3 if not add breach

            return Result.success();
        } catch (IOException e) {
            Log.e(TAG, "caughtIOException while contacting www.haveibeenpwned.com - " + e.getMessage(), e);
            // TODO better error
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(myContext.get(), myContext.get().getString(R.string.toast_error_error_during_check), Toast.LENGTH_LONG).show());
            return Result.failure();
        }
    }

    private boolean handleBreach(Account account, String breachName) throws IOException {
        Breach existing = myBreachDao.findByAccountAndName(account.getId(), breachName);

        if (existing != null) {
            Log.d(TAG, "breach already existing: " + breachName);
            return false;
        }

        BreachedAccount ba = getBreachedAccount(breachName);

        Log.d(TAG, "new breach: " + ba.getName());
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
        Log.i(TAG, "breach inserted into db");

        return true;
    }

    private BreachedAccount getBreachedAccount(String breachName) throws IOException {
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
        Call<BreachedAccount> call = service.getBreach(breachName);
        Response<BreachedAccount> response = call.execute();
        return response.body();
    }

    private void doPostCheckActions(Boolean foundNewBreach) {
        // TODO here or before?
//        Intent localIntent = new Intent(HIBPAccountCheckerWorker.BROADCAST_ACTION_ACCOUNT_CHECK_FINISHED);
//        LocalBroadcastManager.getInstance(myContext.get()).sendBroadcast(localIntent);
//        Log.d(TAG, "broadcast finish sent");

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
