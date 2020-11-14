package li.doerf.hacked.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import li.doerf.hacked.CustomEvent

class Analytics {

    companion object {
        private var runsInTestlab: Boolean = false
        private lateinit var context: Context
        val instance: FirebaseAnalytics by lazy { FirebaseAnalytics.getInstance(context) }

        fun initialize(aContext: Context, isRunsInTestlab: String?) {
            context = aContext
            runsInTestlab = isRunsInTestlab == "true"
        }

        @Synchronized
        fun trackView(name: String) {
            if (runsInTestlab) return
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, name)
            bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "View")
            instance.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle)
        }

        @Synchronized
        fun trackCustomEvent(eventName: CustomEvent) {
            if (runsInTestlab) return
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Function")
            instance.logEvent(eventName.name, bundle)
        }

    }

}