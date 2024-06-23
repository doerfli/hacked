package li.doerf.hacked.ui.adapters

import android.content.Context
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import io.reactivex.processors.PublishProcessor
import li.doerf.hacked.HackedApplication
import li.doerf.hacked.R
import li.doerf.hacked.db.entities.BreachedSite
import li.doerf.hacked.util.NavEvent
import org.joda.time.format.DateTimeFormat


class BreachedSitesAdapter(
        val context: Context, private var myBreachedSites: List<BreachedSite>, private val compactView: Boolean) : RecyclerView.Adapter<RecyclerViewHolder>() {

    private lateinit var navEvents: PublishProcessor<NavEvent>

    override fun onViewAttachedToWindow(holder: RecyclerViewHolder) {
        super.onViewAttachedToWindow(holder)
        navEvents = (context as HackedApplication).navEvents
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        if (compactView) {
            val itemLayout = LayoutInflater.from(parent.context)
                    .inflate(R.layout.card_breached_site_compact, parent, false)
            return RecyclerViewHolder(itemLayout)
        }

        val itemLayout = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_breached_site, parent, false)
        return RecyclerViewHolder(itemLayout)
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        val site = myBreachedSites[position]
        val view = holder.view

        if (compactView) {
            bindCompactView(view, site)
        } else {
            bindFullView(view, site)
        }

    }

    private fun bindFullView(siteCard: View, site: BreachedSite) {
        val nameView = siteCard.findViewById<TextView>(R.id.site_name)
        nameView.text = site.title

        val pwnCountView = siteCard.findViewById<TextView>(R.id.pwn_count)
        pwnCountView.text = String.format(context.resources.configuration.locale, "(%,d)", site.pwnCount)

        val details = siteCard.findViewById<RelativeLayout>(R.id.breach_details)
        val arrowDown = siteCard.findViewById<ImageView>(R.id.arrow_down)
        val arrowUp = siteCard.findViewById<ImageView>(R.id.arrow_up)

        if (!site.detailsVisible) {
            siteCard.setBackgroundColor(ContextCompat.getColor(context, R.color.detailsBackground))
            details.visibility = View.GONE
            arrowDown.visibility = View.VISIBLE
            arrowUp.visibility = View.GONE
        } else {
            siteCard.setBackgroundColor(ContextCompat.getColor(context, R.color.selectedCard))
            details.visibility = View.VISIBLE
            arrowDown.visibility = View.GONE
            arrowUp.visibility = View.VISIBLE
            val domain = siteCard.findViewById<TextView>(R.id.domain)
            domain.text = site.domain
            val dtfOut = DateTimeFormat.forPattern("yyyy/MM/dd")
            val breachDate = siteCard.findViewById<TextView>(R.id.breach_date)
            breachDate.text = dtfOut.print(site.breachDate)
            val compromisedData = siteCard.findViewById<TextView>(R.id.compromised_data)
            compromisedData.text = site.dataClasses
            val description = siteCard.findViewById<TextView>(R.id.description)
            description.text = Html.fromHtml(site.description).toString()

            val logoView = siteCard.findViewById<ImageView>(R.id.logo)
            if (site.logoPath != null && site.logoPath.isNotEmpty()) {
                logoView.visibility = View.VISIBLE
                Picasso.get().load(site.logoPath).into(logoView)
            } else {
                logoView.visibility = View.GONE
            }
        }

        val additionalFlagsLabel: View = siteCard.findViewById<View>(R.id.label_additional_flags)
        val additionalFlags: TextView = siteCard.findViewById<TextView>(R.id.additional_flags)

        if (site.hasAdditionalFlags()) {
            additionalFlagsLabel.visibility = View.VISIBLE
            additionalFlags.visibility = View.VISIBLE
            additionalFlags.setText(getFlags(site))
        } else {
            additionalFlagsLabel.visibility = View.GONE
            additionalFlags.visibility = View.GONE
        }

        siteCard.setOnClickListener {
            site.detailsVisible = ! site.detailsVisible
            notifyDataSetChanged()
        }
    }

    private fun bindCompactView(card: View, site: BreachedSite) {
        val nameView = card.findViewById<TextView>(R.id.site_name)
        nameView.text = site.title

        getFlags(site)

        val pwnCountView = card.findViewById<TextView>(R.id.pwn_count)
        pwnCountView.text = String.format(context.resources.configuration.locale, "(%,d)", site.pwnCount)

        card.setOnClickListener { _ ->
            navEvents.onNext(NavEvent(NavEvent.Destination.ALL_BREACHES, site.id, null))
        }
    }

    private fun getFlags(site: BreachedSite): String? {
        val flags = StringBuilder()
        if (!site.verified) {
            Log.d("BreachedSitesAdapter", "unverified: " + site.name)
            flags.append(context.getString(R.string.unverified)).append(" ")
        }
        if (site.fabricated) {
            Log.d("BreachedSitesAdapter", "fabricated: " + site.name)
            flags.append(context.getString(R.string.fabricated)).append(" ")
        }
        if (site.retired) {
            Log.d("BreachedSitesAdapter", "retired: " + site.name)
            flags.append(context.getString(R.string.retired)).append(" ")
        }
        if (site.sensitive) {
            Log.d("BreachedSitesAdapter", "sensitive: " + site.name)
            flags.append(context.getString(R.string.sensitive)).append(" ")
        }
        if (site.spamList) {
            Log.d("BreachedSitesAdapter", "spam_list: " + site.name)
            flags.append(context.getString(R.string.spam_list)).append(" ")
        }
        return flags.toString()
    }

    override fun getItemCount(): Int {
        return myBreachedSites.size
    }

    fun addItems(list: List<BreachedSite>) {
        myBreachedSites = list
        notifyDataSetChanged()
    }

}