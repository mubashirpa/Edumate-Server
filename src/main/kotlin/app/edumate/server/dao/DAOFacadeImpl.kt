package app.edumate.server.dao

import app.edumate.server.dao.DatabaseSingleton.dbQuery
import app.edumate.server.models.classroom.courseWork.CourseWork
import app.edumate.server.models.classroom.courseWork.CourseWorks
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class DAOFacadeImpl : DAOFacade {
    override suspend fun createCourseWork(courseWork: CourseWork): CourseWork? =
        dbQuery {
            val insertStatement =
                CourseWorks.insert {
                    it[alternateLink] = courseWork.alternateLink
                    it[assigneeMode] = courseWork.assigneeMode
                    it[associatedWithDeveloper] = courseWork.associatedWithDeveloper
                    it[courseId] = courseWork.courseId
                    it[creationTime] = courseWork.creationTime
                    it[creatorUserId] = courseWork.creatorUserId
                    it[description] = courseWork.description
                    it[id] = courseWork.id
                    it[maxPoints] = courseWork.maxPoints
                    it[scheduledTime] = courseWork.scheduledTime
                    it[state] = courseWork.state
                    it[submissionModificationMode] = courseWork.submissionModificationMode
                    it[title] = courseWork.title
                    it[topicId] = courseWork.topicId
                    it[updateTime] = courseWork.updateTime
                    it[workType] = courseWork.workType
                }
            insertStatement.resultedValues?.singleOrNull()?.let(::resultRowToCourseWork)
        }

    override suspend fun deleteCourseWork(id: String): Boolean =
        dbQuery {
            CourseWorks.deleteWhere { CourseWorks.id eq id } > 0
        }

    override suspend fun getCourseWork(id: String): CourseWork? =
        dbQuery {
            CourseWorks
                .selectAll().where { CourseWorks.id eq id }
                .map(::resultRowToCourseWork)
                .singleOrNull()
        }

    override suspend fun listCourseWorks(courseId: String): List<CourseWork> =
        dbQuery {
            CourseWorks
                .selectAll().where { CourseWorks.courseId eq courseId }
                .map(::resultRowToCourseWork)
        }

    override suspend fun listAll(): List<CourseWork> =
        dbQuery {
            CourseWorks.selectAll().map(::resultRowToCourseWork)
        }

    override suspend fun patchCourseWork(
        id: String,
        courseWork: CourseWork,
    ): Boolean =
        dbQuery {
            CourseWorks.update({ CourseWorks.id eq courseWork.id }) {
                it[title] = courseWork.title
                it[description] = courseWork.description
                it[state] = courseWork.state
                it[maxPoints] = courseWork.maxPoints
                it[scheduledTime] = courseWork.scheduledTime
                it[submissionModificationMode] = courseWork.submissionModificationMode
                it[topicId] = courseWork.topicId
                it[updateTime] = courseWork.updateTime
            } > 0
        }

    private fun resultRowToCourseWork(row: ResultRow) =
        CourseWork(
            alternateLink = row[CourseWorks.alternateLink],
            assigneeMode = row[CourseWorks.assigneeMode],
            associatedWithDeveloper = row[CourseWorks.associatedWithDeveloper],
            courseId = row[CourseWorks.courseId],
            creationTime = row[CourseWorks.creationTime],
            creatorUserId = row[CourseWorks.creatorUserId],
            description = row[CourseWorks.description],
            id = row[CourseWorks.id],
            maxPoints = row[CourseWorks.maxPoints],
            scheduledTime = row[CourseWorks.scheduledTime],
            state = row[CourseWorks.state],
            submissionModificationMode = row[CourseWorks.submissionModificationMode],
            title = row[CourseWorks.title],
            topicId = row[CourseWorks.topicId],
            updateTime = row[CourseWorks.updateTime],
            workType = row[CourseWorks.workType],
        )
}
