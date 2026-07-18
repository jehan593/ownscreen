package com.ownscreen.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A manual block persists until the user explicitly unblocks it — no automatic day-based expiry. */
@Entity(tableName = "app_suspend_state")
data class AppSuspendStateEntity(
    @PrimaryKey val packageName: String,
    val isSuspended: Boolean,
    /** Minutes of usage recorded for this package at the moment it was blocked — the baseline
     *  for detecting whether the block is actually taking effect: OwnDroid's broadcast API gives
     *  no confirmation, so if this package keeps accumulating *new* usage well past this baseline
     *  (real suspension would prevent it from launching at all), that's evidence the block isn't
     *  really in effect — e.g. a wrong OwnDroid API key. */
    val usageMinutesAtBlockTime: Int,
    val lastChangedAtEpochMillis: Long
)
