package com.ownscreen.app.data.repository

import com.ownscreen.app.data.db.dao.ModeDao
import com.ownscreen.app.data.db.entity.ModeEntity
import com.ownscreen.app.data.usage.UsageStatsRepository
import com.ownscreen.app.enforcement.OwnDroidController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Owns Mode activation: switching the single "active mode" blocks every package the target mode
 * lists and restores whatever the *previous* mode had blocked — without touching manual blocks
 * made from App Detail (see [AppSuspendStateRepository.reassignModeOwner] and the
 * `blockedByModeId` column). The built-in Default mode ([ModeEntity.DEFAULT_MODE_ID], always
 * present with an empty package set) needs no special-cased "restore everything" code path: it
 * falls straight out of this same algorithm.
 */
class ModeRepository(
    private val modeDao: ModeDao,
    private val suspendStateRepository: AppSuspendStateRepository,
    private val settingsRepository: SettingsRepository,
    private val ownDroidController: OwnDroidController,
    private val usageStatsRepository: UsageStatsRepository
) {
    fun observeModes(): Flow<List<ModeEntity>> = modeDao.observeAllModes()

    val activeModeIdFlow: Flow<Long> = settingsRepository.activeModeIdFlow

    suspend fun getMode(modeId: Long): ModeEntity? = modeDao.getMode(modeId)

    suspend fun getPackagesForMode(modeId: Long): Set<String> =
        modeDao.getPackagesForMode(modeId).toSet()

    fun observePackagesForMode(modeId: Long): Flow<List<String>> =
        modeDao.observePackagesForMode(modeId)

    suspend fun createMode(name: String, packages: Set<String>, requireTrivia: Boolean = false): Long {
        val id = (modeDao.getMaxModeId() ?: ModeEntity.DEFAULT_MODE_ID) + 1
        modeDao.insertMode(ModeEntity(id = id, name = name, isDefault = false, requireTrivia = requireTrivia))
        modeDao.replaceAppsForMode(id, packages)
        return id
    }

    suspend fun updateMode(modeId: Long, name: String, packages: Set<String>, requireTrivia: Boolean) {
        val mode = modeDao.getMode(modeId) ?: return
        modeDao.updateMode(mode.copy(name = name, requireTrivia = requireTrivia))
        modeDao.replaceAppsForMode(modeId, packages)
        // Re-run activation so enforcement immediately reflects the edited app set if this
        // mode happens to be the one currently in effect.
        if (settingsRepository.activeModeIdFlow.first() == modeId) activateMode(modeId)
    }

    /** The Default mode can't be deleted — it's the always-present fallback. */
    suspend fun deleteMode(modeId: Long) {
        require(modeId != ModeEntity.DEFAULT_MODE_ID) { "Default mode can't be deleted" }
        if (settingsRepository.activeModeIdFlow.first() == modeId) {
            activateMode(ModeEntity.DEFAULT_MODE_ID)
        }
        modeDao.deleteMode(modeId)
    }

    /**
     * Switches the active mode: blocks every package [modeId] lists, restores packages the
     * previously active mode owned that the new mode doesn't want, and transfers ownership of
     * packages already blocked by some *other* mode that the new mode also wants (no OwnDroid
     * call needed for those). Packages already blocked manually (owner null) are always left
     * alone, whether or not the new mode also lists them.
     */
    suspend fun activateMode(modeId: Long) {
        val targetPackages = getPackagesForMode(modeId)
        val previousModeId = settingsRepository.activeModeIdFlow.first()

        val previouslyModeBlocked = suspendStateRepository.getBlockedByMode(previousModeId)
        val currentlySuspended = suspendStateRepository.getSuspendedPackages()

        val toUnblock = previouslyModeBlocked - targetPackages
        val toBlock = targetPackages - currentlySuspended
        val toRelabel = (targetPackages intersect currentlySuspended) - toBlock

        toUnblock.forEach { pkg ->
            ownDroidController.unsuspend(pkg)
            suspendStateRepository.markUnsuspended(pkg)
        }
        toBlock.forEach { pkg ->
            val minutes = UsageStatsRepository.millisToMinutes(
                usageStatsRepository.computeTodayUsageMillis()[pkg] ?: 0L
            )
            ownDroidController.suspend(pkg)
            suspendStateRepository.markSuspended(pkg, minutes, blockedByModeId = modeId)
        }
        toRelabel.forEach { pkg -> suspendStateRepository.reassignModeOwner(pkg, modeId) }

        settingsRepository.setActiveModeId(modeId)
    }
}
