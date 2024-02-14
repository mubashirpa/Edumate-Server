package app.edumate.server.models.userProfiles

import kotlinx.serialization.Serializable

@Serializable
data class Name(
    val firstName: String? = null,
    val fullName: String? = null,
    val lastName: String? = null,
)
