package li.doerf.hacked.ui.fragments

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.*
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import li.doerf.hacked.CustomEvent
import li.doerf.hacked.R
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.db.entities.Account
import li.doerf.hacked.db.entities.Breach
import li.doerf.hacked.ui.composable.BreachUi
import li.doerf.hacked.ui.viewmodels.BreachViewModel
import li.doerf.hacked.util.Analytics
import li.doerf.hacked.util.RatingHelper
import li.doerf.hacked.util.createCoroutingExceptionHandler
import li.doerf.hacked.utils.AccountHelper


class AccountDetailsFragment : Fragment() {

    private lateinit var rootView: View
    private var myAccountId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val args: AccountDetailsFragmentArgs by navArgs()
        myAccountId = args.accountId
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_account_details, container, false)
        val accountDao = AppDatabase.get(context).accountDao

        val breachesListView = rootView.findViewById<ComposeView>(R.id.breaches_list)
        val noBreachFound: CardView = rootView.findViewById(R.id.no_breach_found)
        val breachHelp: CardView = rootView.findViewById(R.id.breach_help)

        CoroutineScope(Job()).launch {
            val accounts = accountDao.findById(myAccountId)
            withContext(Dispatchers.Main) {
                for (account in accounts) {
                    displayBreaches(account, noBreachFound, breachHelp, breachesListView)
                }
            }
        }


        val whatNow: AppCompatTextView = rootView.findViewById(R.id.what_now)
        whatNow.setOnClickListener {
            val breachHelpText: Group = rootView.findViewById(R.id.breach_help_text)
            if (breachHelpText.visibility == View.GONE) {
                breachHelpText.visibility = View.VISIBLE
            } else {
                breachHelpText.visibility = View.GONE
            }
        }
        val link1 = "<a href=\"https://lastpass.com\">LastPass</a>"
        val link2 = "<a href=\"https://1password.com\">1Password</a>"
        val link3 = "<a href=\"https://dashlane.com\">Dashlane</a>"
        val text = getString(R.string.breach_details_first_text, link1, link2, link3)
        val textOne: AppCompatTextView = rootView.findViewById(R.id.t1)
        textOne.movementMethod = LinkMovementMethod.getInstance()
        textOne.text = Html.fromHtml(text)

        return rootView
    }

    private fun displayBreaches(
        account: Account,
        noBreachFound: CardView,
        breachHelp: CardView,
        breachesListView: ComposeView
    ) {
        val viewModel: BreachViewModel by viewModels()
        viewModel.getBreachList(
            account.id
        ).observe(viewLifecycleOwner, { breaches: List<Breach> ->
            if (breaches.isEmpty()) {
                noBreachFound.visibility = View.VISIBLE
                breachHelp.visibility = View.GONE
            } else {
                noBreachFound.visibility = View.GONE
                breachHelp.visibility = View.VISIBLE

                breachesListView.setContent {
                    BreachList(breaches)
                }
            }
        })

    }

    @Composable
    private fun BreachList(breaches: List<Breach>) {
        MaterialTheme {
            LazyColumn() {
                for (breach in breaches) {
                    item {
                        BreachRow(breach)
                        Divider(color = Color.LightGray)
                    }
                }
            }
        }
    }

    @Composable
    private fun BreachRow(breach: Breach) {
        val statusColor = if (!breach.acknowledged) {
            requireContext().resources.getColor(R.color.account_status_breached)
        } else {
            requireContext().resources.getColor(R.color.account_status_only_acknowledged)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .height(
                    IntrinsicSize.Max
                )
        ) {
            Spacer(
                modifier = Modifier
                    .width(10.dp)
                    .fillMaxHeight()
                    .background(color = Color(statusColor))
            )
            BreachUi(
                breach,
                requireContext()
            ) { id ->
                handleAcknowledgeClicked(id)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Analytics.trackView("Fragment~AccountDetails")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        return inflater.inflate(R.menu.menu_fragment_account_details, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_delete) {
            deleteAccount()
            return true
        }
        if (item.itemId == R.id.action_reset_acknowledgements) {
            resetAcknowledged()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun resetAcknowledged() {
        runBlocking(context = Dispatchers.IO) {
            launch(createCoroutingExceptionHandler(LOGTAG)) {
                val breachDao = AppDatabase.get(context).breachDao
                val breaches = breachDao.findByAccount(myAccountId)
                for (b in breaches) {
                    b.acknowledged = false
                    breachDao.update(b)
                }
            }
        }
    }

    private fun deleteAccount() {
        runBlocking(context = Dispatchers.IO) {
            launch(createCoroutingExceptionHandler(LOGTAG)) {
                val accountDao = AppDatabase.get(context).accountDao
                val breachDao = AppDatabase.get(context).breachDao
                val account = accountDao.findById(myAccountId).first()
                val breaches = breachDao.findByAccount(myAccountId)
                try {
                    for (b in breaches) {
                        breachDao.delete(b)
                    }
                    accountDao.delete(account)
                } finally {
                    Analytics.trackCustomEvent(CustomEvent.ACCOUNT_DELETED)
                }
            }
        }
        findNavController().popBackStack()
    }

    private fun handleAcknowledgeClicked(breachId: Long) {
        CoroutineScope(Job()).launch {
            val breachDao = AppDatabase.get(context).breachDao
            val breach = breachDao.findById(breachId)
            if (breach != null) {
                setBreachAcknowledged(breach)
                updateAccountIsHacked(breach.account)
                withContext(Dispatchers.Main) {
                    Snackbar.make(rootView, requireContext().getString(R.string.breach_acknowledged), Snackbar.LENGTH_SHORT).show()
                    RatingHelper(requireActivity()).setRatingCounterBelowthreshold()
                }
            } else {
                Log.w(LOGTAG, "no breach found with id $breachId")
            }
        }
    }

    private fun setBreachAcknowledged(breach: Breach) {
        breach.acknowledged = true
        val breachDao = AppDatabase.get(context).breachDao
        breachDao.update(breach)
        Log.d(LOGTAG, "breach updated - acknowledge = true")
        Analytics.trackCustomEvent(CustomEvent.BREACH_ACKNOWLEDGED)
    }

    private fun updateAccountIsHacked(accountId: Long) {
        val breachDao = AppDatabase.get(context).breachDao
        val accountDao = AppDatabase.get(context).accountDao

        val accounts = accountDao.findById(accountId)
        for (account in accounts) {
            if (!account.hacked) {
                return
            }
            AccountHelper(context).updateBreachCounts(account)
            if (breachDao.countUnacknowledged(accountId) == 0L) {
                Log.d(LOGTAG, "account has only acknowledged breaches")
                account.hacked = false
            }
            accountDao.update(account)
            Log.d(LOGTAG, "account updated - hacked = " + account.hacked + " numBreaches = " + account.numBreaches + " numAcknowledgedBreaches = " + account.numAcknowledgedBreaches)
        }
    }

    companion object {
        const val LOGTAG = "AccountDetailsFragment"
    }
}
