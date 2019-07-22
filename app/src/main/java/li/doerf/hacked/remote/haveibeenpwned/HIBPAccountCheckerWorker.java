package li.doerf.hacked.remote.haveibeenpwned;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import li.doerf.hacked.R;
import li.doerf.hacked.db.AppDatabase;
import li.doerf.hacked.db.daos.AccountDao;
import li.doerf.hacked.db.entities.Account;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by moo on 26.03.17.
 */

public class HIBPAccountCheckerWorker extends Worker {
    public static final String BROADCAST_ACTION_ACCOUNT_CHECK_FINISHED = "li.doerf.hacked.BROADCAST_ACTION_ACCOUNT_CHECK_FINISHED";
    public static final String KEY_ID = "ID";
    private final String LOGTAG = getClass().getSimpleName();

    private final WeakReference<Context> myContext;
    private final AccountDao myAccountDao;
    private boolean updateLastCheckTimestamp;

    public HIBPAccountCheckerWorker(@NonNull Context aContext, @NonNull WorkerParameters params) {
        super(aContext, params);
        myContext = new WeakReference<>(aContext);
        myAccountDao = AppDatabase.get(aContext).getAccountDao();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(LOGTAG, "doWork");
        long id = getInputData().getLong(KEY_ID, -1);
        updateLastCheckTimestamp = id < 0;

        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext()) != ConnectionResult.RESULT_SUCCESS.getErrorCode() ) {
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(
                    myContext.get(),
                    myContext.get().getString(R.string.toast_error_google_play_missing),
                    Toast.LENGTH_LONG).show());
            doPostCheckActions();
            return Result.failure();
        }

        Task<InstanceIdResult> deviceTokenTask = FirebaseInstanceId.getInstance().getInstanceId();
        InstanceIdResult result;

        try {
            result = Tasks.await(deviceTokenTask);
        } catch (ExecutionException e) {
            Log.e(LOGTAG, "caught ExecutionException", e);
            return Result.failure();
        } catch (InterruptedException e) {
            Log.e(LOGTAG, "caught InterruptedException", e);
            return Result.failure();
        }

        try {
            check(id, result.getToken());
            return Result.success();
        } catch (IOException e) {
            Log.e(LOGTAG, "caught exception during check", e);
            return Result.retry();
        } finally {
            doPostCheckActions();
        }
    }

    private synchronized void check(Long id, String device_token) throws IOException {
        Log.d(LOGTAG, "starting check for breaches");

        List<Account> accountsToCheck = new ArrayList<>();

        if ( id < 0) {
            accountsToCheck.addAll(myAccountDao.getAll());
        } else {
            Log.d(LOGTAG, "only id " + id);
            accountsToCheck.addAll(myAccountDao.findById(id));
        }

        for (Account account : accountsToCheck) {
            Log.d(LOGTAG, "Checking for account: " + account.getName());
            sendSearch(account.getName(), device_token);
        }

        Log.d(LOGTAG, "finished checking for breaches");
    }

    private void sendSearch(String name, String deviceToken) throws IOException {
        Log.d(LOGTAG, "sending search request for account: " + name);
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Log.v("OkHttp", message));
        // set your desired log level
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(logging);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://hibp-proxy.herokuapp.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();
        
        Log.d(LOGTAG, "firebase token: " + deviceToken);
        HIBPProxy service = retrofit.create(HIBPProxy.class);
        Call<Void> breachedAccountsList = service.search(name, deviceToken);
        breachedAccountsList.execute();
    }

    private void doPostCheckActions() {
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
    }

}
