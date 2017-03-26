package li.doerf.hacked.remote.haveibeenpwned;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import li.doerf.hacked.R;
import li.doerf.hacked.activities.MainActivity;
import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.ui.fragments.AccountListFragment;
import li.doerf.hacked.utils.NotificationHelper;


public class HIBPCheckAccountAsyncTask extends AsyncTask<Long,Account,Boolean> implements IProgressUpdater {
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
                        .setContentText(myContext.getString(R.string.notification_text_click_to_open))
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

    @Override
    public void updateProgress(Account account) {
        publishProgress( account);
    }
}
