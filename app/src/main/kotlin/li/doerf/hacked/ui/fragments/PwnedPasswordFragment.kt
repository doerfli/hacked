package li.doerf.hacked.ui.fragments


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import li.doerf.hacked.CustomEvent
import li.doerf.hacked.HackedApplication
import li.doerf.hacked.R
import li.doerf.hacked.remote.pwnedpasswords.PwnedPassword
import li.doerf.hacked.utils.StringHelper

/**
 * A simple [Fragment] subclass.
 */
class PwnedPasswordFragment : Fragment() {

    private lateinit var pwnedButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var passwordField: EditText
    private lateinit var passwordPwned: TextView
    private lateinit var passwordOk: TextView
    private lateinit var myBroadcastReceiver: LocalBroadcastReceiver
    private lateinit var enteredPassword: String

//    override fun onCreate(savedInstanceState: Bundle?) {
//        if (arguments != null) {
//            val args: PPF by navArgs()
//            if (args.fullView) {
//                isFullView = true
//            }
//        }
//    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootview = inflater.inflate(R.layout.fragment_pwned_password, container, false)

        passwordField = rootview.findViewById(R.id.password)
        passwordOk = rootview.findViewById(R.id.result_ok)
        passwordPwned = rootview.findViewById(R.id.result_pwned)
        progressBar = rootview.findViewById(R.id.progressbar)

        pwnedButton = rootview.findViewById(R.id.check_pwned)
        pwnedButton.setOnClickListener { checkPassword(passwordField.text.toString()) }

        return rootview
    }

    override fun onResume() {
        super.onResume()
        registerReceiver()
        (activity!!.application as HackedApplication).trackView("Fragment~Password")
    }

    override fun onPause() {
        unregisterReceiver()
        super.onPause()
    }

    private fun checkPassword(password: String) {
        passwordOk.visibility = View.GONE
        passwordPwned.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        PwnedPassword(LocalBroadcastManager.getInstance(context!!)).check(password)
        (activity!!.application as HackedApplication).trackCustomEvent(CustomEvent.CHECK_PASSWORD_PWNED)
    }

    private fun registerReceiver() {
        val intentFilter = IntentFilter(PwnedPassword.BROADCAST_ACTION_PASSWORD_PWNED)
        myBroadcastReceiver = LocalBroadcastReceiver(this)
        LocalBroadcastManager.getInstance(context!!).registerReceiver(myBroadcastReceiver, intentFilter)
    }

    private fun unregisterReceiver() {
        LocalBroadcastManager.getInstance(context!!).unregisterReceiver(myBroadcastReceiver)
    }

    private fun handleResult(pwned: Boolean, numPwned: Int, exception: Boolean) {
        progressBar.visibility = View.GONE
        if (exception) {
            (activity!!.application as HackedApplication).trackCustomEvent(CustomEvent.PASSWORD_PWNED_EXCEPTION)
            Toast.makeText(context, getString(R.string.error_download_data), Toast.LENGTH_SHORT).show()
        } else if (!pwned) {
            (activity!!.application as HackedApplication).trackCustomEvent(CustomEvent.PASSWORD_NOT_PWNED)
            passwordOk.visibility = View.VISIBLE
        } else {
            (activity!!.application as HackedApplication).trackCustomEvent(CustomEvent.PASSWORD_PWNED)
            passwordPwned.visibility = View.VISIBLE
            val t = getString(R.string.password_pwned, StringHelper.addDigitSeperator(Integer.toString(numPwned)))
            passwordPwned.setText(t)
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
