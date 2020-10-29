package li.doerf.hacked.activities

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.processors.PublishProcessor
import li.doerf.hacked.HackedApplication
import li.doerf.hacked.R
import li.doerf.hacked.ui.fragments.AccountsFragmentDirections
import li.doerf.hacked.ui.fragments.FirstUseFragmentDirections
import li.doerf.hacked.ui.fragments.OverviewFragmentDirections
import li.doerf.hacked.util.NavEvent


class NavActivity : AppCompatActivity() {

    lateinit var navEvents: PublishProcessor<NavEvent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navEvents = (applicationContext as HackedApplication).navEvents
        setContentView(R.layout.activity_nav)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = getString(R.string.app_name)
        title = getString(R.string.app_name)
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        NavigationUI.setupActionBarWithNavController(this, navController)

        setupNavigation(navController)
    }

    @SuppressLint("CheckResult")
    private fun setupNavigation(navController: NavController) {
        navEvents.subscribe {
            when (it.destination) {
                NavEvent.Destination.OVERVIEW -> if(navController.currentDestination?.id == R.id.firstUseFragment) {
                    navController.navigate(FirstUseFragmentDirections.actionFirstUseFragmentToOverviewFragment())
                }
                NavEvent.Destination.FIRST_USE -> if(navController.currentDestination?.id == R.id.overviewFragment) {
                    navController.navigate(OverviewFragmentDirections.actionOverviewFragmentToFirstUseFragment())
                }
                NavEvent.Destination.ACCOUNTS_DETAILS -> when(navController.currentDestination?.id) {
                    R.id.overviewFragment -> {
                        val action = OverviewFragmentDirections.actionOverviewFragmentToAccountDetailsFragment()
                        action.accountId = it.id!!
                        navController.navigate(action)
                    }
                    R.id.accountsListFullFragment -> {
                        navController.navigate(AccountsFragmentDirections.actionAccountsListFullFragmentToAccountDetailsFragment(it.id!!))
                    }
                }
                NavEvent.Destination.ACCOUNTS_LIST -> if(navController.currentDestination?.id == R.id.overviewFragment) {
                    navController.navigate(OverviewFragmentDirections.actionOverviewFragmentToAccountsListFullFragment())
                }
                NavEvent.Destination.ALL_BREACHES -> if(navController.currentDestination?.id == R.id.overviewFragment) {
                    val action = OverviewFragmentDirections.actionOverviewFragmentToAllBreachesFragment()
                    if (it.id != null) {
                        action.breachedSiteId = it.id
                    }
                    navController.navigate(action)
                }
                NavEvent.Destination.PWNED_PASSWORDS -> if(navController.currentDestination?.id == R.id.overviewFragment) {
                    navController.navigate(OverviewFragmentDirections.actionOverviewFragmentToPwnedPasswordFragment(it.string!!))
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        return NavigationUI.navigateUp(navController, appBarConfiguration)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean { // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_nav, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_privacypolicy -> {
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://doerfli.github.io/hacked/privacy"))
                    startActivity(browserIntent)
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, "caught ActivityNotFoundException", e)
                    FirebaseCrashlytics.getInstance().recordException(e)
                    makeText(applicationContext, getString(R.string.unable_to_start_browser, "https://doerfli.github.io/hacked/privacy"), Toast.LENGTH_LONG).show()
                }
                true
            }
            R.id.action_visit_hibp -> {
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://haveibeenpwned.com"))
                    startActivity(browserIntent)
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, "caught ActivityNotFoundException", e)
                    FirebaseCrashlytics.getInstance().recordException(e)
                    makeText(applicationContext, getString(R.string.unable_to_start_browser, "https://haveibeenpwned.com"), Toast.LENGTH_LONG).show()
                }
                true
            }
            R.id.action_settings -> {
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivity(settingsIntent)
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    companion object {
        private val TAG = "NavActivity"
    }
}
