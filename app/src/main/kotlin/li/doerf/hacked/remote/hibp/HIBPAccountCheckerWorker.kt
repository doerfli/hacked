package li.doerf.hacked.remote.hibp

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.crashlytics.android.Crashlytics
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResponse
import com.github.kittinunf.fuel.httpGet
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.InstanceIdResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import li.doerf.hacked.R
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.db.daos.AccountDao
import li.doerf.hacked.db.entities.Account
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutionException

/**
 * Created by moo on 26.03.17.
 */
class HIBPAccountCheckerWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        private val LOGTAG = "HIBPAccountCheckerWorke"
        const val BROADCAST_ACTION_ACCOUNT_CHECK_FINISHED = "li.doerf.hacked.BROADCAST_ACTION_ACCOUNT_CHECK_FINISHED"
        const val KEY_ID = "ID"
    }

    private val myAccountDao: AccountDao = AppDatabase.get(context).accountDao
    private var updateLastCheckTimestamp = false

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(LOGTAG, "doWork")
        try {
            doIt()
        } catch (ex: WorkFailedException) {
            if (ex.retry) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun doIt(): Result {
        val id = inputData.getLong(KEY_ID, -1)
        updateLastCheckTimestamp = id < 0
        checkGooglePlayServicesAvailable()
        val deviceTokenTask = FirebaseInstanceId.getInstance().instanceId
        return try {
            check(id, getDeviceToken(deviceTokenTask))
            Result.success()
        } catch (e: IOException) {
            Log.e(LOGTAG, "caught exception during check", e)
            Crashlytics.logException(e)
            throw WorkFailedException(true)
        } catch (e: Exception) {
            Crashlytics.logException(e)
            throw e
        } finally {
            doPostCheckActions()
        }
    }

    private fun getDeviceToken(deviceTokenTask: Task<InstanceIdResult>): String {
        return try {
            Tasks.await(deviceTokenTask).token
        } catch (e: ExecutionException) {
            Log.e(LOGTAG, "caught ExecutionException", e)
            Crashlytics.logException(e)
            throw WorkFailedException()
        } catch (e: InterruptedException) {
            Log.e(LOGTAG, "caught InterruptedException", e)
            Crashlytics.logException(e)
            throw WorkFailedException()
        }
    }

    private fun checkGooglePlayServicesAvailable() {
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(applicationContext) != ConnectionResult.RESULT_SUCCESS.errorCode) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                        context,
                        context.getString(R.string.toast_error_google_play_missing),
                        Toast.LENGTH_LONG).show()
            }
            doPostCheckActions()
            throw WorkFailedException()
        }
    }

    @Synchronized
    @Throws(IOException::class)
    private suspend fun check(id: Long, device_token: String) {
        Log.d(LOGTAG, "starting check for breaches")
        val accountsToCheck: MutableList<Account> = ArrayList()
        if (id < 0) {
            accountsToCheck.addAll(myAccountDao.all)
        } else {
            Log.d(LOGTAG, "only id $id")
            accountsToCheck.addAll(myAccountDao.findById(id))
        }
        for (account in accountsToCheck) {
            Log.d(LOGTAG, "Checking for account: " + account.name)
            sendSearch(account.name, device_token)
        }
        Log.d(LOGTAG, "finished checking for breaches")
    }

    @Throws(IOException::class)
    private suspend fun sendSearch(name: String, deviceToken: String) {
        Log.d(LOGTAG, "sending search request for account: $name")

        val url = "https://hibp-proxy.herokuapp.com/search"
        val (_, res, _) = url.httpGet(listOf("account" to name, "device_token" to deviceToken)).awaitByteArrayResponse()

        if (!res.isSuccessful) {
            Log.e(LOGTAG, "failure sending search request")
            throw WorkFailedException(true)
        }
    }

    private fun doPostCheckActions() {
        val localIntent = Intent(BROADCAST_ACTION_ACCOUNT_CHECK_FINISHED)
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent)
        Log.d(LOGTAG, "broadcast finish sent")
        if (updateLastCheckTimestamp) {
            val settings = PreferenceManager.getDefaultSharedPreferences(context)
            val ts = System.currentTimeMillis()
            settings.edit().putLong(context.getString(R.string.PREF_KEY_LAST_SYNC_TIMESTAMP), ts).apply()
            Log.i(LOGTAG, "updated last checked timestamp: $ts")
        }
    }

}

