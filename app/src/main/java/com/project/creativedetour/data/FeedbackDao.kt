package com.project.creativedetour.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.project.creativedetour.engine.RejectReason
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackDao {
    @Insert
    suspend fun insert(feedback: Feedback): Long

    @Query("UPDATE feedback SET outcome = :outcome, reason = :reason, respondedAt = :at WHERE id = :id")
    suspend fun respond(id: Long, outcome: Outcome, reason: RejectReason?, at: Long)

    /** Swiping a nudge away only counts if the user hadn't already answered it. */
    @Query("UPDATE feedback SET outcome = 'DISMISSED', respondedAt = :at WHERE id = :id AND outcome = 'PENDING'")
    suspend fun markDismissed(id: Long, at: Long)

    @Query("SELECT * FROM feedback WHERE shownAt >= :since ORDER BY shownAt DESC")
    suspend fun since(since: Long): List<Feedback>

    @Query("SELECT * FROM feedback ORDER BY shownAt DESC LIMIT 1")
    suspend fun latest(): Feedback?

    @Query("SELECT * FROM feedback ORDER BY shownAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<Feedback>>
}
