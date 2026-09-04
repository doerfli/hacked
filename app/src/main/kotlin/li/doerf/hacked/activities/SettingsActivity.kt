package li.doerf.hacked.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import li.doerf.hacked.ui.screens.settings.SettingsScreen
import li.doerf.hacked.ui.theme.HackedTheme

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            HackedTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}
