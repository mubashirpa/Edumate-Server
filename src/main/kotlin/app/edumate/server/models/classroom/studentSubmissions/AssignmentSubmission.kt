package app.edumate.server.models.classroom.studentSubmissions

import kotlinx.serialization.Serializable

@Serializable
data class AssignmentSubmission(
    val attachments: List<Attachment>? = null,
)
