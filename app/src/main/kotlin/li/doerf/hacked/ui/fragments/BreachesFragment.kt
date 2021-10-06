package li.doerf.hacked.ui.fragments


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.processors.PublishProcessor
import li.doerf.hacked.HackedApplication
import li.doerf.hacked.R
import li.doerf.hacked.db.entities.BreachedSite
import li.doerf.hacked.ui.adapters.BreachedSitesAdapter
import li.doerf.hacked.ui.viewmodels.BreachedSitesViewModel
import li.doerf.hacked.util.NavEvent
import java.util.*

/**
 * A simple [Fragment] subclass.
 * create an instance of this fragment.
 */
class BreachesFragment : Fragment() {
    private val breachedSitesViewModel: BreachedSitesViewModel by viewModels()
    private lateinit var navEvents: PublishProcessor<NavEvent>
    private lateinit var breachedSitesAdapter: BreachedSitesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val fragmentRootView = inflater.inflate(R.layout.fragment_breaches, container, false)

        val headingChevron = fragmentRootView.findViewById<ImageView>(R.id.show_details)
        headingChevron.setOnClickListener {
            navEvents.onNext(NavEvent(NavEvent.Destination.ALL_BREACHES, null, null))
        }

        val breachedSites: RecyclerView = fragmentRootView.findViewById(R.id.breached_sites_list)
//        breachedSites.setHasFixedSize(true)
        val lmbs = LinearLayoutManager(context)
        breachedSites.layoutManager = lmbs
        breachedSites.adapter = breachedSitesAdapter

        return fragmentRootView
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        breachedSitesAdapter = BreachedSitesAdapter(requireActivity().applicationContext, ArrayList(), true)
        breachedSitesViewModel.breachesSitesMostRecent!!.observe(this, Observer { sites: List<BreachedSite> -> breachedSitesAdapter.addItems(sites) })
        navEvents = (requireActivity().applicationContext as HackedApplication).navEvents
    }

    override fun onResume() {
        super.onResume()
        if (breachedSitesAdapter.itemCount == 0 ) {
            AllBreachesFragment.reloadBreachedSites(requireActivity())
        }
    }

}
