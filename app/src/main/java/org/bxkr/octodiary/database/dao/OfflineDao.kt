package org.bxkr.octodiary.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.bxkr.octodiary.database.entity.offline.EventEntity
import org.bxkr.octodiary.database.entity.offline.RankingMemberEntity
import org.bxkr.octodiary.database.entity.offline.SubjectMarksEntity
import org.bxkr.octodiary.database.entity.offline.VisitDayEntity

@Dao
interface OfflineDao {
    // Events
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Query("DELETE FROM events")
    suspend fun clearEvents()

    @Query("SELECT * FROM events WHERE startAt >= :start AND finishAt <= :end")
    suspend fun getEvents(start: String, end: String): List<EventEntity>

    @Query("SELECT * FROM events")
    suspend fun getAllEvents(): List<EventEntity>

    // Marks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjectMarks(marks: List<SubjectMarksEntity>)

    @Query("DELETE FROM subject_marks")
    suspend fun clearSubjectMarks()

    @Query("SELECT * FROM subject_marks")
    suspend fun getAllSubjectMarks(): List<SubjectMarksEntity>

    // Ranking
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRankingMembers(members: List<RankingMemberEntity>)

    @Query("DELETE FROM ranking_members")
    suspend fun clearRankingMembers()

    @Query("SELECT * FROM ranking_members")
    suspend fun getAllRankingMembers(): List<RankingMemberEntity>

    // Visits
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisits(visits: List<VisitDayEntity>)

    @Query("DELETE FROM visits")
    suspend fun clearVisits()

    @Query("SELECT * FROM visits ORDER BY date DESC")
    suspend fun getAllVisits(): List<VisitDayEntity>
}
