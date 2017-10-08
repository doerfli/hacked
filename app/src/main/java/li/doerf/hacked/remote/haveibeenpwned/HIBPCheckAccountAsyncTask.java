package li.doerf.hacked.remote.haveibeenpwned;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import li.doerf.hacked.R;
import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.services.CheckServiceHelper;
import li.doerf.hacked.ui.fragments.AccountListFragment;


public class HIBPCheckAccountAsyncTask extends AsyncTask<Long,Account,Boolean> implements IProgressUpdater {

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

        HIBPAccountChecker checker = new HIBPAccountChecker(myContext, this);

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

        if ( updateLastCheckTimestamp ) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(myContext);
            SharedPreferences.Editor editor = settings.edit();
            long ts = System.currentTimeMillis();
            editor.putLong(myContext.getString(R.string.PREF_KEY_LAST_SYNC_TIMESTAMP), ts);
            editor.apply();
            Log.i(LOGTAG, "updated last checked timestamp: " + ts);
        }

        if ( aFoundNewBreach) {
            CheckServiceHelper h = new CheckServiceHelper(myContext);
            h.showNotification();
        }
    }

    @Override
    public void updateProgress(Account account) {
        publishProgress( account);
    }
}
