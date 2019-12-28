package li.doerf.hacked.ui.fragments


import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import li.doerf.hacked.R
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.db.daos.AccountDao
import li.doerf.hacked.db.entities.Account
import li.doerf.hacked.ui.adapters.AccountsAdapter
import li.doerf.hacked.ui.viewmodels.AccountViewModel
import org.joda.time.format.DateTimeFormat
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Use the [AccountsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AccountsFragment : Fragment() {
    private lateinit var accountDao: AccountDao
    private lateinit var accountsAdapter: AccountsAdapter

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accountDao = AppDatabase.get(context).accountDao
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val fragmentRootView = inflater.inflate(R.layout.fragment_accounts, container, false)

        val accountsList: RecyclerView = fragmentRootView.findViewById(R.id.accounts_list)
        accountsList.setHasFixedSize(true)
        val lm = LinearLayoutManager(context)
        accountsList.layoutManager = lm
        accountsList.adapter = accountsAdapter

        CoroutineScope(Job()).launch {
            withContext(Dispatchers.IO) {
                val lastCheckedAccount = accountDao.lastChecked
                withContext(Dispatchers.Main) {
                    Log.d("AccountsFragment", "lastChecked: " + lastCheckedAccount.lastChecked)
                    val lastChecked = fragmentRootView.findViewById<TextView>(R.id.last_checked)
                    if (lastCheckedAccount != null) {
                        val dtfOut = DateTimeFormat.forPattern("yyyy/MM/dd HH:mm")
                        Log.d("AccountsFragment", dtfOut.print(lastCheckedAccount.lastChecked))
                        lastChecked.text = dtfOut.print(lastCheckedAccount.lastChecked)
                        lastChecked.visibility = View.VISIBLE
                    } else {
                        lastChecked.visibility = View.INVISIBLE
                    }
                }
            }

        }

        return fragmentRootView
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        accountsAdapter = AccountsAdapter(context, ArrayList())
        val accountsViewModel = ViewModelProviders.of(this).get(AccountViewModel::class.java)
        accountsViewModel.accountList.observe(this, Observer { accounts: List<Account> -> accountsAdapter.addItems(accounts) })
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         * @return A new instance of fragment AccountsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() = AccountsFragment()
//        fun newInstance(param1: String, param2: String) =
//                AccountsFragment().apply {
//                    arguments = Bundle().apply {
//                        putString(ARG_PARAM1, param1)
//                        putString(ARG_PARAM2, param2)
//                    }
//                }
    }
}
