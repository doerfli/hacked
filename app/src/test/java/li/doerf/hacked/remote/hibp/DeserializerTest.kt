package li.doerf.hacked.remote.hibp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeserializerTest {

    @Test
    fun testBreachedAccountDeserializer() {
        val json = """
            {
                "Name": "Adobe",
                "Title": "Adobe",
                "Domain": "adobe.com",
                "BreachDate": "2013-10-04",
                "AddedDate": "2013-12-04T00:00:00Z",
                "ModifiedDate": "2013-12-04T00:00:00Z",
                "PwnCount": 152445165,
                "Description": "In October 2013, 153 million Adobe accounts were breached...",
                "DataClasses": ["Email addresses", "Password hints", "Passwords", "Usernames"],
                "IsVerified": true,
                "IsFabricated": false,
                "IsSensitive": false,
                "IsRetired": false,
                "IsSpamList": false,
                "LogoPath": "https://haveibeenpwned.com/Content/Images/PwnedLogos/Adobe.png"
            }
        """.trimIndent()

        val breachedAccount = BreachedAccountDeserializer.deserialize(json)
        assertNotNull(breachedAccount)
        assertEquals("Adobe", breachedAccount.name)
        assertEquals("adobe.com", breachedAccount.domain)
        assertEquals(152445165L, breachedAccount.pwnCount)
        assertEquals(true, breachedAccount.isVerified)
        assertEquals(false, breachedAccount.IsSpamList)
        assertEquals("https://haveibeenpwned.com/Content/Images/PwnedLogos/Adobe.png", breachedAccount.LogoPath)
    }

    @Test
    fun testBreachedAccountListDeserializer() {
        val json = """
            [
                {
                    "Name": "Adobe",
                    "Title": "Adobe",
                    "Domain": "adobe.com",
                    "BreachDate": "2013-10-04",
                    "AddedDate": "2013-12-04T00:00:00Z",
                    "ModifiedDate": "2013-12-04T00:00:00Z",
                    "PwnCount": 152445165,
                    "Description": "In October 2013...",
                    "DataClasses": ["Email addresses"],
                    "IsVerified": true,
                    "IsFabricated": false,
                    "IsSensitive": false,
                    "IsRetired": false,
                    "IsSpamList": false,
                    "LogoPath": "https://haveibeenpwned.com/Content/Images/PwnedLogos/Adobe.png"
                }
            ]
        """.trimIndent()

        val list = BreachedAccountListDeserializer.deserialize(json)
        assertNotNull(list)
        assertEquals(1, list.size)
        val first = list.first()
        assertEquals("Adobe", first.name)
    }
}
