package li.doerf.hacked.initializer

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.startup.Initializer
import com.google.android.gms.security.ProviderInstaller
import com.google.android.gms.security.ProviderInstaller.ProviderInstallListener

class Tls12Initializer : Initializer<String> {

    override fun create(context: Context): String {
        Log.i(TAG, "initializing")
        ProviderInstaller.installIfNeededAsync(context, object : ProviderInstallListener {
            override fun onProviderInstalled() {}
            override fun onProviderInstallFailed(i: Int, intent: Intent) {
                Log.i(TAG, "Provider install failed ($i) : SSL Problems may occurs")
            }
        })
        return "initialized"
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf()
    }

    companion object {
        private const val TAG = "Tls12Initializer"
    }
}