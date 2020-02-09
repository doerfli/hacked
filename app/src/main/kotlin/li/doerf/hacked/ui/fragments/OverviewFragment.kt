package li.doerf.hacked.ui.fragments

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import li.doerf.hacked.R
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.utils.RatingHelper

class OverviewFragment : Fragment() {

    private lateinit var hibpInfo: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_overview, container, false)

        hibpInfo = rootView.findViewById(R.id.hibp_info)
        hibpInfo.movementMethod = LinkMovementMethod.getInstance()
        hibpInfo.text = Html.fromHtml("${getString(R.string.data_provided_by)} <a href=\"https://haveibeenpwned.com\">Have i been pwned?</a>")

        return rootView
    }

    override fun onStart() {
        super.onStart()

        GlobalScope.launch {
            if (isFirstUse()) {
                val action = OverviewFragmentDirections.actionOverviewFragmentToFirstUseFragment()
                withContext(Dispatchers.Main) {
                    findNavController().navigate(action)
                }
            }
        }
    }

    private fun isFirstUse(): Boolean {
        val sharedPref = activity!!.getPreferences(Context.MODE_PRIVATE)
        val firstUseSeen = sharedPref.getBoolean(PREF_KEY_FIRST_USE_SEEN, false)
        val numAccounts = AppDatabase.get(context).accountDao.all.size
        Log.d(LOGTAG, "firstUseSeen: $firstUseSeen / numAccounts: $numAccounts")
        return ! firstUseSeen && numAccounts == 0
    }


    override fun onResume() {
        super.onResume()
        RatingHelper(context).showRateUsDialogDelayed()
        hibpInfo.visibility = View.VISIBLE
    }

    companion object {
        const val PREF_KEY_FIRST_USE_SEEN: String = "PREF_KEY_FIRST_USE_SEEN"
        val LOGTAG: String = this::class.java.name
    }

}
