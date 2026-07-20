package com.ownscreen.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.ownscreen.app.ui.appdetail.AppDetailScreen
import com.ownscreen.app.ui.applist.AppListScreen
import com.ownscreen.app.ui.dashboard.DashboardScreen
import com.ownscreen.app.ui.history.HistoryDayScreen
import com.ownscreen.app.ui.history.HistoryScreen
import com.ownscreen.app.ui.settings.SettingsScreen

private object Routes {
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val APP_DETAIL = "app_detail/{packageName}?minutes={minutes}"
    const val APP_LIST = "app_list"
    const val HISTORY = "history"
    const val HISTORY_DAY = "history_day/{epochDay}"

    // initialMinutes lets a caller that already knows today's usage (e.g. Dashboard, which just
    // fetched it) pass it straight through instead of AppDetailScreen showing "0m" while it
    // re-queries UsageStatsManager from scratch. -1 means "unknown" (e.g. from AppListScreen).
    fun appDetail(packageName: String, initialMinutes: Int = -1) = "app_detail/$packageName?minutes=$initialMinutes"
    fun historyDay(epochDay: Long) = "history_day/$epochDay"
}

// Navigation-compose defaults every destination to EnterTransition.None/ExitTransition.None
// when nothing is set here, which (a) reads as an abrupt cut between screens and (b) is a known
// source of the outgoing screen's first post-pop tap landing on the wrong composable frame.
// Setting real transitions once at the NavHost level (applies to every composable() in the
// graph) fixes both.
private const val NAV_TRANSITION_MILLIS = 300

@Composable
fun OwnScreenNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(NAV_TRANSITION_MILLIS)) +
                fadeIn(tween(NAV_TRANSITION_MILLIS))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(NAV_TRANSITION_MILLIS)) +
                fadeOut(tween(NAV_TRANSITION_MILLIS))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(NAV_TRANSITION_MILLIS)) +
                fadeIn(tween(NAV_TRANSITION_MILLIS))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(NAV_TRANSITION_MILLIS)) +
                fadeOut(tween(NAV_TRANSITION_MILLIS))
        }
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenAppDetail = { pkg, minutes -> navController.navigate(Routes.appDetail(pkg, minutes)) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAppList = { navController.navigate(Routes.APP_LIST) }
            )
        }
        composable(
            Routes.APP_DETAIL,
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType },
                navArgument("minutes") { type = NavType.IntType; defaultValue = -1 }
            )
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName").orEmpty()
            val initialMinutes = backStackEntry.arguments?.getInt("minutes") ?: -1
            AppDetailScreen(
                packageName = packageName,
                initialMinutes = initialMinutes,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.APP_LIST) {
            AppListScreen(
                onBack = { navController.popBackStack() },
                onOpenAppDetail = { pkg -> navController.navigate(Routes.appDetail(pkg)) }
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenDay = { day -> navController.navigate(Routes.historyDay(day)) }
            )
        }
        composable(
            Routes.HISTORY_DAY,
            arguments = listOf(navArgument("epochDay") { type = NavType.LongType })
        ) { backStackEntry ->
            val epochDay = backStackEntry.arguments?.getLong("epochDay") ?: 0L
            HistoryDayScreen(
                epochDay = epochDay,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
