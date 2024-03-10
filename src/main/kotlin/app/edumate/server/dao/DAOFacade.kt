package app.edumate.server.dao

import app.edumate.server.models.classroom.courseWork.CourseWork

interface DAOFacade {
    suspend fun createCourseWork(courseWork: CourseWork): CourseWork?

    suspend fun deleteCourseWork(id: String): Boolean

    suspend fun getCourseWork(id: String): CourseWork?

    suspend fun listCourseWorks(courseId: String): List<CourseWork>

    suspend fun listAll(): List<CourseWork>

    suspend fun patchCourseWork(
        id: String,
        courseWork: CourseWork,
    ): Boolean
}
