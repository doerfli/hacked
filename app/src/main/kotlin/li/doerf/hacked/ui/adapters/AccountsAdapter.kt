package li.doerf.hacked.ui.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.processors.PublishProcessor
import li.doerf.hacked.HackedApplication
import li.doerf.hacked.R
import li.doerf.hacked.db.entities.Account
import li.doerf.hacked.util.NavEvent
import li.doerf.hacked.utils.NotificationHelper
import java.util.*


class AccountsAdapter(private val context: Context, private var accountList: List<Account>) : RecyclerView.Adapter<RecyclerViewHolder>() {

    private lateinit var navEvents: PublishProcessor<NavEvent>

    override fun onViewAttachedToWindow(holder: RecyclerViewHolder) {
        super.onViewAttachedToWindow(holder)
        navEvents = (context as HackedApplication).navEvents
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val itemLayout = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_account, parent, false)
        return RecyclerViewHolder(itemLayout)
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        val view = holder.view
        val account = accountList[position]
        bindView(view, account)
    }

    private fun bindView(view: View, account: Account) {
        val statusIndicator = view.findViewById<View>(R.id.status_indicator)
        setStatusIndicatorColor(account, statusIndicator)

        val nameView = view.findViewById<TextView>(R.id.name)
        nameView.text = account.name

        val breachCount = view.findViewById<TextView>(R.id.breach_count)
        val breachCounter = account.numBreaches
        Log.d(LOGTAG, "breachCounter: ${breachCounter}")

        if (breachCounter > 0) {
            breachCount.text = String.format(Locale.getDefault(), "%d", breachCounter)
            breachCount.visibility = View.VISIBLE
        } else {
            breachCount.visibility = View.GONE
        }

        view.setOnClickListener {
            NotificationHelper.cancelAll(context)
            navEvents.onNext(NavEvent(NavEvent.Destination.ACCOUNTS_DETAILS, account.id, null))
        }
    }

    private fun setStatusIndicatorColor(account: Account, statusIndicator: View) {
        if (account.hacked) {
            statusIndicator.setBackgroundColor(context.resources.getColor(R.color.account_status_breached))
        } else if (!account.hacked && account.lastChecked == null) {
            statusIndicator.setBackgroundColor(context.resources.getColor(R.color.account_status_unknown))
        } else {
            if (account.numBreaches == 0) {
                statusIndicator.setBackgroundColor(context.resources.getColor(R.color.account_status_ok))
            } else {
                statusIndicator.setBackgroundColor(context.resources.getColor(R.color.account_status_only_acknowledged))
            }
        }
    }

    override fun getItemCount(): Int {
        return accountList.size
    }

    fun addItems(accountList: List<Account>) {
        this.accountList = accountList
        notifyDataSetChanged()
    }

    companion object {
        private val LOGTAG = this::class.java.name
    }

}