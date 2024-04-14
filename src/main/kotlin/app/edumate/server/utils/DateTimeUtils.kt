package app.edumate.server.utils

import app.edumate.server.models.classroom.courseWork.DueDate
import app.edumate.server.models.classroom.courseWork.TimeOfDay
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

object DateTimeUtils {
    fun getCurrentDateTime(pattern: String): String {
        val currentDateTime = LocalDateTime.now(ZoneOffset.UTC)
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return currentDateTime.format(formatter)
    }

    fun stringToDate(
        dateString: String,
        pattern: String,
    ): LocalDateTime {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        val dateTime = LocalDateTime.parse(dateString, formatter)
        return dateTime
    }

    fun parseDueDateTime(
        dueDate: DueDate,
        dueTime: TimeOfDay,
    ): Date? {
        if (dueDate.day != null && dueDate.month != null && dueDate.year != null) {
            val calendar =
                Calendar.getInstance().apply {
                    set(dueDate.year, dueDate.month - 1, dueDate.day) // month is 0-based in Calendar
                }

            if (dueTime.hours != null && dueTime.minutes != null) {
                calendar.set(Calendar.HOUR_OF_DAY, dueTime.hours)
                calendar.set(Calendar.MINUTE, dueTime.minutes)

                dueTime.seconds?.let { calendar.set(Calendar.SECOND, it) }
                dueTime.nanos?.let { calendar.set(Calendar.MILLISECOND, it / 1_000_000) }
            }

            return calendar.time
        }

        return null
    }

    fun isPast(date: Date): Boolean {
        val currentDate = Date()
        return date.before(currentDate)
    }
}
