package app.edumate.server.models.classroom.studentSubmissions

import kotlinx.serialization.Serializable

@Serializable
data class ModifyAttachments(
    val addAttachments: List<Attachment>? = null,
)
