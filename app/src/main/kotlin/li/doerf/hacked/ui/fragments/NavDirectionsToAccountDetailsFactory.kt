package li.doerf.hacked.ui.fragments

import androidx.navigation.NavDirections

interface NavDirectionsToAccountDetailsFactory {

    fun createNavDirections(accountId: Long): NavDirections

}