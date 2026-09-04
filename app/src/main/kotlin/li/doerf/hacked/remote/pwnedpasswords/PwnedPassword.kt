package li.doerf.hacked.remote.pwnedpasswords

import android.util.Log
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.google.firebase.crashlytics.FirebaseCrashlytics
import li.doerf.hacked.util.logException
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import java.util.Locale

class PwnedPassword {

    sealed class Result {
        data object Safe : Result()
        data class Pwned(val count: Int) : Result()
        data object Error : Result()
    }

    suspend fun check(password: String): Result {
        return try {
            checkPassword(password)
        } catch (e: FuelError) {
            FirebaseCrashlytics.getInstance().recordException(e)
            logException(TAG, Log.ERROR, e, "caught FuelError during pwned password check")
            Result.Error
        }
    }

    private suspend fun checkPassword(password: String): Result {
        val pwdHash = String(Hex.encodeHex(DigestUtils.sha1(password))).uppercase(Locale.getDefault())
        val pwdHashHead = pwdHash.substring(0, 5)

        val (_, res, result) = "$URL/$pwdHashHead".httpGet().awaitStringResponseResult()
        if (!res.isSuccessful) {
            Log.w(TAG, res.toString())
            return Result.Error
        }

        val pwnedCount = processResult(result.get(), pwdHashHead, pwdHash)
        return if (pwnedCount > -1) Result.Pwned(pwnedCount) else Result.Safe
    }

    private fun processResult(result: String, pwdHashHead: String, pwdHash: String): Int {
        var pwnedCount = -1
        result.split("\r\n").forEach { line ->
            if (!line.contains(":")) {
                return@forEach
            }
            val (e, numPwns, _) = line.split(":")
            val hash = "$pwdHashHead$e"
            if (pwdHash != hash) {
                return@forEach
            }
            pwnedCount = Integer.parseInt(numPwns.replace(",", ""))
        }
        return pwnedCount
    }

    companion object {
        const val TAG = "PwnedPassword"
        const val URL = "https://api.pwnedpasswords.com/range"
    }
}
