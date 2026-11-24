package com.panamatrip.app.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navOptions
import com.panamatrip.app.R
import com.panamatrip.app.core.designsystem.icons.PanamaIcons
import com.panamatrip.app.core.designsystem.haptic.rememberPanamaHaptics
import com.panamatrip.app.core.designsystem.theme.PanamaMotion
import com.panamatrip.app.core.ui.LocalSnackbarHostState
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.features.destinationdetail.ui.DestinationDetailRoute
import com.panamatrip.app.features.explore.ui.ExploreRoute
import com.panamatrip.app.features.guides.ui.GuidesRoute
import com.panamatrip.app.features.postcomposer.ui.PostComposerRoute
import com.panamatrip.app.features.routeplan.ui.RoutePlanRoute
import com.panamatrip.app.features.saved.ui.SavedRoute
import com.panamatrip.app.features.trackrecorder.ui.TrackRecorderRoute

object Routes {
    const val EXPLORE = "explore"
    const val SAVED = "saved"
    const val GUIDES = "guides"
    const val ROUTE = "route?destinationId={destinationId}"
    const val ROUTE_BASE = "route"
    const val DESTINATION_DETAIL = "destination/{destinationId}"
    const val TRACK_RECORDER = "recorder?destinationId={destinationId}"
    const val TRACK_RECORDER_BASE = "recorder"
    const val POST_COMPOSER = "composer"

    const val ARG_DESTINATION_ID = "destinationId"

    fun destination(destinationId: String): String = "destination/$destinationId"

    fun route(destinationId: String): String = "$ROUTE_BASE?$ARG_DESTINATION_ID=$destinationId"

    fun trackRecorder(destinationId: String? = null): String = destinationId
        ?.let { "$TRACK_RECORDER_BASE?$ARG_DESTINATION_ID=$it" }
        ?: TRACK_RECORDER_BASE
}

private data class BottomTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

private val bottomTabs = listOf(
    BottomTab(Routes.EXPLORE, R.string.nav_explore, PanamaIcons.Compass, PanamaIcons.Compass),
    BottomTab(Routes.ROUTE_BASE, R.string.nav_route, PanamaIcons.Route, PanamaIcons.Route),
    BottomTab(
        Routes.SAVED,
        R.string.nav_saved,
        PanamaIcons.BookmarkOutline,
        PanamaIcons.Bookmark,
    ),
    BottomTab(
        Routes.GUIDES,
        R.string.nav_guides,
        PanamaIcons.Verified,
        PanamaIcons.Verified,
    ),
)

@Composable
fun PanamaTripApp(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = currentRoute != null && isTopLevelRoute(currentRoute)
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (isTopLevel) {
                PanamaBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route -> navController.navigateToTab(route) },
                )
            }
        },
    ) { innerPadding ->
        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
            NavHost(
                navController = navController,
                startDestination = Routes.EXPLORE,
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    slideInHorizontally(
                        animationSpec = PanamaMotion.spatial<IntOffset>(),
                        initialOffsetX = { it / 10 },
                    ) + fadeIn(animationSpec = tween(FADE_MILLIS))
                },
                exitTransition = { fadeOut(animationSpec = tween(FADE_EXIT_MILLIS)) },
                popEnterTransition = { fadeIn(animationSpec = tween(FADE_MILLIS)) },
                popExitTransition = {
                    slideOutHorizontally(
                        animationSpec = PanamaMotion.spatial<IntOffset>(),
                        targetOffsetX = { it / 10 },
                    ) + fadeOut(animationSpec = tween(FADE_EXIT_MILLIS))
                },
            ) {
            composable(Routes.EXPLORE) {
                ExploreRoute(
                    container = container,
                    onOpenDestination = { id -> navController.navigate(Routes.destination(id)) },
                    onPlanRoute = { id -> navController.navigate(Routes.route(id)) },
                    onOpenSaved = { navController.navigateToTab(Routes.SAVED) },
                    onNewPost = { navController.navigate(Routes.POST_COMPOSER) },
                )
            }
            composable(
                route = Routes.DESTINATION_DETAIL,
                arguments = listOf(
                    navArgument(Routes.ARG_DESTINATION_ID) { type = NavType.StringType },
                ),
            ) {
                DestinationDetailRoute(
                    container = container,
                    onBack = { navController.popBackStack() },
                    onPlanRoute = { id -> navController.navigate(Routes.route(id)) },
                )
            }
            composable(
                route = Routes.ROUTE,
                arguments = listOf(
                    navArgument(Routes.ARG_DESTINATION_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                RoutePlanRoute(
                    container = container,
                    onExplore = { navController.navigateToTab(Routes.EXPLORE) },
                    onOpenRecorder = { destinationId ->
                        navController.navigate(Routes.trackRecorder(destinationId))
                    },
                )
            }
            composable(Routes.SAVED) {
                SavedRoute(
                    container = container,
                    onOpenDestination = { id -> navController.navigate(Routes.destination(id)) },
                    onExplore = { navController.navigateToTab(Routes.EXPLORE) },
                )
            }
            composable(Routes.GUIDES) {
                GuidesRoute(container = container)
            }
            composable(
                route = Routes.TRACK_RECORDER,
                arguments = listOf(
                    navArgument(Routes.ARG_DESTINATION_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                TrackRecorderRoute(
                    container = container,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.POST_COMPOSER) {
                PostComposerRoute(
                    container = container,
                    onBack = { navController.popBackStack() },
                    onPublished = { navController.popBackStack() },
                )
            }
            }
        }
    }
}

@Composable
private fun PanamaBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    val haptics = rememberPanamaHaptics()
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        bottomTabs.forEach { tab ->
            val selected = when (tab.route) {
                Routes.ROUTE_BASE -> currentRoute?.startsWith(Routes.ROUTE_BASE) == true
                else -> currentRoute == tab.route
            }
            NavigationBarItem(
                selected = selected,
                onClick = {
                    haptics.segment()
                    onNavigate(tab.route)
                },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(text = stringResource(tab.labelRes)) },
            )
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(
        route = route,
        navOptions = navOptions {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        },
    )
}

private fun isTopLevelRoute(route: String): Boolean =
    route == Routes.EXPLORE ||
        route == Routes.SAVED ||
        route == Routes.GUIDES ||
        route.startsWith(Routes.ROUTE_BASE)

private const val FADE_MILLIS = 180
private const val FADE_EXIT_MILLIS = 120
