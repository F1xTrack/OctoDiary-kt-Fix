package org.bxkr.octodiary.database.entity.offline

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey
    val id: Long,
    val startAt: Long,
    val finishAt: Long,
    val json: String // Full object JSON
)
