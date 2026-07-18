package com.ownscreen.app.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ownscreen.app.OwnScreenApplication
import com.ownscreen.app.data.usage.UsageStatsRepository
import com.ownscreen.app.ui.theme.nord0
import com.ownscreen.app.ui.theme.nord4
import com.ownscreen.app.ui.theme.nord6
import com.ownscreen.app.ui.theme.nord8
import com.ownscreen.app.util.TimeUtils

private data class WidgetAppRow(val label: String, val minutes: Int, val icon: Bitmap)

/**
 * Glance widgets render through RemoteViews in the launcher's own process, where custom
 * FontFamily support is inconsistent across launchers/OS versions. FontFamily.Monospace is used
 * here as a safe stand-in for the Martian Mono look — true Martian Mono rendering is only
 * guaranteed in the in-app Compose UI, which isn't RemoteViews-constrained.
 */
class ScreenTimeWidget : GlanceAppWidget() {

    // Exact (not the default Single) so the widget actually gets recomposed against its real
    // current size on every resize, letting WidgetContent adapt what it shows rather than
    // rendering one fixed layout regardless of how small or large the user makes it.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as OwnScreenApplication).container
        val usageMillis = container.usageStatsRepository.computeTodayUsageMillis()
        val appsByPackage = container.installedAppsRepository.getLaunchableApps().associateBy { it.packageName }

        // The "top apps" breakdown only lists recognized launchable apps (excluding OwnScreen
        // itself and system packages like the launcher/keyboard/systemui) — but the *total*
        // below deliberately does NOT reuse this filtered subset. It must match the persistent
        // notification's total (the real "today's screen time" figure), which sums every
        // package; see UsageStatsRepository.totalMinutes for why.
        val topApps = usageMillis.entries
            .mapNotNull { (pkg, ms) ->
                appsByPackage[pkg]?.let { app ->
                    WidgetAppRow(
                        label = app.label,
                        minutes = UsageStatsRepository.millisToMinutes(ms),
                        icon = app.icon.toBitmap(width = 96, height = 96)
                    )
                }
            }
            .sortedByDescending { it.minutes }
            .take(4)

        val totalMinutes = UsageStatsRepository.totalMinutes(usageMillis)

        provideContent {
            WidgetContent(totalMinutes = totalMinutes, topApps = topApps)
        }
    }
}

@Composable
private fun WidgetContent(totalMinutes: Int, topApps: List<WidgetAppRow>) {
    val size = LocalSize.current
    val compact = size.height < 100.dp
    // Rows are two lines tall now (name, then time underneath), so they take more vertical
    // space per row than the old single-line layout — thresholds bumped up to match.
    val maxAppRows = when {
        size.height >= 240.dp -> 4
        size.height >= 195.dp -> 3
        size.height >= 155.dp -> 2
        size.height >= 120.dp -> 1
        else -> 0
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(nord0)
            .padding(12.dp),
        verticalAlignment = if (compact) Alignment.Vertical.CenterVertically else Alignment.Vertical.Top
    ) {
        if (!compact) {
            Text(
                text = "Screen time today",
                style = TextStyle(color = ColorProvider(nord8), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            )
        }
        Text(
            text = TimeUtils.formatHoursMinutes(totalMinutes),
            style = TextStyle(
                color = ColorProvider(nord6),
                fontSize = if (compact) 18.sp else 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        )
        if (maxAppRows > 0 && topApps.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(8.dp))
            topApps.take(maxAppRows).forEach { app ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(app.icon),
                        contentDescription = null,
                        modifier = GlanceModifier.width(22.dp).height(22.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Column {
                        Text(
                            text = app.label,
                            style = TextStyle(color = ColorProvider(nord4), fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                            maxLines = 1
                        )
                        Text(
                            text = TimeUtils.formatHoursMinutes(app.minutes),
                            style = TextStyle(
                                color = ColorProvider(nord8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }
    }
}
