package com.ownscreen.app.ui.modes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ownscreen.app.data.db.entity.ModeEntity
import com.ownscreen.app.data.repository.ModeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ModesUiState(
    val modes: List<ModeEntity> = emptyList(),
    val activeModeId: Long = ModeEntity.DEFAULT_MODE_ID
)

class ModesViewModel(private val modeRepository: ModeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ModesUiState())
    val uiState: StateFlow<ModesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(modeRepository.observeModes(), modeRepository.activeModeIdFlow) { modes, activeId ->
                ModesUiState(modes = modes, activeModeId = activeId)
            }.collect { _uiState.value = it }
        }
    }

    /** Switching modes is the "way in" — instant, no confirmation, mirroring the existing
     *  block-button philosophy (friction belongs on destructive actions, not activation). */
    fun activateMode(modeId: Long) {
        viewModelScope.launch { modeRepository.activateMode(modeId) }
    }
}
