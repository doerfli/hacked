package li.doerf.hacked.activities

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.ui.HackedApp
import li.doerf.hacked.ui.theme.HackedTheme
import li.doerf.hacked.util.FirstUseTracker

class NavActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        var startDestinationReady by mutableStateOf(false)
        var startWithFirstUse by mutableStateOf(false)
        splashScreen.setKeepOnScreenCondition { !startDestinationReady }

        lifecycleScope.launch {
            val numAccounts = withContext(Dispatchers.IO) {
                AppDatabase.get(applicationContext).accountDao.all.size
            }
            val firstUseSeen = getPreferences(Context.MODE_PRIVATE)
                .getBoolean(FirstUseTracker.PREF_KEY_FIRST_USE_SEEN, false)
            startWithFirstUse = !firstUseSeen && numAccounts == 0
            startDestinationReady = true
        }

        setContent {
            if (startDestinationReady) {
                HackedTheme {
                    HackedApp(startWithFirstUse = startWithFirstUse)
                }
            }
        }
    }
}
