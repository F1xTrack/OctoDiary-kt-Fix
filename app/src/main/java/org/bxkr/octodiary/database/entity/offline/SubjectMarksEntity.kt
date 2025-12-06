package org.bxkr.octodiary.database.entity.offline

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subject_marks")
data class SubjectMarksEntity(
    @PrimaryKey
    val subjectId: Long,
    val json: String // Full object JSON
)