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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
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
import li.doerf.hacked.ui.composable.RateUsDialog
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
    var showRateUsDialog by rememberSaveable { mutableStateOf(false) }
    val ratingHelper = remember { RatingHelper(activity) }

    LaunchedEffect(Unit) {
        showRateUsDialog = ratingHelper.showRateUsDialogDelayed()
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
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        val count = withContext(Dispatchers.IO) { AppDatabase.get(context).accountDao.all.size }
                        if (count > MAX_ACCOUNTS) {
                            snackbarHostState.showSnackbar(context.getString(R.string.snackbar_max_accounts))
                        } else {
                            showAddSheet = true
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add_account))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (accounts.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.accounts_empty_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val needingAttention = accounts.count { it.hacked }
                val unresolvedCount = accounts.sumOf { (it.numBreaches ?: 0) - (it.numAcknowledgedBreaches ?: 0) }
                AccountsSummaryBanner(
                    unresolvedCount = unresolvedCount,
                    accountsNeedingAttention = needingAttention,
                    totalAccounts = accounts.size
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.accounts_section_label).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    lastChecked?.lastChecked?.let { millis ->
                        Text(
                            stringResource(R.string.accounts_last_checked, DateTimeFormat.forPattern("yyyy/MM/dd HH:mm").print(millis)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        accounts.forEachIndexed { index, account ->
                            AccountRow(account, onClick = {
                                NotificationHelper.cancelAll(context)
                                onAccountClick(account.id)
                            })
                            if (index != accounts.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
            HtmlLinkText(
                "${stringResource(R.string.data_provided_by)} <a href=\"https://haveibeenpwned.com\">Have i been pwned?</a>",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 4.dp)
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

    if (showRateUsDialog) {
        RateUsDialog(
            onDismissRequest = { showRateUsDialog = false },
            onPositive = { showRateUsDialog = false; ratingHelper.showReview() },
            onNeutral = { showRateUsDialog = false; ratingHelper.rateLater() },
            onNegative = { showRateUsDialog = false; ratingHelper.rateNever() }
        )
    }
}

@Composable
private fun AccountsSummaryBanner(unresolvedCount: Int, accountsNeedingAttention: Int, totalAccounts: Int) {
    val clean = unresolvedCount <= 0
    val containerColor = if (clean) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val contentColor = if (clean) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer

    Row(
        Modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            if (clean) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = contentColor
        )
        if (!clean) {
            Text(
                "$unresolvedCount",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
        Text(
            if (clean) {
                stringResource(R.string.accounts_summary_clean, totalAccounts)
            } else {
                stringResource(R.string.accounts_summary_needs_attention, unresolvedCount, accountsNeedingAttention, totalAccounts)
            },
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AccountRow(account: Account, onClick: () -> Unit) {
    val status = MaterialTheme.statusColors
    val cs = MaterialTheme.colorScheme
    val numBreaches = account.numBreaches ?: 0
    val numAcknowledged = account.numAcknowledgedBreaches ?: 0

    data class RowStyle(val dotColor: androidx.compose.ui.graphics.Color, val stateText: String, val badgeCount: Int, val badgeContainer: androidx.compose.ui.graphics.Color, val badgeContent: androidx.compose.ui.graphics.Color)

    val style = when {
        account.hacked -> RowStyle(status.breached, stringResource(R.string.account_state_needs_attention), numBreaches - numAcknowledged, cs.errorContainer, cs.onErrorContainer)
        account.lastChecked == null -> RowStyle(status.unchecked, stringResource(R.string.account_state_not_checked), 0, cs.surface, cs.onSurfaceVariant)
        numBreaches == 0 -> RowStyle(status.clean, stringResource(R.string.account_state_clean), 0, cs.surface, cs.onSurfaceVariant)
        else -> RowStyle(status.acknowledged, stringResource(R.string.account_state_acknowledged), numBreaches, cs.surface, cs.onSurfaceVariant)
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(10.dp)
                .background(style.dotColor, CircleShape)
        )
        Column(Modifier.weight(1f)) {
            Text(account.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            Text(
                style.stateText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (style.badgeCount > 0) {
            Box(
                Modifier
                    .background(style.badgeContainer, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("${style.badgeCount}", style = MaterialTheme.typography.labelMedium, color = style.badgeContent)
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}
