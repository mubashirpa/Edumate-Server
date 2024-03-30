package app.edumate.server.models.classroom.studentSubmissions

enum class SubmissionState {
    CREATED,
    RECLAIMED_BY_STUDENT,
    RETURNED,
    STATE_UNSPECIFIED,
    STUDENT_EDITED_AFTER_TURN_IN,
    TURNED_IN,
}
