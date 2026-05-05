package pro.xservis.client.ui.nav

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pro.xservis.client.R
import pro.xservis.client.ui.screens.account.AccountScreen
import pro.xservis.client.ui.screens.home.HomeScreen
import pro.xservis.client.ui.screens.payments.PaymentsScreen
import pro.xservis.client.ui.screens.servers.ServersScreen
import pro.xservis.client.ui.theme.BgBlack
import pro.xservis.client.ui.theme.Teal
import pro.xservis.client.ui.theme.TextSecondary

private sealed class Tab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    data object Home : Tab("home", R.string.tab_home, Icons.Outlined.Shield)
    data object Servers : Tab("servers", R.string.tab_servers, Icons.Outlined.Public)
    data object Payments : Tab("payments", R.string.tab_payments, Icons.Outlined.CreditCard)
    data object Account : Tab("account", R.string.tab_account, Icons.Outlined.AccountCircle)
}

private val tabs = listOf(Tab.Home, Tab.Servers, Tab.Payments, Tab.Account)

@Composable
fun RootNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = BgBlack, tonalElevation = 0.dp) {
                tabs.forEach { tab ->
                    val selected = backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Teal,
                            selectedTextColor = Teal,
                            indicatorColor = BgBlack,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                        ),
                    )
                }
            }
        },
    ) { padding: PaddingValues ->
        NavHost(
            navController = navController,
            startDestination = Tab.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.Home.route) { HomeScreen() }
            composable(Tab.Servers.route) { ServersScreen() }
            composable(Tab.Payments.route) { PaymentsScreen() }
            composable(Tab.Account.route) { AccountScreen() }
        }
    }
}
