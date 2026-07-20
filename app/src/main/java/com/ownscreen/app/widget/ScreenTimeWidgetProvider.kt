package com.ownscreen.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import com.ownscreen.app.MainActivity
import com.ownscreen.app.OwnScreenApplication
import com.ownscreen.app.R
import com.ownscreen.app.data.usage.UsageStatsRepository
import com.ownscreen.app.util.AppColorUtils
import com.ownscreen.app.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Classic AppWidgetProvider + RemoteViews rather than Jetpack Glance. Glance itself (not just our
 * own code) transitively depends on androidx.work, which unconditionally merges WAKE_LOCK and
 * ACCESS_NETWORK_STATE into the manifest along with a dozen WorkManager-internal
 * services/receivers — permissions and components this fully-offline app has no other reason to
 * carry. RemoteViews custom-font support is inconsistent across launchers (same reason the old
 * Glance version fell back to system monospace), so this uses android:fontFamily="monospace"
 * directly rather than referencing the bundled Martian Mono font.
 */
class ScreenTimeWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            refreshAll(context)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refreshAll(context)
    }

    // Fires whenever the widget is placed or resized, with the real, system-reported size — this
    // is what keeps the compact/row-count layout matched to the actual current size rather than a
    // one-size-fits-all guess.
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        refreshAll(context)
    }

    private fun refreshAll(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val ids = manager.getAppWidgetIds(ComponentName(appContext, ScreenTimeWidgetProvider::class.java))
        if (ids.isEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val container = (appContext as OwnScreenApplication).container
                val usageMillis = container.usageStatsRepository.computeTodayUsageMillis()
                val appsByPackage = container.installedAppsRepository.getLaunchableApps().associateBy { it.packageName }

                // The "top apps" breakdown only lists recognized launchable apps (excluding
                // OwnScreen itself and system packages like the launcher/keyboard/systemui) — but
                // the *total* below deliberately does NOT reuse this filtered subset. It must
                // match the persistent notification's total (the real "today's screen time"
                // figure), which sums every package; see UsageStatsRepository.totalMinutes.
                val topApps = usageMillis.entries
                    .mapNotNull { (pkg, ms) ->
                        appsByPackage[pkg]?.let { app ->
                            val iconBitmap = app.icon.toBitmap(width = 96, height = 96)
                            val accent = AppColorUtils.nordAccentFor(iconBitmap)
                            WidgetAppRow(
                                label = app.label,
                                minutes = UsageStatsRepository.millisToMinutes(ms),
                                dot = AppColorUtils.dotBitmap(accent, sizePx = 96)
                            )
                        }
                    }
                    .sortedByDescending { it.minutes }
                    .take(4)

                val totalMinutes = UsageStatsRepository.totalMinutes(usageMillis)

                for (id in ids) {
                    val options = manager.getAppWidgetOptions(id)
                    val views = buildViews(appContext, options, totalMinutes, topApps)
                    manager.updateAppWidget(id, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun buildViews(
        context: Context,
        options: Bundle,
        totalMinutes: Int,
        topApps: List<WidgetAppRow>
    ): RemoteViews {
        val heightDp = maxOf(
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0),
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        )
        val compact = heightDp in 1 until 100
        // Rows are two lines tall (name, then time underneath), so thresholds are spaced to match
        // that per-row height rather than a single-line layout.
        val maxAppRows = when {
            heightDp >= 240 -> 4
            heightDp >= 195 -> 3
            heightDp >= 155 -> 2
            heightDp >= 120 -> 1
            else -> 0
        }

        val views = RemoteViews(context.packageName, R.layout.widget_screen_time)

        views.setInt(
            R.id.root,
            "setGravity",
            if (compact) Gravity.CENTER_VERTICAL else Gravity.TOP
        )
        views.setViewVisibility(R.id.subtitle, if (compact) View.GONE else View.VISIBLE)
        views.setTextViewText(R.id.total, TimeUtils.formatHoursMinutes(totalMinutes))
        views.setTextViewTextSize(R.id.total, TypedValue.COMPLEX_UNIT_SP, if (compact) 18f else 26f)

        views.removeAllViews(R.id.rows_container)
        if (maxAppRows > 0) {
            topApps.take(maxAppRows).forEach { app ->
                val row = RemoteViews(context.packageName, R.layout.widget_screen_time_row)
                row.setImageViewBitmap(R.id.dot, app.dot)
                row.setTextViewText(R.id.label, app.label)
                row.setTextViewText(R.id.time, TimeUtils.formatHoursMinutes(app.minutes))
                views.addView(R.id.rows_container, row)
            }
        }

        val launchIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.root, launchIntent)

        return views
    }

    private data class WidgetAppRow(val label: String, val minutes: Int, val dot: android.graphics.Bitmap)

    companion object {
        private const val ACTION_REFRESH = "com.ownscreen.app.widget.ACTION_REFRESH_SCREEN_TIME"
        private const val REQUEST_CODE = 4001

        /**
         * Routes every refresh trigger (foreground-service tick, boot-fallback alarm) through a
         * self-broadcast rather than calling the data-fetch/render logic directly. This gives the
         * update a proper background-execution grace period from the OS's broadcast dispatch,
         * instead of running as an ordinary background coroutine that a cached-app freezer could
         * suspend mid-flight — same pattern used for Noter's note/task widgets.
         */
        fun requestUpdate(context: Context) {
            val intent = Intent(context, ScreenTimeWidgetProvider::class.java).apply { action = ACTION_REFRESH }
            context.sendBroadcast(intent)
        }
    }
}
