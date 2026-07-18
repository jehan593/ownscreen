package com.ownscreen.app.ui.history

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ownscreen.app.data.pm.InstalledAppsRepository
import com.ownscreen.app.data.repository.UsageHistoryRepository
import com.ownscreen.app.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryDayAppRow(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val usedMinutes: Int
)

data class HistoryDayUiState(
    val dayLabel: String = "",
    val totalMinutes: Int = 0,
    val apps: List<HistoryDayAppRow> = emptyList(),
    val isLoading: Boolean = true
)

class HistoryDayViewModel(
    private val epochDay: Long,
    private val usageHistoryRepository: UsageHistoryRepository,
    private val installedAppsRepository: InstalledAppsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryDayUiState(dayLabel = TimeUtils.formatDayLabel(epochDay)))
    val uiState: StateFlow<HistoryDayUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val snapshots = usageHistoryRepository.getDay(epochDay)
            val appsByPackage = installedAppsRepository.getLaunchableApps().associateBy { it.packageName }

            val rows = snapshots
                .mapNotNull { snapshot ->
                    val app = appsByPackage[snapshot.packageName] ?: return@mapNotNull null
                    HistoryDayAppRow(
                        packageName = snapshot.packageName,
                        label = app.label,
                        icon = app.icon,
                        usedMinutes = snapshot.minutesUsed
                    )
                }
                .sortedByDescending { it.usedMinutes }

            _uiState.value = _uiState.value.copy(
                totalMinutes = snapshots.sumOf { it.minutesUsed },
                apps = rows,
                isLoading = false
            )
        }
    }
}
