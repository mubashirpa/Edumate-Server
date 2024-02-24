package app.edumate.server.models.classroom.courseWork

import kotlinx.serialization.Serializable

@Serializable
data class MultipleChoiceQuestion(
    val choices: List<String>? = null,
)
