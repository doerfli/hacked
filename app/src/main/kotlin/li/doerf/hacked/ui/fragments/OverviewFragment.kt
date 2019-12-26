package li.doerf.hacked.ui.fragments

import android.content.Context
import android.net.Uri
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
import li.doerf.hacked.db.entities.Account
import li.doerf.hacked.db.entities.BreachedSite
import li.doerf.hacked.remote.hibp.BreachedSitesWorker
import li.doerf.hacked.ui.adapters.AccountsAdapter
import li.doerf.hacked.ui.adapters.BreachedSitesAdapter
import li.doerf.hacked.ui.viewmodels.AccountViewModel
import li.doerf.hacked.ui.viewmodels.BreachedSitesViewModel
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [OverviewFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [OverviewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OverviewFragment : Fragment() {
    private lateinit var breachedSitesAdapter: BreachedSitesAdapter
    private lateinit var accountsAdapter: AccountsAdapter
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val fragmentRootView = inflater.inflate(R.layout.fragment_overview, container, false)

        val accountsList: RecyclerView = fragmentRootView.findViewById(R.id.accounts_list)
        accountsList.setHasFixedSize(true)
        val lm = LinearLayoutManager(context)
        accountsList.layoutManager = lm
        accountsList.adapter = accountsAdapter

        val breachedSites: RecyclerView = fragmentRootView.findViewById(R.id.breached_sites_list)
        breachedSites.setHasFixedSize(true)
        val lmbs = LinearLayoutManager(context)
        breachedSites.layoutManager = lmbs
        breachedSites.adapter = breachedSitesAdapter

        return fragmentRootView
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
//        if (context is OnFragmentInteractionListener) {
//            listener = context
//        } else {
//            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
//        }

        accountsAdapter = AccountsAdapter(getContext(), ArrayList(), fragmentManager)
        val accountsViewModel = ViewModelProviders.of(this).get(AccountViewModel::class.java)
        accountsViewModel.accountList.observe(this, Observer { accounts: List<Account?>? -> accountsAdapter.addItems(accounts) })

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

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment OverviewFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                OverviewFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}
