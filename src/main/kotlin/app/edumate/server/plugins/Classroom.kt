package app.edumate.server.plugins

import app.edumate.server.models.classroom.courseWork.CourseWork
import app.edumate.server.models.classroom.courses.Course
import app.edumate.server.models.userProfiles.UserProfile
import kotlinx.serialization.json.Json
import java.io.FileInputStream
import java.io.InputStreamReader

class Classroom {
    val coursesStorage: MutableList<Course> = fetchCourses().toMutableList()
    val courseWorkStorage: MutableList<CourseWork> = mutableListOf()
    val usersStorage: MutableList<UserProfile> = fetchUsers().toMutableList()
    val images =
        listOf(
            "https://gstatic.com/classroom/themes/Geography_thumb.jpg",
            "https://gstatic.com/classroom/themes/Writing_thumb.jpg",
            "https://gstatic.com/classroom/themes/Math_thumb.jpg",
            "https://gstatic.com/classroom/themes/Chemistry_thumb.jpg",
            "https://gstatic.com/classroom/themes/Physics_thumb.jpg",
            "https://gstatic.com/classroom/themes/Psychology_thumb.jpg",
            "https://gstatic.com/classroom/themes/img_graduation_thumb.jpg",
            "https://gstatic.com/classroom/themes/SocialStudies_thumb.jpg",
        )
}

private val json = Json { ignoreUnknownKeys = true }

private fun fetchCourses(): List<Course> {
    val coursesFile = FileInputStream("src/main/resources/courses-db.json")
    val coursesData = InputStreamReader(coursesFile).readText()
    return json.decodeFromString(coursesData)
}

private fun fetchUsers(): List<UserProfile> {
    val userFile = FileInputStream("src/main/resources/users-db.json")
    val userData = InputStreamReader(userFile).readText()
    return json.decodeFromString(userData)
}
