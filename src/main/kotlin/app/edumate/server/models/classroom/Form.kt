package app.edumate.server.models.classroom

import kotlinx.serialization.Serializable

@Serializable
data class Form(
    val formUrl: String? = null,
    val responseUrl: String? = null,
    val thumbnailUrl: String? = null,
    val title: String? = null,
)
