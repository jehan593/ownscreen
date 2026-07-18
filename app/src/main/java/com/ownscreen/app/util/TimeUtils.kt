package com.ownscreen.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeUtils {

    /** Epoch millis for the start of "today" in the device's current default timezone/locale. */
    fun localMidnightEpochMillis(now: Long = System.currentTimeMillis()): Long =
        LocalDate.ofEpochDay(localEpochDay(now))
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    /**
     * A day number (days since 1970-01-01 in the device's local calendar) that increases by
     * exactly 1 at each local midnight; used to detect day rollover and to key/display history
     * records. Deliberately computed via [LocalDate] rather than raw millisecond division —
     * floor-dividing a UTC instant by 86_400_000 is subtly wrong for timezones ahead of UTC
     * (local midnight can floor-divide into the *previous* UTC day), which would make this
     * number off-by-one right around midnight for a large fraction of the world.
     */
    fun localEpochDay(now: Long = System.currentTimeMillis()): Long =
        Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

    fun formatHoursMinutes(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    /** Human-friendly label for a history day: "Today", "Yesterday", or a short date like "Jul 15". */
    fun formatDayLabel(epochDay: Long): String {
        val today = localEpochDay()
        return when (epochDay) {
            today -> "Today"
            today - 1 -> "Yesterday"
            else -> LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("MMM d"))
        }
    }
}
