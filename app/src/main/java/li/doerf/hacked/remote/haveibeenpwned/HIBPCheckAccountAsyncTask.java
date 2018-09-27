package li.doerf.hacked.remote.haveibeenpwned;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import java.lang.ref.WeakReference;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import li.doerf.hacked.R;
import li.doerf.hacked.db.entities.Account;
import li.doerf.hacked.services.CheckServiceHelper;

public class HIBPCheckAccountAsyncTask extends AsyncTask<Long,Account,Boolean> implements IProgressUpdater {

    public static final String BROADCAST_ACTION_ACCOUNT_CHECK_FINISHED = "li.doerf.hacked.BROADCAST_ACTION_ACCOUNT_CHECK_FINISHED";
    private final String LOGTAG = getClass().getSimpleName();
    private final WeakReference<Context> myContext;
    private static boolean running;
    private boolean updateLastCheckTimestamp = false;

    public HIBPCheckAccountAsyncTask(Context aContext) {
        myContext = new WeakReference<>(aContext);
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

        HIBPAccountChecker checker = new HIBPAccountChecker(myContext.get(), this);

        if ( accountids.length > 0 ) {
            for (Long id : accountids) {
                foundNewBreaches = checker.check(id);
            }
        } else {
            updateLastCheckTimestamp = true;
            foundNewBreaches = checker.check(null);
        }

        return foundNewBreaches;
    }

    @Override
    protected void onPostExecute(Boolean aFoundNewBreach) {
        super.onPostExecute(aFoundNewBreach);
        running = false;

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

        if ( aFoundNewBreach) {
            CheckServiceHelper h = new CheckServiceHelper(myContext.get());
            h.showNotification();
        }
    }

    @Override
    public void updateProgress(Account account) {
        publishProgress( account);
    }
}
