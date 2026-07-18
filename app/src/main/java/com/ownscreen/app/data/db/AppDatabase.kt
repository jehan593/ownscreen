package com.ownscreen.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ownscreen.app.data.db.dao.AppSuspendStateDao
import com.ownscreen.app.data.db.dao.DailyUsageSnapshotDao
import com.ownscreen.app.data.db.entity.AppSuspendStateEntity
import com.ownscreen.app.data.db.entity.DailyUsageSnapshotEntity

@Database(
    entities = [
        AppSuspendStateEntity::class,
        DailyUsageSnapshotEntity::class
    ],
    // v2->v3: forced a one-time wipe after TimeUtils.localEpochDay()'s definition changed.
    // v3->v4: added AppSuspendStateEntity.usageMinutesAtBlockTime (real column change).
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appSuspendStateDao(): AppSuspendStateDao
    abstract fun dailyUsageSnapshotDao(): DailyUsageSnapshotDao

    companion object {
        const val DATABASE_NAME = "ownscreen.db"
    }
}
