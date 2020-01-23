package li.doerf.hacked.ui.adapters

import android.content.Context
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import li.doerf.hacked.R
import li.doerf.hacked.db.entities.BreachedSite
import org.joda.time.format.DateTimeFormat

class BreachedSitesAdapter(
        val context: Context, private var myBreachedSites: List<BreachedSite>, private val compactView: Boolean) : RecyclerView.Adapter<RecyclerViewHolder>() {

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

    private fun bindFullView(view: View, site: BreachedSite) {
        val nameView = view.findViewById<TextView>(R.id.site_name)
        nameView.text = site.name
        //        val unconfirmed = cardView.findViewById<TextView>(R.id.unconfirmed)
        if (site.verified) {
            nameView.setTextColor(context.resources.getColor(android.R.color.primary_text_light))
            //            unconfirmed.visibility = View.GONE
        } else {
            nameView.setTextColor(context.resources.getColor(R.color.account_status_unknown))
            //            unconfirmed.visibility = View.VISIBLE
        }
        val pwnCountView = view.findViewById<TextView>(R.id.pwn_count)
        pwnCountView.text = String.format(context.resources.configuration.locale, "(%,d)", site.pwnCount)

        val details = view.findViewById<RelativeLayout>(R.id.breach_details)
        val arrowDown = view.findViewById<ImageView>(R.id.arrow_down)
        val arrowUp = view.findViewById<ImageView>(R.id.arrow_up)
        view.setOnClickListener { view: View ->
            if (details.visibility == View.VISIBLE) {
                details.visibility = View.GONE
                arrowDown.visibility = View.VISIBLE
                arrowUp.visibility = View.GONE
            } else {
                details.visibility = View.VISIBLE
                arrowDown.visibility = View.GONE
                arrowUp.visibility = View.VISIBLE
                val domain = view.findViewById<TextView>(R.id.domain)
                domain.text = site.domain
                val dtfOut = DateTimeFormat.forPattern("yyyy/MM/dd")
                val breachDate = view.findViewById<TextView>(R.id.breach_date)
                breachDate.text = dtfOut.print(site.breachDate)
                val compromisedData = view.findViewById<TextView>(R.id.compromised_data)
                compromisedData.text = site.dataClasses
                val description = view.findViewById<TextView>(R.id.description)
                description.text = Html.fromHtml(site.description).toString()
            }
        }
}

    private fun bindCompactView(view: View, site: BreachedSite) {
        val nameView = view.findViewById<TextView>(R.id.site_name)
        nameView.text = site.name
        //        val unconfirmed = cardView.findViewById<TextView>(R.id.unconfirmed)
        if (site.verified) {
            nameView.setTextColor(context.resources.getColor(android.R.color.primary_text_light))
            //            unconfirmed.visibility = View.GONE
        } else {
            nameView.setTextColor(context.resources.getColor(R.color.account_status_unknown))
            //            unconfirmed.visibility = View.VISIBLE
        }
        val pwnCountView = view.findViewById<TextView>(R.id.pwn_count)
        pwnCountView.text = String.format(context.resources.configuration.locale, "(%,d)", site.pwnCount)

        // TODO clicklistener for chevron
    }

    override fun getItemCount(): Int {
        return myBreachedSites.size
    }

    fun addItems(list: List<BreachedSite>) {
        myBreachedSites = list
        notifyDataSetChanged()
    }

}