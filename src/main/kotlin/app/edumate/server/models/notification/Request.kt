package app.edumate.server.models.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Request(
    @SerialName("app_id")
    val appId: String,
    @SerialName("included_segments")
    val includedSegments: List<String>,
    @SerialName("include_aliases")
    val includeAliases: Aliases,
    @SerialName("target_channel")
    val targetChannel: String,
    val contents: Message,
    val headings: Message,
)
