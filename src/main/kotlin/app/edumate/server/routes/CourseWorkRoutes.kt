package app.edumate.server.routes

import app.edumate.server.core.utils.DatabaseUtils
import app.edumate.server.core.utils.DateTimeUtils
import app.edumate.server.core.utils.FirebaseUtils
import app.edumate.server.dao.DAOFacade
import app.edumate.server.models.classroom.AssigneeMode
import app.edumate.server.models.classroom.courseWork.CourseWork
import app.edumate.server.models.classroom.courseWork.CourseWorkDto
import app.edumate.server.models.classroom.courseWork.CourseWorkState
import app.edumate.server.models.classroom.courseWork.SubmissionModificationMode
import app.edumate.server.plugins.Classroom
import com.google.firebase.auth.FirebaseAuthException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.courseWorkRouting(
    daoFacade: DAOFacade,
    classroom: Classroom,
) {
    val coursesStorage = classroom.coursesStorage

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
                    text = "You must specify a courseId",
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
                val id = courseWork.id ?: DatabaseUtils.generateId(12)
                val assigneeMode = courseWork.assigneeMode ?: AssigneeMode.ALL_STUDENTS
                val state = courseWork.state ?: CourseWorkState.DRAFT
                val submissionModificationMode =
                    courseWork.submissionModificationMode ?: SubmissionModificationMode.MODIFIABLE_UNTIL_TURNED_IN
                val time = DateTimeUtils.getCurrentDateTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                val newCourseWork =
                    CourseWork(
                        alternateLink = "https://classroom.google.com/c/$courseId/a/$id/details",
                        assigneeMode = assigneeMode,
                        assignment = courseWork.assignment,
                        associatedWithDeveloper = courseWork.associatedWithDeveloper,
                        courseId = courseId,
                        creationTime = time,
                        creatorUserId = userId,
                        description = courseWork.description,
                        dueDate = courseWork.dueDate,
                        dueTime = courseWork.dueTime,
                        gradeCategory = courseWork.gradeCategory,
                        id = id,
                        individualStudentsOptions = courseWork.individualStudentsOptions,
                        materials = courseWork.materials,
                        maxPoints = courseWork.maxPoints,
                        multipleChoiceQuestion = courseWork.multipleChoiceQuestion,
                        scheduledTime = courseWork.scheduledTime,
                        state = state,
                        submissionModificationMode = submissionModificationMode,
                        title = title,
                        topicId = courseWork.topicId,
                        updateTime = time,
                        workType = workType,
                    )

                daoFacade.createCourseWork(newCourseWork)
                call.respond(
                    status = HttpStatusCode.Created,
                    message = newCourseWork,
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
                    text = "You must specify a courseId",
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
                if (daoFacade.deleteCourseWork(id)) {
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
                    text = "You must specify a courseId",
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
            val havePermission =
                course.teachers?.any { it.userId == userId } == true || course.students?.any { it.userId == userId } == true

            if (havePermission) {
                val courseWork = daoFacade.getCourseWork(id)
                if (courseWork != null) {
                    call.respond(courseWork)
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
                    text = "You must specify a courseId",
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
                val courseWorks =
                    daoFacade.listCourseWorks(courseId)
                        .filter { courseWork ->
                            courseWorkStates.any { courseWorkState ->
                                courseWork.state?.name == courseWorkState
                            }
                        }
                        .sort(orderBy)
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
                    text = "You must specify a courseId",
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
            val havePermission =
                course.teachers?.any { it.userId == userId } == true

            if (havePermission) {
                val oldCourseWork = daoFacade.getCourseWork(id)
                val updatedCourseWork =
                    oldCourseWork?.copy(
                        title = if (updateMask.contains("title")) courseWork.title else oldCourseWork.title,
                        description = if (updateMask.contains("description")) courseWork.description else oldCourseWork.description,
                        state = if (updateMask.contains("state")) courseWork.state else oldCourseWork.state,
                        dueDate = if (updateMask.contains("dueDate")) courseWork.dueDate else oldCourseWork.dueDate,
                        dueTime = if (updateMask.contains("dueTime")) courseWork.dueTime else oldCourseWork.dueTime,
                        maxPoints = if (updateMask.contains("maxPoints")) courseWork.maxPoints else oldCourseWork.maxPoints,
                        scheduledTime =
                            if (updateMask.contains("scheduledTime")) {
                                courseWork.scheduledTime
                            } else {
                                oldCourseWork.scheduledTime
                            },
                        submissionModificationMode =
                            if (updateMask.contains("submissionModificationMode")) {
                                courseWork.submissionModificationMode
                            } else {
                                oldCourseWork.submissionModificationMode
                            },
                        topicId = if (updateMask.contains("topicId")) courseWork.topicId else oldCourseWork.topicId,
                        updateTime = DateTimeUtils.getCurrentDateTime("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
                    )

                if (updatedCourseWork != null && daoFacade.patchCourseWork(id, updatedCourseWork)) {
                    call.respond(updatedCourseWork)
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
    }
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