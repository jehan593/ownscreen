package com.ownscreen.app.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.ownscreen.app.data.db.AppDatabase
import com.ownscreen.app.data.pm.InstalledAppsRepository
import com.ownscreen.app.data.pm.PackageSuspensionChecker
import com.ownscreen.app.data.repository.AppSuspendStateRepository
import com.ownscreen.app.data.repository.SettingsRepository
import com.ownscreen.app.data.repository.UsageHistoryRepository
import com.ownscreen.app.data.usage.UsageStatsRepository
import com.ownscreen.app.enforcement.OwnDroidBroadcastSender
import com.ownscreen.app.enforcement.OwnDroidController
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "ownscreen_settings")

interface AppContainer {
    val database: AppDatabase
    val settingsRepository: SettingsRepository
    val suspendStateRepository: AppSuspendStateRepository
    val usageStatsRepository: UsageStatsRepository
    val usageHistoryRepository: UsageHistoryRepository
    val ownDroidController: OwnDroidController
    val installedAppsRepository: InstalledAppsRepository
    val packageSuspensionChecker: PackageSuspensionChecker
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: AppDatabase by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            // No real migrations written for any past version bump (see AppDatabase's version
            // history comment) — acceptable for this local-only data (usage snapshots/block
            // state), so just recreate the DB on a schema mismatch rather than crash.
            .fallbackToDestructiveMigration()
            .build()
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context.dataStore)
    }

    override val suspendStateRepository: AppSuspendStateRepository by lazy {
        AppSuspendStateRepository(database.appSuspendStateDao())
    }

    override val usageStatsRepository: UsageStatsRepository by lazy {
        UsageStatsRepository(context)
    }

    override val usageHistoryRepository: UsageHistoryRepository by lazy {
        UsageHistoryRepository(database.dailyUsageSnapshotDao())
    }

    override val ownDroidController: OwnDroidController by lazy {
        OwnDroidBroadcastSender(context) { settingsRepository.apiKeyFlow.first() }
    }

    override val installedAppsRepository: InstalledAppsRepository by lazy {
        InstalledAppsRepository(context.packageManager, context.packageName)
    }

    override val packageSuspensionChecker: PackageSuspensionChecker by lazy {
        PackageSuspensionChecker(context.packageManager)
    }
}
