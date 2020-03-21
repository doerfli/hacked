package li.doerf.hacked.util

class NavEvent(val destination: Destination, val id: Long?, val string: String?) {
    enum class Destination {
        OVERVIEW,
        FIRST_USE,
        ACCOUNTS_LIST,
        ACCOUNTS_DETAILS,
        ALL_BREACHES,
        PWNED_PASSWORDS
    }
}