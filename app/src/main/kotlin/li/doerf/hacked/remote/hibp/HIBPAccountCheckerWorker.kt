package li.doerf.hacked.remote.hibp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResponse
import com.github.kittinunf.fuel.httpGet
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import li.doerf.hacked.R
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.db.daos.AccountDao
import li.doerf.hacked.db.entities.Account
import li.doerf.hacked.ui.fragments.AccountsFragment
import li.doerf.hacked.util.FcmTokenManager
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import java.io.IOException
import java.util.*
import kotlin.random.Random


/**
 * Created by moo on 26.03.17.
 */
class HIBPAccountCheckerWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        private const val LOGTAG = "HIBPAccountCheckerWorke"
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
        return try {
            check(id, FcmTokenManager.getDeviceToken(context))
            Result.success()
        } catch (e: IOException) {
            Log.e(LOGTAG, "caught exception during check", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            throw WorkFailedException(true)
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            throw e
        } finally {
            doPostCheckActions()
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

    @Throws(IOException::class)
    private suspend fun check(id: Long, device_token: String) {
        Log.d(LOGTAG, "starting check for breaches")
        var accountsToCheck: MutableList<Account> = ArrayList()
        if (id < 0) {
            accountsToCheck.addAll(myAccountDao.all)
        } else {
            Log.d(LOGTAG, "only id $id")
            accountsToCheck.addAll(myAccountDao.findById(id))
        }
        if (accountsToCheck.size > AccountsFragment.MAX_ACCOUNTS) {
            Log.w(LOGTAG, "limiting results to first 50")
            accountsToCheck = accountsToCheck.filterIndexed { index, _ -> index < 50 }.toMutableList()
        }
        accountsToCheck.forEachIndexed { index, account ->
            delayAfter25Requests(index)
            Log.d(LOGTAG, "Checking for account ($index): " + account.name)
            sendSearch(account.name, device_token)
        }
        Log.d(LOGTAG, "finished checking for breaches")
    }

    private suspend fun delayAfter25Requests(index: Int) {
        if ((index > 0) && (index % 25 == 0)) {
            // when more than 25 accounts are sent, delay further request by 1-5 minutes
            val delayMs = ((Random.nextInt(3) + 1) * 60 * 1000).toLong()
            Log.i(LOGTAG, "delaying next requests by ${delayMs}ms to reduce strain on proxy")
            delay(delayMs)
        }
    }

    @Throws(IOException::class)
    private suspend fun sendSearch(name: String, deviceToken: String) {
        Log.d(LOGTAG, "sending search request for account: $name")

        val url = "https://hibp-proxy.bytes.li/search"
        val now = System.currentTimeMillis()
        val reqToken = String(Hex.encodeHex(DigestUtils.sha1("${name}-${now}-${deviceToken}}"))).toUpperCase(Locale.getDefault())
        val (_, res, _) = url
                .httpGet(listOf("account" to name, "device_token" to deviceToken))
                .header(mapOf("x-hacked-requestToken" to reqToken, "x-hacked-now" to now, "user-agent" to "Hacked android app: ${getVersion(context)}"))
                .awaitByteArrayResponse()

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

    private fun getVersion(context: Context): String? {
        try {
            val pInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(LOGTAG, "caught PackageManager.NameNotFoundException", e)
        }
        return "unknown"
    }

}

