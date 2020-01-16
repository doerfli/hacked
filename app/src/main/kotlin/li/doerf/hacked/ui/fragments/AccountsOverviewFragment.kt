package li.doerf.hacked.ui.fragments


import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections

/**
 * A simple [Fragment] subclass.
 * Use the [AccountsOverviewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AccountsOverviewFragment : AccountsFragmentBase() {

    override fun createNavDirections(accountId: Long): NavDirections {
        val action = OverviewFragmentDirections.actionOverviewFragmentToAccountDetailsFragment()
        action.accountId = accountId
        return action
    }

}
