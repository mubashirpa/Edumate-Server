package app.edumate.server.models.meet

import kotlinx.serialization.Serializable

@Serializable
data class Meet(
    val alternateLink: String? = null,
    val callType: String? = null,
    val courseId: String? = null,
    val creationTime: String? = null,
    val creatorUserId: String? = null,
    val endTime: String? = null,
    val id: String? = null,
    val startTime: String? = null,
    val state: MeetState? = null,
    val updateTime: String? = null,
)
