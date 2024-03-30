package app.edumate.server.models.classroom.studentSubmissions

import app.edumate.server.models.classroom.DriveFile
import app.edumate.server.models.classroom.Form
import app.edumate.server.models.classroom.Link
import app.edumate.server.models.classroom.YouTubeVideo
import kotlinx.serialization.Serializable

@Serializable
data class Attachment(
    val driveFile: DriveFile? = null,
    val form: Form? = null,
    val link: Link? = null,
    val youTubeVideo: YouTubeVideo? = null,
)
