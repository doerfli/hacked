package li.doerf.hacked.ui.adapters

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.accompanist.coil.rememberCoilPainter
import com.google.android.material.snackbar.Snackbar
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

        val statusColor =  if (!breach.acknowledged) {
            context.resources.getColor(R.color.account_status_breached)
        } else {
            context.resources.getColor(R.color.account_status_only_acknowledged)
        }

        val greeting = cardView.findViewById<ComposeView>(R.id.breach)
        greeting.setContent {
            MaterialTheme {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                    Spacer(modifier = Modifier
                        .width(10.dp)
                        .fillMaxHeight()
                        .background(color = Color(statusColor)))
                    BreachUi(breach)
                }
            }
        }
    }

    @Composable
    fun BreachUi(breach: Breach) {
        val dtfOut = DateTimeFormat.forPattern("yyyy/MM/dd")

        // TODO layout
        Box(Modifier.padding(8.dp)) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.End) {
                Image(
                    painter = rememberCoilPainter(
                        request = breach.logoPath
                    ),
                    contentDescription = "Logo of ${breach.title}",
                    modifier = Modifier
                        .width(48.dp)
                        .height(48.dp)
                )
            }
            Column() {
                Text(breach.title, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp))
                NameValue(context.getString(R.string.label_domain), breach.domain)
                NameValue(context.getString(R.string.label_breach_date), dtfOut.print(breach.breachDate))
                NameValue(context.getString(R.string.label_compromised_data), breach.dataClasses, true)
                if (breach.hasAdditionalFlags()) {
                    NameValue(context.getString(R.string.label_additional_flags), getFlags(breach))
                }
                Text(HtmlCompat.fromHtml(breach.description, HtmlCompat.FROM_HTML_MODE_COMPACT).toString())

                if (! breach.acknowledged) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.End) {
                        TextButton(onClick = { handleAcknowledgeClicked(breach.id) }) {
                            Text(
                                context.getString(R.string.acknowledge),
                                color = Color(context.resources.getColor(R.color.colorAccent))
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun NameValue(name: String, value: String, valueIsRed: Boolean = false) {
        Row() {
            Text(name, color = Color.Gray, modifier = Modifier.padding(end = 2.dp))
            if (valueIsRed) {
                Text(value, color = Color.Red)
            } else {
                Text(value)
            }
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

    fun handleAcknowledgeClicked(breachId: Long) {
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