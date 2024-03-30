package app.edumate.server.models.classroom.announcements

import app.edumate.server.models.classroom.AssigneeMode
import app.edumate.server.models.classroom.IndividualStudentsOptions
import app.edumate.server.models.classroom.Material
import app.edumate.server.models.userProfiles.UserProfile
import kotlinx.serialization.Serializable

@Serializable
data class Announcement(
    val alternateLink: String? = null,
    val assigneeMode: AssigneeMode? = null,
    val courseId: String? = null,
    val creationTime: String? = null,
    var creator: UserProfile? = null, // Extra
    val creatorUserId: String? = null,
    val id: String? = null,
    val individualStudentsOptions: IndividualStudentsOptions? = null,
    val materials: List<Material>? = null,
    val scheduledTime: String? = null,
    val state: AnnouncementState? = null,
    val text: String? = null,
    val updateTime: String? = null,
)
