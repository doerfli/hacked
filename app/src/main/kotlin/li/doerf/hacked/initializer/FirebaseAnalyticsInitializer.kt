package li.doerf.hacked.initializer

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import com.google.firebase.analytics.FirebaseAnalytics
import li.doerf.hacked.util.Analytics

class FirebaseAnalyticsInitializer : Initializer<FirebaseAnalytics> {

    override fun create(context: Context): FirebaseAnalytics {
        val instance = Analytics.getInstance(context)
        Log.i(TAG, "initialized")
        return instance
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf()
    }

    companion object {
        private const val TAG = "FirebaseAnalyticsInitia"
    }
}