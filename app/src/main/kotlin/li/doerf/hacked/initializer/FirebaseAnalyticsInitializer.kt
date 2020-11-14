package li.doerf.hacked.initializer

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import li.doerf.hacked.util.Analytics

class FirebaseAnalyticsInitializer : Initializer<String> {

    override fun create(context: Context): String {
        val instance = Analytics.initialize(context)
        Log.i(TAG, "initialized")
        return "firebaseanalyticsinitialized"
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf()
    }

    companion object {
        private const val TAG = "FirebaseAnalyticsInitia"
    }
}