package li.doerf.hacked.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Status is never the only signal a row gives — every use of these colours is
 * paired with text stating the same state, for colour-blind users and for
 * legibility under system font scaling.
 */
data class StatusColors(
    val breached: Color,
    val acknowledged: Color,
    val clean: Color,
    val unchecked: Color,
)

val LightStatusColors = StatusColors(
    breached = Color(0xFFB3261E),
    acknowledged = Color(0xFFD69A00),
    clean = Color(0xFF2E6B34),
    unchecked = Color(0xFF767D73),
)

val DarkStatusColors = StatusColors(
    breached = Color(0xFFFFB4AB),
    acknowledged = Color(0xFFF0C048),
    clean = Color(0xFF8FD695),
    unchecked = Color(0xFF8E958A),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
