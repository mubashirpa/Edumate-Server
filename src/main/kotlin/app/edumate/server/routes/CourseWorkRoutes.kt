package app.edumate.server.routes

import app.edumate.server.models.classroom.AssigneeMode
import app.edumate.server.models.classroom.courseWork.*
import app.edumate.server.models.notification.Aliases
import app.edumate.server.models.notification.Message
import app.edumate.server.models.notification.Request
import app.edumate.server.services.OneSignalService
import app.edumate.server.utils.Classroom
import app.edumate.server.utils.DatabaseUtils
import app.edumate.server.utils.DateTimeUtils
import app.edumate.server.utils.FirebaseUtils
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.Firestore
import com.google.firebase.auth.FirebaseAuthException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.courseWorkRouting(
    classroom: Classroom,
    firestore: Firestore,
    oneSignalAppId: String,
    oneSignalService: OneSignalService,
) {
    val coursesStorage = classroom.coursesStorage
    val usersStorage = classroom.usersStorage

    route("/v1/courses/{courseId}/courseWork") {
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
            val courseWork = call.receive<CourseWork>()

            val course =
                coursesStorage.find { it.id == courseId } ?: return@post call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val havePermission = course.teachers?.any { it.userId == userId } == true

            if (havePermission) {
                val title =
                    courseWork.title ?: return@post call.respondText(
                        text = "Title is required",
                        status = HttpStatusCode.BadRequest,
                    )
                val workType =
                    courseWork.workType ?: return@post call.respondText(
                        text = "The workType field is required",
                        status = HttpStatusCode.BadRequest,
                    )
                val multipleChoiceQuestion = courseWork.multipleChoiceQuestion
                if (workType == CourseWorkType.MULTIPLE_CHOICE_QUESTION) {
                    multipleChoiceQuestion ?: return@post call.respondText(
                        text = "Multiple choice field must be populated for work of type \"MULTIPLE_CHOICE_QUESTION\"",
                        status = HttpStatusCode.BadRequest,
                    )
                    if (multipleChoiceQuestion.choices.isNullOrEmpty()) {
                        return@post call.respondText(
                            text = "At least one choice must be specified for work of type \"MULTIPLE_CHOICE_QUESTION\"",
                            status = HttpStatusCode.BadRequest,
                        )
                    }
                }
                val id = courseWork.id ?: DatabaseUtils.generateId(12)
                val state = courseWork.state ?: CourseWorkState.DRAFT
                val creationTime = DateTimeUtils.getCurrentDateTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                val submissionModificationMode =
                    courseWork.submissionModificationMode ?: SubmissionModificationMode.MODIFIABLE_UNTIL_TURNED_IN
                val assignment = courseWork.assignment
                val associatedWithDeveloper = courseWork.associatedWithDeveloper
                val assigneeMode = courseWork.assigneeMode ?: AssigneeMode.ALL_STUDENTS

                val newCourseWork =
                    CourseWork(
                        courseId = courseId,
                        id = id,
                        title = title,
                        state = state,
                        creationTime = creationTime,
                        updateTime = creationTime,
                        workType = workType,
                        submissionModificationMode = submissionModificationMode,
                        assignment = assignment,
                        multipleChoiceQuestion = multipleChoiceQuestion,
                        associatedWithDeveloper = associatedWithDeveloper,
                        assigneeMode = assigneeMode,
                        creatorUserId = userId, // [END]
                        alternateLink = "https://classroom.google.com/c/$courseId/a/$id/details",
                        description = courseWork.description,
                        dueDate = courseWork.dueDate,
                        dueTime = courseWork.dueTime,
                        gradeCategory = courseWork.gradeCategory,
                        individualStudentsOptions = courseWork.individualStudentsOptions,
                        materials = courseWork.materials,
                        maxPoints = courseWork.maxPoints,
                        scheduledTime = courseWork.scheduledTime,
                        topicId = courseWork.topicId,
                    )

                courseWorkDatabase(firestore, courseId, id).set(newCourseWork).get()

                call.respond(
                    status = HttpStatusCode.Created,
                    message = newCourseWork,
                )

                val externalIds = course.students?.mapNotNull { it.userId } ?: emptyList()
                val owner = usersStorage.find { it.id == userId }
                val heading: String
                val content: String
                when (workType) {
                    CourseWorkType.ASSIGNMENT -> {
                        heading = "${owner?.name?.fullName} Posted a New Assignment!"
                        content =
                            "A new assignment for your ${course.name} class has been posted by your teacher. Check it out and submit your work before the deadline."
                    }

                    CourseWorkType.MULTIPLE_CHOICE_QUESTION, CourseWorkType.SHORT_ANSWER_QUESTION -> {
                        heading = "${owner?.name?.fullName} Posted a New Question!"
                        content =
                            "Your teacher has posted a new question for your ${course.name} class. Take a moment to answer and participate in the discussion."
                    }

                    else -> {
                        heading = "${owner?.name?.fullName} Uploaded New Study Material!"
                        content =
                            "Your teacher has uploaded new study material for your ${course.name} class. Access it now to enhance your understanding of the topic."
                    }
                }
                oneSignalService.send(
                    Request(
                        appId = oneSignalAppId,
                        includedSegments = emptyList(),
                        includeAliases = Aliases(externalId = externalIds),
                        targetChannel = "Classwork",
                        contents = Message(en = content),
                        headings = Message(en = heading),
                    ),
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
                    text = "You must specify a course work id",
                    status = HttpStatusCode.BadRequest,
                )

            val course =
                coursesStorage.find { it.id == courseId } ?: return@delete call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val havePermission = course.teachers?.any { it.userId == userId } == true

            if (havePermission) {
                val document = courseWorkDatabase(firestore, courseId, id)

                if (document.get().get().exists()) {
                    document.delete().get()

                    call.respondText(
                        text = "The course work with id $id was deleted",
                        status = HttpStatusCode.Accepted,
                    )
                } else {
                    call.respondText(text = "No course work with id $id", status = HttpStatusCode.NotFound)
                }
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
                    text = "You must specify a course work id",
                    status = HttpStatusCode.BadRequest,
                )

            val course =
                coursesStorage.find { it.id == courseId } ?: return@get call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val document = courseWorkDatabase(firestore, courseId, id).get().get()
            val courseWork =
                document.toObject(CourseWork::class.java) ?: return@get call.respondText(
                    text = "No course work with id $id",
                    status = HttpStatusCode.NotFound,
                )
            val havePermission =
                course.teachers?.any { it.userId == userId } == true || course.students?.any { it.userId == userId } == true

            if (havePermission) {
                call.respond(courseWork)
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
            val courseWorkStates =
                call.parameters.getAll("courseWorkStates") ?: listOf(CourseWorkState.PUBLISHED.name)
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
                        .collection("courseWork")
                        .whereIn("state", courseWorkStates)
                        .get().get()
                val courseWorks = document.toObjects(CourseWork::class.java).sort(orderBy)

                call.respond(getCourseWorkDto(courseWorks, page, pageSize))
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
                    text = "You must specify a course work id",
                    status = HttpStatusCode.BadRequest,
                )
            val updateMask =
                call.parameters["updateMask"] ?: return@patch call.respondText(
                    text = "Update mask is required",
                    status = HttpStatusCode.BadRequest,
                )
            val courseWork = call.receive<CourseWork>()

            val course =
                coursesStorage.find { it.id == courseId } ?: return@patch call.respondText(
                    text = "No course with id $courseId",
                    status = HttpStatusCode.NotFound,
                )
            val document = courseWorkDatabase(firestore, courseId, id)
            val havePermission = course.teachers?.any { it.userId == userId } == true

            if (document.get().get().exists()) {
                if (havePermission) {
                    val updates: MutableMap<String, Any?> = HashMap()
                    if (updateMask.contains("title")) {
                        updates["title"] = courseWork.title
                    }
                    if (updateMask.contains("description")) {
                        updates["description"] = courseWork.description
                    }
                    if (updateMask.contains("state")) {
                        updates["state"] = courseWork.state
                    }
                    if (updateMask.contains("dueDate")) {
                        updates["dueDate"] = courseWork.dueDate
                    }
                    if (updateMask.contains("dueTime")) {
                        updates["dueTime"] = courseWork.dueTime
                    }
                    if (updateMask.contains("maxPoints")) {
                        updates["maxPoints"] = courseWork.maxPoints
                    }
                    if (updateMask.contains("scheduledTime")) {
                        updates["scheduledTime"] = courseWork.scheduledTime
                    }
                    if (updateMask.contains("submissionModificationMode")) {
                        updates["submissionModificationMode"] = courseWork.submissionModificationMode
                    }
                    if (updateMask.contains("topicId")) {
                        updates["topicId"] = courseWork.topicId
                    }
                    updates["materials"] = courseWork.materials
                    updates["multipleChoiceQuestion"] = courseWork.multipleChoiceQuestion
                    updates["updateTime"] = DateTimeUtils.getCurrentDateTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

                    document.update(updates).get()
                    val updatedCourseWork =
                        document.get().get().toObject(CourseWork::class.java) ?: return@patch call.respondText(
                            text = "No course work with id $id",
                            status = HttpStatusCode.NotFound,
                        )

                    call.respond(updatedCourseWork)
                } else {
                    call.respondText(
                        text = "You don't have permission",
                        status = HttpStatusCode.Forbidden,
                    )
                }
            } else {
                call.respondText(
                    text = "No course work with id $id",
                    status = HttpStatusCode.NotFound,
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

private fun getCourseWorkDto(
    items: List<CourseWork>,
    currentPage: Int,
    pageSize: Int,
): CourseWorkDto {
    val totalItems = items.size
    val totalPages = (totalItems + pageSize - 1) / pageSize

    if (currentPage < 0 || currentPage >= totalPages) {
        return CourseWorkDto(courseWork = emptyList())
    }

    val startIndex = currentPage * pageSize
    val endIndex = minOf(startIndex + pageSize, totalItems)

    val itemsForPage = items.subList(startIndex, endIndex)
    val nextPage = if (currentPage < totalPages - 1) currentPage + 1 else null

    return CourseWorkDto(
        courseWork = itemsForPage,
        nextPage = nextPage,
    )
}

private fun List<CourseWork>.sort(orderBy: String): List<CourseWork> {
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
