package com.ownscreen.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** The Default mode always exists with a fixed id (see [DEFAULT_MODE_ID]) and an empty app set —
 *  activating it is how all mode-owned blocks get restored, with no special-cased code path. */
@Entity(tableName = "mode")
data class ModeEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val isDefault: Boolean,
    /** When true, both saving changes to this mode and switching away from it while it's the
     *  active mode require solving a [com.ownscreen.app.util.MathChallenge] first — the same
     *  friction-on-the-way-out mechanic already used for unblocking a single app. Always false
     *  for the Default mode (it has no edit screen to set it from). */
    @ColumnInfo(defaultValue = "0") val requireTrivia: Boolean = false
) {
    companion object {
        const val DEFAULT_MODE_ID = 0L
    }
}
