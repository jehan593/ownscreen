package com.ownscreen.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ownscreen.app.data.repository.SettingsRepository
import com.ownscreen.app.enforcement.OwnDroidController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKey: String = "",
    val pollIntervalSeconds: Int = SettingsRepository.DEFAULT_POLL_INTERVAL_SECONDS,
    val ownDroidInstalled: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val ownDroidController: OwnDroidController
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(ownDroidInstalled = ownDroidController.isOwnDroidInstalled())
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.apiKeyFlow,
                settingsRepository.pollIntervalSecondsFlow
            ) { apiKey, pollInterval ->
                _uiState.value.copy(apiKey = apiKey, pollIntervalSeconds = pollInterval)
            }.collect { _uiState.value = it }
        }
    }

    fun setApiKey(key: String) {
        viewModelScope.launch { settingsRepository.setApiKey(key) }
    }

    fun setPollIntervalSeconds(seconds: Int) {
        viewModelScope.launch { settingsRepository.setPollIntervalSeconds(seconds) }
    }
}
