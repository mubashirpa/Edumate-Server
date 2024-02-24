package app.edumate.server.models.classroom.courses

import kotlinx.serialization.Serializable

@Serializable
data class CoursesDto(
    val courses: List<Course>? = null,
    val nextPageToken: String? = null,
)
