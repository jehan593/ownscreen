package com.ownscreen.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ownscreen.app.data.repository.UsageHistoryRepository
import com.ownscreen.app.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryDayRow(val epochDay: Long, val label: String, val totalMinutes: Int)

data class HistoryUiState(
    val days: List<HistoryDayRow> = emptyList(),
    val isLoading: Boolean = true
)

/** Past days are a fixed record, unlike Dashboard/AppDetail — a one-shot load is enough,
 *  no live polling needed. */
class HistoryViewModel(private val usageHistoryRepository: UsageHistoryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val today = TimeUtils.localEpochDay()
            val rows = usageHistoryRepository.getDaysWithData()
                .filter { it != today } // today's live total is already on the Dashboard
                .map { day ->
                    val totalMinutes = usageHistoryRepository.getDay(day).sumOf { it.minutesUsed }
                    HistoryDayRow(day, TimeUtils.formatDayLabel(day), totalMinutes)
                }
            _uiState.value = HistoryUiState(days = rows, isLoading = false)
        }
    }
}
