package li.doerf.hacked.remote.hibp

import androidx.annotation.Keep
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Created by moo on 05/09/16.
 */
@Keep
data class BreachedAccount(
        var name: String? = null,
        var title: String? = null,
        var domain: String? = null,
        var breachDate: String? = null,
        var addedDate: String? = null,
        var modifiedDate: String? = null,
        var pwnCount: Long? = null,
        var description: String? = null,
        var dataClasses: Array<String>? = null,
        var isVerified: Boolean? = null,
        var isSensitive: Boolean? = null,
        var isRetired: Boolean? = null,
        var isFabricated: Boolean? = null,
        @field:JsonProperty("IsSpamList")
        var IsSpamList: Boolean? = null,
        @field:JsonProperty("LogoPath")
        var LogoPath: String? = null
)
