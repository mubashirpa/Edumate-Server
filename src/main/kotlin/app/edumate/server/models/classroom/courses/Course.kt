package app.edumate.server.models.classroom.courses

import app.edumate.server.models.classroom.students.Student
import app.edumate.server.models.classroom.teachers.Teacher
import app.edumate.server.models.userProfiles.UserProfile
import kotlinx.serialization.Serializable

@Serializable
data class Course(
    val alternateLink: String? = null,
    val courseState: CourseState? = null,
    val creationTime: String? = null,
    val description: String? = null,
    val enrollmentCode: String? = null,
    val id: String? = null,
    val name: String? = null,
    var owner: UserProfile? = null,
    val ownerId: String? = null,
    val photoUrl: String? = null,
    val room: String? = null,
    val section: String? = null,
    var students: MutableList<Student>? = null,
    val subject: String? = null,
    var teachers: MutableList<Teacher>? = null,
    val updateTime: String? = null,
)
