package li.doerf.hacked.ui.fragments


import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import li.doerf.hacked.R
import li.doerf.hacked.db.entities.BreachedSite
import li.doerf.hacked.remote.hibp.BreachedSitesWorker
import li.doerf.hacked.ui.adapters.BreachedSitesAdapter
import li.doerf.hacked.ui.viewmodels.BreachedSitesViewModel
import java.util.*

/**
 * A simple [Fragment] subclass.
 * create an instance of this fragment.
 */
class AllBreachesFragment : Fragment() {
    private lateinit var layoutManager: LinearLayoutManager
    private var breachedSiteId: Long = -1
    private lateinit var breachedSitesAdapter: BreachedSitesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args: AllBreachesFragmentArgs by navArgs()
        breachedSiteId = args.breachedSiteId
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val fragmentRootView = inflater.inflate(R.layout.fragment_all_breaches, container, false)

        val breachedSites: RecyclerView = fragmentRootView.findViewById(R.id.breached_sites_list)
        breachedSites.setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context)
        breachedSites.layoutManager = layoutManager
        breachedSites.adapter = breachedSitesAdapter

        return fragmentRootView
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        breachedSitesAdapter = BreachedSitesAdapter(context, ArrayList(), false)
        val breachedSitesViewModel = ViewModelProviders.of(this).get(BreachedSitesViewModel::class.java)
        breachedSitesViewModel.breachesSites.observe(this, Observer { sites: List<BreachedSite> ->
            sites.find { it.id == breachedSiteId }?.detailsVisible = true
            breachedSitesAdapter.addItems(sites)
            if (breachedSiteId > -1 && sites.isNotEmpty()) {
                val position = sites.indexOfFirst { it.id == breachedSiteId  }
                if (position > -1) {
                    layoutManager.scrollToPositionWithOffset(position, 0)
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (breachedSitesAdapter.itemCount == 0 ) {
            reloadBreachedSites(activity!!)
        }
    }



    companion object {
        private const val PREF_KEY_LAST_BREACHED_SITES_SYNC = "PREF_KEY_LAST_BREACHED_SITES_SYNC"
        private const val SIXHOURS = 6 * 60 * 60 * 1000

        fun reloadBreachedSites(activity: Activity) {
            val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return
            val lastSync = sharedPref.getLong(PREF_KEY_LAST_BREACHED_SITES_SYNC, 0)

            if (System.currentTimeMillis() - lastSync > SIXHOURS) {
                val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .build()
                val checker = OneTimeWorkRequest.Builder(BreachedSitesWorker::class.java)
                        .setConstraints(constraints)
                        .build()
                WorkManager.getInstance().enqueue(checker)
                with (sharedPref.edit()) {
                    putLong(PREF_KEY_LAST_BREACHED_SITES_SYNC, System.currentTimeMillis());
                    commit()
                }
            }
        }
    }

}
