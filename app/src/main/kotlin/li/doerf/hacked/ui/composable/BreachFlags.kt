package li.doerf.hacked.ui.composable

import android.content.Context
import li.doerf.hacked.R
import li.doerf.hacked.db.entities.Breach
import li.doerf.hacked.db.entities.BreachedSite

fun flagsText(context: Context, verified: Boolean, fabricated: Boolean, retired: Boolean, sensitive: Boolean, spamList: Boolean): String {
    val flags = StringBuilder()
    if (!verified) flags.append(context.getString(R.string.unverified)).append(" ")
    if (fabricated) flags.append(context.getString(R.string.fabricated)).append(" ")
    if (retired) flags.append(context.getString(R.string.retired)).append(" ")
    if (sensitive) flags.append(context.getString(R.string.sensitive)).append(" ")
    if (spamList) flags.append(context.getString(R.string.spam_list)).append(" ")
    return flags.toString().trim()
}

fun flagsText(context: Context, breach: Breach): String =
    flagsText(context, breach.verified, breach.fabricated, breach.retired, breach.sensitive, breach.spamList)

fun flagsText(context: Context, site: BreachedSite): String =
    flagsText(context, site.verified, site.fabricated, site.retired, site.sensitive, site.spamList)
