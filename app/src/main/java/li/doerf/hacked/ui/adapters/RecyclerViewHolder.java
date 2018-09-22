package li.doerf.hacked.ui.adapters;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by moo on 31/01/15.
 */
class RecyclerViewHolder extends RecyclerView.ViewHolder {

    private final View myView;

    RecyclerViewHolder(View itemView) {
        super(itemView);
        myView = itemView;
    }

    View getView()
    {
        return myView;
    }
}
