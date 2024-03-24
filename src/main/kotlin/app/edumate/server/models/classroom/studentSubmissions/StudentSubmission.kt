package app.edumate.server.models.classroom.studentSubmissions

import app.edumate.server.models.classroom.courseWork.CourseWorkType
import kotlinx.serialization.Serializable

@Serializable
data class StudentSubmission(
    val alternateLink: String? = null,
    val assignedGrade: Int? = null,
    val assignmentSubmission: AssignmentSubmission? = null, // Union field details can be only one
    val associatedWithDeveloper: Boolean? = null,
    val courseId: String? = null,
    val courseWorkId: String? = null,
    val courseWorkType: CourseWorkType? = null,
    val creationTime: String? = null,
    val draftGrade: Int? = null,
    val id: String? = null,
    val late: Boolean? = null,
    val multipleChoiceSubmission: MultipleChoiceSubmission? = null, // Union field details can be only one
    val shortAnswerSubmission: ShortAnswerSubmission? = null, // Union field details can be only one
    val state: SubmissionState? = null,
    val updateTime: String? = null,
    val userId: String? = null,
)
