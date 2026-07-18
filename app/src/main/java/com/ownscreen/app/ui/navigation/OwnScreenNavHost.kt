package com.ownscreen.app.ui.navigation

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
    const val APP_DETAIL = "app_detail/{packageName}"
    const val APP_LIST = "app_list"
    const val HISTORY = "history"
    const val HISTORY_DAY = "history_day/{epochDay}"

    fun appDetail(packageName: String) = "app_detail/$packageName"
    fun historyDay(epochDay: Long) = "history_day/$epochDay"
}

@Composable
fun OwnScreenNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenAppDetail = { pkg -> navController.navigate(Routes.appDetail(pkg)) },
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
            arguments = listOf(navArgument("packageName") { type = NavType.StringType })
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName").orEmpty()
            AppDetailScreen(
                packageName = packageName,
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
