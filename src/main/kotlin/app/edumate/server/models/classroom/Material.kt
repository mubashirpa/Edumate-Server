package app.edumate.server.models.classroom

import kotlinx.serialization.Serializable

@Serializable
data class Material(
    val driveFile: DriveFile? = null,
    val form: Form? = null,
    val link: Link? = null,
    val youTubeVideo: YouTubeVideo? = null,
)
