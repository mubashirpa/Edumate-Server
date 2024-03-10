package app.edumate.server.routes

import app.edumate.server.core.utils.DatabaseUtils
import app.edumate.server.core.utils.DateTimeUtils
import app.edumate.server.core.utils.FirebaseUtils
import app.edumate.server.models.classroom.courses.Course
import app.edumate.server.models.classroom.courses.CourseState
import app.edumate.server.models.classroom.courses.CoursesDto
import app.edumate.server.models.classroom.teachers.Teacher
import app.edumate.server.models.userProfiles.UserProfile
import app.edumate.server.plugins.Classroom
import com.google.firebase.auth.FirebaseAuthException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.coursesRouting(classroom: Classroom) {
    val coursesStorage = classroom.coursesStorage
    val usersStorage = classroom.usersStorage

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
            val userId =
                try {
                    FirebaseUtils.getUserIdFromToken(token)
                } catch (e: FirebaseAuthException) {
                    when (e.authErrorCode.name) {
                        "EXPIRED_ID_TOKEN" -> return@post call.respondText(
                            text = "The access token is expired",
                            status = HttpStatusCode.Unauthorized,
                        )

                        "REVOKED_ID_TOKEN" -> return@post call.respondText(
                            text = "The access token has been revoked",
                            status = HttpStatusCode.Unauthorized,
                        )

                        else -> return@post call.respondText(
                            text = "The provided access token is not valid",
                            status = HttpStatusCode.Unauthorized,
                        )
                    }
                }
            val course = call.receive<Course>()

            val id = DatabaseUtils.generateId(12)
            val teachers = mutableListOf(Teacher(userId = userId))
            val time = DateTimeUtils.getCurrentDateTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            val createdCourse =
                Course(
                    alternateLink = "https://classroom.google.com/c/$id",
                    courseState = course.courseState ?: CourseState.PROVISIONED,
                    creationTime = time,
                    enrollmentCode = null, // TODO
                    id = id,
                    name = course.name,
                    ownerId = userId,
                    photoUrl = classroom.images.random(),
                    room = course.room,
                    section = course.section,
                    students = mutableListOf(),
                    subject = course.subject,
                    teachers = teachers,
                    updateTime = time,
                )

            coursesStorage.add(createdCourse)
            call.respond(
                status = HttpStatusCode.Created,
                message = createdCourse,
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
            val userId =
                try {
                    FirebaseUtils.getUserIdFromToken(token)
                } catch (e: FirebaseAuthException) {
                    when (e.authErrorCode.name) {
                        "EXPIRED_ID_TOKEN" -> return@delete call.respondText(
                            text = "The access token is expired",
                            status = HttpStatusCode.Unauthorized,
                        )

                        "REVOKED_ID_TOKEN" -> return@delete call.respondText(
                            text = "The access token has been revoked",
                            status = HttpStatusCode.Unauthorized,
                        )

                        else -> return@delete call.respondText(
                            text = "The provided access token is not valid",
                            status = HttpStatusCode.Unauthorized,
                        )
                    }
                }
            val id =
                call.parameters["id"] ?: return@delete call.respondText(
                    text = "You must specify an id",
                    status = HttpStatusCode.BadRequest,
                )

            if (coursesStorage.removeIf { it.id == id && it.ownerId == userId }) {
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
            val userId =
                try {
                    FirebaseUtils.getUserIdFromToken(token)
                } catch (e: FirebaseAuthException) {
                    when (e.authErrorCode.name) {
                        "EXPIRED_ID_TOKEN" -> return@get call.respondText(
                            text = "The access token is expired",
                            status = HttpStatusCode.Unauthorized,
                        )

                        "REVOKED_ID_TOKEN" -> return@get call.respondText(
                            text = "The access token has been revoked",
                            status = HttpStatusCode.Unauthorized,
                        )

                        else -> return@get call.respondText(
                            text = "The provided access token is not valid",
                            status = HttpStatusCode.Unauthorized,
                        )
                    }
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
            val havePermission =
                course.students?.any { it.userId == userId } == true || course.teachers?.any { it.userId == userId } == true

            if (havePermission) {
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
            println("hello: $token") // TODO("Remove")
            if (token.isBlank()) {
                return@get call.respondText(
                    text = "Only valid authentication supported",
                    status = HttpStatusCode.BadRequest,
                )
            }
            val userId =
                try {
                    FirebaseUtils.getUserIdFromToken(token)
                } catch (e: FirebaseAuthException) {
                    when (e.authErrorCode.name) {
                        "EXPIRED_ID_TOKEN" -> return@get call.respondText(
                            text = "The access token is expired",
                            status = HttpStatusCode.Unauthorized,
                        )

                        "REVOKED_ID_TOKEN" -> return@get call.respondText(
                            text = "The access token has been revoked",
                            status = HttpStatusCode.Unauthorized,
                        )

                        else -> return@get call.respondText(
                            text = "The provided access token is not valid",
                            status = HttpStatusCode.Unauthorized,
                        )
                    }
                }
            val courseStates =
                call.parameters.getAll("courseStates") ?: listOf(
                    CourseState.ACTIVE.name,
                    CourseState.ARCHIVED.name,
                    CourseState.PROVISIONED.name,
                    CourseState.DECLINED.name,
                )
            val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 20
            val page = call.parameters["page"]?.toIntOrNull() ?: 0
            val studentId = call.parameters["studentId"]
            val teacherId = call.parameters["teacherId"]

            val courses =
                listCourses(
                    userId = userId,
                    coursesStorage = coursesStorage,
                    usersStorage = usersStorage,
                    courseStates = courseStates,
                    studentId = studentId,
                    teacherId = teacherId,
                )

            if (coursesStorage.isNotEmpty()) {
                call.respond(getCoursesDto(courses, page, pageSize))
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
            val userId =
                try {
                    FirebaseUtils.getUserIdFromToken(token)
                } catch (e: FirebaseAuthException) {
                    when (e.authErrorCode.name) {
                        "EXPIRED_ID_TOKEN" -> return@put call.respondText(
                            text = "The access token is expired",
                            status = HttpStatusCode.Unauthorized,
                        )

                        "REVOKED_ID_TOKEN" -> return@put call.respondText(
                            text = "The access token has been revoked",
                            status = HttpStatusCode.Unauthorized,
                        )

                        else -> return@put call.respondText(
                            text = "The provided access token is not valid",
                            status = HttpStatusCode.Unauthorized,
                        )
                    }
                }
            val id =
                call.parameters["id"] ?: return@put call.respondText(
                    text = "You must specify an id",
                    status = HttpStatusCode.BadRequest,
                )
            val updatedCourse = call.receive<Course>()

            val index = coursesStorage.indexOfFirst { it.id == id }

            if (index == -1) {
                call.respondText(text = "No course with id $id", status = HttpStatusCode.NotFound)
            } else {
                val havePermission = coursesStorage[index].teachers?.any { it.userId == userId } == true
                if (havePermission) {
                    coursesStorage[index] = updatedCourse
                    call.respond(coursesStorage[index])
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
    userId: String,
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
                    it.teachers?.any { teacher ->
                        teacher.userId == userId
                    } == true ||
                        it.students?.any { student ->
                            student.userId == userId
                        } == true
                }
        }

    courses.forEach { course ->
        val owner = usersStorage.find { it.id == course.ownerId }
        val courseId = course.id

        course.owner = owner
        course.students?.forEach { student ->
            val user = usersStorage.find { it.id == student.userId }

            student.courseId = courseId
            user?.let {
                student.profile = user
            }
        }
        course.teachers?.forEach { teacher ->
            val user = usersStorage.find { it.id == teacher.userId }

            teacher.courseId = courseId
            user?.let {
                teacher.profile = user
            }
        }
    }
    return courses
}

private fun getCoursesDto(
    items: List<Course>,
    currentPage: Int,
    pageSize: Int,
): CoursesDto {
    val totalItems = items.size
    val totalPages = (totalItems + pageSize - 1) / pageSize

    if (currentPage < 0 || currentPage >= totalPages) {
        return CoursesDto(courses = emptyList())
    }

    val startIndex = currentPage * pageSize
    val endIndex = minOf(startIndex + pageSize, totalItems)

    val itemsForPage = items.subList(startIndex, endIndex)
    val nextPage = if (currentPage < totalPages - 1) currentPage + 1 else null

    return CoursesDto(
        courses = itemsForPage,
        nextPage = nextPage,
    )
}
