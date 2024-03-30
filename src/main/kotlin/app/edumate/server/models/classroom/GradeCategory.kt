package app.edumate.server.models.classroom

import kotlinx.serialization.Serializable

@Serializable
data class GradeCategory(
    val defaultGradeDenominator: Int? = null,
    val id: String? = null,
    val name: String? = null,
    val weight: Int? = null,
)
