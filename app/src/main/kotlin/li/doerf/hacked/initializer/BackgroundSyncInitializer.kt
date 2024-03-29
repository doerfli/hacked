package li.doerf.hacked.initializer

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import androidx.work.WorkManagerInitializer
import li.doerf.hacked.utils.SynchronizationHelper

class BackgroundSyncInitializer : Initializer<String> {

    override fun create(context: Context): String {
        SynchronizationHelper.setupInitialSync(context)
        Log.i(TAG, "initialized")
        return "sync initialized"
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(WorkManagerInitializer::class.java)
    }
    
    companion object {
        private const val TAG = "BackgroundSyncInitializ"
    }

}