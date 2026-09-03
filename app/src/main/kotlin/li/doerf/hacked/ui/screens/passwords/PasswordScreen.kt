package li.doerf.hacked.ui.screens.passwords

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import li.doerf.hacked.R
import li.doerf.hacked.ui.composable.AppOverflowMenu
import li.doerf.hacked.ui.theme.statusColors
import li.doerf.hacked.util.Analytics
import li.doerf.hacked.utils.StringHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordScreen() {
    val viewModel: PasswordViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) { Analytics.trackView("Screen~Password") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_pwned_passwords)) },
                actions = { AppOverflowMenu() }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    viewModel.reset()
                },
                label = { Text(stringResource(R.string.password)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (password.isNotEmpty()) viewModel.check(password) }),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { viewModel.check(password) },
                enabled = password.isNotEmpty() && state !is PasswordCheckState.Checking
            ) {
                Text(stringResource(R.string.pwned))
            }
            Spacer(Modifier.height(16.dp))
            when (val s = state) {
                PasswordCheckState.Checking -> LinearProgressIndicator(Modifier.fillMaxWidth())
                PasswordCheckState.Safe -> ResultCard(stringResource(R.string.password_ok), MaterialTheme.statusColors.clean)
                is PasswordCheckState.Pwned -> ResultCard(
                    stringResource(R.string.password_pwned, StringHelper.addDigitSeperator(s.count.toString())),
                    MaterialTheme.statusColors.breached
                )
                PasswordCheckState.Error -> Text(
                    stringResource(R.string.error_download_data),
                    color = MaterialTheme.colorScheme.error
                )
                PasswordCheckState.Idle -> {}
            }
        }
    }
}

@Composable
private fun ResultCard(text: String, color: androidx.compose.ui.graphics.Color) {
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))) {
        Text(text, Modifier.padding(16.dp), color = color)
    }
}
