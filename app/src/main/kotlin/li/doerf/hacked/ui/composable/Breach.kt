package li.doerf.hacked.ui.composable

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.google.accompanist.coil.rememberCoilPainter
import li.doerf.hacked.R
import li.doerf.hacked.db.entities.Breach
import org.joda.time.format.DateTimeFormat

@Composable
fun BreachUi(breach: Breach, context: Context, handleAcknowledgeClicked: (id: Long) -> Unit) {
    val dtfOut = DateTimeFormat.forPattern("yyyy/MM/dd")

    Box(Modifier.padding(8.dp)) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.End) {
            Image(
                painter = rememberCoilPainter(
                    request = breach.logoPath
                ),
                contentDescription = "Logo of ${breach.title}",
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp)
            )
        }
        Column() {
            Text(breach.title, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp))
            NameValue(context.getString(R.string.label_domain), breach.domain)
            NameValue(context.getString(R.string.label_breach_date), dtfOut.print(breach.breachDate))
            NameValue(context.getString(R.string.label_compromised_data), breach.dataClasses, true)
            if (breach.hasAdditionalFlags()) {
                NameValue(context.getString(R.string.label_additional_flags), getFlags(breach, context))
            }
            Text(HtmlCompat.fromHtml(breach.description, HtmlCompat.FROM_HTML_MODE_COMPACT).toString())

            if (! breach.acknowledged) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { handleAcknowledgeClicked(breach.id) }) {
                        Text(
                            context.getString(R.string.acknowledge),
                            color = Color(context.resources.getColor(R.color.colorAccent))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NameValue(name: String, value: String, valueIsRed: Boolean = false) {
    Row() {
        Text(name, color = Color.Gray, modifier = Modifier.padding(end = 2.dp))
        if (valueIsRed) {
            Text(value, color = Color.Red)
        } else {
            Text(value)
        }
    }
}

private fun getFlags(breach: Breach, context: Context): String {
    val flags = StringBuilder()
    if (!breach.verified) {
        flags.append(context.getString(R.string.unverified)).append(" ")
    }
    if (breach.fabricated) {
        flags.append(context.getString(R.string.fabricated)).append(" ")
    }
    if (breach.retired) {
        flags.append(context.getString(R.string.retired)).append(" ")
    }
    if (breach.sensitive) {
        flags.append(context.getString(R.string.sensitive)).append(" ")
    }
    if (breach.spamList) {
        flags.append(context.getString(R.string.spam_list)).append(" ")
    }
    return flags.toString()
}

