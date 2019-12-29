package li.doerf.hacked.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_overview, container, false)
    }

}
