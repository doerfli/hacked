package li.doerf.hacked.ui.composable

import android.text.style.URLSpan
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat

private const val URL_TAG = "URL"

/**
 * Renders HTML containing plain text and <a href> links, replacing the
 * Html.fromHtml + LinkMovementMethod pattern the legacy views used.
 */
@Composable
fun HtmlLinkText(html: String, modifier: Modifier = Modifier, style: TextStyle = LocalTextStyle.current) {
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary
    val contentColor = LocalContentColor.current
    val annotated = remember(html, linkColor) { annotatedStringFromHtml(html, linkColor) }
    ClickableText(
        text = annotated,
        modifier = modifier,
        style = style.copy(color = contentColor),
        onClick = { offset ->
            annotated.getStringAnnotations(URL_TAG, offset, offset).firstOrNull()?.let {
                uriHandler.openUri(it.item)
            }
        }
    )
}

private fun annotatedStringFromHtml(html: String, linkColor: androidx.compose.ui.graphics.Color): AnnotatedString {
    val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
    return buildAnnotatedString {
        append(spanned.toString())
        spanned.getSpans(0, spanned.length, URLSpan::class.java).forEach { span ->
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            addStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline), start, end)
            addStringAnnotation(URL_TAG, span.url, start, end)
        }
    }
}
