package li.doerf.hacked.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineExceptionHandler

fun createCoroutingExceptionHandler(logtag: String): CoroutineExceptionHandler {
    return CoroutineExceptionHandler { _, exception ->
        Log.e(logtag, "caught exception ${exception.message}")
        Log.e(logtag, exception.stackTrace.joinToString("\n"))
        FirebaseCrashlytics.getInstance().recordException(exception)
    }
}