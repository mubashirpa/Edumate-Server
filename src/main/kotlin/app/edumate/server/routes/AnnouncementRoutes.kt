package app.edumate.server.routes

import app.edumate.server.core.utils.DatabaseUtils
import app.edumate.server.core.utils.DateTimeUtils
import app.edumate.server.core.utils.FirebaseUtils
import app.edumate.server.models.classroom.AssigneeMode
import app.edumate.server.models.classroom.announcements.Announcement
import app.edumate.server.models.classroom.announcements.AnnouncementState
import app.edumate.server.models.classroom.announcements.AnnouncementsDto
import app.edumate.server.plugins.Classroom
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.Firestore
import com.google.firebase.auth.FirebaseAuthException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.announcementRouting(
    classroom: Classroom,
    firestore: Firestore,
) {
    val coursesStorage = classroom.coursesStorage

    route("/v1/courses/{courseId}/announcements") {
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
            val courseId =
                call.parameters["courseId"] ?: return@post call.respondText(
                    text = "You must specify a course id",
                    status = HttpStatusCode.BadRequest,
                )
            val announcement = call.receive<Announcement>()

            val course =
                coursesStorage.find { it.id == courseId } ?: return@post call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val havePermission =
                course.teachers?.any { it.userId == userId } == true || course.students?.any { it.userId == userId } == true

            if (havePermission) {
                val text =
                    announcement.text ?: return@post call.respondText(
                        text = "Text is required",
                        status = HttpStatusCode.BadRequest,
                    )
                val id = announcement.id ?: DatabaseUtils.generateId(12)
                val state = announcement.state ?: AnnouncementState.PUBLISHED
                val alternateLink = "https://classroom.google.com/c/$courseId/p/$id"
                val creationTime = DateTimeUtils.getCurrentDateTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                val materials = announcement.materials

                val createdAnnouncement =
                    Announcement(
                        courseId = courseId,
                        id = id,
                        text = text,
                        state = state,
                        alternateLink = alternateLink,
                        creationTime = creationTime,
                        updateTime = creationTime,
                        creatorUserId = userId, // [END]
                        assigneeMode = AssigneeMode.ALL_STUDENTS,
                        individualStudentsOptions = null,
                        materials = materials,
                    )

                announcementDatabase(firestore, courseId, id).set(createdAnnouncement).get()
                call.respond(
                    status = HttpStatusCode.Created,
                    message = createdAnnouncement,
                )
            } else {
                call.respondText(
                    text = "You don't have permission",
                    status = HttpStatusCode.Forbidden,
                )
            }
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
            val courseId =
                call.parameters["courseId"] ?: return@delete call.respondText(
                    text = "You must specify a course id",
                    status = HttpStatusCode.BadRequest,
                )
            val id =
                call.parameters["id"] ?: return@delete call.respondText(
                    text = "You must specify an announcement id",
                    status = HttpStatusCode.BadRequest,
                )

            val course =
                coursesStorage.find { it.id == courseId } ?: return@delete call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val document = announcementDatabase(firestore, courseId, id)
            val announcement =
                document.get().get().toObject(Announcement::class.java) ?: return@delete call.respondText(
                    text = "No announcement with id $id",
                    status = HttpStatusCode.NotFound,
                )
            val havePermission =
                course.teachers?.any { it.userId == userId } == true || announcement.creatorUserId == userId

            if (havePermission) {
                document.delete().get()

                call.respondText(
                    text = "The announcement with id $id was deleted",
                    status = HttpStatusCode.Accepted,
                )
            } else {
                call.respondText(
                    text = "You don't have permission",
                    status = HttpStatusCode.Forbidden,
                )
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
            val courseId =
                call.parameters["courseId"] ?: return@get call.respondText(
                    text = "You must specify a course id",
                    status = HttpStatusCode.BadRequest,
                )
            val id =
                call.parameters["id"] ?: return@get call.respondText(
                    text = "You must specify an announcement id",
                    status = HttpStatusCode.BadRequest,
                )

            val course =
                coursesStorage.find { it.id == courseId } ?: return@get call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val document = announcementDatabase(firestore, courseId, id).get().get()
            val announcement =
                document.toObject(Announcement::class.java) ?: return@get call.respondText(
                    text = "No announcement with id $id",
                    status = HttpStatusCode.NotFound,
                )
            val havePermission =
                course.teachers?.any { it.userId == userId } == true || course.students?.any { it.userId == userId } == true

            if (havePermission) {
                call.respond(announcement)
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
            val announcementStates =
                call.parameters.getAll("announcementStates") ?: listOf(AnnouncementState.PUBLISHED.name)
            val orderBy = call.parameters["orderBy"] ?: "updateTime desc"
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
                val document =
                    firestore
                        .collection("courses").document(courseId)
                        .collection("announcements")
                        .whereIn("state", announcementStates)
                        .get().get()
                val announcement = document.toObjects(Announcement::class.java).sort(orderBy)

                call.respond(getAnnouncementsDto(announcement, page, pageSize))
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
            val id =
                call.parameters["id"] ?: return@patch call.respondText(
                    text = "You must specify an announcement id",
                    status = HttpStatusCode.BadRequest,
                )
            val updateMask =
                call.parameters["updateMask"] ?: return@patch call.respondText(
                    text = "Update mask is required",
                    status = HttpStatusCode.BadRequest,
                )
            val announcement = call.receive<Announcement>()

            val course =
                coursesStorage.find { it.id == courseId } ?: return@patch call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val document = announcementDatabase(firestore, courseId, id)
            val currentAnnouncement =
                document.get().get().toObject(Announcement::class.java) ?: return@patch call.respondText(
                    text = "No announcement with id $id",
                    status = HttpStatusCode.NotFound,
                )
            val havePermission =
                course.teachers?.any { it.userId == userId } == true || currentAnnouncement.creatorUserId == userId

            if (havePermission) {
                val updates: MutableMap<String, Any?> = HashMap()
                if (updateMask.contains("text")) {
                    updates["text"] = announcement.text
                }
                if (updateMask.contains("state")) {
                    updates["state"] = announcement.state
                }
                if (updateMask.contains("scheduledTime")) {
                    updates["scheduledTime"] = announcement.scheduledTime
                }
                updates["materials"] = announcement.materials
                updates["updateTime"] = DateTimeUtils.getCurrentDateTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

                document.update(updates).get()
                val updatedAnnouncement = document.get().get().toObject(Announcement::class.java)

                if (updatedAnnouncement != null) {
                    call.respond(updatedAnnouncement)
                } else {
                    call.respondText(text = "No announcement with id $id", status = HttpStatusCode.NotFound)
                }
            } else {
                call.respondText(
                    text = "You don't have permission",
                    status = HttpStatusCode.Forbidden,
                )
            }
        }
    }
}

private fun announcementDatabase(
    firestore: Firestore,
    courseId: String,
    id: String,
): DocumentReference {
    return firestore
        .collection("courses").document(courseId)
        .collection("announcements").document(id)
}

private fun getAnnouncementsDto(
    items: List<Announcement>,
    currentPage: Int,
    pageSize: Int,
): AnnouncementsDto {
    val totalItems = items.size
    val totalPages = (totalItems + pageSize - 1) / pageSize

    if (currentPage < 0 || currentPage >= totalPages) {
        return AnnouncementsDto(announcements = emptyList())
    }

    val startIndex = currentPage * pageSize
    val endIndex = minOf(startIndex + pageSize, totalItems)

    val itemsForPage = items.subList(startIndex, endIndex)
    val nextPage = if (currentPage < totalPages - 1) currentPage + 1 else null

    return AnnouncementsDto(
        announcements = itemsForPage,
        nextPage = nextPage,
    )
}

private fun List<Announcement>.sort(orderBy: String): List<Announcement> {
    return when (orderBy) {
        "creationTime asc" -> {
            sortedBy {
                DateTimeUtils.stringToDate(it.creationTime!!, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            }
        }

        "creationTime desc" -> {
            sortedByDescending {
                DateTimeUtils.stringToDate(it.creationTime!!, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            }
        }

        "updateTime asc" -> {
            sortedBy {
                DateTimeUtils.stringToDate(it.updateTime!!, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            }
        }

        else -> {
            sortedByDescending {
                DateTimeUtils.stringToDate(it.updateTime!!, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            }
        }
    }
}
