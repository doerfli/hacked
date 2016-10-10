package li.doerf.hacked.ui;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;

import li.doerf.hacked.R;

/**
 * Created by moo on 10/10/16.
 */
public class ColoredSwipeRefreshLayout extends SwipeRefreshLayout {
    private final String LOGTAG = getClass().getSimpleName();

    public ColoredSwipeRefreshLayout(Context context) {
        super(context);
        setMyColorScheme();
    }

    public ColoredSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMyColorScheme();
    }

    private void setMyColorScheme() {
        setColorSchemeResources(
                R.color.refresh_progress_1,
                R.color.refresh_progress_2,
                R.color.refresh_progress_3,
                R.color.refresh_progress_4);
    }
}
