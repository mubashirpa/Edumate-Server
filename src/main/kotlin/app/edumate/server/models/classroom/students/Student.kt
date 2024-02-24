package app.edumate.server.models.classroom.students

import app.edumate.server.models.classroom.DriveFolder
import app.edumate.server.models.userProfiles.UserProfile
import kotlinx.serialization.Serializable

@Serializable
data class Student(
    val courseId: String? = null,
    var profile: UserProfile? = null,
    val studentWorkFolder: DriveFolder? = null,
    val userId: String? = null,
)
