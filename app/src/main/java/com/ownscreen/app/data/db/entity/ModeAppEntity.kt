package com.ownscreen.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Join table: which packages belong to a mode's block set. Cascade-deletes with its mode. */
@Entity(
    tableName = "mode_app",
    primaryKeys = ["modeId", "packageName"],
    foreignKeys = [
        ForeignKey(
            entity = ModeEntity::class,
            parentColumns = ["id"],
            childColumns = ["modeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("modeId")]
)
data class ModeAppEntity(
    val modeId: Long,
    val packageName: String
)
