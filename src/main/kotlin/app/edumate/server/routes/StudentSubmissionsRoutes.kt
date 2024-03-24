package app.edumate.server.routes

import app.edumate.server.core.utils.DateTimeUtils
import app.edumate.server.core.utils.FirebaseUtils
import app.edumate.server.models.classroom.courseWork.CourseWork
import app.edumate.server.models.classroom.courseWork.CourseWorkType
import app.edumate.server.models.classroom.studentSubmissions.AssignmentSubmission
import app.edumate.server.models.classroom.studentSubmissions.ModifyAttachments
import app.edumate.server.models.classroom.studentSubmissions.StudentSubmission
import app.edumate.server.models.classroom.studentSubmissions.SubmissionState
import app.edumate.server.plugins.Classroom
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.Firestore
import com.google.firebase.auth.FirebaseAuthException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
            val courseWorkDocument = courseWorkDatabase(firestore, courseId, id).get().get()
            if (!courseWorkDocument.exists()) {
                return@get call.respondText(
                    text = "No course work with id $id",
                    status = HttpStatusCode.NotFound,
                )
            }
            val document = studentSubmissionDatabase(firestore, courseId, courseWorkId, id).get().get()
            val studentSubmission =
                document.toObject(StudentSubmission::class.java) ?: return@get call.respondText(
                    text = "No student submission with id $id",
                    status = HttpStatusCode.NotFound,
                )
            val havePermission =
                course.teachers?.any { it.userId == userId } == true || course.students?.any { it.userId == userId } == true

            if (havePermission) {
                call.respond(studentSubmission)
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
            val courseWorkDocument = courseWorkDatabase(firestore, courseId, id).get().get()
            val courseWork =
                courseWorkDocument.toObject(CourseWork::class.java) ?: return@post call.respondText(
                    text = "No course work with id $id",
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
            val courseWorkDocument = courseWorkDatabase(firestore, courseId, id).get().get()
            if (!courseWorkDocument.exists()) {
                return@patch call.respondText(
                    text = "No course work with id $id",
                    status = HttpStatusCode.NotFound,
                )
            }
            val document = studentSubmissionDatabase(firestore, courseId, courseWorkId, id)
            val havePermission =
                course.teachers?.any { it.userId == userId } == true

            if (document.get().get().exists()) {
                if (havePermission) {
                    val updates: MutableMap<String, Any?> = HashMap()
                    if (updateMask.contains("draftGrade")) {
                        updates["draftGrade"] = studentSubmission.draftGrade
                    }
                    if (updateMask.contains("assignedGrade")) {
                        updates["assignedGrade"] = studentSubmission.assignedGrade
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
            } else {
                call.respondText(
                    text = "No student submission with id $id",
                    status = HttpStatusCode.NotFound,
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
            val courseWorkDocument = courseWorkDatabase(firestore, courseId, id).get().get()
            if (!courseWorkDocument.exists()) {
                return@post call.respondText(
                    text = "No course work with id $id",
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
            val courseWorkDocument = courseWorkDatabase(firestore, courseId, id).get().get()
            if (!courseWorkDocument.exists()) {
                return@post call.respondText(
                    text = "No course work with id $id",
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
            val courseWorkDocument = courseWorkDatabase(firestore, courseId, id).get().get()
            if (!courseWorkDocument.exists()) {
                return@post call.respondText(
                    text = "No course work with id $id",
                    status = HttpStatusCode.NotFound,
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
