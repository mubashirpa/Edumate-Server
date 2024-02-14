package app.edumate.server.models.courses

import kotlinx.serialization.Serializable

@Serializable
data class CoursesDto(
    val courses: List<Course>? = null,
    val nextPageToken: String? = null,
)
