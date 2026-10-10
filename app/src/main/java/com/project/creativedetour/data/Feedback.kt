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
    /** "template" or a model id like "gemma-4-e2b". */
    val writtenBy: String = "template",
    /** Activity.name of what was suggested; feeds ActivityPicker's learning. */
    val activity: String? = null,
    val outcome: Outcome = Outcome.PENDING,
    val reason: RejectReason? = null,
    val respondedAt: Long? = null,
)
