package com.ownscreen.app.data.repository

import com.ownscreen.app.data.db.dao.AppSuspendStateDao
import com.ownscreen.app.data.db.entity.AppSuspendStateEntity
import kotlinx.coroutines.flow.Flow

class AppSuspendStateRepository(private val dao: AppSuspendStateDao) {

    fun observeAllSuspended(): Flow<List<AppSuspendStateEntity>> = dao.observeAllSuspended()

    suspend fun getSuspendedPackages(): Set<String> =
        dao.getAllSuspended().map { it.packageName }.toSet()

    suspend fun get(packageName: String): AppSuspendStateEntity? = dao.get(packageName)

    suspend fun markSuspended(packageName: String, usageMinutesAtBlockTime: Int) {
        dao.upsert(
            AppSuspendStateEntity(
                packageName = packageName,
                isSuspended = true,
                usageMinutesAtBlockTime = usageMinutesAtBlockTime,
                lastChangedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun markUnsuspended(packageName: String) {
        dao.upsert(
            AppSuspendStateEntity(
                packageName = packageName,
                isSuspended = false,
                usageMinutesAtBlockTime = 0,
                lastChangedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }
}
