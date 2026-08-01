package com.ownscreen.app.ui.modes

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ownscreen.app.data.pm.InstalledAppsRepository
import com.ownscreen.app.data.repository.ModeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ModeAppCheckRow(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val isChecked: Boolean
)

data class ModeDetailUiState(
    val name: String = "",
    val isNew: Boolean = true,
    val requireTrivia: Boolean = false,
    /** The persisted value of [requireTrivia] from *before* this editing session started — set
     *  once on load and never touched by [ModeDetailViewModel.setRequireTrivia]. The screen
     *  (see ModeDetailScreen) gates opening the editor itself behind a trivia challenge when this
     *  was already true; flipping the switch or saving afterwards never needs to be re-solved. */
    val originalRequireTrivia: Boolean = false,
    val apps: List<ModeAppCheckRow> = emptyList(),
    val isLoading: Boolean = true
)

/** [modeId] is [NEW_MODE_ID] when creating a brand-new mode rather than editing an existing one. */
class ModeDetailViewModel(
    private val modeId: Long,
    private val installedAppsRepository: InstalledAppsRepository,
    private val modeRepository: ModeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModeDetailUiState(isNew = modeId == NEW_MODE_ID))
    val uiState: StateFlow<ModeDetailUiState> = _uiState.asStateFlow()

    private var checkedPackages: MutableSet<String> = mutableSetOf()

    init {
        viewModelScope.launch {
            val existingMode = if (modeId != NEW_MODE_ID) modeRepository.getMode(modeId) else null
            checkedPackages = if (modeId != NEW_MODE_ID) {
                modeRepository.getPackagesForMode(modeId).toMutableSet()
            } else {
                mutableSetOf()
            }
            val installedApps = installedAppsRepository.getLaunchableApps()
            _uiState.value = _uiState.value.copy(
                name = existingMode?.name.orEmpty(),
                requireTrivia = existingMode?.requireTrivia ?: false,
                originalRequireTrivia = existingMode?.requireTrivia ?: false,
                apps = installedApps.map { app ->
                    ModeAppCheckRow(app.packageName, app.label, app.icon, app.packageName in checkedPackages)
                },
                isLoading = false
            )
        }
    }

    fun setName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun setRequireTrivia(requireTrivia: Boolean) {
        _uiState.value = _uiState.value.copy(requireTrivia = requireTrivia)
    }

    fun toggleApp(packageName: String) {
        if (!checkedPackages.remove(packageName)) checkedPackages.add(packageName)
        _uiState.value = _uiState.value.copy(
            apps = _uiState.value.apps.map { row ->
                if (row.packageName == packageName) row.copy(isChecked = !row.isChecked) else row
            }
        )
    }

    /** Suspends until persisted (and, if this mode is currently active, until enforcement has
     *  caught up with the edited app set) so the caller can safely navigate back right after. */
    suspend fun save() {
        val name = _uiState.value.name.trim().ifBlank { "Untitled mode" }
        val requireTrivia = _uiState.value.requireTrivia
        if (modeId == NEW_MODE_ID) {
            modeRepository.createMode(name, checkedPackages, requireTrivia)
        } else {
            modeRepository.updateMode(modeId, name, checkedPackages, requireTrivia)
        }
    }

    suspend fun delete() {
        if (modeId != NEW_MODE_ID) modeRepository.deleteMode(modeId)
    }

    companion object {
        const val NEW_MODE_ID = -1L
    }
}
