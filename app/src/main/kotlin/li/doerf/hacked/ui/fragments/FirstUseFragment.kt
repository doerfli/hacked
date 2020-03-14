package li.doerf.hacked.ui.fragments


import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import li.doerf.hacked.CustomEvent
import li.doerf.hacked.HackedApplication
import li.doerf.hacked.R
import li.doerf.hacked.services.AccountService

/**
 * A simple [Fragment] subclass.
 */
class FirstUseFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val fragmentRootView = inflater.inflate(R.layout.fragment_first_use, container, false)

        val accountEditText = fragmentRootView.findViewById<EditText>(R.id.account)
        val addButton = fragmentRootView.findViewById<Button>(R.id.button_add_initial_account)
        addButton.setOnClickListener {
            val accountName = accountEditText.text
            AccountService(activity!!.application).addAccount(accountName.toString())
            accountEditText.visibility = View.GONE
            addButton.visibility = View.GONE
            (activity!!.application as HackedApplication).trackCustomEvent(CustomEvent.FIRST_ACCOUNT_ADDED)
            navigateToOverview()
        }

        val dismissButton = fragmentRootView.findViewById<Button>(R.id.dismiss)
        dismissButton.setOnClickListener {
            navigateToOverview()
        }

        val text4 = fragmentRootView.findViewById<TextView>(R.id.text4)
        text4.movementMethod = LinkMovementMethod.getInstance()

        return fragmentRootView
    }

    private fun navigateToOverview() {
        val sharedPref = activity!!.getPreferences(Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putBoolean(OverviewFragment.PREF_KEY_FIRST_USE_SEEN, true)
            commit()
        }
        val action = FirstUseFragmentDirections.actionFirstUseFragmentToOverviewFragment()
        findNavController().navigate(action)
    }

}
