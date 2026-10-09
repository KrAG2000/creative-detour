package com.project.creativedetour.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.project.creativedetour.engine.RejectReason

enum class Outcome { PENDING, ACCEPTED, REJECTED, DISMISSED }

/** One nudge that was shown, and what the user did with it. One row per nudge. */
@Entity(tableName = "feedback")
data class Feedback(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shownAt: Long,
    val hourOfDay: Int,
    val message: String,
    val outcome: Outcome = Outcome.PENDING,
    val reason: RejectReason? = null,
    val respondedAt: Long? = null,
)
