package app.edumate.server.models.classroom.studentSubmissions

import app.edumate.server.models.classroom.DriveFile
import app.edumate.server.models.classroom.Link
import kotlinx.serialization.Serializable

@Serializable
data class Attachment(
    val driveFile: DriveFile? = null,
    val link: Link? = null,
)
