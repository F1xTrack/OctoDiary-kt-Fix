package org.bxkr.octodiary.database.entity.offline

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visits")
data class VisitDayEntity(
    @PrimaryKey
    val date: String,
    val json: String // Full object JSON (Payload)
)