package li.doerf.hacked.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_overview, container, false)
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
    }

    companion object {
        const val PREF_KEY_FIRST_USE_SEEN: String = "PREF_KEY_FIRST_USE_SEEN"
        val LOGTAG: String = this::class.java.name
    }

}
