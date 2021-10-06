package li.doerf.hacked.remote.hibp

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.fuel.httpGet
import com.google.firebase.crashlytics.FirebaseCrashlytics
import li.doerf.hacked.R
import li.doerf.hacked.activities.NavActivity
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.db.daos.AccountDao
import li.doerf.hacked.db.daos.BreachDao
import li.doerf.hacked.db.entities.Account
import li.doerf.hacked.db.entities.Breach
import li.doerf.hacked.utils.AccountHelper
import li.doerf.hacked.utils.NotificationHelper
import li.doerf.hacked.utils.OreoNotificationHelper
import li.doerf.hacked.utils.StringHelper
import org.joda.time.DateTime
import java.io.IOException

class HIBPAccountResponseWorker(private val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "HIBPAccountResponseWork"
        private const val NOTIFICATION_GROUP_KEY_BREACHES = "group_key_breachs"
        const val KEY_ACCOUNT = "Account"
        const val KEY_BREACHES = "Breaches"
    }

    private val myAccountDao: AccountDao = AppDatabase.get(context).accountDao
    private val myBreachDao: BreachDao = AppDatabase.get(context).breachDao

    override suspend fun doWork(): Result {
        val breaches = inputData.getStringArray(KEY_BREACHES)
        val accountName = inputData.getString(KEY_ACCOUNT)
        val account = myAccountDao.findByName(accountName)[0]
        return try {
            var foundNewBreach = false
            for (breachName in breaches!!) {
                Log.d(TAG, breachName)
                foundNewBreach = foundNewBreach or handleBreach(account, breachName)
            }
            if (foundNewBreach) {
                showNotification()
            }
            updateAccount(account, foundNewBreach)
            Result.success()
        } catch (e: IOException) {
            Log.e(TAG, "caughtIOException while contacting www.haveibeenpwned.com - " + e.message, e)
            FirebaseCrashlytics.getInstance().recordException(e)
            // TODO better error
            Handler(Looper.getMainLooper()).post { Toast.makeText(context, context.getString(R.string.toast_error_error_during_check), Toast.LENGTH_LONG).show() }
            Result.failure()
        } catch (e: WorkFailedException) {
            if (e.retry) {
                Result.retry()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            throw e
        }
    }

    private fun updateAccount(account: Account, foundNewBreach: Boolean) {
        account.setLastChecked(DateTime.now())
        if (foundNewBreach && !account.hacked) {
            account.hacked = true
        }
        if (foundNewBreach) {
            AccountHelper(context).updateBreachCounts(account)
        }
        myAccountDao.update(account)
    }

    @Throws(IOException::class)
    private suspend fun handleBreach(account: Account, breachName: String): Boolean {
        val existing = myBreachDao.findByAccountAndName(account.id, breachName)
        if (existing != null) {
            Log.d(TAG, "breach already existing: $breachName")
            updateBreachedAccount(existing)
            return false
        }

        val ba = getBreachedAccount(breachName)
                ?: return false // request not successful

        addNewBreachedAccount(ba, account)
        return true
    }

    private suspend fun updateBreachedAccount(existing: Breach) {
        val lastCheckedDate = DateTime(existing.lastChecked)

        if (lastCheckedDate.isAfter(DateTime.now().minusWeeks(2))) {
            Log.d(TAG, "last check within last 14 days");
            return
        }

        val ba = getBreachedAccount(existing.name)
                ?: return // request not successful

        if (existing.modifiedDate == DateTime.parse(ba.modifiedDate).millis) {
            // no update to data
            existing.lastChecked = DateTime.now().millis
            myBreachDao.update(existing)
            Log.i(TAG, "existing breach last checked timestamp updated in db: ${existing.name}")
            return
        }

        existing.title = ba.title
        existing.domain = ba.domain
        existing.breachDate = DateTime.parse(ba.breachDate).millis
        existing.modifiedDate = DateTime.parse(ba.modifiedDate).millis
        existing.pwnCount = ba.pwnCount
        existing.description = ba.description
        existing.dataClasses = if (ba.dataClasses != null) StringHelper.join(ba.dataClasses, ", ") else ""
        existing.verified = ba.isVerified
        existing.fabricated = ba.isFabricated
        existing.retired = ba.isRetired
        existing.sensitive = ba.isSensitive
        existing.spamList = ba.IsSpamList
        existing.logoPath = ba.LogoPath
        existing.acknowledged = false
        existing.lastChecked = DateTime.now().millis
        myBreachDao.update(existing)
        Log.i(TAG, "existing breach updated in db: ${existing.name}")
    }

    private fun addNewBreachedAccount(ba: BreachedAccount, account: Account) {
        Log.d(TAG, "new breach: " + ba.name)
        val newBreach = Breach()
        newBreach.account = account.id
        newBreach.name = ba.name
        newBreach.title = ba.title
        newBreach.domain = ba.domain
        newBreach.breachDate = DateTime.parse(ba.breachDate).millis
        newBreach.addedDate = DateTime.parse(ba.addedDate).millis
        newBreach.modifiedDate = DateTime.parse(ba.modifiedDate).millis
        newBreach.pwnCount = ba.pwnCount
        newBreach.description = ba.description
        newBreach.dataClasses = if (ba.dataClasses != null) StringHelper.join(ba.dataClasses, ", ") else ""
        newBreach.verified = ba.isVerified
        newBreach.fabricated = ba.isFabricated
        newBreach.retired = ba.isRetired
        newBreach.sensitive = ba.isSensitive
        newBreach.spamList = ba.IsSpamList
        newBreach.logoPath = ba.LogoPath
        newBreach.acknowledged = false
        newBreach.lastChecked = DateTime.now().millis
        myBreachDao.insert(newBreach)
        Log.i(TAG, "breach inserted into db: " + newBreach.name)
    }

    @Throws(IOException::class)
    private suspend fun getBreachedAccount(breachName: String): BreachedAccount? {
        Log.d(TAG, "retrieving breaches for $breachName")
        val url = "https://haveibeenpwned.com/api/v3/breach/${breachName}"

        val (_, res, result) = url.httpGet()
                .header("Accept", "application/vnd.haveibeenpwned.v2+json")
                .header("User-Agent", "Hacked_Android_App")
                .awaitObjectResponseResult(BreachedAccountDeserializer)

        if (!res.isSuccessful) {
            Log.w(TAG, "request was not successful")
            return null
        }

        return result.get()
    }

    private fun showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val onh = OreoNotificationHelper(context)
            onh.createNotificationChannel()
        }
        val title = context.getString(R.string.notification_title_new_breaches_found)
        val mBuilder = NotificationCompat.Builder(context, OreoNotificationHelper.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(context.getString(R.string.notification_text_click_to_open))
                .setChannelId(OreoNotificationHelper.CHANNEL_ID)
                .setOnlyAlertOnce(true)
                .setGroup(NOTIFICATION_GROUP_KEY_BREACHES)
                .setAutoCancel(true)

        // TODO navigate directly to breaches
        val showBreachDetails = Intent(context, NavActivity::class.java)
        val resultPendingIntent = PendingIntent.getActivity(
                context,
                0,
                showBreachDetails,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )
        mBuilder.setContentIntent(resultPendingIntent)
        val notification = mBuilder.build()
        NotificationHelper.notify(context, notification)
    }

}

object BreachedAccountDeserializer : ResponseDeserializable<BreachedAccount> {
    override fun deserialize(content: String) = run {
        Log.d("BreachedAccountDeserial", content)
        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.propertyNamingStrategy = PropertyNamingStrategy.UPPER_CAMEL_CASE
        mapper.readValue<BreachedAccount>(content)
    }
}