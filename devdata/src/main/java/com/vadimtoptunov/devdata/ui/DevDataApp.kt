package com.vadimtoptunov.devdata.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vadimtoptunov.devdata.ui.screens.IdentityListScreen
import com.vadimtoptunov.devdata.ui.screens.NewIdentityScreen
import com.vadimtoptunov.devdata.ui.screens.SuiteListScreen

// ── Navigation destinations ────────────────────────────────────────────────

sealed class Screen(val route: String, val label: String) {
    object NewIdentity  : Screen("new",    "Generate")
    object Identities   : Screen("list",   "Identities")
    object Suites       : Screen("suites", "Suites")
}

private val bottomNavScreens = listOf(
    Screen.NewIdentity,
    Screen.Identities,
    Screen.Suites
)

// ── App root ───────────────────────────────────────────────────────────────

@Composable
fun DevDataApp() {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentDest  = navBackStack?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavScreens.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = when (screen) {
                                    Screen.NewIdentity -> Icons.Default.Add
                                    Screen.Identities  -> Icons.Default.List
                                    Screen.Suites      -> Icons.Default.Settings
                                },
                                contentDescription = screen.label
                            )
                        },
                        label   = { Text(screen.label) },
                        selected = currentDest?.hierarchy?.any { it.route == screen.route } == true,
                        onClick  = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.NewIdentity.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.NewIdentity.route) { NewIdentityScreen() }
            composable(Screen.Identities.route)  { IdentityListScreen() }
            composable(Screen.Suites.route)      { SuiteListScreen() }
        }
    }
}
