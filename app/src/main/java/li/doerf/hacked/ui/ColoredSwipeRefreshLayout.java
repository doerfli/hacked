package li.doerf.hacked.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import li.doerf.hacked.R;

/**
 * SwipeRefreshLayout with custom coloring.
 */
public class ColoredSwipeRefreshLayout extends SwipeRefreshLayout {

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
