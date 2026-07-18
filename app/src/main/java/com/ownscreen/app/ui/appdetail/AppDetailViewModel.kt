package com.ownscreen.app.ui.appdetail

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ownscreen.app.data.pm.InstalledAppsRepository
import com.ownscreen.app.data.pm.PackageSuspensionChecker
import com.ownscreen.app.data.repository.AppSuspendStateRepository
import com.ownscreen.app.data.repository.SettingsRepository
import com.ownscreen.app.data.usage.UsageStatsRepository
import com.ownscreen.app.enforcement.OwnDroidController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppDetailUiState(
    val label: String = "",
    val icon: Drawable? = null,
    val usedMinutes: Int = 0,
    val isSuspended: Boolean = false,
    val blockLikelyIneffective: Boolean = false,
    val ownDroidInstalled: Boolean = false,
    val apiKeyPresent: Boolean = false
) {
    /** Blocking is a no-op on the real device unless OwnDroid is both installed and configured
     *  with a key — without this, the app would look "blocked" in our own UI while OwnDroid
     *  silently ignores the broadcast, which is worse than not offering the button at all. */
    val canBlock: Boolean get() = ownDroidInstalled && apiKeyPresent
}

class AppDetailViewModel(
    private val packageName: String,
    private val usageStatsRepository: UsageStatsRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val suspendStateRepository: AppSuspendStateRepository,
    private val settingsRepository: SettingsRepository,
    private val packageSuspensionChecker: PackageSuspensionChecker,
    private val ownDroidController: OwnDroidController
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AppDetailUiState(ownDroidInstalled = ownDroidController.isOwnDroidInstalled())
    )
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val app = installedAppsRepository.getLaunchableApps().find { it.packageName == packageName }
            _uiState.value = _uiState.value.copy(label = app?.label ?: packageName, icon = app?.icon)
        }

        // Flow collectors are cheap (event-driven, not polling) and fine to keep alive for the
        // ViewModel's full lifetime — only the recurring UsageStatsManager query below needs to
        // be gated to on-screen visibility (see LifecycleAwarePoll).
        viewModelScope.launch {
            suspendStateRepository.observeAllSuspended().collect { suspendedList ->
                _uiState.value = _uiState.value.copy(
                    isSuspended = suspendedList.any { it.packageName == packageName }
                )
            }
        }

        viewModelScope.launch {
            settingsRepository.apiKeyFlow.collect { key ->
                _uiState.value = _uiState.value.copy(apiKeyPresent = key.isNotBlank())
            }
        }
    }

    /** Called by the screen's lifecycle-aware poll loop (see LifecycleAwarePoll) — only runs
     *  while this screen is actually visible, not for the ViewModel's whole lifetime. Also called
     *  immediately after block()/unblock() so the UI reflects the action right away instead of
     *  waiting for the next scheduled tick. */
    suspend fun refreshUsageAndBlockStatus() {
        val usageMillis = usageStatsRepository.computeTodayUsageMillis()[packageName] ?: 0L
        val minutes = UsageStatsRepository.millisToMinutes(usageMillis)
        _uiState.value = _uiState.value.copy(
            usedMinutes = minutes,
            blockLikelyIneffective = checkBlockLikelyIneffective(minutes)
        )
    }

    /**
     * OwnDroid's broadcast API gives no confirmation either way, so a wrong (but non-blank) API
     * key looks identical to a correct one from our side — the block button "succeeds" the same
     * regardless. Two independent signals here, since neither alone is fully reliable:
     *
     * 1. Ask Android directly whether it actually has this package suspended right now — real
     *    ground truth when available, but whether a third-party app is even allowed to query
     *    another package's suspended state isn't reliably documented, so this can come back null
     *    ("unknown") rather than a definite answer.
     * 2. Otherwise, fall back to a heuristic: real suspension prevents the app from launching at
     *    all, so if it keeps accumulating *new* usage well past a grace period after being
     *    blocked, that's strong indirect evidence the block isn't actually in effect.
     */
    private suspend fun checkBlockLikelyIneffective(currentMinutes: Int): Boolean {
        val state = suspendStateRepository.get(packageName) ?: return false
        if (!state.isSuspended) return false

        packageSuspensionChecker.isSuspendedOrNull(packageName)?.let { osSuspended ->
            val elapsedSinceBlock = System.currentTimeMillis() - state.lastChangedAtEpochMillis
            if (elapsedSinceBlock > GRACE_PERIOD_MILLIS) {
                return !osSuspended
            }
        }

        return currentMinutes > state.usageMinutesAtBlockTime + USAGE_GRACE_MINUTES
    }

    /** Blocking is instant and unguarded once [AppDetailUiState.canBlock] is true — friction
     *  belongs on the way *out*, not the way in. The UI is responsible for only calling this
     *  when canBlock is true. */
    fun block() {
        viewModelScope.launch {
            ownDroidController.suspend(packageName)
            suspendStateRepository.markSuspended(packageName, _uiState.value.usedMinutes)
            refreshUsageAndBlockStatus()
        }
    }

    /** Only ever called by the UI after its math-challenge friction has been solved. Always
     *  allowed regardless of [AppDetailUiState.canBlock] so a stale local "blocked" flag (e.g.
     *  from before OwnDroid was configured) can always be cleared. */
    fun unblock() {
        viewModelScope.launch {
            ownDroidController.unsuspend(packageName)
            suspendStateRepository.markUnsuspended(packageName)
            refreshUsageAndBlockStatus()
        }
    }

    companion object {
        const val POLL_INTERVAL_MILLIS = 3_000L
        private const val GRACE_PERIOD_MILLIS = 10_000L
        private const val USAGE_GRACE_MINUTES = 1
    }
}
