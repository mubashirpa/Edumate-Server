package app.edumate.server.models.classroom

import kotlinx.serialization.Serializable

@Serializable
data class DriveFolder(
    val alternateLink: String? = null,
    val id: String? = null,
    val title: String? = null,
)
