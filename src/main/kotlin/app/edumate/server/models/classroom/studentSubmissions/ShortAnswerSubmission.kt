package app.edumate.server.models.classroom.studentSubmissions

import kotlinx.serialization.Serializable

@Serializable
data class ShortAnswerSubmission(
    val answer: String? = null,
)
