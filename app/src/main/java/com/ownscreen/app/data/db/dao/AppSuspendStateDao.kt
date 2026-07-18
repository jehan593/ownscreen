package com.ownscreen.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ownscreen.app.data.db.entity.AppSuspendStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSuspendStateDao {
    @Query("SELECT * FROM app_suspend_state WHERE isSuspended = 1")
    suspend fun getAllSuspended(): List<AppSuspendStateEntity>

    @Query("SELECT * FROM app_suspend_state WHERE isSuspended = 1")
    fun observeAllSuspended(): Flow<List<AppSuspendStateEntity>>

    @Query("SELECT * FROM app_suspend_state WHERE packageName = :packageName")
    suspend fun get(packageName: String): AppSuspendStateEntity?

    @Upsert
    suspend fun upsert(entity: AppSuspendStateEntity)
}
