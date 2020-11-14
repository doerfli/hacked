package li.doerf.hacked.util

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics

class Analytics {

    companion object {
        private lateinit var context: Context
        val instance: FirebaseAnalytics by lazy { FirebaseAnalytics.getInstance(context) }

        fun initialize(aContext: Context) {
            context = aContext
        }

    }

}