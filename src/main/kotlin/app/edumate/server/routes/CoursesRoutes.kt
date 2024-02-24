package app.edumate.server.routes

import app.edumate.server.models.classroom.courses.Course
import app.edumate.server.models.classroom.courses.CourseState
import app.edumate.server.models.classroom.courses.CoursesDto
import app.edumate.server.models.userProfiles.UserProfile
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import java.io.FileInputStream
import java.io.InputStreamReader

fun Route.coursesRouting() {
    val coursesStorage: MutableList<Course> = fetchCourses().toMutableList()
    val usersStorage: MutableList<UserProfile> = fetchUsers().toMutableList()

    route("/v1/courses") {
        post {
            val token =
                call.request.header(HttpHeaders.Authorization) ?: return@post call.respondText(
                    text = "No token provided",
                    status = HttpStatusCode.Unauthorized,
                )
            if (token.isBlank()) {
                return@post call.respondText(
                    text = "Only valid authentication supported",
                    status = HttpStatusCode.BadRequest,
                )
            }

            val course = call.receive<Course>()
            coursesStorage.add(course)
            call.respond(
                status = HttpStatusCode.Created,
                message = course,
            )
        }
        delete("/{id}") {
            val token =
                call.request.header(HttpHeaders.Authorization) ?: return@delete call.respondText(
                    text = "No token provided",
                    status = HttpStatusCode.Unauthorized,
                )
            if (token.isBlank()) {
                return@delete call.respondText(
                    text = "Only valid authentication supported",
                    status = HttpStatusCode.BadRequest,
                )
            }

            val id =
                call.parameters["id"] ?: return@delete call.respondText(
                    text = "You must specify an id",
                    status = HttpStatusCode.BadRequest,
                )

            if (coursesStorage.removeIf { it.id == id && it.ownerId == token }) {
                call.respondText(text = "The course with id $id was deleted", status = HttpStatusCode.Accepted)
            } else if (coursesStorage.any { it.id == id }) {
                call.respondText(
                    text = "You don't have permission to delete the course with id $id",
                    status = HttpStatusCode.Forbidden,
                )
            } else {
                call.respondText(text = "No course with id $id", status = HttpStatusCode.NotFound)
            }
        }
        get("/{id}") {
            val token =
                call.request.header(HttpHeaders.Authorization) ?: return@get call.respondText(
                    text = "No token provided",
                    status = HttpStatusCode.Unauthorized,
                )
            if (token.isBlank()) {
                return@get call.respondText(
                    text = "Only valid authentication supported",
                    status = HttpStatusCode.BadRequest,
                )
            }

            val id =
                call.parameters["id"] ?: return@get call.respondText(
                    text = "You must specify an id",
                    status = HttpStatusCode.BadRequest,
                )
            val course =
                coursesStorage.find { it.id == id } ?: return@get call.respondText(
                    text = "No course with id $id",
                    status = HttpStatusCode.NotFound,
                )

            if (course.students?.any { it.userId == token } == true || course.teachers?.any { it.userId == token } == true) {
                call.respond(course)
            } else {
                call.respondText(
                    text = "You don't have permission to view this course",
                    status = HttpStatusCode.Forbidden,
                )
            }
        }
        get {
            val token =
                call.request.header(HttpHeaders.Authorization) ?: return@get call.respondText(
                    text = "No token provided",
                    status = HttpStatusCode.Unauthorized,
                )
            if (token.isBlank()) {
                return@get call.respondText(
                    text = "Only valid authentication supported",
                    status = HttpStatusCode.BadRequest,
                )
            }

            val courseStates =
                call.parameters.getAll("courseStates") ?: listOf(
                    CourseState.ACTIVE.name,
                    CourseState.ARCHIVED.name,
                    CourseState.PROVISIONED.name,
                    CourseState.DECLINED.name,
                )
            val studentId = call.parameters["studentId"]
            val teacherId = call.parameters["teacherId"]

            val courses =
                listCourses(
                    token = token,
                    coursesStorage = coursesStorage,
                    usersStorage = usersStorage,
                    courseStates = courseStates,
                    studentId = studentId,
                    teacherId = teacherId,
                )

            if (coursesStorage.isNotEmpty()) {
                val courseDto = CoursesDto(courses = courses)
                call.respond(courseDto)
            } else {
                call.respondText(text = "No courses found", status = HttpStatusCode.OK)
            }
        }
        put("/{id}") {
            val token =
                call.request.header(HttpHeaders.Authorization) ?: return@put call.respondText(
                    text = "No token provided",
                    status = HttpStatusCode.Unauthorized,
                )
            if (token.isBlank()) {
                return@put call.respondText(
                    text = "Only valid authentication supported",
                    status = HttpStatusCode.BadRequest,
                )
            }

            val id =
                call.parameters["id"] ?: return@put call.respondText(
                    text = "You must specify an id",
                    status = HttpStatusCode.BadRequest,
                )
            val index = coursesStorage.indexOfFirst { it.id == id }

            if (index == -1) {
                call.respondText(text = "No course with id $id", status = HttpStatusCode.NotFound)
            } else {
                val updatedCourse = call.receive<Course>()
                if (coursesStorage[index].ownerId == token) {
                    println("hello: ${coursesStorage.size}")
                    coursesStorage[index] = updatedCourse
                    call.respond(coursesStorage[index])
                    println("hello: ${coursesStorage.size}")
                    println("hello: ${coursesStorage[index]}")
                } else {
                    call.respondText(
                        text = "You don't have permission to update this course",
                        status = HttpStatusCode.Forbidden,
                    )
                }
            }
        }
    }
}

private fun listCourses(
    token: String,
    coursesStorage: MutableList<Course>,
    usersStorage: MutableList<UserProfile>,
    courseStates: List<String>,
    studentId: String?,
    teacherId: String?,
): List<Course> {
    val courses: List<Course> =
        when {
            studentId != null ->
                coursesStorage.filter {
                    courseStates.any { courseState ->
                        it.courseState?.name == courseState
                    }
                }.filter {
                    it.students?.any { student ->
                        student.userId == studentId
                    } == true
                }

            teacherId != null ->
                coursesStorage.filter {
                    courseStates.any { courseState ->
                        it.courseState?.name == courseState
                    }
                }.filter {
                    it.teachers?.any { teacher ->
                        teacher.userId == teacherId
                    } == true
                }

            else ->
                coursesStorage.toList().filter {
                    courseStates.any { courseState ->
                        it.courseState?.name == courseState
                    }
                }.filter {
                    it.ownerId == token
                }
        }

    courses.forEach { course ->
        val owner = usersStorage.find { it.id == course.ownerId }
        course.owner = owner
        course.students?.forEach { student ->
            val user = usersStorage.find { it.id == student.userId }
            user?.let {
                student.profile = user
            }
        }
        course.teachers?.forEach { teacher ->
            val user = usersStorage.find { it.id == teacher.userId }
            user?.let {
                teacher.profile = user
            }
        }
    }
    return courses
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
