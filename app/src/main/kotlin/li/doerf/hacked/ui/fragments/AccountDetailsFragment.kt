package li.doerf.hacked.ui.fragments

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import li.doerf.hacked.HackedApplication
import li.doerf.hacked.R
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.db.entities.Account
import li.doerf.hacked.db.entities.Breach
import li.doerf.hacked.ui.HibpInfo
import li.doerf.hacked.ui.adapters.BreachesAdapter
import li.doerf.hacked.ui.viewmodels.BreachViewModel
import li.doerf.hacked.utils.BackgroundTaskHelper
import java.util.*


class AccountDetailsFragment : Fragment() {

    private lateinit var myBreachesAdapter: BreachesAdapter
    private lateinit var myViewModel: BreachViewModel
    private var myAccountId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args: AccountDetailsFragmentArgs by navArgs()
        myAccountId = args.accountId
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        myViewModel = ViewModelProviders.of(this).get(BreachViewModel::class.java)
        myBreachesAdapter = BreachesAdapter(getContext(), ArrayList())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_account_details, container, false)
        val noBreachFound: CardView = view.findViewById(R.id.no_breach_found)
        val breachHelp: CardView = view.findViewById(R.id.breach_help)
        val accountDao = AppDatabase.get(context).accountDao
        // TODO coroutine
        BackgroundTaskHelper<List<Account>>().runInBackgroundAndConsumeOnMain({ accountDao.findById(myAccountId) }) { accounts: List<Account> ->
            for (account in accounts) {
                activity!!.title = account.name
                myViewModel.getBreachList(
                        account.id).observe(
                        this, Observer { breaches: List<Breach?>? ->
                    myBreachesAdapter.addItems(breaches)
                    if (myBreachesAdapter.itemCount == 0) {
                        noBreachFound.visibility = View.VISIBLE
                        breachHelp.visibility = View.GONE
                    } else {
                        noBreachFound.visibility = View.GONE
                        breachHelp.visibility = View.VISIBLE
                    }
                })
            }
        }
        val whatNow: AppCompatTextView = view.findViewById(R.id.what_now)
        whatNow.setOnClickListener { event: View? ->
            val breachHelpText: Group = view.findViewById(R.id.breach_help_text)
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
        val textOne: AppCompatTextView = view.findViewById(R.id.t1)
        textOne.movementMethod = LinkMovementMethod.getInstance()
        textOne.text = Html.fromHtml(text)
        val breachesList: RecyclerView = view.findViewById(R.id.breaches_list)
        breachesList.setHasFixedSize(true)
        val lm = LinearLayoutManager(context)
        breachesList.layoutManager = lm
        breachesList.adapter = myBreachesAdapter
        val dividerItemDecoration = DividerItemDecoration(breachesList.context,
                lm.orientation)
        breachesList.addItemDecoration(dividerItemDecoration)
        HibpInfo.prepare(context, view.findViewById(R.id.hibp_info), breachesList)
        return view
    }

    override fun onResume() {
        super.onResume()
        (activity!!.application as HackedApplication).trackView("Fragment~AccountDetails")
    }

}
