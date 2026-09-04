package li.doerf.hacked.ui.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import li.doerf.hacked.R
import li.doerf.hacked.db.entities.Breach
import li.doerf.hacked.ui.theme.statusColors
import org.joda.time.format.DateTimeFormat
import java.text.NumberFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BreachCard(breach: Breach, onAcknowledge: () -> Unit) {
    val status = MaterialTheme.statusColors
    val context = LocalContext.current
    val edgeColor = if (breach.acknowledged) status.acknowledged else status.breached
    val dtfOut = remember { DateTimeFormat.forPattern("yyyy/MM/dd") }

    Card(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(edgeColor)
            )
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BreachLogo(breach)
                    Spacer(Modifier.width(12.dp))
                    Text(breach.title, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                LabeledValue(stringResource(R.string.label_domain), breach.domain)
                LabeledValue(stringResource(R.string.label_breach_date), dtfOut.print(breach.breachDate))
                breach.pwnCount?.takeIf { it > 0 }?.let { count ->
                    LabeledValue(stringResource(R.string.label_accounts_affected), NumberFormat.getNumberInstance().format(count))
                }
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.label_compromised_data), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    breach.dataClasses.orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { dataClass ->
                        DataClassChip(dataClass)
                    }
                }
                if (breach.hasAdditionalFlags()) {
                    Spacer(Modifier.height(6.dp))
                    LabeledValue(stringResource(R.string.label_additional_flags), flagsText(context, breach))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    HtmlCompat.fromHtml(breach.description, HtmlCompat.FROM_HTML_MODE_COMPACT).toString(),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!breach.acknowledged) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onAcknowledge) {
                            Text(stringResource(R.string.acknowledge))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreachLogo(breach: Breach) {
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        if (!breach.logoPath.isNullOrEmpty()) {
            SubcomposeAsyncImage(
                model = breach.logoPath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            ) {
                if (painter.state is AsyncImagePainter.State.Success) {
                    SubcomposeAsyncImageContent()
                } else {
                    BreachLogoFallback(breach.title)
                }
            }
        } else {
            BreachLogoFallback(breach.title)
        }
    }
}

@Composable
private fun BreachLogoFallback(title: String) {
    Text(
        title.take(1).uppercase(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun DataClassChip(label: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}
