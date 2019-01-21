package li.doerf.hacked;


import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import li.doerf.hacked.activities.MainActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class SmokeTest {

    static final String ACCOUNT_NAME = "aaa";
    @Rule
    public ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule<>(MainActivity.class);

    @Test
    public void addAccountAaaWithLeaks() {
        removeAccountIfExists();

        onView(withId(R.id.action_add_account)).perform(click());
        onView(withId(R.id.account)).perform(typeText(ACCOUNT_NAME));
        onView(withText("Add")).perform(click());
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        onView(withId(R.id.last_checked)).check(matches(not(withText("yyyy/mm/dd hh:mm"))));
        onView(withId(R.id.last_checked)).check(matches(not(withText("-"))));
        onView(withId(R.id.breach_state)).check(matches(withText(containsString("breaches"))));
    }

    private void removeAccountIfExists() {
        try {
            onView(withText(ACCOUNT_NAME)).check(matches(isDisplayed()));
            onView(withText(ACCOUNT_NAME)).perform(longClick());
            onView(withText("OK")).perform(click());
        } catch (NoMatchingViewException e) {
            //view not displayed - nothing to do
        }
    }

}
