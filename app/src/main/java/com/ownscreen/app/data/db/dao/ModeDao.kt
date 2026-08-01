package com.ownscreen.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.ownscreen.app.data.db.entity.ModeAppEntity
import com.ownscreen.app.data.db.entity.ModeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModeDao {
    @Query("SELECT * FROM mode ORDER BY isDefault DESC, id ASC")
    fun observeAllModes(): Flow<List<ModeEntity>>

    @Query("SELECT * FROM mode WHERE id = :id")
    suspend fun getMode(id: Long): ModeEntity?

    @Query("SELECT MAX(id) FROM mode")
    suspend fun getMaxModeId(): Long?

    @Insert
    suspend fun insertMode(entity: ModeEntity)

    @Update
    suspend fun updateMode(entity: ModeEntity)

    @Query("DELETE FROM mode WHERE id = :id")
    suspend fun deleteMode(id: Long)

    @Query("SELECT packageName FROM mode_app WHERE modeId = :modeId")
    suspend fun getPackagesForMode(modeId: Long): List<String>

    @Query("SELECT packageName FROM mode_app WHERE modeId = :modeId")
    fun observePackagesForMode(modeId: Long): Flow<List<String>>

    @Upsert
    suspend fun insertModeApps(entities: List<ModeAppEntity>)

    @Query("DELETE FROM mode_app WHERE modeId = :modeId")
    suspend fun deleteAppsForMode(modeId: Long)

    @Transaction
    suspend fun replaceAppsForMode(modeId: Long, packages: Set<String>) {
        deleteAppsForMode(modeId)
        insertModeApps(packages.map { ModeAppEntity(modeId, it) })
    }
}
