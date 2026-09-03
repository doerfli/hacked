package li.doerf.hacked.ui.screens.firstuse

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import li.doerf.hacked.CustomEvent
import li.doerf.hacked.R
import li.doerf.hacked.services.AccountService
import li.doerf.hacked.ui.composable.HtmlLinkText
import li.doerf.hacked.util.Analytics
import li.doerf.hacked.util.FirstUseTracker

@Composable
fun FirstUseScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    var accountName by rememberSaveable { mutableStateOf("") }

    fun finish() {
        activity.getPreferences(Context.MODE_PRIVATE).edit {
            putBoolean(FirstUseTracker.PREF_KEY_FIRST_USE_SEEN, true)
        }
        onFinished()
    }

    fun addAccountAndFinish() {
        AccountService(activity.application).addAccount(accountName)
        Analytics.trackCustomEvent(CustomEvent.FIRST_ACCOUNT_ADDED)
        finish()
    }

    Scaffold { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(stringResource(R.string.firstuse_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.firstuse_p1), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.firstuse_p2), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = { Text(stringResource(R.string.hint_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (accountName.isNotBlank()) addAccountAndFinish() }),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.firstuse_p3), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            HtmlLinkText(stringResource(R.string.firstuse_p4), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { finish() }) {
                    Text(stringResource(R.string.skip))
                }
                Button(onClick = { if (accountName.isNotBlank()) addAccountAndFinish() }) {
                    Text(stringResource(R.string.add_and_dismiss_help))
                }
            }
        }
    }
}
