package com.democ.civilsunrisealarm.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.democ.civilsunrisealarm.R
import com.democ.civilsunrisealarm.ui.settings.SettingsScreen
import com.democ.civilsunrisealarm.ui.status.StatusScreen

sealed class Screen(val route: String, val titleResId: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Settings : Screen("settings", R.string.settings_title, Icons.Default.Settings)
    object Status : Screen("status", R.string.status_title, Icons.Default.Info)
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Settings.route
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                val screens = listOf(Screen.Status, Screen.Settings)
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(stringResource(screen.titleResId)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(Screen.Status.route) {
                StatusScreen()
            }
        }
    }
}

