package li.doerf.hacked.ui.fragments

import androidx.navigation.NavDirections

interface NavDirectionsToAccountDetailsFactory {

    fun getNavDirections(accountId: Long): NavDirections

}