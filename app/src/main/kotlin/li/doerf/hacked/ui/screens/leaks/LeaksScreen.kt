package li.doerf.hacked.ui.screens.leaks

import android.content.Context
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
import li.doerf.hacked.ui.theme.statusColors
import li.doerf.hacked.ui.viewmodels.BreachedSitesViewModel
import li.doerf.hacked.util.findActivity
import org.joda.time.format.DateTimeFormat
import java.text.NumberFormat
import android.content.SharedPreferences

private enum class SortOrder { NAME, COUNT, DATE }

private sealed class LeaksBlock {
    data class Group(val sites: List<BreachedSite>) : LeaksBlock()
    data class Expanded(val site: BreachedSite) : LeaksBlock()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaksScreen() {
    val context = LocalContext.current
    val activity = context.findActivity() ?: return
    val viewModel: BreachedSitesViewModel = viewModel()
    val sites by (viewModel.breachesSites ?: return).observeAsState(emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(SortOrder.NAME) }
    var expandedId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        val prefs: SharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)
        viewModel.reloadIfStale(prefs, context)
    }

    val blocks = remember(sites, expandedId) {
        buildList {
            var group = mutableListOf<BreachedSite>()
            sites.forEach { site ->
                if (site.id == expandedId) {
                    if (group.isNotEmpty()) {
                        add(LeaksBlock.Group(group.toList()))
                        group = mutableListOf()
                    }
                    add(LeaksBlock.Expanded(site))
                } else {
                    group.add(site)
                }
            }
            if (group.isNotEmpty()) add(LeaksBlock.Group(group))
        }
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
            TextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.setFilter(it)
                },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.enter_filter_text)) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
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
                    selected = sort == SortOrder.DATE,
                    onClick = { sort = SortOrder.DATE; viewModel.orderByDate() },
                    label = { Text(stringResource(R.string.action_sort_dateadded)) }
                )
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
            }
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                LazyColumn(Modifier.fillMaxSize()) {
                    blocks.forEach { block ->
                        when (block) {
                            is LeaksBlock.Group -> {
                                itemsIndexed(block.sites, key = { _, site -> site.id }) { index, site ->
                                    Column {
                                        SiteRow(site = site, onClick = { expandedId = site.id })
                                        if (index != block.sites.lastIndex) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        }
                                    }
                                }
                            }
                            is LeaksBlock.Expanded -> item(key = block.site.id) {
                                ExpandedSiteCard(
                                    site = block.site,
                                    onToggle = { expandedId = null }
                                )
                            }
                        }
                    }
                    item {
                        HtmlLinkText(
                            "${stringResource(R.string.data_provided_by)} <a href=\"https://haveibeenpwned.com\">Have i been pwned?</a>",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SiteRow(site: BreachedSite, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(site.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${NumberFormat.getNumberInstance().format(site.pwnCount)} accounts · ${DateTimeFormat.forPattern("yyyy/MM/dd").print(site.breachDate)}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExpandedSiteCard(site: BreachedSite, onToggle: () -> Unit) {
    val context = LocalContext.current
    val status = MaterialTheme.statusColors
    val edgeWidthPx = with(LocalDensity.current) { 4.dp.toPx() }

    Card(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .drawBehind { drawRect(color = status.acknowledged, size = size.copy(width = edgeWidthPx)) }
        ) {
            Column(Modifier.padding(start = 20.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onToggle),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!site.logoPath.isNullOrEmpty()) {
                        AsyncImage(model = site.logoPath, contentDescription = null, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(site.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ExpandLess, contentDescription = null)
                }
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledValue(stringResource(R.string.label_domain), site.domain)
                    LabeledValue(stringResource(R.string.label_breach_date), DateTimeFormat.forPattern("yyyy/MM/dd").print(site.breachDate))
                    LabeledValue(stringResource(R.string.label_accounts_affected), NumberFormat.getNumberInstance().format(site.pwnCount))
                    LabeledValue(stringResource(R.string.label_compromised_data), site.dataClasses)
                    if (site.hasAdditionalFlags()) {
                        LabeledValue(stringResource(R.string.label_additional_flags), flagsText(context, site))
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
