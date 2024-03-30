package app.edumate.server.models.classroom

import kotlinx.serialization.Serializable

@Serializable
data class YouTubeVideo(
    val alternateLink: String? = null,
    val id: String? = null,
    val thumbnailUrl: String? = null,
    val title: String? = null,
)
