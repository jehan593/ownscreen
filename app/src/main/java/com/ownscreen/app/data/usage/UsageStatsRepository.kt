package com.ownscreen.app.data.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.ownscreen.app.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Computes per-app foreground time. Always recomputed fresh from local midnight to "now" on
 * every call rather than maintained as a stateful running counter — this is deliberate: it means
 * day rollover, DST shifts, and process death all self-heal on the very next read, with no need
 * for an exact-alarm-scheduled midnight reset anywhere in the app.
 */
class UsageStatsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val selfPackageName = appContext.packageName
    private val usageStatsManager =
        appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    /** packageName -> foreground milliseconds accumulated since local midnight, through now. */
    suspend fun computeTodayUsageMillis(): Map<String, Long> = withContext(Dispatchers.Default) {
        val startTime = TimeUtils.localMidnightEpochMillis()
        val endTime = System.currentTimeMillis()
        accumulateForegroundMillis(startTime, endTime)
    }

    /**
     * Tracks a single global "current foreground app" (only one app can genuinely be topmost at
     * a time) and gates accumulation on the device actually being awake AND unlocked.
     *
     * Two separate gates are needed, confirmed against Digital Wellbeing's own event usage:
     * - SCREEN_INTERACTIVE / SCREEN_NON_INTERACTIVE: Android does NOT reliably emit
     *   MOVE_TO_BACKGROUND just because the screen turns off — an activity can remain "resumed"
     *   from the OS's point of view while the phone is locked in a pocket. Without this gate,
     *   all of that screen-off time gets attributed to whichever app was last foregrounded.
     * - KEYGUARD_SHOWN / KEYGUARD_HIDDEN (Digital Wellbeing's own on-device database literally
     *   labels KEYGUARD_HIDDEN as "device unlock"): the screen can be woken (SCREEN_INTERACTIVE)
     *   while still locked — glancing at notifications on the lock screen, or the moment before
     *   entering a PIN — and that time must not be attributed to whatever app was open before
     *   the phone was locked either. Tracking only resumes once the keyguard is actually hidden.
     *
     * OwnScreen's own foreground time ([selfPackageName]) is excluded at the source, so every
     * consumer — notification, Dashboard, widget — is automatically consistent without each
     * having to remember the rule.
     */
    private fun accumulateForegroundMillis(startTime: Long, endTime: Long): Map<String, Long> {
        val totals = HashMap<String, Long>()
        var currentPkg: String? = null
        var segmentStart: Long? = null
        var screenOn = true // corrected by the first SCREEN_* event actually seen, if any
        var keyguardShowing = false // corrected by the first KEYGUARD_* event actually seen, if any

        fun isTracking() = screenOn && !keyguardShowing

        fun closeSegment(atTime: Long) {
            val pkg = currentPkg
            val start = segmentStart
            if (pkg != null && start != null && atTime > start) {
                totals[pkg] = (totals[pkg] ?: 0L) + (atTime - start)
            }
            segmentStart = null
        }

        fun maybeResumeSegment(atTime: Long) {
            if (isTracking() && currentPkg != null && segmentStart == null) {
                segmentStart = atTime
            }
        }

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                // Same integer values as the API-29 ACTIVITY_RESUMED/ACTIVITY_PAUSED aliases;
                // branching on these (available since API 21) keeps this uniform across minSdk.
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    val pkg = event.packageName ?: continue
                    if (isTracking()) closeSegment(event.timeStamp)
                    currentPkg = pkg.takeIf { it != selfPackageName }
                    segmentStart = if (isTracking() && currentPkg != null) event.timeStamp else null
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val pkg = event.packageName ?: continue
                    if (pkg == currentPkg) {
                        if (isTracking()) closeSegment(event.timeStamp)
                        currentPkg = null
                        segmentStart = null
                    }
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    if (isTracking()) closeSegment(event.timeStamp)
                    screenOn = false
                }
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    screenOn = true
                    // Don't resume yet if the keyguard turns out to still be showing —
                    // maybeResumeSegment checks isTracking() itself.
                    maybeResumeSegment(event.timeStamp)
                }
                KEYGUARD_SHOWN -> {
                    if (isTracking()) closeSegment(event.timeStamp)
                    keyguardShowing = true
                }
                KEYGUARD_HIDDEN -> {
                    keyguardShowing = false
                    maybeResumeSegment(event.timeStamp)
                }
            }
        }

        if (isTracking()) closeSegment(endTime)

        return totals
    }

    companion object {
        // Not exposed as named constants in the public SDK stub, but these integer event codes
        // are part of the stable data UsageStatsManager.queryEvents() returns regardless — the
        // same codes Digital Wellbeing's own on-device database uses (confirmed via its schema:
        // 17 = KEYGUARD_SHOWN, 18 = KEYGUARD_HIDDEN, the latter literally documented there as
        // "device unlock").
        private const val KEYGUARD_SHOWN = 17
        private const val KEYGUARD_HIDDEN = 18

        fun millisToMinutes(millis: Long): Int = (millis / 60_000L).toInt()

        /**
         * The one canonical "today's total screen time" figure — always the full sum over every
         * package in [usageMillis] (which already excludes OwnScreen's own usage — see
         * [accumulateForegroundMillis]), matching the persistent notification and Digital
         * Wellbeing. Deliberately NOT filtered to exclude the launcher or System UI: an earlier
         * attempt to exclude those swung the total notably *under* Digital Wellbeing's own
         * number, which is evidence its total counts that time too (every app switch briefly
         * passes through the launcher, and that time is real screen-on, unlocked time). Per-app
         * displays (Dashboard rows, widget "top apps") separately filter the same map down to
         * recognized launchable apps for *listing* purposes, but must never re-derive the total
         * from that filtered subset — that's caused repeated "app/widget total doesn't match
         * reality" bugs already, from different call sites filtering the total differently.
         */
        fun totalMinutes(usageMillis: Map<String, Long>): Int =
            usageMillis.values.sumOf { millisToMinutes(it) }
    }
}
