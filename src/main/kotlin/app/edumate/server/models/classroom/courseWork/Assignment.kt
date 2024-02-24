package app.edumate.server.models.classroom.courseWork

import app.edumate.server.models.classroom.DriveFolder
import kotlinx.serialization.Serializable

@Serializable
data class Assignment(
    val studentWorkFolder: DriveFolder? = null,
)
