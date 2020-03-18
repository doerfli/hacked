package li.doerf.hacked.util

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

fun logException(logtag: String, prio: Int, e: Exception, msg: String) {
    ByteArrayOutputStream().use {
        PrintWriter(it).use { pw ->
            e.printStackTrace(pw)
        }
        val message = "$msg\n${String(it.toByteArray())}"
        Log.println(prio, logtag, message)
    }
}