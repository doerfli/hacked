package li.doerf.hacked.ui.adapters;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

/**
 * Created by moo on 31/01/15.
 */
public abstract class RecyclerViewCursorAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    private final String LOGTAG = getClass().getSimpleName();
    private Cursor myCursor;
    private NotifyingDataSetObserver myDataSetObserver;
    private boolean mDataValid;
    private Context myContext;

    public RecyclerViewCursorAdapter(Context aContext, Cursor aCursor)
    {
        myContext = aContext;
        myCursor = aCursor;
        mDataValid = myCursor != null;
        myDataSetObserver = new NotifyingDataSetObserver();
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        if (myCursor == null)
        {
            Log.w(LOGTAG, "no cursor, cannnot bind. ");
            return;
        }

        myCursor.moveToPosition(position);
        onBindViewHolder(holder, myCursor);
    }

    public abstract void onBindViewHolder(VH holder, Cursor myCursor);

    @Override
    public int getItemCount() {
        if (myCursor == null)
        {
            return 0;
        }

        return myCursor.getCount();
    }

    @Override
    public long getItemId(int position) {
        if (mDataValid && myCursor != null && myCursor.moveToPosition(position)) {
            return myCursor.getLong(myCursor.getColumnIndex("_id"));
        }

        return 0;
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    /**
     * Change the underlying cursor to a new cursor. If there is an existing cursor it will be
     * closed.
     */
    public void changeCursor(Cursor cursor) {
        Cursor old = swapCursor(cursor);
        if (old != null) {
            old.close();
        }
    }

    /**
     * Swap in a new Cursor, returning the old Cursor.  Unlike
     * {@link #changeCursor(Cursor)}, the returned old Cursor is <em>not</em>
     * closed.
     */
    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == myCursor) {
            return null;
        }

        final Cursor oldCursor = myCursor;
        if (oldCursor != null && myDataSetObserver != null) {
            oldCursor.unregisterDataSetObserver(myDataSetObserver);
        }

        myCursor = newCursor;

        if (myCursor != null) {
            if (myDataSetObserver != null) {
                myCursor.registerDataSetObserver(myDataSetObserver);
            }
            mDataValid = true;
        } else {
            mDataValid = false;
        }
        notifyDataSetChanged();

        return oldCursor;
    }

    public Cursor getCursor()
    {
        return myCursor;
    }

    public Context getContext()
    {
        return myContext;
    }

    private class NotifyingDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            super.onChanged();
            mDataValid = true;
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            mDataValid = false;
            notifyDataSetChanged();
            //There is no notifyDataSetInvalidated() method in RecyclerView.Adapter
        }
    }

}
