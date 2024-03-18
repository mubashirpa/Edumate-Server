package app.edumate.server.models.classroom.announcements

import kotlinx.serialization.Serializable

@Serializable
data class AnnouncementsDto(
    val announcements: List<Announcement>? = null,
    val nextPage: Int? = null,
)
