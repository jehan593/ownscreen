package com.ownscreen.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Belt-and-suspenders fallback only: the foreground UsageMonitorService pushes widget updates
 * on every tick while it's running, which is the primary refresh path. This worker exists in
 * case an aggressive OEM battery manager kills that service — the OS enforces a ~30 minute
 * floor on widget update periods anyway, so this isn't a responsiveness downgrade.
 */
class WidgetUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val glanceIds = GlanceAppWidgetManager(applicationContext).getGlanceIds(ScreenTimeWidget::class.java)
        if (glanceIds.isNotEmpty()) {
            ScreenTimeWidget().updateAll(applicationContext)
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "widget_update_fallback"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
