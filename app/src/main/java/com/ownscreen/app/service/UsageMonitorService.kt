package com.ownscreen.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ownscreen.app.MainActivity
import com.ownscreen.app.OwnScreenApplication
import com.ownscreen.app.R
import com.ownscreen.app.data.db.entity.DailyUsageSnapshotEntity
import com.ownscreen.app.data.usage.UsageStatsRepository
import com.ownscreen.app.util.TimeUtils
import com.ownscreen.app.widget.ScreenTimeWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The near-real-time tracking loop: keeps the persistent notification, the Room usage-history
 * cache, and the home-screen widget fresh. Blocking itself is a direct, manual, immediate action
 * (see AppDetailViewModel) — this loop has nothing to do with it. It's a genuine foreground
 * Service with a coroutine loop rather than WorkManager because WorkManager's ~15-30 minute
 * periodic floor is too coarse for a notification/widget that's supposed to feel live.
 */
class UsageMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var loopJob: Job? = null

    private val container get() = (application as OwnScreenApplication).container

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0
        startForeground(NOTIFICATION_ID, buildNotification("Starting…"), type)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (loopJob?.isActive != true) {
            loopJob = serviceScope.launch { runLoop() }
        }
        return START_STICKY
    }

    private suspend fun runLoop() {
        val settings = container.settingsRepository
        while (serviceScope.isActive) {
            // A single failure here (DB write, widget push, ...) must not be allowed to kill this
            // loop permanently — without this catch, one transient error would silently stop all
            // future ticks (notification, widget) for the rest of the service's lifetime, even
            // though the persistent notification stays up and makes it look like tracking is
            // still running.
            try {
                tick()
            } catch (e: Exception) {
                Log.e(TAG, "tick() failed, will retry next interval", e)
            }
            val intervalSeconds = settings.pollIntervalSecondsFlow.first()
            delay(intervalSeconds * 1000L)
        }
    }

    private suspend fun tick() {
        val usageRepo = container.usageStatsRepository
        val usageMillis = usageRepo.computeTodayUsageMillis()
        val usageMinutes = usageMillis.mapValues { (_, ms) -> UsageStatsRepository.millisToMinutes(ms) }
        val todayEpochDay = TimeUtils.localEpochDay()

        val totalMinutes = UsageStatsRepository.totalMinutes(usageMillis)
        updateNotification(TimeUtils.formatHoursMinutes(totalMinutes))

        val snapshotDao = container.database.dailyUsageSnapshotDao()
        val now = System.currentTimeMillis()
        snapshotDao.upsertAll(
            usageMinutes.map { (pkg, minutes) ->
                DailyUsageSnapshotEntity(
                    packageName = pkg,
                    epochDay = todayEpochDay,
                    minutesUsed = minutes,
                    lastUpdatedAtEpochMillis = now
                )
            }
        )

        ScreenTimeWidgetProvider.requestUpdate(applicationContext)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen time tracking",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Ongoing notification while OwnScreen tracks app usage"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(contentText: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_tracking_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    companion object {
        private const val TAG = "UsageMonitorService"
        private const val CHANNEL_ID = "usage_monitor"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            val intent = Intent(context, UsageMonitorService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UsageMonitorService::class.java))
        }
    }
}
