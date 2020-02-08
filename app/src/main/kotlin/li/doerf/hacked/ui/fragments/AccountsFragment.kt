package li.doerf.hacked.ui.fragments


import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import li.doerf.hacked.R
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.db.daos.AccountDao
import li.doerf.hacked.db.entities.Account
import li.doerf.hacked.remote.haveibeenpwned.HIBPAccountCheckerWorker
import li.doerf.hacked.services.AccountService
import li.doerf.hacked.ui.adapters.AccountsAdapter
import li.doerf.hacked.ui.viewmodels.AccountViewModel
import org.joda.time.format.DateTimeFormat
import java.util.*

class AccountsFragment : Fragment(), NavDirectionsToAccountDetailsFactory {
    private lateinit var fragmentRootView: View
    private lateinit var accountEditText: EditText
    private lateinit var groupAddAccount: Group
    private lateinit var accountDao: AccountDao
    private lateinit var accountsAdapter: AccountsAdapter
    private var isFullView: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        accountDao = AppDatabase.get(context).accountDao

        if (arguments != null) {
            val args: AccountsFragmentArgs by navArgs()
            if (args.fullView) {
                isFullView = true
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        fragmentRootView = inflater.inflate(R.layout.fragment_accounts, container, false)

        val accountsList: RecyclerView = fragmentRootView.findViewById(R.id.accounts_list)
        accountsList.setHasFixedSize(true)
        val lm = LinearLayoutManager(context)
        accountsList.layoutManager = lm
        accountsList.adapter = accountsAdapter

        CoroutineScope(Job()).launch {
            withContext(Dispatchers.IO) {
                val lastCheckedAccount = accountDao.lastChecked
                withContext(Dispatchers.Main) {
                    Log.d("AccountsFragment", "lastChecked: " + lastCheckedAccount?.lastChecked)
                    val lastChecked = fragmentRootView.findViewById<TextView>(R.id.last_checked)
                    if (lastCheckedAccount != null && lastCheckedAccount.lastChecked != null) {
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

        val title = fragmentRootView.findViewById<TextView>(R.id.title_accounts)
        title.setOnClickListener {
            val action = OverviewFragmentDirections.actionOverviewFragmentToAccountsListFullFragment()
            fragmentRootView.findNavController().navigate(action)
        }

        accountEditText = fragmentRootView.findViewById(R.id.account)

        val addButton = fragmentRootView.findViewById<Button>(R.id.add)
        addButton.setOnClickListener {
            val accountName = accountEditText.text
            AccountService(activity!!.application).addAccount(accountName.toString())
            hideSectionAndKeyboard(fragmentRootView)
        }

        val cancelButton = fragmentRootView.findViewById<Button>(R.id.cancel)
        cancelButton.setOnClickListener {
            hideSectionAndKeyboard(fragmentRootView)
        }

        groupAddAccount = fragmentRootView.findViewById(R.id.group_add_account)

        return fragmentRootView
    }

    private fun hideSectionAndKeyboard(fragmentRootView: View) {
        groupAddAccount.visibility = View.GONE
        val imm = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(fragmentRootView.windowToken, 0)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        accountsAdapter = AccountsAdapter(context, ArrayList(), this)
        val accountsViewModel = ViewModelProviders.of(this).get(AccountViewModel::class.java)
        accountsViewModel.accountList.observe(this, Observer { accounts: List<Account> -> accountsAdapter.addItems(accounts) })
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_fragment_accounts, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_add -> {
                accountEditText.text.clear()
                groupAddAccount.visibility = View.VISIBLE
                true
            }
            R.id.action_refresh -> {
                val checker = OneTimeWorkRequest.Builder(HIBPAccountCheckerWorker::class.java)
                        .build()
                WorkManager.getInstance(context!!).enqueue(checker)
                Snackbar.make(fragmentRootView, getString(R.string.snackbar_checking_account), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
                true
            }
            else -> super.onOptionsItemSelected(item)

        }
    }



    override fun createNavDirections(accountId: Long): NavDirections {
        if (isFullView) {
            val action = AccountsFragmentDirections.actionAccountsListFullFragmentToAccountDetailsFragment(accountId)
            action.accountId = accountId
            return action
        }

        val action = OverviewFragmentDirections.actionOverviewFragmentToAccountDetailsFragment()
        action.accountId = accountId
        return action
    }

    companion object {
        const val LOGTAG = "AccountsFragmentBase"
        const val ARG_FULL_VIEW = "FullView"
    }

}