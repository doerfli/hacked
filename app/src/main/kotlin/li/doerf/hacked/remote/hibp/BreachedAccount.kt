package li.doerf.hacked.remote.hibp

/**
 * Created by moo on 05/09/16.
 */
data class BreachedAccount(
        var name: String? = null,
        var title: String? = null,
        var domain: String? = null,
        var breachDate: String? = null,
        var addedDate: String? = null,
        var pwnCount: Long? = null,
        var description: String? = null,
        var dataClasses: Array<String>? = null,
        var isVerified: Boolean? = null,
        var sensitive: Boolean? = null,
        var retired: Boolean? = null
)