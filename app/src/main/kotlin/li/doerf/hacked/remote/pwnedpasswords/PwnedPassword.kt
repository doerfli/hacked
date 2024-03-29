package li.doerf.hacked.remote.pwnedpasswords

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import li.doerf.hacked.util.logException
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import java.util.*

class PwnedPassword(private val broadcastManager: LocalBroadcastManager) {

    companion object {
        const val TAG = "PwnedPassword"
        const val BROADCAST_ACTION_PASSWORD_PWNED = "li.doerf.hacked.BROADCAST_ACTION_PASSWORD_PWNED"
        const val EXTRA_PASSWORD_PWNED = "ExtraPwned"
        const val EXTRA_PASSWORD_PWNED_Count = "ExtraPwnedNum"
        const val EXTRA_EXCEPTION = "ExtraException"
        const val URL = "https://api.pwnedpasswords.com/range"
    }

    fun check(password: String) {
        CoroutineScope(Job()).launch {
            try {
                checkPassword(password)
            } catch (e: FuelError) {
                FirebaseCrashlytics.getInstance().recordException(e)
                logException(TAG, Log.ERROR, e, "caught FuelError during pwned password check")
                notifyException()
            }
        }
    }

    private suspend fun checkPassword(password: String) {
        val pwdHash = String(Hex.encodeHex(DigestUtils.sha1(password))).toUpperCase(Locale.getDefault())
        val pwdHashHead = pwdHash.substring(0, 5)

        Log.d(TAG, "checking password: ")
        val (_, res, result) = "$URL/$pwdHashHead".httpGet().awaitStringResponseResult()
        Log.d(TAG, "status: ${res.statusCode}")
        if (!res.isSuccessful) {
            Log.w(TAG, result.component2())
            Log.w(TAG, res.toString())
            notifyException()
            return
        }

        val pwnedCount = processResult(result.get(), pwdHashHead, pwdHash)

        if (pwnedCount > -1) {
            notifyPwned(pwnedCount)
        } else {
            notifyNotPwned()
        }
    }

    private fun processResult(result: String, pwdHashHead: String, pwdHash: String): Int {
        var pwnedCount = -1
        result.split("\r\n").forEach { line ->
            if (!line.contains(":")) {
                return@forEach
            }
            val (e, numPwns, _) = line.split(":")
            val hash = "$pwdHashHead$e"
            Log.d(TAG, "$hash   $numPwns")
            if (pwdHash != hash) {
                return@forEach
            }
            if (numPwns.contains(',')) {
                numPwns.replace(",", "")
            }
            pwnedCount = Integer.parseInt(numPwns)
        }
        return pwnedCount
    }

    private fun notifyPwned(pwnedCount: Int) {
        val localIntent = Intent(BROADCAST_ACTION_PASSWORD_PWNED)
        localIntent.putExtra(EXTRA_PASSWORD_PWNED, true)
        localIntent.putExtra(EXTRA_PASSWORD_PWNED_Count, pwnedCount)
        notify(localIntent)
    }

    private fun notifyNotPwned() {
        val localIntent = Intent(BROADCAST_ACTION_PASSWORD_PWNED)
        localIntent.putExtra(EXTRA_PASSWORD_PWNED, false)
        notify(localIntent)
    }

    private fun notifyException() {
        val localIntent = Intent(BROADCAST_ACTION_PASSWORD_PWNED)
        localIntent.putExtra(EXTRA_EXCEPTION, true)
        notify(localIntent)
    }

    private fun notify(intent: Intent) {
        broadcastManager.sendBroadcast(intent)
        Log.d(TAG, "broadcast finish sent")
    }

}