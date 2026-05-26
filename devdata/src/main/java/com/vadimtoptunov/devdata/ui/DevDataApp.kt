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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vadimtoptunov.devdata.ui.screens.IdentityDetailScreen
import com.vadimtoptunov.devdata.ui.screens.IdentityListScreen
import com.vadimtoptunov.devdata.ui.screens.NewIdentityScreen
import com.vadimtoptunov.devdata.ui.screens.NfcSessionScreen
import com.vadimtoptunov.devdata.ui.screens.ResultsScreen
import com.vadimtoptunov.devdata.ui.screens.SuiteListScreen

// ── Navigation destinations ────────────────────────────────────────────────

sealed class Screen(val route: String, val label: String) {
    // Bottom-nav roots
    object NewIdentity : Screen("new",    "Generate")
    object Identities  : Screen("list",   "Identities")
    object Suites      : Screen("suites", "Suites")

    // Detail screens — not shown in bottom nav
    object IdentityDetail : Screen("detail/{identityId}", "Identity") {
        fun route(id: String) = "detail/$id"
    }
    object NfcSession : Screen("nfc/{identityId}?suiteId={suiteId}", "NFC session") {
        fun route(identityId: String, suiteId: String? = null) =
            if (suiteId != null) "nfc/$identityId?suiteId=$suiteId"
            else "nfc/$identityId?suiteId="
    }
    object Results : Screen("results/{suiteId}", "Results") {
        fun route(suiteId: String) = "results/$suiteId"
    }
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
    val navBackStack  by navController.currentBackStackEntryAsState()
    val currentDest   = navBackStack?.destination

    // Hide bottom bar when on detail/session/results screens
    val showBottomBar = bottomNavScreens.any { screen ->
        currentDest?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = when (screen) {
                                        Screen.NewIdentity -> Icons.Default.Add
                                        Screen.Identities  -> Icons.Default.List
                                        Screen.Suites      -> Icons.Default.Settings
                                        else               -> Icons.Default.List
                                    },
                                    contentDescription = screen.label
                                )
                            },
                            label    = { Text(screen.label) },
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
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.NewIdentity.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            // ── Bottom-nav screens ─────────────────────────────────────────
            composable(Screen.NewIdentity.route) {
                NewIdentityScreen()
            }
            composable(Screen.Identities.route) {
                IdentityListScreen(
                    onOpenDetail = { id ->
                        navController.navigate(Screen.IdentityDetail.route(id))
                    },
                    onOpenNfc = { id ->
                        navController.navigate(Screen.NfcSession.route(id))
                    }
                )
            }
            composable(Screen.Suites.route) {
                SuiteListScreen(
                    onOpenResults = { suiteId ->
                        navController.navigate(Screen.Results.route(suiteId))
                    }
                )
            }

            // ── Detail screens ─────────────────────────────────────────────
            composable(
                route     = Screen.IdentityDetail.route,
                arguments = listOf(navArgument("identityId") { type = NavType.StringType })
            ) { backStack ->
                val identityId = backStack.arguments?.getString("identityId") ?: return@composable
                IdentityDetailScreen(
                    identityId = identityId,
                    onBack     = { navController.popBackStack() }
                )
            }

            composable(
                route     = Screen.NfcSession.route,
                arguments = listOf(
                    navArgument("identityId") { type = NavType.StringType },
                    navArgument("suiteId") {
                        type         = NavType.StringType
                        nullable     = true
                        defaultValue = null
                    }
                )
            ) { backStack ->
                val identityId = backStack.arguments?.getString("identityId") ?: return@composable
                val suiteId    = backStack.arguments?.getString("suiteId")?.takeIf { it.isNotBlank() }
                NfcSessionScreen(
                    identityId = identityId,
                    suiteId    = suiteId,
                    onBack     = { navController.popBackStack() }
                )
            }

            composable(
                route     = Screen.Results.route,
                arguments = listOf(navArgument("suiteId") { type = NavType.StringType })
            ) { backStack ->
                val suiteId = backStack.arguments?.getString("suiteId") ?: return@composable
                ResultsScreen(
                    suiteId = suiteId,
                    onBack  = { navController.popBackStack() }
                )
            }
        }
    }
}
