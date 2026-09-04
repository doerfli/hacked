package li.doerf.hacked.ui.composable

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.firebase.crashlytics.FirebaseCrashlytics
import li.doerf.hacked.R
import li.doerf.hacked.activities.SettingsActivity

private const val TAG = "AppOverflowMenu"
private const val URL_PRIVACY_POLICY = "https://doerfli.github.io/hacked/privacy"
private const val URL_HIBP = "https://haveibeenpwned.com"

@Composable
fun AppOverflowMenu() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_settings))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_settings)) },
            onClick = {
                expanded = false
                context.startActivity(Intent(context, SettingsActivity::class.java))
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_privacypolicy)) },
            onClick = {
                expanded = false
                openUrlSafely(context, URL_PRIVACY_POLICY)
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_visit_hibp)) },
            onClick = {
                expanded = false
                openUrlSafely(context, URL_HIBP)
            }
        )
    }
}

private fun openUrlSafely(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Log.e(TAG, "caught ActivityNotFoundException", e)
        FirebaseCrashlytics.getInstance().recordException(e)
        Toast.makeText(context, context.getString(R.string.unable_to_start_browser, url), Toast.LENGTH_LONG).show()
    }
}
