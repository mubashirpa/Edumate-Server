package app.edumate.server.models.classroom.courses

import app.edumate.server.models.classroom.GradeCategory
import kotlinx.serialization.Serializable

@Serializable
data class GradebookSettings(
    val calculationType: CalculationType? = null,
    val displaySetting: DisplaySetting? = null,
    val gradeCategories: List<GradeCategory>? = null,
)
