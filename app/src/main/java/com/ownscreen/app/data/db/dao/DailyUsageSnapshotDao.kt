package com.ownscreen.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ownscreen.app.data.db.entity.DailyUsageSnapshotEntity

@Dao
interface DailyUsageSnapshotDao {
    @Query("SELECT * FROM daily_usage_snapshot WHERE epochDay = :epochDay ORDER BY minutesUsed DESC")
    suspend fun getForDay(epochDay: Long): List<DailyUsageSnapshotEntity>

    @Query("SELECT DISTINCT epochDay FROM daily_usage_snapshot ORDER BY epochDay DESC")
    suspend fun getDistinctDays(): List<Long>

    @Upsert
    suspend fun upsertAll(entities: List<DailyUsageSnapshotEntity>)
}
