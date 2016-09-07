package li.doerf.hacked.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;

import java.util.List;

import li.doerf.hacked.utils.Identifiable;

/**
 * Created by moo on 31/01/15.
 */
public abstract class RecyclerViewListAdapter<VH extends RecyclerView.ViewHolder, T extends Identifiable> extends RecyclerView.Adapter<VH> {
//    private final String LOGTAG = getClass().getSimpleName();
    private List<T> myList;
    private Context myContext;

    public RecyclerViewListAdapter(Context aContext, List<T> aList)
    {
        myContext = aContext;
        myList = aList;
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        onBindViewHolder(holder, myList.get(position));
    }

    public abstract void onBindViewHolder(VH holder, T anItem);

    @Override
    public int getItemCount() {
        return myList.size();
    }

    @Override
    public long getItemId(int position) {
        return myList.get(0).getId();
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    /**
     * Change the underlying cursor to a new cursor. If there is an existing cursor it will be
     * closed.
     */
    public void changeList(List aNewList) {
        myList = aNewList;
    }

    public List getList()
    {
        return myList;
    }

    public Context getContext()
    {
        return myContext;
    }

}
