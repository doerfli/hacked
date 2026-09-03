package li.doerf.hacked.ui.screens.accounts

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import li.doerf.hacked.R
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.db.entities.Account
import li.doerf.hacked.remote.hibp.HIBPAccountCheckerWorker
import li.doerf.hacked.services.AccountService
import li.doerf.hacked.ui.composable.AppOverflowMenu
import li.doerf.hacked.ui.composable.HtmlLinkText
import li.doerf.hacked.ui.theme.statusColors
import li.doerf.hacked.ui.viewmodels.AccountViewModel
import li.doerf.hacked.util.AccountConstants.MAX_ACCOUNTS
import li.doerf.hacked.util.RatingHelper
import li.doerf.hacked.utils.NotificationHelper
import org.joda.time.format.DateTimeFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(onAccountClick: (Long) -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val viewModel: AccountViewModel = viewModel()
    val accounts by viewModel.accountList.observeAsState(emptyList())
    val lastChecked by viewModel.lastChecked.observeAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        RatingHelper(activity).showRateUsDialogDelayed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_accounts)) },
                actions = {
                    IconButton(onClick = {
                        val checker = OneTimeWorkRequest.Builder(HIBPAccountCheckerWorker::class.java).build()
                        WorkManager.getInstance(context).enqueue(checker)
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.snackbar_checking_account)) }
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                    AppOverflowMenu()
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scope.launch {
                    val count = withContext(Dispatchers.IO) { AppDatabase.get(context).accountDao.all.size }
                    if (count > MAX_ACCOUNTS) {
                        snackbarHostState.showSnackbar(context.getString(R.string.snackbar_max_accounts))
                    } else {
                        showAddSheet = true
                    }
                }
            }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add_account))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            lastChecked?.lastChecked?.let { millis ->
                Text(
                    DateTimeFormat.forPattern("yyyy/MM/dd HH:mm").print(millis),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            if (accounts.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.accounts_empty_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(accounts, key = { it.id }) { account ->
                        AccountRow(account, onClick = {
                            NotificationHelper.cancelAll(context)
                            onAccountClick(account.id)
                        })
                        HorizontalDivider()
                    }
                }
            }
            HtmlLinkText(
                "${stringResource(R.string.data_provided_by)} <a href=\"https://haveibeenpwned.com\">Have i been pwned?</a>",
                modifier = Modifier.padding(12.dp)
            )
        }
    }

    if (showAddSheet) {
        val sheetState = rememberModalBottomSheetState()
        var name by rememberSaveable { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }, sheetState = sheetState) {
            Column(Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                Text(stringResource(R.string.action_add_account), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.hint_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (name.isNotBlank()) {
                            AccountService(activity.application).addAccount(name)
                            showAddSheet = false
                        }
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showAddSheet = false }) { Text(stringResource(R.string.cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            AccountService(activity.application).addAccount(name)
                            showAddSheet = false
                        },
                        enabled = name.isNotBlank()
                    ) { Text(stringResource(R.string.add)) }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(account: Account, onClick: () -> Unit) {
    val status = MaterialTheme.statusColors
    val (color, stateText) = when {
        account.hacked -> status.breached to stringResource(R.string.account_state_needs_attention)
        account.lastChecked == null -> status.unchecked to stringResource(R.string.account_state_not_checked)
        account.numBreaches == 0 -> status.clean to stringResource(R.string.account_state_clean)
        else -> status.acknowledged to stringResource(R.string.account_state_acknowledged)
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(account.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            Text(
                stateText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (account.numBreaches > 0) {
            Spacer(Modifier.width(8.dp))
            Badge(containerColor = color) { Text("${account.numBreaches}") }
        }
    }
}
