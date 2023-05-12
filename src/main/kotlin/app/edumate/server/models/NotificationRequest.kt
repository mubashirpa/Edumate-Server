package app.edumate.server.models

import kotlinx.serialization.Serializable

@Serializable
data class NotificationRequest(
    val title: String,
    val description: String,
    val userIds: List<String>
)