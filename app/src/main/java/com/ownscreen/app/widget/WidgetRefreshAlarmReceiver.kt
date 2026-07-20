package com.ownscreen.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

/**
 * Belt-and-suspenders fallback only: the foreground UsageMonitorService pushes widget updates
 * on every tick while it's running, which is the primary refresh path. This alarm exists in
 * case an aggressive OEM battery manager kills that service — the OS enforces a ~30 minute
 * floor on widget update periods anyway, so this isn't a responsiveness downgrade.
 *
 * AlarmManager instead of WorkManager: the only thing this needs is "trigger a widget refresh
 * every ~30 minutes," and WorkManager's own manifest unconditionally adds WAKE_LOCK,
 * ACCESS_NETWORK_STATE, RECEIVE_BOOT_COMPLETED, and FOREGROUND_SERVICE permissions to every
 * build regardless of whether this job uses any of that. setInexactRepeating with
 * ELAPSED_REALTIME (not ...WAKEUP) needs no extra permission, doesn't wake a sleeping device for
 * a refresh with no urgency, and lets the system batch it with other apps' alarms.
 */
class WidgetRefreshAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ScreenTimeWidgetProvider.requestUpdate(context)
    }

    companion object {
        private const val INTERVAL_MILLIS = 30 * 60 * 1000L
        private const val REQUEST_CODE = 3001

        fun schedule(context: Context) {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                Intent(context, WidgetRefreshAlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + INTERVAL_MILLIS,
                INTERVAL_MILLIS,
                pendingIntent
            )
        }
    }
}
