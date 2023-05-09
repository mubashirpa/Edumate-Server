package app.edumate.server.models

import kotlinx.serialization.Serializable

@Serializable
data class NotificationMessage(
    val en: String
)