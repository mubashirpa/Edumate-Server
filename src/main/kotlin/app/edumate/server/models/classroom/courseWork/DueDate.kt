package app.edumate.server.models.classroom.courseWork

import kotlinx.serialization.Serializable

@Serializable
data class DueDate(
    val day: Int? = null,
    val month: Int? = null,
    val year: Int? = null,
)
