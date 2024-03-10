package app.edumate.server.models.classroom.courseWork

import app.edumate.server.models.classroom.AssigneeMode
import app.edumate.server.models.classroom.IndividualStudentsOptions
import app.edumate.server.models.classroom.Material
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table

@Serializable
data class CourseWork(
    val alternateLink: String? = null,
    val assigneeMode: AssigneeMode? = null,
    val assignment: Assignment? = null, // Union field details can be only one
    val associatedWithDeveloper: Boolean? = null,
    val courseId: String? = null,
    val creationTime: String? = null,
    val creatorUserId: String? = null,
    val description: String? = null,
    val dueDate: DueDate? = null,
    val dueTime: DueTime? = null,
    val gradeCategory: GradeCategory? = null,
    val id: String? = null,
    val individualStudentsOptions: IndividualStudentsOptions? = null,
    val materials: List<Material>? = null,
    val maxPoints: Int? = null,
    val multipleChoiceQuestion: MultipleChoiceQuestion? = null, // Union field details can be only one
    val scheduledTime: String? = null,
    val state: CourseWorkState? = null,
    val submissionModificationMode: SubmissionModificationMode? = null,
    val title: String? = null,
    val topicId: String? = null,
    val updateTime: String? = null,
    val workType: CourseWorkType? = null,
)

object CourseWorks : Table() {
    val alternateLink = varchar("alternateLink", 512).nullable()
    val assigneeMode = enumerationByName("assigneeMode", 128, AssigneeMode::class).nullable()

    // assignment

    val associatedWithDeveloper = bool("associatedWithDeveloper").nullable()
    val courseId = varchar("courseId", 128).nullable()
    val creationTime = varchar("creationTime", 128).nullable()
    val creatorUserId = varchar("creatorUserId", 128).nullable()
    val description = varchar("description", 1024).nullable()

    // dueDate
    // dueTime
    // gradeCategory

    val id = varchar("id", 128).nullable()

    // individualStudentOptions
    // materials

    val maxPoints = integer("maxPoints").nullable()

    // multipleChoiceQuestion

    val scheduledTime = varchar("scheduledTime", 128).nullable()
    val state = enumerationByName("state", 128, CourseWorkState::class).nullable()
    val submissionModificationMode =
        enumerationByName("submissionModificationMode", 128, SubmissionModificationMode::class).nullable()
    val title = varchar("title", 128).nullable()
    val topicId = varchar("topicId", 128).nullable()
    val updateTime = varchar("updateTime", 128).nullable()
    val workType = enumerationByName("workType", 128, CourseWorkType::class).nullable()
}
