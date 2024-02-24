package app.edumate.server.models.classroom.courseWork

import kotlinx.serialization.Serializable

@Serializable
data class CourseWorkDto(
    val courseWork: List<CourseWork>? = null,
    val nextPageToken: String? = null,
)
