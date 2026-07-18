package com.ownscreen.app.ui.dashboard

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import com.ownscreen.app.data.pm.InstalledAppsRepository
import com.ownscreen.app.data.pm.LaunchableApp
import com.ownscreen.app.data.repository.AppSuspendStateRepository
import com.ownscreen.app.data.usage.UsageStatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppUsageUiState(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val usedMinutes: Int,
    val isSuspended: Boolean
)

data class DashboardUiState(
    val totalMinutes: Int = 0,
    val apps: List<AppUsageUiState> = emptyList(),
    val isLoading: Boolean = true
)

class DashboardViewModel(
    private val usageStatsRepository: UsageStatsRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val suspendStateRepository: AppSuspendStateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var appsCache: Map<String, LaunchableApp> = emptyMap()
    private var appsCacheLoaded = false

    /** Called by the screen's lifecycle-aware poll loop (see LifecycleAwarePoll) — only runs
     *  while the Dashboard is actually visible, not for the ViewModel's whole lifetime. A plain
     *  suspend fun (not its own viewModelScope.launch) so the poll loop awaits each refresh
     *  before scheduling the next one, rather than potentially stacking up overlapping queries. */
    suspend fun refresh() {
        if (!appsCacheLoaded) {
            appsCache = installedAppsRepository.getLaunchableApps().associateBy { it.packageName }
            appsCacheLoaded = true
        }

        val usageMillis = usageStatsRepository.computeTodayUsageMillis()
        val suspendedPackages = suspendStateRepository.getSuspendedPackages()

        val rows = usageMillis.entries
            .mapNotNull { (pkg, ms) ->
                val app = appsCache[pkg] ?: return@mapNotNull null
                AppUsageUiState(
                    packageName = pkg,
                    label = app.label,
                    icon = app.icon,
                    usedMinutes = UsageStatsRepository.millisToMinutes(ms),
                    isSuspended = pkg in suspendedPackages
                )
            }
            .sortedByDescending { it.usedMinutes }

        _uiState.value = DashboardUiState(
            totalMinutes = UsageStatsRepository.totalMinutes(usageMillis),
            apps = rows,
            isLoading = false
        )
    }

    companion object {
        const val REFRESH_INTERVAL_MILLIS = 5_000L
    }
}
