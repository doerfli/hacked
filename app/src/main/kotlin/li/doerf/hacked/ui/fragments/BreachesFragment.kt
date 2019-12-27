package li.doerf.hacked.ui.fragments


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
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
 * Use the [BreachesFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BreachesFragment : Fragment() {
    private lateinit var breachedSitesAdapter: BreachedSitesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val fragmentRootView = inflater.inflate(R.layout.fragment_breaches, container, false)

        val breachedSites: RecyclerView = fragmentRootView.findViewById(R.id.breached_sites_list)
        breachedSites.setHasFixedSize(true)
        val lmbs = LinearLayoutManager(context)
        breachedSites.layoutManager = lmbs
        breachedSites.adapter = breachedSitesAdapter

        return fragmentRootView
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        breachedSitesAdapter = BreachedSitesAdapter(getContext(), ArrayList())
        val breachedSitesViewModel = ViewModelProviders.of(this).get(BreachedSitesViewModel::class.java)
        breachedSitesViewModel.breachesSitesMostRecent.observe(this, Observer { sites: List<BreachedSite?>? -> breachedSitesAdapter.addItems(sites) })
    }

    override fun onResume() {
        super.onResume()
        if (breachedSitesAdapter.getItemCount() == 0 ) {
            reloadBreachedSites()
        }
    }

    fun reloadBreachedSites() {
        val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build()
        val checker = OneTimeWorkRequest.Builder(BreachedSitesWorker::class.java)
                .setConstraints(constraints)
                .build()
        WorkManager.getInstance().enqueue(checker)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         * @return A new instance of fragment BreachesFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() = BreachesFragment()
    }
}
