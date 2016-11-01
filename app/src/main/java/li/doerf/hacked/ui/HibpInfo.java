package li.doerf.hacked.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import li.doerf.hacked.R;

/**
 * Created by moo on 01/11/16.
 */
public class HibpInfo {

    public static void prepare(final Context aContext, final TextView hibpInfo, RecyclerView aRecyclerView) {
        String text = aContext.getString(R.string.data_provided_by) + " <a href=\"https://haveibeenpwned.com\">Have i been pwned?</a>";
        hibpInfo.setMovementMethod(LinkMovementMethod.getInstance());
        hibpInfo.setText(Html.fromHtml(text));

        if ( aRecyclerView != null ) {
            aRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    // show animation only once
                    if (hibpInfo.getVisibility() == View.GONE) {
                        return;
                    }

                    // animation running ... ignore
                    if (hibpInfo.getAnimation() != null) {
                        return;
                    }

                    // animate hipb info out of page
                    Animation a = AnimationUtils.loadAnimation(aContext, R.anim.slide_out);
                    a.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            hibpInfo.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    hibpInfo.startAnimation(a);
                }
            });
        }
    }

}
