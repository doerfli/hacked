package li.doerf.hacked.activities

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val numAccounts = withContext(Dispatchers.IO) {
                AppDatabase.get(applicationContext).accountDao.all.size
            }
            val firstUseSeen = getPreferences(Context.MODE_PRIVATE)
                .getBoolean(FirstUseTracker.PREF_KEY_FIRST_USE_SEEN, false)
            val startWithFirstUse = !firstUseSeen && numAccounts == 0

            setContent {
                HackedTheme {
                    HackedApp(startWithFirstUse = startWithFirstUse)
                }
            }
        }
    }
}
