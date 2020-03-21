package li.doerf.hacked.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.reactivex.processors.PublishProcessor
import kotlinx.coroutines.*
import li.doerf.hacked.HackedApplication
import li.doerf.hacked.R
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.util.NavEvent
import li.doerf.hacked.util.RatingHelper

class OverviewFragment : Fragment() {

    private lateinit var navEvents: PublishProcessor<NavEvent>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_overview, container, false)
    }

    override fun onStart() {
        super.onStart()

        CoroutineScope(Job()).launch {
            if (isFirstUse()) {
                withContext(Dispatchers.Main) {
                    navEvents.onNext(NavEvent(NavEvent.Destination.FIRST_USE, null))
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navEvents = (activity?.applicationContext as HackedApplication).navEvents
    }

    override fun onResume() {
        super.onResume()
        CoroutineScope(Job()).launch {
            RatingHelper(activity!!).showRateUsDialogDelayed()
        }
    }

    companion object {
        const val PREF_KEY_FIRST_USE_SEEN: String = "PREF_KEY_FIRST_USE_SEEN"
        val LOGTAG: String = this::class.java.name
    }

}
