package org.bxkr.octodiary.database.entity.offline

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ranking_members")
data class RankingMemberEntity(
    @PrimaryKey
    val personId: String,
    val json: String // Full object JSON
)