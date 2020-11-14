package li.doerf.hacked.ui.fragments


import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import io.reactivex.processors.PublishProcessor
import li.doerf.hacked.CustomEvent
import li.doerf.hacked.HackedApplication
import li.doerf.hacked.R
import li.doerf.hacked.services.AccountService
import li.doerf.hacked.util.Analytics
import li.doerf.hacked.util.NavEvent

/**
 * A simple [Fragment] subclass.
 */
class FirstUseFragment : Fragment() {

    private lateinit var navEvents: PublishProcessor<NavEvent>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val fragmentRootView = inflater.inflate(R.layout.fragment_first_use, container, false)

        val accountEditText = fragmentRootView.findViewById<EditText>(R.id.account)
        val addButton = fragmentRootView.findViewById<Button>(R.id.button_add_initial_account)
        val dismissButton = fragmentRootView.findViewById<Button>(R.id.dismiss)

        accountEditText.setOnEditorActionListener { _, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH,
                EditorInfo.IME_ACTION_DONE -> {
                    addNewAccount(accountEditText, addButton)
                    true
                }
                else -> {
                    if(event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                        addNewAccount(accountEditText, addButton)
                        true
                    } else {
                        false
                    }
                 }
            }
        }

        addButton.setOnClickListener {
            addNewAccount(accountEditText, addButton)
        }

        dismissButton.setOnClickListener {
            navigateToOverview()
        }

        val text4 = fragmentRootView.findViewById<TextView>(R.id.text4)
        text4.movementMethod = LinkMovementMethod.getInstance()

        return fragmentRootView
    }

    private fun addNewAccount(accountEditText: EditText, addButton: Button) {
        val accountName = accountEditText.text
        AccountService(activity!!.application).addAccount(accountName.toString())
        accountEditText.visibility = View.GONE
        addButton.visibility = View.GONE
        Analytics.trackCustomEvent(CustomEvent.FIRST_ACCOUNT_ADDED)
        navigateToOverview()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navEvents = (activity!!.applicationContext as HackedApplication).navEvents
    }

    private fun navigateToOverview() {
        val sharedPref = activity!!.getPreferences(Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putBoolean(OverviewFragment.PREF_KEY_FIRST_USE_SEEN, true)
            commit()
        }
        navEvents.onNext(NavEvent(NavEvent.Destination.OVERVIEW, null, null))
    }

}
