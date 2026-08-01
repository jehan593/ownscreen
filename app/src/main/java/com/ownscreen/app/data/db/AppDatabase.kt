package com.ownscreen.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.ownscreen.app.data.db.dao.AppSuspendStateDao
import com.ownscreen.app.data.db.dao.DailyUsageSnapshotDao
import com.ownscreen.app.data.db.dao.ModeDao
import com.ownscreen.app.data.db.entity.AppSuspendStateEntity
import com.ownscreen.app.data.db.entity.DailyUsageSnapshotEntity
import com.ownscreen.app.data.db.entity.ModeAppEntity
import com.ownscreen.app.data.db.entity.ModeEntity

@Database(
    entities = [
        AppSuspendStateEntity::class,
        DailyUsageSnapshotEntity::class,
        ModeEntity::class,
        ModeAppEntity::class
    ],
    // v2->v3: forced a one-time wipe after TimeUtils.localEpochDay()'s definition changed.
    // v3->v4: added AppSuspendStateEntity.usageMinutesAtBlockTime (real column change).
    // v4->v5: added Modes (mode/mode_app tables) + AppSuspendStateEntity.blockedByModeId — a real
    // Migration (MIGRATION_4_5 below) rather than a destructive wipe, since users may already have
    // apps manually blocked that must survive the upgrade.
    // v5->v6: added ModeEntity.requireTrivia.
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appSuspendStateDao(): AppSuspendStateDao
    abstract fun dailyUsageSnapshotDao(): DailyUsageSnapshotDao
    abstract fun modeDao(): ModeDao

    companion object {
        const val DATABASE_NAME = "ownscreen.db"

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `mode` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, " +
                        "`isDefault` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO `mode` (`id`, `name`, `isDefault`) VALUES " +
                        "(${ModeEntity.DEFAULT_MODE_ID}, 'Default', 1)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `mode_app` (`modeId` INTEGER NOT NULL, " +
                        "`packageName` TEXT NOT NULL, PRIMARY KEY(`modeId`, `packageName`), " +
                        "FOREIGN KEY(`modeId`) REFERENCES `mode`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mode_app_modeId` ON `mode_app` (`modeId`)")
                db.execSQL("ALTER TABLE `app_suspend_state` ADD COLUMN `blockedByModeId` INTEGER")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `mode` ADD COLUMN `requireTrivia` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** MIGRATION_4_5 only runs when upgrading an *existing* pre-Modes database — a brand-new
         *  install has no database to migrate from, so Room creates the v6 schema directly from
         *  the entity definitions and this seed row would otherwise never get inserted, leaving
         *  the Default mode missing from a fresh install (and from any already-installed copy
         *  from before this fix existed). onOpen runs on every app launch, not just the first
         *  time the file is created, so this self-heals an already-broken local database too, not
         *  just future fresh installs — the insert is idempotent (OR IGNORE), so it's a no-op
         *  once the row exists. */
        val CALLBACK = object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL(
                    "INSERT OR IGNORE INTO `mode` (`id`, `name`, `isDefault`, `requireTrivia`) VALUES " +
                        "(${ModeEntity.DEFAULT_MODE_ID}, 'Default', 1, 0)"
                )
            }
        }
    }
}
