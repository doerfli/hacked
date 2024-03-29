package li.doerf.hacked.ui.fragments


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.navArgs
import io.reactivex.processors.PublishProcessor
import li.doerf.hacked.CustomEvent
import li.doerf.hacked.HackedApplication
import li.doerf.hacked.R
import li.doerf.hacked.remote.pwnedpasswords.PwnedPassword
import li.doerf.hacked.util.Analytics
import li.doerf.hacked.util.NavEvent
import li.doerf.hacked.utils.StringHelper

/**
 * A simple [Fragment] subclass.
 */
class PwnedPasswordFragment : Fragment() {

    private lateinit var navEvents: PublishProcessor<NavEvent>
    private lateinit var fragmentRootView: View
    private var isFullFragment: Boolean = false
    private lateinit var pwnedButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var passwordField: EditText
    private lateinit var passwordPwned: TextView
    private lateinit var passwordOk: TextView
    private lateinit var errorMsg: TextView
    private lateinit var myBroadcastReceiver: LocalBroadcastReceiver
    private var enteredPassword: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            val args: PwnedPasswordFragmentArgs by navArgs()
            isFullFragment = true
            enteredPassword = args.password
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        fragmentRootView = inflater.inflate(R.layout.fragment_pwned_password, container, false)

        passwordField = fragmentRootView.findViewById(R.id.password)
        passwordOk = fragmentRootView.findViewById(R.id.result_ok)
        passwordPwned = fragmentRootView.findViewById(R.id.result_pwned)
        progressBar = fragmentRootView.findViewById(R.id.progressbar)
        pwnedButton = fragmentRootView.findViewById(R.id.check_pwned)
        errorMsg = fragmentRootView.findViewById(R.id.error_msg)

        pwnedButton.setOnClickListener { checkPassword(passwordField.text.toString()) }

        passwordField.doAfterTextChanged { action ->
            pwnedButton.isEnabled = action.toString().isNotEmpty()
            passwordOk.visibility = View.GONE
            passwordPwned.visibility = View.GONE
        }
        passwordField.setOnKeyListener { _, kCode, evt ->
            when {
                ((kCode == KeyEvent.KEYCODE_ENTER) && (evt.action == KeyEvent.ACTION_DOWN)) -> {
                    checkPassword(passwordField.text.toString())
                    return@setOnKeyListener true
                }
                else -> false
            }
        }
        if (isFullFragment) {
            fragmentRootView.findViewById<TextView>(R.id.title_pwned_passwords).visibility = View.GONE
        }

        return fragmentRootView
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navEvents = (requireActivity().applicationContext as HackedApplication).navEvents
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(fragmentRootView.windowToken, 0)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver()
        Analytics.trackView("Fragment~Password")
        if (isFullFragment && enteredPassword != "" ) {
            passwordField.setText(enteredPassword)
            checkPassword(enteredPassword)
            enteredPassword = ""
        }
        passwordOk.visibility = View.GONE
        passwordPwned.visibility = View.GONE
        progressBar.visibility = View.GONE
    }

    override fun onPause() {
        passwordField.text.clear()
        unregisterReceiver()
        super.onPause()
    }

    private fun checkPassword(password: String) {
        // navigate to full screen pwnedpassword fragment
        if (! isFullFragment) {
            navEvents.onNext(NavEvent(NavEvent.Destination.PWNED_PASSWORDS, null, password))
            return
        }

        passwordOk.visibility = View.GONE
        passwordPwned.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        errorMsg.visibility = View.GONE
        PwnedPassword(LocalBroadcastManager.getInstance(requireContext())).check(password)
        Analytics.trackCustomEvent(CustomEvent.CHECK_PASSWORD_PWNED)
    }

    private fun registerReceiver() {
        val intentFilter = IntentFilter(PwnedPassword.BROADCAST_ACTION_PASSWORD_PWNED)
        myBroadcastReceiver = LocalBroadcastReceiver(this)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(myBroadcastReceiver, intentFilter)
    }

    private fun unregisterReceiver() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(myBroadcastReceiver)
    }

    private fun handleResult(pwned: Boolean, numPwned: Int, exception: Boolean) {
        progressBar.visibility = View.GONE
        if (exception) {
            Analytics.trackCustomEvent(CustomEvent.PASSWORD_PWNED_EXCEPTION)
            errorMsg.visibility = View.VISIBLE
        } else if (!pwned) {
            Analytics.trackCustomEvent(CustomEvent.PASSWORD_NOT_PWNED)
            passwordOk.visibility = View.VISIBLE
        } else {
            Analytics.trackCustomEvent(CustomEvent.PASSWORD_PWNED)
            passwordPwned.visibility = View.VISIBLE
            val t = getString(R.string.password_pwned, StringHelper.addDigitSeperator(numPwned.toString()))
            passwordPwned.text = t
        }
    }

    private class LocalBroadcastReceiver(private val pwnedPasswordFragment: PwnedPasswordFragment) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "received local broadcast message")
            pwnedPasswordFragment.handleResult(
                    intent.getBooleanExtra(PwnedPassword.EXTRA_PASSWORD_PWNED, false),
                    intent.getIntExtra(PwnedPassword.EXTRA_PASSWORD_PWNED_Count, 0),
                    intent.getBooleanExtra(PwnedPassword.EXTRA_EXCEPTION, false))
        }
    }

    companion object {
        const val TAG = "PwnedPasswordFragment"
    }

}
