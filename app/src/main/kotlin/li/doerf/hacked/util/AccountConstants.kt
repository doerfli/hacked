package li.doerf.hacked.util

import li.doerf.hacked.db.entities.Account

object AccountConstants {
    const val LOGTAG = "AccountsFragmentBase"
    const val MAX_ACCOUNTS = 50
    const val CHECK_TIMEOUT_MILLIS = 10 * 60 * 1000L
}

fun Account.isChecking(): Boolean {
    val requestedAt = checkRequestedAt ?: return false
    return System.currentTimeMillis() - requestedAt < AccountConstants.CHECK_TIMEOUT_MILLIS
}
