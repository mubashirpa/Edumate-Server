package app.edumate.server.routes

import app.edumate.server.models.meet.Meet
import app.edumate.server.models.meet.MeetState
import app.edumate.server.models.notification.Notification
import app.edumate.server.models.notification.NotificationMessage
import app.edumate.server.services.OneSignalService
import app.edumate.server.utils.Classroom
import app.edumate.server.utils.DatabaseUtils
import app.edumate.server.utils.DateTimeUtils
import app.edumate.server.utils.FirebaseUtils
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.database.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*

fun Route.meetRouting(
    classroom: Classroom,
    database: FirebaseDatabase,
    oneSignalAppId: String,
    oneSignalService: OneSignalService,
) {
    val coursesStorage = classroom.coursesStorage

    route("/v1/courses/{courseId}/meet") {
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

            val course =
                coursesStorage.find { it.id == courseId } ?: return@post call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val havePermission = course.teachers?.any { it.userId == userId } == true

            if (havePermission) {
                val id = DatabaseUtils.generateId(12)
                val alternateLink = "https://getstream.io/video/$courseId/join/$id"
                val callType = "default"
                val creationTime = DateTimeUtils.getCurrentDateTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                val state = MeetState.CREATED

                val meet =
                    Meet(
                        alternateLink = alternateLink,
                        callType = callType,
                        courseId = courseId,
                        creationTime = creationTime,
                        creatorUserId = userId,
                        id = id,
                        state = state,
                        updateTime = creationTime,
                    )

                meetDatabase(database, courseId, id).setValueAsync(meet).get()
                call.respond(
                    status = HttpStatusCode.Created,
                    message = meet,
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
                    text = "You must specify a meet id",
                    status = HttpStatusCode.BadRequest,
                )

            val course =
                coursesStorage.find { it.id == courseId } ?: return@delete call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val havePermission = course.teachers?.any { it.userId == userId } == true

            if (havePermission) {
                val reference = meetDatabase(database, courseId, id)

                reference.removeValueAsync().get()

                call.respondText(
                    text = "The meet with id $id was deleted",
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
                    text = "You must specify a meet id",
                    status = HttpStatusCode.BadRequest,
                )

            val course =
                coursesStorage.find { it.id == courseId } ?: return@get call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val havePermission =
                course.teachers?.any { it.userId == userId } == true || course.students?.any { it.userId == userId } == true

            if (havePermission) {
                getMeet(call, database, courseId, id)
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

            val course =
                coursesStorage.find { it.id == courseId } ?: return@get call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val havePermission =
                course.teachers?.any { it.userId == userId } == true || course.students?.any { it.userId == userId } == true

            if (havePermission) {
                listMeet(call, database, courseId)
            } else {
                call.respondText(
                    text = "You don't have permission",
                    status = HttpStatusCode.Forbidden,
                )
            }
        }
        post("/{id}:join") {
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
            val id =
                call.parameters["id"] ?: return@post call.respondText(
                    text = "You must specify a meet id",
                    status = HttpStatusCode.BadRequest,
                )

            val course =
                coursesStorage.find { it.id == courseId } ?: return@post call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val havePermission = course.teachers?.any { it.userId == userId } == true

            if (havePermission) {
                val reference = meetDatabase(database, courseId, id)
                val updates: MutableMap<String, Any> = HashMap()
                val startTime = DateTimeUtils.getCurrentDateTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                updates["startTime"] = startTime
                updates["state"] = MeetState.IN_PROGRESS
                updates["updateTime"] = startTime

                reference.updateChildrenAsync(updates).get()

                call.respond(HttpStatusCode.OK)

                val success =
                    oneSignalService.send(
                        Notification(
                            appId = oneSignalAppId,
                            contents = NotificationMessage(en = "New meeting has started"),
                            headings = NotificationMessage(en = "Meeting started"),
                            includedSegments = emptyList(),
                            includeExternalUserIds = course.students?.map { it.userId.orEmpty() } ?: emptyList(),
                        ),
                    )
                if (success) {
                    println("Notification send successfully")
                } else {
                    println("Notification send failed")
                }
            } else {
                call.respondText(
                    text = "You don't have permission",
                    status = HttpStatusCode.Forbidden,
                )
            }
        }
        post("/{id}:leave") {
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
            val id =
                call.parameters["id"] ?: return@post call.respondText(
                    text = "You must specify a meet id",
                    status = HttpStatusCode.BadRequest,
                )

            val course =
                coursesStorage.find { it.id == courseId } ?: return@post call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val havePermission = course.teachers?.any { it.userId == userId } == true

            if (havePermission) {
                val reference = meetDatabase(database, courseId, id)
                val updates: MutableMap<String, Any> = HashMap()
                val endTime = DateTimeUtils.getCurrentDateTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                updates["endTime"] = endTime
                updates["state"] = MeetState.DISCONNECTED
                updates["updateTime"] = endTime

                reference.updateChildrenAsync(updates).get()

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

private fun meetDatabase(
    database: FirebaseDatabase,
    courseId: String,
    id: String,
): DatabaseReference {
    return database.getReference(courseId).child("meet").child(id)
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun getMeet(
    call: ApplicationCall,
    database: FirebaseDatabase,
    courseId: String,
    id: String,
) {
    val reference = meetDatabase(database, courseId, id)
    val snapshot =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val listener =
                    object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot?) {
                            continuation.resume(dataSnapshot) {
                                // Clean-up code if needed
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError?) {
                            continuation.resume(null) {
                                // Clean-up code if needed
                            }
                        }
                    }
                reference.addListenerForSingleValueEvent(listener)
                continuation.invokeOnCancellation {
                    reference.removeEventListener(listener)
                }
            }
        }
    val meet = snapshot?.getValue(Meet::class.java)

    if (meet != null) {
        call.respond(meet)
    } else {
        call.respondText(
            text = "No meet with id $id",
            status = HttpStatusCode.NotFound,
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun listMeet(
    call: ApplicationCall,
    database: FirebaseDatabase,
    courseId: String,
) {
    val reference = database.getReference(courseId).child("meet")
    val snapshot =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val listener =
                    object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot?) {
                            continuation.resume(dataSnapshot) {
                                // Clean-up code if needed
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError?) {
                            continuation.resume(null) {
                                // Clean-up code if needed
                            }
                        }
                    }
                reference.addListenerForSingleValueEvent(listener)
                continuation.invokeOnCancellation {
                    reference.removeEventListener(listener)
                }
            }
        }
    val meet: MutableList<Meet> = mutableListOf()
    if (snapshot != null) {
        for (item in snapshot.children) {
            meet.add(item.getValue(Meet::class.java))
        }
    }
    val first = meet.firstOrNull()

    if (first != null) {
        call.respond(first)
    } else {
        call.respondText(
            text = "No meet found",
            status = HttpStatusCode.NotFound,
        )
    }
}
