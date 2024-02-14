package app.edumate.server.models.notification

import kotlinx.serialization.Serializable

@Serializable
data class NotificationRequest(
    val title: String,
    val description: String,
    val userIds: List<String>,
)
