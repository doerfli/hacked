package li.doerf.hacked.util

class NavEvent(val destination: Destination) {
    enum class Destination {
        OVERVIEW,
        FIRST_USE,
        ACCOUNTS_LIST,
        ACCOUNTS_DETAILS,
        ALL_BREACHES,
        PWNED_PASSWORDS
    }
}