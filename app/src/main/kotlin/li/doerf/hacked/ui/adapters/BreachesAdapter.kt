package li.doerf.hacked.ui.adapters

import android.app.Activity
import android.content.Context
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*
import li.doerf.hacked.CustomEvent
import li.doerf.hacked.R
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.db.daos.AccountDao
import li.doerf.hacked.db.daos.BreachDao
import li.doerf.hacked.db.entities.Breach
import li.doerf.hacked.util.Analytics.Companion.trackCustomEvent
import li.doerf.hacked.util.RatingHelper
import li.doerf.hacked.utils.AccountHelper
import org.joda.time.format.DateTimeFormat

/**
 * Created by moo on 07/09/16.
 */
class BreachesAdapter(private val myActivity: Activity, aList: List<Breach>) : RecyclerView.Adapter<RecyclerViewHolder>() {
    private val LOGTAG = javaClass.simpleName
    private val context: Context
    private val myBreachDao: BreachDao
    private val myAccountDao: AccountDao
    private var myBreachList: List<Breach>
    private var myParentView: ViewGroup? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        myParentView = parent
        val itemLayout = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_breach, parent, false) as CardView
        return RecyclerViewHolder(itemLayout)
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        val breach = myBreachList[position]
        val cardView = holder.view as CardView
        val breachId = breach.id
        val title = cardView.findViewById<TextView>(R.id.title)
        title.text = breach.title
        val domain = cardView.findViewById<TextView>(R.id.domain)
        domain.text = breach.domain
        val dtfOut = DateTimeFormat.forPattern("yyyy/MM/dd")
        val breachDate = cardView.findViewById<TextView>(R.id.breach_date)
        breachDate.text = dtfOut.print(breach.breachDate)
        val compromisedData = cardView.findViewById<TextView>(R.id.compromised_data)
        compromisedData.text = breach.dataClasses
        val description = cardView.findViewById<TextView>(R.id.description)
        description.text = Html.fromHtml(breach.description).toString()
        val statusIndicator = cardView.findViewById<View>(R.id.status_indicator)
        val acknowledge = cardView.findViewById<Button>(R.id.acknowledge)
        if (!breach.acknowledged) {
            statusIndicator.setBackgroundColor(context.resources.getColor(R.color.account_status_breached))
            acknowledge.visibility = View.VISIBLE
            acknowledge.setOnClickListener { v: View? -> handleAcknowledgeClicked(breachId) }
        } else {
            statusIndicator.setBackgroundColor(context.resources.getColor(R.color.account_status_only_acknowledged))
            acknowledge.visibility = View.GONE
        }
        val additionalFlagsLabel = cardView.findViewById<View>(R.id.label_additional_flags)
        val additionalFlags = cardView.findViewById<TextView>(R.id.additional_flags)
        if (breach.hasAdditionalFlags()) {
            additionalFlagsLabel.visibility = View.VISIBLE
            additionalFlags.visibility = View.VISIBLE
            additionalFlags.text = getFlags(breach)
        } else {
            additionalFlagsLabel.visibility = View.GONE
            additionalFlags.visibility = View.GONE
        }
        val logoView = cardView.findViewById<ImageView>(R.id.logo)
        if (breach.logoPath != null && breach.logoPath !== "") {
            logoView.visibility = View.VISIBLE
            Picasso.get().load(breach.logoPath).into(logoView)
        } else {
            logoView.visibility = View.GONE
        }
    }

    private fun getFlags(breach: Breach): String {
        val flags = StringBuilder()
        if (!breach.verified) {
            flags.append(context.getString(R.string.unverified)).append(" ")
        }
        if (breach.fabricated) {
            flags.append(context.getString(R.string.fabricated)).append(" ")
        }
        if (breach.retired) {
            flags.append(context.getString(R.string.retired)).append(" ")
        }
        if (breach.sensitive) {
            flags.append(context.getString(R.string.sensitive)).append(" ")
        }
        if (breach.spamList) {
            flags.append(context.getString(R.string.spam_list)).append(" ")
        }
        return flags.toString()
    }

    private fun handleAcknowledgeClicked(breachId: Long) {
        CoroutineScope(Job()).launch {
            val breach = myBreachDao.findById(breachId)
            if (breach != null) {
                setBreachAcknowledged(breach)
                updateAccountIsHacked(breach.account)
                withContext(Dispatchers.Main) {
                    notifyDataSetChanged()
                    Snackbar.make(myParentView!!, context.getString(R.string.breach_acknowledged), Snackbar.LENGTH_SHORT).show()
                    RatingHelper(myActivity).setRatingCounterBelowthreshold()
                }
            } else {
                Log.w(LOGTAG, "no breach found with id $breachId")
            }
        }
    }

    private fun setBreachAcknowledged(breach: Breach) {
        breach.acknowledged = true
        myBreachDao.update(breach)
        Log.d(LOGTAG, "breach updated - acknowledge = true")
        trackCustomEvent(CustomEvent.BREACH_ACKNOWLEDGED)
    }

    override fun getItemCount(): Int {
        return myBreachList.size
    }

    private fun updateAccountIsHacked(accountId: Long) {
        val accounts = myAccountDao.findById(accountId)
        for (account in accounts) {
            if (!account.hacked) {
                return
            }
            AccountHelper(context).updateBreachCounts(account)
            if (myBreachDao.countUnacknowledged(accountId) == 0L) {
                Log.d(LOGTAG, "account has only acknowledged breaches")
                account.hacked = false
            }
            myAccountDao.update(account)
            Log.d(LOGTAG, "account updated - hacked = " + account.hacked + " numBreaches = " + account.numBreaches + " numAcknowledgedBreaches = " + account.numAcknowledgedBreaches)
        }
    }

    fun addItems(list: List<Breach>) {
        myBreachList = list
        notifyDataSetChanged()
    }

    init {
        context = myActivity.applicationContext
        myBreachList = aList
        myBreachDao = AppDatabase.get(context).breachDao
        myAccountDao = AppDatabase.get(context).accountDao
    }
}