package app.edumate.server.models.classroom

import kotlinx.serialization.Serializable

@Serializable
data class IndividualStudentsOptions(
    val studentIds: List<String>? = null,
)
