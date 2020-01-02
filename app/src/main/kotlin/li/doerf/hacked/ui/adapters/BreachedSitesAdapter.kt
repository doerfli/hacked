package li.doerf.hacked.ui.adapters

import android.content.Context
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import li.doerf.hacked.R
import li.doerf.hacked.db.entities.BreachedSite
import org.joda.time.format.DateTimeFormat

class BreachedSitesAdapter(//    private final String LOGTAG = getClass().getSimpleName();
        val context: Context, private var myBreachedSites: List<BreachedSite>) : RecyclerView.Adapter<RecyclerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val itemLayout = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_breached_site, parent, false) as CardView
        return RecyclerViewHolder(itemLayout)
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        val site = myBreachedSites[position]
        val cardView = holder.view as CardView
        val nameView = cardView.findViewById<TextView>(R.id.site_name)
        nameView.text = site.name
        val unconfirmed = cardView.findViewById<TextView>(R.id.unconfirmed)
        if (site.verified) {
            nameView.setTextColor(context.resources.getColor(android.R.color.primary_text_light))
            unconfirmed.visibility = View.GONE
        } else {
            nameView.setTextColor(context.resources.getColor(R.color.account_status_unknown))
            unconfirmed.visibility = View.VISIBLE
        }
        val pwnCountView = cardView.findViewById<TextView>(R.id.pwn_count)
        pwnCountView.text = String.format(context.resources.configuration.locale, "%,d %s", site.pwnCount, context.getString(R.string.accounts))
        val details = cardView.findViewById<RelativeLayout>(R.id.breach_details)
        val arrowDown = cardView.findViewById<ImageView>(R.id.arrow_down)
        val arrowUp = cardView.findViewById<ImageView>(R.id.arrow_up)
        cardView.setOnClickListener { view: View? ->
            if (details.visibility == View.VISIBLE) {
                details.visibility = View.GONE
                arrowDown.visibility = View.VISIBLE
                arrowUp.visibility = View.GONE
            } else {
                details.visibility = View.VISIBLE
                arrowDown.visibility = View.GONE
                arrowUp.visibility = View.VISIBLE
                val domain = cardView.findViewById<TextView>(R.id.domain)
                domain.text = site.domain
                val dtfOut = DateTimeFormat.forPattern("yyyy/MM/dd")
                val breachDate = cardView.findViewById<TextView>(R.id.breach_date)
                breachDate.text = dtfOut.print(site.breachDate)
                val compromisedData = cardView.findViewById<TextView>(R.id.compromised_data)
                compromisedData.text = site.dataClasses
                val description = cardView.findViewById<TextView>(R.id.description)
                description.text = Html.fromHtml(site.description).toString()
            }
        }
    }

    override fun getItemCount(): Int {
        return myBreachedSites.size
    }

    fun addItems(list: List<BreachedSite>) {
        myBreachedSites = list
        notifyDataSetChanged()
    }

}