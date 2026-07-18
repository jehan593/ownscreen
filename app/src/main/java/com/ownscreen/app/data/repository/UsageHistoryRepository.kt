package com.ownscreen.app.data.repository

import com.ownscreen.app.data.db.dao.DailyUsageSnapshotDao
import com.ownscreen.app.data.db.entity.DailyUsageSnapshotEntity

class UsageHistoryRepository(private val dao: DailyUsageSnapshotDao) {

    /** Days that have any recorded usage, most recent first. */
    suspend fun getDaysWithData(): List<Long> = dao.getDistinctDays()

    /** Every package's minutes for a given day — unfiltered, same convention as
     *  UsageStatsRepository.totalMinutes: sum this directly for that day's total. */
    suspend fun getDay(epochDay: Long): List<DailyUsageSnapshotEntity> = dao.getForDay(epochDay)
}
