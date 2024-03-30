package app.edumate.server.routes

import app.edumate.server.core.utils.DateTimeUtils
import app.edumate.server.core.utils.FirebaseUtils
import app.edumate.server.models.classroom.courseWork.CourseWork
import app.edumate.server.models.classroom.courseWork.CourseWorkType
import app.edumate.server.models.classroom.studentSubmissions.*
import app.edumate.server.plugins.Classroom
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import com.google.firebase.auth.FirebaseAuthException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun Route.studentSubmissionsRouting(
    classroom: Classroom,
    firestore: Firestore,
) {
    val coursesStorage = classroom.coursesStorage

    route("/v1/courses/{courseId}/courseWork/{courseWorkId}/studentSubmissions") {
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
            val courseId =
                call.parameters["courseId"] ?: return@get call.respondText(
                    text = "You must specify a course id",
                    status = HttpStatusCode.BadRequest,
                )
            val courseWorkId =
                call.parameters["courseWorkId"] ?: return@get call.respondText(
                    text = "You must specify a course work id",
                    status = HttpStatusCode.BadRequest,
                )
            val id =
                call.parameters["id"] ?: return@get call.respondText(
                    text = "You must specify a student submission id",
                    status = HttpStatusCode.BadRequest,
                )

            val course =
                coursesStorage.find { it.id == courseId } ?: return@get call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val isStudent = course.students?.any { it.userId == userId } == true
            val havePermission =
                course.teachers?.any { it.userId == userId } == true || isStudent

            if (havePermission) {
                val courseWorkDocument = courseWorkDatabase(firestore, courseId, courseWorkId).get().get()
                val courseWork =
                    courseWorkDocument.toObject(CourseWork::class.java) ?: return@get call.respondText(
                        text = "No course work with id $courseWorkId",
                        status = HttpStatusCode.NotFound,
                    )
                val document = studentSubmissionDatabase(firestore, courseId, courseWorkId, id)
                var studentSubmissionSnapshot = document.get().get()

                if (!studentSubmissionSnapshot.exists() && isStudent) {
                    createStudentSubmission(firestore, courseId, courseWorkId, userId, courseWork.workType)
                    studentSubmissionSnapshot = document.get().get()
                }

                val studentSubmission =
                    studentSubmissionSnapshot.toObject(StudentSubmission::class.java) ?: return@get call.respondText(
                        text = "No student submission with id $id",
                        status = HttpStatusCode.NotFound,
                    )

                call.respond(studentSubmission)
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
            val courseWorkId =
                call.parameters["courseWorkId"] ?: return@get call.respondText(
                    text = "You must specify a course work id",
                    status = HttpStatusCode.BadRequest,
                )
            val late = call.parameters["late"]
            val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: 20
            val page = call.parameters["page"]?.toIntOrNull() ?: 0
            val states = call.parameters.getAll("states")
            val uid = call.parameters["userId"]

            val course =
                coursesStorage.find { it.id == courseId } ?: return@get call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val courseWorkDocument = courseWorkDatabase(firestore, courseId, courseWorkId).get().get()
            if (!courseWorkDocument.exists()) {
                return@get call.respondText(
                    text = "No course work with id $courseWorkId",
                    status = HttpStatusCode.NotFound,
                )
            }
            val havePermission =
                course.teachers?.any { it.userId == userId } == true || course.students?.any { it.userId == userId } == true

            if (havePermission) {
                val studentSubmissions =
                    listSubmissions(
                        firestore = firestore,
                        courseId = courseId,
                        courseWorkId = courseWorkId,
                        lateValues = late?.let { enumValueOf<LateValues>(it) },
                        states = states,
                        userId = uid,
                    )

                call.respond(getStudentSubmissionsDto(studentSubmissions, page, pageSize))
            } else {
                call.respondText(
                    text = "You don't have permission",
                    status = HttpStatusCode.Forbidden,
                )
            }
        }
        post("/{id}:modifyAttachments") {
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
            val courseId =
                call.parameters["courseId"] ?: return@post call.respondText(
                    text = "You must specify a course id",
                    status = HttpStatusCode.BadRequest,
                )
            val courseWorkId =
                call.parameters["courseWorkId"] ?: return@post call.respondText(
                    text = "You must specify a course work id",
                    status = HttpStatusCode.BadRequest,
                )
            val id =
                call.parameters["id"] ?: return@post call.respondText(
                    text = "You must specify a student submission id",
                    status = HttpStatusCode.BadRequest,
                )
            val modifyAttachments = call.receive<ModifyAttachments>()

            coursesStorage.find { it.id == courseId } ?: return@post call.respondText(
                text = "No course with id $courseId",
                status = HttpStatusCode.NotFound,
            )
            val courseWorkDocument = courseWorkDatabase(firestore, courseId, courseWorkId).get().get()
            val courseWork =
                courseWorkDocument.toObject(CourseWork::class.java) ?: return@post call.respondText(
                    text = "No course work with id $courseWorkId",
                    status = HttpStatusCode.NotFound,
                )
            if (courseWork.workType != CourseWorkType.ASSIGNMENT) {
                return@post call.respondText(
                    text = "Attachments may only be added to student submissions belonging to course work objects with a workType of ASSIGNMENT",
                    status = HttpStatusCode.BadRequest,
                )
            }
            val document = studentSubmissionDatabase(firestore, courseId, courseWorkId, id)
            val studentSubmission =
                document.get().get().toObject(StudentSubmission::class.java) ?: return@post call.respondText(
                    text = "No student submission with id $id",
                    status = HttpStatusCode.NotFound,
                )
            val havePermission = studentSubmission.userId == userId

            if (havePermission) {
                val updates: MutableMap<String, Any?> = HashMap()
                updates["assignmentSubmission"] = AssignmentSubmission(attachments = modifyAttachments.addAttachments)
                updates["updateTime"] = DateTimeUtils.getCurrentDateTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

                document.update(updates).get()
                val updatedStudentSubmission =
                    document.get().get().toObject(StudentSubmission::class.java) ?: return@post call.respondText(
                        text = "No student submission with id $id",
                        status = HttpStatusCode.NotFound,
                    )

                call.respond(updatedStudentSubmission)
            } else {
                call.respondText(
                    text = "You don't have permission",
                    status = HttpStatusCode.Forbidden,
                )
            }
        }
        patch("/{id}") {
            val token =
                call.request.header(HttpHeaders.Authorization) ?: return@patch call.respondText(
                    text = "No token provided",
                    status = HttpStatusCode.Unauthorized,
                )
            if (token.isBlank()) {
                return@patch call.respondText(
                    text = "Only valid authentication supported",
                    status = HttpStatusCode.BadRequest,
                )
            }
            val userId =
                try {
                    FirebaseUtils.getUserIdFromToken(token)
                } catch (e: FirebaseAuthException) {
                    when (e.authErrorCode.name) {
                        "EXPIRED_ID_TOKEN" -> return@patch call.respondText(
                            text = "The access token is expired",
                            status = HttpStatusCode.Unauthorized,
                        )

                        "REVOKED_ID_TOKEN" -> return@patch call.respondText(
                            text = "The access token has been revoked",
                            status = HttpStatusCode.Unauthorized,
                        )

                        else -> return@patch call.respondText(
                            text = "The provided access token is not valid",
                            status = HttpStatusCode.Unauthorized,
                        )
                    }
                }
            val courseId =
                call.parameters["courseId"] ?: return@patch call.respondText(
                    text = "You must specify a course id",
                    status = HttpStatusCode.BadRequest,
                )
            val courseWorkId =
                call.parameters["courseWorkId"] ?: return@patch call.respondText(
                    text = "You must specify a course work id",
                    status = HttpStatusCode.BadRequest,
                )
            val id =
                call.parameters["id"] ?: return@patch call.respondText(
                    text = "You must specify a student submission id",
                    status = HttpStatusCode.BadRequest,
                )
            val updateMask =
                call.parameters["updateMask"] ?: return@patch call.respondText(
                    text = "Update mask is required",
                    status = HttpStatusCode.BadRequest,
                )
            val studentSubmission = call.receive<StudentSubmission>()

            val course =
                coursesStorage.find { it.id == courseId } ?: return@patch call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val courseWorkDocument = courseWorkDatabase(firestore, courseId, courseWorkId).get().get()
            if (!courseWorkDocument.exists()) {
                return@patch call.respondText(
                    text = "No course work with id $courseWorkId",
                    status = HttpStatusCode.NotFound,
                )
            }
            val document = studentSubmissionDatabase(firestore, courseId, courseWorkId, id)
            val currentStudentSubmission =
                document.get().get().toObject(StudentSubmission::class.java) ?: return@patch call.respondText(
                    text = "No student submission with id $id",
                    status = HttpStatusCode.NotFound,
                )
            val havePermission =
                course.teachers?.any { it.userId == userId } == true || currentStudentSubmission.userId == userId

            if (havePermission) {
                val updates: MutableMap<String, Any?> = HashMap()
                if (updateMask.contains("draftGrade")) {
                    updates["draftGrade"] = studentSubmission.draftGrade
                }
                if (updateMask.contains("assignedGrade")) {
                    updates["assignedGrade"] = studentSubmission.assignedGrade
                }
                if (updateMask.contains("multipleChoiceSubmission.answer")) {
                    updates["multipleChoiceSubmission"] = studentSubmission.multipleChoiceSubmission
                }
                if (updateMask.contains("shortAnswerSubmission.answer")) {
                    updates["shortAnswerSubmission"] = studentSubmission.shortAnswerSubmission
                }
                updates["updateTime"] = DateTimeUtils.getCurrentDateTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

                document.update(updates).get()
                val updatedStudentSubmission =
                    document.get().get().toObject(StudentSubmission::class.java) ?: return@patch call.respondText(
                        text = "No student submission with id $id",
                        status = HttpStatusCode.NotFound,
                    )

                call.respond(updatedStudentSubmission)
            } else {
                call.respondText(
                    text = "You don't have permission",
                    status = HttpStatusCode.Forbidden,
                )
            }
        }
        post("/{id}:reclaim") {
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
            val courseId =
                call.parameters["courseId"] ?: return@post call.respondText(
                    text = "You must specify a course id",
                    status = HttpStatusCode.BadRequest,
                )
            val courseWorkId =
                call.parameters["courseWorkId"] ?: return@post call.respondText(
                    text = "You must specify a course work id",
                    status = HttpStatusCode.BadRequest,
                )
            val id =
                call.parameters["id"] ?: return@post call.respondText(
                    text = "You must specify a student submission id",
                    status = HttpStatusCode.BadRequest,
                )

            coursesStorage.find { it.id == courseId } ?: return@post call.respondText(
                text = "No course with id $courseId",
                status = HttpStatusCode.NotFound,
            )
            val courseWorkDocument = courseWorkDatabase(firestore, courseId, courseWorkId).get().get()
            if (!courseWorkDocument.exists()) {
                return@post call.respondText(
                    text = "No course work with id $courseWorkId",
                    status = HttpStatusCode.NotFound,
                )
            }
            val document = studentSubmissionDatabase(firestore, courseId, courseWorkId, id)
            val studentSubmission =
                document.get().get().toObject(StudentSubmission::class.java) ?: return@post call.respondText(
                    text = "No student submission with id $id",
                    status = HttpStatusCode.NotFound,
                )
            if (studentSubmission.state != SubmissionState.TURNED_IN) {
                return@post call.respondText(
                    text = "This method is only available for student submissions that have been turned in.",
                    status = HttpStatusCode.Forbidden,
                )
            }
            val havePermission = studentSubmission.userId == userId

            if (havePermission) {
                val updates: MutableMap<String, Any?> = HashMap()
                updates["state"] = SubmissionState.RECLAIMED_BY_STUDENT
                updates["updateTime"] = DateTimeUtils.getCurrentDateTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

                document.update(updates).get()

                call.respond(HttpStatusCode.OK)
            } else {
                call.respondText(
                    text = "You don't have permission",
                    status = HttpStatusCode.Forbidden,
                )
            }
        }
        post("/{id}:return") {
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
            val courseId =
                call.parameters["courseId"] ?: return@post call.respondText(
                    text = "You must specify a course id",
                    status = HttpStatusCode.BadRequest,
                )
            val courseWorkId =
                call.parameters["courseWorkId"] ?: return@post call.respondText(
                    text = "You must specify a course work id",
                    status = HttpStatusCode.BadRequest,
                )
            val id =
                call.parameters["id"] ?: return@post call.respondText(
                    text = "You must specify a student submission id",
                    status = HttpStatusCode.BadRequest,
                )

            val course =
                coursesStorage.find { it.id == courseId } ?: return@post call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val courseWorkDocument = courseWorkDatabase(firestore, courseId, courseWorkId).get().get()
            if (!courseWorkDocument.exists()) {
                return@post call.respondText(
                    text = "No course work with id $courseWorkId",
                    status = HttpStatusCode.NotFound,
                )
            }
            val document = studentSubmissionDatabase(firestore, courseId, courseWorkId, id)
            if (!document.get().get().exists()) {
                return@post call.respondText(
                    text = "No student submission with id $id",
                    status = HttpStatusCode.NotFound,
                )
            }
            val havePermission = course.teachers?.any { it.userId == userId } == true

            if (havePermission) {
                val updates: MutableMap<String, Any?> = HashMap()
                updates["state"] = SubmissionState.RETURNED
                updates["updateTime"] = DateTimeUtils.getCurrentDateTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

                document.update(updates).get()

                call.respond(HttpStatusCode.OK)
            } else {
                call.respondText(
                    text = "You don't have permission",
                    status = HttpStatusCode.Forbidden,
                )
            }
        }
        post("/{id}:turnIn") {
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
            val courseId =
                call.parameters["courseId"] ?: return@post call.respondText(
                    text = "You must specify a course id",
                    status = HttpStatusCode.BadRequest,
                )
            val courseWorkId =
                call.parameters["courseWorkId"] ?: return@post call.respondText(
                    text = "You must specify a course work id",
                    status = HttpStatusCode.BadRequest,
                )
            val id =
                call.parameters["id"] ?: return@post call.respondText(
                    text = "You must specify a student submission id",
                    status = HttpStatusCode.BadRequest,
                )

            coursesStorage.find { it.id == courseId } ?: return@post call.respondText(
                text = "No course with id $courseId",
                status = HttpStatusCode.NotFound,
            )
            val courseWorkDocument = courseWorkDatabase(firestore, courseId, courseWorkId).get().get()
            val courseWork =
                courseWorkDocument.toObject(CourseWork::class.java) ?: return@post call.respondText(
                    text = "No course work with id $courseWorkId",
                    status = HttpStatusCode.NotFound,
                )
            val dueDate = courseWork.dueDate
            val dueTime = courseWork.dueTime
            // TODO: Add late value on turn in
            val late =
                if (dueDate == null || dueTime == null) {
                    null
                } else {
                    val dueDateTime = DateTimeUtils.parseDueDateTime(dueDate, dueTime)
                    if (dueDateTime != null) {
                        DateTimeUtils.isPast(dueDateTime)
                    } else {
                        null
                    }
                }
            val document = studentSubmissionDatabase(firestore, courseId, courseWorkId, id)
            val studentSubmission =
                document.get().get().toObject(StudentSubmission::class.java) ?: return@post call.respondText(
                    text = "No student submission with id $id",
                    status = HttpStatusCode.NotFound,
                )
            val havePermission = studentSubmission.userId == userId

            if (havePermission) {
                val updates: MutableMap<String, Any?> = HashMap()
                updates["late"] = late
                updates["state"] = SubmissionState.TURNED_IN
                updates["updateTime"] = DateTimeUtils.getCurrentDateTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

                document.update(updates).get()

                call.respond(HttpStatusCode.OK)
            } else {
                call.respondText(
                    text = "You don't have permission",
                    status = HttpStatusCode.Forbidden,
                )
            }
        }
    }
}

private fun courseWorkDatabase(
    firestore: Firestore,
    courseId: String,
    id: String,
): DocumentReference {
    return firestore
        .collection("courses").document(courseId)
        .collection("courseWork").document(id)
}

private fun studentSubmissionDatabase(
    firestore: Firestore,
    courseId: String,
    courseWorkId: String,
    id: String,
): DocumentReference {
    return firestore
        .collection("courses").document(courseId)
        .collection("courseWork").document(courseWorkId)
        .collection("studentSubmissions").document(id)
}

private fun createStudentSubmission(
    firestore: Firestore,
    courseId: String,
    courseWorkId: String,
    userId: String,
    courseWorkType: CourseWorkType?,
) {
    val creationTime = DateTimeUtils.getCurrentDateTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val state = SubmissionState.CREATED
    val alternateLink =
        "https://classroom.google.com/c/$courseId/a/$courseWorkId/submissions/by-status/and-sort-last-name/student/$userId"
    val assignmentSubmission =
        if (courseWorkType == CourseWorkType.ASSIGNMENT) {
            AssignmentSubmission()
        } else {
            null
        }
    val multipleChoiceSubmission =
        if (courseWorkType == CourseWorkType.MULTIPLE_CHOICE_QUESTION) {
            MultipleChoiceSubmission()
        } else {
            null
        }
    val shortAnswerSubmission =
        if (courseWorkType == CourseWorkType.SHORT_ANSWER_QUESTION) {
            ShortAnswerSubmission()
        } else {
            null
        }

    val createdStudentSubmission =
        StudentSubmission(
            courseId = courseId,
            courseWorkId = courseWorkId,
            id = userId,
            userId = userId,
            creationTime = creationTime,
            updateTime = creationTime,
            state = state,
            alternateLink = alternateLink,
            courseWorkType = courseWorkType,
            assignmentSubmission = assignmentSubmission,
            multipleChoiceSubmission = multipleChoiceSubmission,
            shortAnswerSubmission = shortAnswerSubmission,
        )

    studentSubmissionDatabase(firestore, courseId, courseWorkId, userId).set(createdStudentSubmission).get()
}

private fun listSubmissions(
    firestore: Firestore,
    courseId: String,
    courseWorkId: String,
    lateValues: LateValues?,
    states: List<String>?,
    userId: String?,
): List<StudentSubmission> {
    val document =
        firestore
            .collection("courses").document(courseId)
            .collection("courseWork").document(courseWorkId)
            .collection("studentSubmissions")
    var query: Query = document

    when (lateValues) {
        LateValues.LATE_ONLY -> query = query.whereEqualTo("late", true)
        LateValues.NOT_LATE_ONLY -> query = query.whereEqualTo("late", false)
        else -> {}
    }

    if (!states.isNullOrEmpty()) {
        query = query.whereIn("state", states)
    }

    userId?.let {
        query = query.whereEqualTo("userId", it)
    }

    val result = query.get().get()
    val studentSubmissions = result.toObjects(StudentSubmission::class.java)
    return studentSubmissions
}

private fun getStudentSubmissionsDto(
    items: List<StudentSubmission>,
    currentPage: Int,
    pageSize: Int,
): StudentSubmissionsDto {
    val totalItems = items.size
    val totalPages = (totalItems + pageSize - 1) / pageSize

    if (currentPage < 0 || currentPage >= totalPages) {
        return StudentSubmissionsDto(studentSubmissions = emptyList())
    }

    val startIndex = currentPage * pageSize
    val endIndex = minOf(startIndex + pageSize, totalItems)

    val itemsForPage = items.subList(startIndex, endIndex)
    val nextPage = if (currentPage < totalPages - 1) currentPage + 1 else null

    return StudentSubmissionsDto(
        nextPage = nextPage,
        studentSubmissions = itemsForPage,
    )
}

enum class LateValues {
    LATE_ONLY,
    LATE_VALUES_UNSPECIFIED,
    NOT_LATE_ONLY,
}

@Serializable
data class ModifyAttachments(
    val addAttachments: List<Attachment>? = null,
)
