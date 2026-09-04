package li.doerf.hacked.ui

object Routes {
    const val FIRST_USE = "first_use"
    const val ACCOUNTS = "accounts"
    const val ACCOUNT_DETAIL = "account_detail/{accountId}"
    const val ACCOUNT_DETAIL_ARG = "accountId"
    const val PASSWORDS = "passwords"
    const val LEAKS = "leaks"

    fun accountDetail(accountId: Long) = "account_detail/$accountId"
}
