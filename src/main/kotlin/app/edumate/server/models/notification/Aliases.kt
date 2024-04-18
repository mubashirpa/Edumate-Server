package app.edumate.server.models.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Aliases(
    @SerialName("external_id")
    val externalId: List<String>,
)
