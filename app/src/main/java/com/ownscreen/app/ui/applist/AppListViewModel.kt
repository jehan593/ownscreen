package com.ownscreen.app.ui.applist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ownscreen.app.data.pm.InstalledAppsRepository
import com.ownscreen.app.data.pm.LaunchableApp
import com.ownscreen.app.data.repository.AppSuspendStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class AppListRow(
    val packageName: String,
    val label: String,
    val icon: android.graphics.drawable.Drawable,
    val isSuspended: Boolean
)

data class AppListUiState(
    val apps: List<AppListRow> = emptyList(),
    val isLoading: Boolean = true
)

class AppListViewModel(
    private val installedAppsRepository: InstalledAppsRepository,
    private val suspendStateRepository: AppSuspendStateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    private var appsCache: List<LaunchableApp> = emptyList()

    init {
        viewModelScope.launch {
            appsCache = installedAppsRepository.getLaunchableApps()
            suspendStateRepository.observeAllSuspended().collectLatest { suspended ->
                val suspendedPackages = suspended.map { it.packageName }.toSet()
                _uiState.value = AppListUiState(
                    apps = appsCache.map { app ->
                        AppListRow(
                            packageName = app.packageName,
                            label = app.label,
                            icon = app.icon,
                            isSuspended = app.packageName in suspendedPackages
                        )
                    },
                    isLoading = false
                )
            }
        }
    }
}
