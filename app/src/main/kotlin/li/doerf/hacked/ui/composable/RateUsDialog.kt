package li.doerf.hacked.ui.composable

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import li.doerf.hacked.R

@Composable
fun RateUsDialog(
    onDismissRequest: () -> Unit,
    onPositive: () -> Unit,
    onNeutral: () -> Unit,
    onNegative: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.rating_dialog_title)) },
        text = { Text(stringResource(R.string.rating_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onPositive) { Text(stringResource(R.string.rating_dialog_positive)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onNeutral) { Text(stringResource(R.string.rating_dialog_neutral)) }
                TextButton(onClick = onNegative) { Text(stringResource(R.string.rating_dialog_negative)) }
            }
        }
    )
}
