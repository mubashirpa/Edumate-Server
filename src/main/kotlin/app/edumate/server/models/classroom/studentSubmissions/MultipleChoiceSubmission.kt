package app.edumate.server.models.classroom.studentSubmissions

import kotlinx.serialization.Serializable

@Serializable
data class MultipleChoiceSubmission(
    val answer: String? = null,
)
