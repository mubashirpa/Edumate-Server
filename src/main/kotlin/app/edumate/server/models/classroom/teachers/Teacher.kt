package app.edumate.server.models.classroom.teachers

import app.edumate.server.models.userProfiles.UserProfile
import kotlinx.serialization.Serializable

@Serializable
data class Teacher(
    var courseId: String? = null,
    var profile: UserProfile? = null,
    val userId: String? = null,
)
