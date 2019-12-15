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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import li.doerf.hacked.R
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.db.entities.BreachedSite
import li.doerf.hacked.remote.haveibeenpwned.HaveIBeenPwned
import li.doerf.hacked.utils.StringHelper
import org.joda.time.IllegalInstantException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat

class BreachedSitesWorker(private val context: Context, private val params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val BROADCAST_ACTION_GET_BREACHED_SITES_FINISHED = "li.doerf.hacked.BROADCAST_ACTION_GET_BREACHED_SITES_FINISHED"
        private const val LOGTAG = "BreachedSitesWorker"
    }

    private val breachedSiteDao = AppDatabase.get(context).brachedSiteDao

    override suspend fun doWork(): Result = withContext(Dispatchers.IO)  {
        retrieveSites()
    }

    private fun retrieveSites(): Result {
        Log.d(LOGTAG, "doWork")
        val allOldSites: List<BreachedSite> = breachedSiteDao.getAll()
        if (!allOldSites.isEmpty()) {
            Log.d(LOGTAG, "deleting old sites")
            breachedSiteDao.delete(*allOldSites.toTypedArray())
        }

        Log.d(LOGTAG, "retrieving breached sites")
        val retrofit = Retrofit.Builder()
                .baseUrl("https://haveibeenpwned.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        try {
            val service = retrofit.create(HaveIBeenPwned::class.java)
            val breachedSitesCall = service.breachedSites
            val response = breachedSitesCall.execute()
            if (!response.isSuccessful) {
                Log.w(LOGTAG, "request was not successful")
                return Result.failure()
            }
            val breachedSites = response.body()
            if (breachedSites == null) {
                Log.e(LOGTAG, "body of response was empty")
                return Result.failure()
            }
            @SuppressLint("SimpleDateFormat") val date = SimpleDateFormat("yyyy-MM-dd")
            @SuppressLint("SimpleDateFormat") val datetime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            for (ba in breachedSites) {
                Log.d(LOGTAG, "breached site: " + ba.name)
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
                breachedSiteDao.insert(site)
            }
        } catch (e: IOException) {
            Log.e(LOGTAG, "caught IOException while getting breached sites", e)
            Handler(Looper.getMainLooper()).post { Toast.makeText(context, context.getString(R.string.error_download_data), Toast.LENGTH_LONG).show() }
        } finally {
            val settings = PreferenceManager.getDefaultSharedPreferences(context)
            settings.edit().putLong(context.getString(R.string.PREF_KEY_LAST_SYNC_HIBP_TOP20), System.currentTimeMillis()).apply()
            val localIntent = Intent(BROADCAST_ACTION_GET_BREACHED_SITES_FINISHED)
            LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent)
            Log.d(LOGTAG, "broadcast finish sent")
        }

        return Result.success()

    }

}