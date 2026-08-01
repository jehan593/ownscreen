package com.ownscreen.app.data.repository

import com.ownscreen.app.data.db.dao.AppSuspendStateDao
import com.ownscreen.app.data.db.entity.AppSuspendStateEntity
import kotlinx.coroutines.flow.Flow

class AppSuspendStateRepository(private val dao: AppSuspendStateDao) {

    fun observeAllSuspended(): Flow<List<AppSuspendStateEntity>> = dao.observeAllSuspended()

    suspend fun getSuspendedPackages(): Set<String> =
        dao.getAllSuspended().map { it.packageName }.toSet()

    suspend fun get(packageName: String): AppSuspendStateEntity? = dao.get(packageName)

    /** Packages currently suspended as a result of activating [modeId] (see ModeRepository) —
     *  excludes manual blocks made from App Detail, which always have a null owner. */
    suspend fun getBlockedByMode(modeId: Long): Set<String> =
        dao.getBlockedByMode(modeId).map { it.packageName }.toSet()

    suspend fun markSuspended(
        packageName: String,
        usageMinutesAtBlockTime: Int,
        blockedByModeId: Long? = null
    ) {
        dao.upsert(
            AppSuspendStateEntity(
                packageName = packageName,
                isSuspended = true,
                usageMinutesAtBlockTime = usageMinutesAtBlockTime,
                lastChangedAtEpochMillis = System.currentTimeMillis(),
                blockedByModeId = blockedByModeId
            )
        )
    }

    suspend fun markUnsuspended(packageName: String) {
        dao.upsert(
            AppSuspendStateEntity(
                packageName = packageName,
                isSuspended = false,
                usageMinutesAtBlockTime = 0,
                lastChangedAtEpochMillis = System.currentTimeMillis(),
                blockedByModeId = null
            )
        )
    }

    /** Transfers ownership of an already-suspended package to [modeId] — used when switching
     *  modes and the new mode wants a package that's already blocked by a *different* mode, so no
     *  OwnDroid call is needed. No-ops if the package isn't currently suspended, or is suspended
     *  as a manual block (null owner) — manual blocks are never taken over by a mode. */
    suspend fun reassignModeOwner(packageName: String, modeId: Long) {
        val current = dao.get(packageName) ?: return
        if (!current.isSuspended || current.blockedByModeId == null) return
        dao.upsert(current.copy(blockedByModeId = modeId))
    }
}
