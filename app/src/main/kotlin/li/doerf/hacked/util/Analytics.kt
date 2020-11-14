package li.doerf.hacked.util

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics

class Analytics {

    companion object {
        private lateinit var analyticsInstance: FirebaseAnalytics

        fun getInstance(context: Context): FirebaseAnalytics {
            if (! this::analyticsInstance.isInitialized) {
                analyticsInstance = FirebaseAnalytics.getInstance(context)
            }
            return analyticsInstance
        }

    }

}