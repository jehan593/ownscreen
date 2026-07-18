package com.ownscreen.app.data.db.entity

import androidx.room.Entity

/**
 * Display/history cache only. The live blocking decision always recomputes usage
 * from UsageStatsManager directly (see UsageStatsRepository) — this table is never
 * read for enforcement, only for the dashboard/widget to render without re-querying.
 */
@Entity(tableName = "daily_usage_snapshot", primaryKeys = ["packageName", "epochDay"])
data class DailyUsageSnapshotEntity(
    val packageName: String,
    val epochDay: Long,
    val minutesUsed: Int,
    val lastUpdatedAtEpochMillis: Long
)
