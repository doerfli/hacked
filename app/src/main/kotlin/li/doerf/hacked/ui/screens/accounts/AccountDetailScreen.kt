package li.doerf.hacked.ui.screens.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import li.doerf.hacked.R
import li.doerf.hacked.ui.composable.BreachCard
import li.doerf.hacked.ui.composable.HtmlLinkText
import li.doerf.hacked.ui.theme.statusColors
import li.doerf.hacked.ui.viewmodels.AccountDetailViewModel
import li.doerf.hacked.util.Analytics
import li.doerf.hacked.util.RatingHelper
import li.doerf.hacked.util.findActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context.findActivity() ?: return
    val viewModel: AccountDetailViewModel = viewModel()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val breaches by viewModel.breaches.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var helpExpanded by rememberSaveable { mutableStateOf(false) }
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { Analytics.trackView("Screen~AccountDetails") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_reset_acknowledgements)) },
                            onClick = {
                                menuExpanded = false
                                viewModel.resetAcknowledged()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete_account)) },
                            onClick = {
                                menuExpanded = false
                                viewModel.deleteAccount { onBack() }
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (breaches.isEmpty()) {
            NoBreachFoundCard(Modifier.padding(padding).padding(16.dp).fillMaxWidth())
        } else {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                WhatNowCard(
                    expanded = helpExpanded,
                    onToggle = { helpExpanded = !helpExpanded },
                    modifier = Modifier.padding(12.dp)
                )
                LazyColumn(Modifier.fillMaxSize(), state = listState) {
                    itemsIndexed(breaches, key = { _, breach -> breach.id }) { index, breach ->
                        BreachCard(breach) {
                            scope.launch {
                                // Acknowledging moves this card to the end of the (re-sorted)
                                // list. If it's the anchor item Compose's scroll-position
                                // correction would otherwise follow it there. Step the anchor
                                // onto the next card first so the viewport just reveals what
                                // was already below, instead of chasing the moved card down.
                                if (index == listState.firstVisibleItemIndex && index < breaches.lastIndex) {
                                    listState.scrollToItem(index + 1, 0)
                                }
                                viewModel.acknowledge(breach) {
                                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.breach_acknowledged)) }
                                    RatingHelper(activity).setRatingCounterBelowthreshold()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoBreachFoundCard(modifier: Modifier = Modifier) {
    val status = MaterialTheme.statusColors
    Card(modifier, colors = CardDefaults.cardColors(containerColor = status.clean.copy(alpha = 0.12f))) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.congratulations), style = MaterialTheme.typography.titleLarge, color = status.clean)
            Text(stringResource(R.string.no_breaches_found), style = MaterialTheme.typography.bodyLarge, color = status.clean)
        }
    }
}

@Composable
private fun WhatNowCard(expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onToggle), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${stringResource(R.string.breach_details_compromised_account_found)} ${stringResource(R.string.breach_details_dash)}\n${stringResource(R.string.breach_details_what_now)}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(top = 8.dp)) {
                    val firstText = stringResource(
                        R.string.breach_details_first_text,
                        "<a href=\"https://bitwarden.com\">Bitwarden</a>",
                        "<a href=\"https://1password.com\">1Password</a>",
                        "<a href=\"https://nordpass.com\">NordPass</a>",
                        "<a href=\"https://proton.me/pass\">Proton Pass</a>"
                    )
                    HelpStep(stringResource(R.string.breach_details_first), firstText)
                    HelpStep(stringResource(R.string.breach_details_second), stringResource(R.string.breach_details_second_text))
                    HelpStep(stringResource(R.string.breach_details_third), stringResource(R.string.breach_details_third_text))
                }
            }
        }
    }
}

@Composable
private fun HelpStep(number: String, htmlText: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(number, modifier = Modifier.width(20.dp))
        HtmlLinkText(htmlText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
