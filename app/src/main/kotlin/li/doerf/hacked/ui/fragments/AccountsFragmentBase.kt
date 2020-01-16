package li.doerf.hacked.ui.fragments


import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import io.reactivex.Single
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import li.doerf.hacked.CustomEvent
import li.doerf.hacked.HackedApplication
import li.doerf.hacked.R
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.db.daos.AccountDao
import li.doerf.hacked.db.entities.Account
import li.doerf.hacked.remote.haveibeenpwned.HIBPAccountCheckerWorker
import li.doerf.hacked.ui.adapters.AccountsAdapter
import li.doerf.hacked.ui.viewmodels.AccountViewModel
import org.joda.time.format.DateTimeFormat
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Use the [AccountsFragmentBase.newInstance] factory method to
 * create an instance of this fragment.
 */
abstract class AccountsFragmentBase : Fragment(), NavDirectionsToAccountDetailsFactory {
    private lateinit var groupAddAccount: Group
    private lateinit var accountDao: AccountDao
    private lateinit var accountsAdapter: AccountsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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

        val title = fragmentRootView.findViewById<TextView>(R.id.title_accounts)
        title.setOnClickListener {
            val action = OverviewFragmentDirections.actionOverviewFragmentToAccountsListFullFragment()
            fragmentRootView.findNavController().navigate(action)
        }

        val account = fragmentRootView.findViewById<EditText>(R.id.account)

        val addButton = fragmentRootView.findViewById<Button>(R.id.add)
        addButton.setOnClickListener {
            val accountName = account.text
            addAccount(accountName.toString())
            groupAddAccount.visibility = View.GONE
        }

        val cancelButton = fragmentRootView.findViewById<Button>(R.id.cancel)
        cancelButton.setOnClickListener {
            groupAddAccount.visibility = View.GONE
        }

        groupAddAccount = fragmentRootView.findViewById(R.id.group_add_account)

        return fragmentRootView
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
        if (item.itemId == R.id.action_add) {
            groupAddAccount.visibility = View.VISIBLE
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun addAccount(aName: String) {
        if (aName == null || aName.trim { it <= ' ' } == "") {
            Toast.makeText(context, getString(R.string.toast_enter_valid_name), Toast.LENGTH_LONG).show()
            Log.w(LOGTAG, "account name not valid")
            return
        }
        val name = aName.trim { it <= ' ' }
        val accountDao = AppDatabase.get(context).accountDao
        getAccountCount(name, accountDao)
                .subscribe(Consumer { count: Int ->
                    if (count == 0) {
                        val account = Account()
                        account.name = name
                        account.numBreaches = 0
                        account.numAcknowledgedBreaches = 0
                        insertAccount(accountDao, account, activity!!.application)
                    }

                })
    }

    private fun getAccountCount(name: String, accountDao: AccountDao): Single<Int> {
        return Single.fromCallable { accountDao.countByName(name) }
                .subscribeOn(Schedulers.io())
    }

    private fun insertAccount(accountDao: AccountDao, account: Account, application: Application) {
        Single.fromCallable { accountDao.insert(account) }
                .subscribeOn(Schedulers.io())
                .subscribe({ ids: List<Long> ->
                    (application as HackedApplication).trackCustomEvent(CustomEvent.ACCOUNT_ADDED)
                    val inputData = Data.Builder()
                            .putLong(HIBPAccountCheckerWorker.KEY_ID, ids[0])
                            .build()
                    val constraints = Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.UNMETERED)
                            .build()
                    val checker = OneTimeWorkRequest.Builder(HIBPAccountCheckerWorker::class.java)
                            .setInputData(inputData)
                            .setConstraints(constraints)
                            .build()
                    WorkManager.getInstance().enqueue(checker)
                }) { throwable: Throwable? -> Log.e(ContentValues.TAG, "Error msg", throwable) }
    }

    companion object {
        const val LOGTAG = "AccountsFragmentBase"
    }

}
