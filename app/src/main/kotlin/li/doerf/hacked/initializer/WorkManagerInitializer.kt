package li.doerf.hacked.initializer

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import androidx.work.Configuration
import androidx.work.WorkManager

class WorkManagerInitializer : Initializer<WorkManager> {

    override fun create(context: Context): WorkManager {
        val configuration = Configuration.Builder().build()
        WorkManager.initialize(context, configuration)
        Log.i(TAG, "initialized")
        return WorkManager.getInstance(context)
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf()
    }

    companion object {
        private const val TAG = "WorkManagerInitializer"
    }
}