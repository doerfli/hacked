package li.doerf.hacked.remote.hibp

import android.annotation.SuppressLint
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
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.fuel.httpGet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import li.doerf.hacked.R
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.db.entities.BreachedSite
import li.doerf.hacked.remote.haveibeenpwned.BreachedAccount
import li.doerf.hacked.remote.pwnedpasswords.PwnedPassword
import li.doerf.hacked.utils.StringHelper
import org.joda.time.IllegalInstantException
import java.text.ParseException
import java.text.SimpleDateFormat

class BreachedSitesWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val BROADCAST_ACTION_GET_BREACHED_SITES_FINISHED = "li.doerf.hacked.BROADCAST_ACTION_GET_BREACHED_SITES_FINISHED"
        private const val LOGTAG = "BreachedSitesWorker"
    }

    private val breachedSiteDao = AppDatabase.get(context).brachedSiteDao

    override suspend fun doWork(): Result = withContext(Dispatchers.IO)  {
        try {
            retrieveSites()
        } catch (exception: Exception) {
            Log.e(PwnedPassword.TAG, "caught exception while reteieving sites: ${exception.message}")
            Log.e(PwnedPassword.TAG, exception.stackTrace.joinToString("\n"))
            Crashlytics.logException(exception)
            Handler(Looper.getMainLooper()).post { Toast.makeText(context, context.getString(R.string.error_download_data), Toast.LENGTH_LONG).show() }
            Result.failure()
        }
    }

    private suspend fun retrieveSites(): Result {
        Log.d(LOGTAG, "retrieveSites")
//        val allOldSites: List<BreachedSite> = breachedSiteDao.all
//        if (allOldSites.isNotEmpty()) {
//            Log.d(LOGTAG, "deleting old sites")
//            breachedSiteDao.delete(*allOldSites.toTypedArray())
//        }

        Log.d(LOGTAG, "retrieving breached sites")
        @SuppressLint("SimpleDateFormat") val date = SimpleDateFormat("yyyy-MM-dd")
        @SuppressLint("SimpleDateFormat") val datetime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

        try {
            val url = "https://haveibeenpwned.com/api/v3/breaches"

            val (_, res, result) = url.httpGet()
                    .header("Accept", "application/vnd.haveibeenpwned.v2+json")
                    .header("User-Agent", "Hacked_Android_App")
                    .awaitObjectResponseResult(BreachedAccountDeserializer)

            if (!res.isSuccessful) {
                Log.w(LOGTAG, "request was not successful")
                return Result.failure()
            }

            for (ba in result.get()) {
                Log.d(LOGTAG, "breached site: " + ba.name)
                val newSite = mapAccountToSite(ba, date, datetime)
                val site = breachedSiteDao.getByName(newSite.name)
                if (site == null) {
                    breachedSiteDao.insert(newSite)
                } else {
                    site.title = newSite.title
                    site.domain = newSite.domain
                    site.addedDate = newSite.addedDate
                    site.breachDate = newSite.breachDate
                    site.pwnCount = newSite.pwnCount
                    site.description = newSite.description
                    site.dataClasses = newSite.dataClasses
                    site.verified = newSite.verified
                    breachedSiteDao.update(site)
                }
            }

            return Result.success()
        } finally {
            updateLastSyncTs()
            sendFinishedBroadcast()
        }
    }

    private fun updateLastSyncTs() {
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        settings.edit().putLong(context.getString(R.string.PREF_KEY_LAST_SYNC_HIBP_TOP20), System.currentTimeMillis()).apply()
    }

    private fun sendFinishedBroadcast() {
        val localIntent = Intent(BROADCAST_ACTION_GET_BREACHED_SITES_FINISHED)
        LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent)
        Log.d(LOGTAG, "broadcast finish sent")
    }

    private fun mapAccountToSite(ba: BreachedAccount, date: SimpleDateFormat, datetime: SimpleDateFormat): BreachedSite {
        val site = BreachedSite()
        site.name = ba.name
        site.title = ba.title
        site.domain = ba.domain
        try {
            site.breachDate = date.parse(ba.breachDate).time
            site.addedDate = datetime.parse(ba.addedDate).time
        } catch (e: IllegalInstantException) {
            throw IllegalArgumentException("caught IllegalInstantException - breachdate: " + ba.breachDate + " addeddate: " + ba.addedDate, e)
        } catch (e: ParseException) {
            throw IllegalArgumentException("caught ParseException - breachdate: " + ba.breachDate + " addeddate: " + ba.addedDate, e)
        }
        site.pwnCount = ba.pwnCount
        site.description = ba.description
        site.dataClasses = if (ba.dataClasses != null) StringHelper.join(ba.dataClasses, ", ") else ""
        site.verified = ba.isVerified
        return site
    }

}

object BreachedAccountDeserializer : ResponseDeserializable<Collection<BreachedAccount>> {
    override fun deserialize(content: String) = run {
        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.propertyNamingStrategy = PropertyNamingStrategy.UPPER_CAMEL_CASE
        mapper.readValue<Collection<BreachedAccount>>(content)
    }
}
