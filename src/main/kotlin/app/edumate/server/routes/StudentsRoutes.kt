package app.edumate.server.routes

import app.edumate.server.models.classroom.courses.Course
import app.edumate.server.models.classroom.students.Student
import app.edumate.server.models.classroom.students.StudentsDto
import app.edumate.server.models.userProfiles.UserProfile
import app.edumate.server.utils.Classroom
import app.edumate.server.utils.FirebaseUtils
import app.edumate.server.utils.ListUtils
import com.google.firebase.auth.FirebaseAuthException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.studentsRouting(classroom: Classroom) {
    val coursesStorage = classroom.coursesStorage
    val usersStorage = classroom.usersStorage

    route("/v1/courses/{courseId}/students") {
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
            val courseId =
                call.parameters["courseId"] ?: return@post call.respondText(
                    text = "You must specify a course id",
                    status = HttpStatusCode.BadRequest,
                )
            // TODO(" If the requester is a student, the enrollmentCode is required.")
            val enrollmentCode = call.parameters["enrollmentCode"]
            val student = call.receive<Student>()

            val course =
                coursesStorage.find { it.id == courseId } ?: return@post call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val alreadyMember =
                course.students?.any { it.userId == student.userId } == true ||
                    course.teachers?.any { it.userId == student.userId } == true
            val newStudent =
                Student(
                    courseId = courseId,
                    profile = usersStorage.find { it.id == student.userId },
                    userId = student.userId,
                )

            if (alreadyMember) {
                call.respondText(
                    text = "User is already a member of this course",
                    status = HttpStatusCode.Conflict,
                )
            } else {
                if (course.students == null) {
                    course.students = mutableListOf()
                }
                course.students?.add(student)

                call.respond(
                    status = HttpStatusCode.Created,
                    message = newStudent,
                )
            }
        }
        delete("/{userId}") {
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
            val courseId =
                call.parameters["courseId"] ?: return@delete call.respondText(
                    text = "You must specify a course id",
                    status = HttpStatusCode.BadRequest,
                )
            val deleteUserId =
                call.parameters["userId"] ?: return@delete call.respondText(
                    text = "You must specify a user id",
                    status = HttpStatusCode.BadRequest,
                )

            val course =
                coursesStorage.find { it.id == courseId } ?: return@delete call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val havePermission =
                course.teachers?.any { it.userId == userId } == true ||
                    (course.students?.any { it.userId == userId } == true && deleteUserId == userId)

            if (havePermission) {
                if (course.students?.removeIf { it.userId == deleteUserId } == true) {
                    call.respondText(
                        text = "The user with id $deleteUserId was deleted",
                        status = HttpStatusCode.Accepted,
                    )
                } else {
                    call.respondText(text = "No user with id $deleteUserId", status = HttpStatusCode.NotFound)
                }
            } else {
                call.respondText(
                    text = "You don't have permission to remove the user",
                    status = HttpStatusCode.Forbidden,
                )
            }
        }
        get("/{userId}") {
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
            val courseId =
                call.parameters["courseId"] ?: return@get call.respondText(
                    text = "You must specify a course id",
                    status = HttpStatusCode.BadRequest,
                )
            val getUserId =
                call.parameters["userId"] ?: return@get call.respondText(
                    text = "You must specify a user id",
                    status = HttpStatusCode.BadRequest,
                )

            val course =
                coursesStorage.find { it.id == courseId } ?: return@get call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val havePermission = course.teachers?.any { it.userId == userId } == true

            if (havePermission) {
                if (course.students?.any { it.userId == getUserId } == true) {
                    call.respond(
                        Student(
                            courseId = courseId,
                            profile = usersStorage.find { it.id == getUserId },
                            userId = getUserId,
                        ),
                    )
                } else {
                    call.respondText(text = "No user with id $getUserId", status = HttpStatusCode.NotFound)
                }
            } else {
                call.respondText(
                    text = "You don't have permission",
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
            val courseId =
                call.parameters["courseId"] ?: return@get call.respondText(
                    text = "You must specify a course id",
                    status = HttpStatusCode.BadRequest,
                )
            val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 20
            val page = call.parameters["page"]?.toIntOrNull() ?: 0

            val course =
                coursesStorage.find { it.id == courseId } ?: return@get call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val havePermission =
                course.teachers?.any { it.userId == userId } == true || course.students?.any { it.userId == userId } == true

            if (havePermission) {
                val students = listStudents(usersStorage, course, userId)

                call.respond(getStudentsDto(students, page, pageSize))
            } else {
                call.respondText(
                    text = "You don't have permission",
                    status = HttpStatusCode.Forbidden,
                )
            }
        }
    }
}

private fun listStudents(
    usersStorage: MutableList<UserProfile>,
    course: Course,
    userId: String,
): List<Student> {
    val students: MutableList<Student> = mutableListOf()
    course.students?.forEach { student ->
        val user = usersStorage.find { it.id == student.userId }
        students.add(
            Student(
                courseId = course.id,
                profile = user,
                userId = user?.id,
            ),
        )
    }
    ListUtils.moveToIndex(students, 0) { it.userId == userId }
    return students
}

private fun getStudentsDto(
    items: List<Student>,
    currentPage: Int,
    pageSize: Int,
): StudentsDto {
    val totalItems = items.size
    val totalPages = (totalItems + pageSize - 1) / pageSize

    if (currentPage < 0 || currentPage >= totalPages) {
        return StudentsDto(students = emptyList())
    }

    val startIndex = currentPage * pageSize
    val endIndex = minOf(startIndex + pageSize, totalItems)

    val itemsForPage = items.subList(startIndex, endIndex)
    val nextPage = if (currentPage < totalPages - 1) currentPage + 1 else null

    return StudentsDto(
        nextPage = nextPage,
        students = itemsForPage,
    )
}
