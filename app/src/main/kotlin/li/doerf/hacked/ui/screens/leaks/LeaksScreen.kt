package li.doerf.hacked.ui.screens.leaks

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import li.doerf.hacked.R
import li.doerf.hacked.db.entities.BreachedSite
import li.doerf.hacked.ui.composable.AppOverflowMenu
import li.doerf.hacked.ui.composable.HtmlLinkText
import li.doerf.hacked.ui.composable.LabeledValue
import li.doerf.hacked.ui.composable.flagsText
import li.doerf.hacked.ui.viewmodels.BreachedSitesViewModel
import org.joda.time.format.DateTimeFormat
import java.text.NumberFormat
import android.content.SharedPreferences

private enum class SortOrder { NAME, COUNT, DATE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaksScreen() {
    val context = LocalContext.current
    val activity = context as Activity
    val viewModel: BreachedSitesViewModel = viewModel()
    val sites by (viewModel.breachesSites ?: return).observeAsState(emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(SortOrder.NAME) }
    var expandedIds by rememberSaveable { mutableStateOf(setOf<Long>()) }

    LaunchedEffect(Unit) {
        val prefs: SharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)
        viewModel.reloadIfStale(prefs, context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_leaks)) },
                actions = { AppOverflowMenu() }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.setFilter(it)
                },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.enter_filter_text)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sort == SortOrder.NAME,
                    onClick = { sort = SortOrder.NAME; viewModel.orderByName() },
                    label = { Text(stringResource(R.string.action_sort_name)) }
                )
                FilterChip(
                    selected = sort == SortOrder.COUNT,
                    onClick = { sort = SortOrder.COUNT; viewModel.orderByCount() },
                    label = { Text(stringResource(R.string.action_sort_numpwned)) }
                )
                FilterChip(
                    selected = sort == SortOrder.DATE,
                    onClick = { sort = SortOrder.DATE; viewModel.orderByDate() },
                    label = { Text(stringResource(R.string.action_sort_dateadded)) }
                )
            }
            Spacer(Modifier.height(4.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(sites, key = { it.id }) { site ->
                    BreachSiteRow(
                        site = site,
                        expanded = expandedIds.contains(site.id),
                        onToggle = {
                            expandedIds = if (expandedIds.contains(site.id)) {
                                expandedIds - site.id
                            } else {
                                expandedIds + site.id
                            }
                        }
                    )
                }
                item {
                    HtmlLinkText(
                        "${stringResource(R.string.data_provided_by)} <a href=\"https://haveibeenpwned.com\">Have i been pwned?</a>",
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BreachSiteRow(site: BreachedSite, expanded: Boolean, onToggle: () -> Unit) {
    val context = LocalContext.current
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onToggle)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(site.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    "(${NumberFormat.getNumberInstance().format(site.pwnCount)})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Row {
                    if (!site.logoPath.isNullOrEmpty()) {
                        AsyncImage(model = site.logoPath, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.width(12.dp))
                    }
                    Column {
                        LabeledValue(stringResource(R.string.label_domain), site.domain)
                        LabeledValue(
                            stringResource(R.string.label_breach_date),
                            DateTimeFormat.forPattern("yyyy/MM/dd").print(site.breachDate)
                        )
                        LabeledValue(stringResource(R.string.label_compromised_data), site.dataClasses)
                        if (site.hasAdditionalFlags()) {
                            LabeledValue(stringResource(R.string.label_additional_flags), flagsText(context, site))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    HtmlCompat.fromHtml(site.description, HtmlCompat.FROM_HTML_MODE_COMPACT).toString(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
