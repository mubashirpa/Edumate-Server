package app.edumate.server.models.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    @SerialName("included_segments")
    val includedSegments: List<String>,
    @SerialName("include_external_user_ids")
    val includeExternalUserIds: List<String>,
    val contents: NotificationMessage,
    val headings: NotificationMessage,
    @SerialName("app_id")
    val appId: String,
)
