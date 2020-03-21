package li.doerf.hacked.ui.fragments


import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.android.material.snackbar.Snackbar
import io.reactivex.processors.PublishProcessor
import li.doerf.hacked.HackedApplication
import li.doerf.hacked.R
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.db.daos.AccountDao
import li.doerf.hacked.db.entities.Account
import li.doerf.hacked.remote.haveibeenpwned.HIBPAccountCheckerWorker
import li.doerf.hacked.services.AccountService
import li.doerf.hacked.ui.adapters.AccountsAdapter
import li.doerf.hacked.ui.viewmodels.AccountViewModel
import li.doerf.hacked.util.NavEvent
import org.joda.time.format.DateTimeFormat
import java.util.*

class AccountsFragment : Fragment() {
    private lateinit var hibpInfo: TextView
    private lateinit var fragmentRootView: View
    private lateinit var accountEditText: EditText
    private lateinit var groupAddAccount: Group
    private lateinit var accountDao: AccountDao
    private lateinit var accountsAdapter: AccountsAdapter
    private lateinit var navEvents: PublishProcessor<NavEvent>
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

        val lastChecked = fragmentRootView.findViewById<TextView>(R.id.last_checked)
        val viewModel: AccountViewModel by viewModels()
        viewModel.lastChecked.observe(viewLifecycleOwner, Observer { lastCheckedAccount: Account? ->
            if (lastCheckedAccount?.lastChecked != null) {
                val dtfOut = DateTimeFormat.forPattern("yyyy/MM/dd HH:mm")
                Log.d("AccountsFragment", dtfOut.print(lastCheckedAccount.lastChecked))
                lastChecked.text = dtfOut.print(lastCheckedAccount.lastChecked)
                lastChecked.visibility = View.VISIBLE
            } else {
                lastChecked.visibility = View.INVISIBLE
            }
        })

        if (!isFullView) {
            val title = fragmentRootView.findViewById<TextView>(R.id.title_accounts)
            title.setOnClickListener {
                navEvents.onNext(NavEvent(NavEvent.Destination.ACCOUNTS_LIST, null, null))
            }
        }

        accountEditText = fragmentRootView.findViewById(R.id.account)

        val addButton = fragmentRootView.findViewById<Button>(R.id.add)
        addButton.setOnClickListener {
            val accountName = accountEditText.text
            AccountService(activity!!.application).addAccount(accountName.toString())
            hideSectionAndKeyboard(fragmentRootView)
            hibpInfo.visibility = View.VISIBLE
        }

        val cancelButton = fragmentRootView.findViewById<Button>(R.id.cancel)
        cancelButton.setOnClickListener {
            hideSectionAndKeyboard(fragmentRootView)
            hibpInfo.visibility = View.VISIBLE
        }

        groupAddAccount = fragmentRootView.findViewById(R.id.group_add_account)

        hibpInfo = fragmentRootView.findViewById(R.id.hibp_info)
        hibpInfo.movementMethod = LinkMovementMethod.getInstance()
        hibpInfo.text = Html.fromHtml("${getString(R.string.data_provided_by)} <a href=\"https://haveibeenpwned.com\">Have i been pwned?</a>")

        return fragmentRootView
    }

    private fun hideSectionAndKeyboard(fragmentRootView: View) {
        groupAddAccount.visibility = View.GONE
        val imm = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(fragmentRootView.windowToken, 0)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        accountsAdapter = AccountsAdapter(activity!!.applicationContext, ArrayList())
        val accountsViewModel: AccountViewModel by viewModels()
        accountsViewModel.accountList.observe(this, Observer { accounts: List<Account> -> accountsAdapter.addItems(accounts) })
        navEvents = (activity!!.applicationContext as HackedApplication).navEvents
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_fragment_accounts, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_add -> {
                accountEditText.text.clear()
                groupAddAccount.visibility = View.VISIBLE
                hibpInfo.visibility = View.GONE
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

    companion object {
        const val LOGTAG = "AccountsFragmentBase"
    }

}
