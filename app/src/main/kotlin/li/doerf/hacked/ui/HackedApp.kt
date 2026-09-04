package li.doerf.hacked.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import li.doerf.hacked.R
import li.doerf.hacked.ui.screens.accounts.AccountDetailScreen
import li.doerf.hacked.ui.screens.accounts.AccountsScreen
import li.doerf.hacked.ui.screens.firstuse.FirstUseScreen
import li.doerf.hacked.ui.screens.leaks.LeaksScreen
import li.doerf.hacked.ui.screens.passwords.PasswordScreen

private data class BottomTab(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.ACCOUNTS, R.string.title_accounts, Icons.Filled.Person),
    BottomTab(Routes.PASSWORDS, R.string.title_pwned_passwords, Icons.Filled.Lock),
    BottomTab(Routes.LEAKS, R.string.title_leaks, Icons.Filled.Shield),
)

@Composable
fun HackedApp(startWithFirstUse: Boolean) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = bottomTabs.any { it.route == currentRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Each screen owns its own Scaffold + TopAppBar, which already consumes the
        // status bar inset. Without this, that inset gets reserved twice (once here,
        // once in the child), showing up as a big blank gap above every screen's title.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (startWithFirstUse) Routes.FIRST_USE else Routes.ACCOUNTS,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.FIRST_USE) {
                FirstUseScreen(onFinished = {
                    navController.navigate(Routes.ACCOUNTS) {
                        popUpTo(Routes.FIRST_USE) { inclusive = true }
                    }
                })
            }
            composable(Routes.ACCOUNTS) {
                AccountsScreen(onAccountClick = { id -> navController.navigate(Routes.accountDetail(id)) })
            }
            composable(
                route = Routes.ACCOUNT_DETAIL,
                arguments = listOf(navArgument(Routes.ACCOUNT_DETAIL_ARG) { type = NavType.LongType })
            ) { entry ->
                val accountId = entry.arguments!!.getLong(Routes.ACCOUNT_DETAIL_ARG)
                AccountDetailScreen(accountId = accountId, onBack = { navController.popBackStack() })
            }
            composable(Routes.PASSWORDS) { PasswordScreen() }
            composable(Routes.LEAKS) { LeaksScreen() }
        }
    }
}
