package li.doerf.hacked.ui.composable

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
