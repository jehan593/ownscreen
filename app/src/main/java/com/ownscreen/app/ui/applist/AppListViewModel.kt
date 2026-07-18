package com.ownscreen.app.ui.applist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ownscreen.app.data.pm.InstalledAppsRepository
import com.ownscreen.app.data.pm.LaunchableApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppListUiState(
    val apps: List<LaunchableApp> = emptyList(),
    val isLoading: Boolean = true
)

class AppListViewModel(private val installedAppsRepository: InstalledAppsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = AppListUiState(apps = installedAppsRepository.getLaunchableApps(), isLoading = false)
        }
    }
}
