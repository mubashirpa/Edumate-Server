package app.edumate.server.models.classroom.studentSubmissions

import app.edumate.server.models.classroom.Link
import kotlinx.serialization.Serializable

@Serializable
data class Attachment(
    val link: Link? = null,
)
