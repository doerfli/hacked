package li.doerf.hacked.ui.fragments

import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import li.doerf.hacked.R

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [OverviewFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [OverviewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OverviewFragment : Fragment() {

    private lateinit var hibpInfo: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_overview, container, false)

        hibpInfo = rootView.findViewById<TextView>(R.id.hibp_info)
        hibpInfo.movementMethod = LinkMovementMethod.getInstance()
        hibpInfo.text = Html.fromHtml("${getString(R.string.data_provided_by)} <a href=\"https://haveibeenpwned.com\">Have i been pwned?</a>")

        return rootView
    }

    override fun onResume() {
        super.onResume()

        hibpInfo.visibility = View.VISIBLE

        GlobalScope.launch {
            delay(5000)
            withContext(Dispatchers.Main) {
                hibpInfo.visibility = View.GONE
            }
        }
    }

}
