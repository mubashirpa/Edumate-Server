package app.edumate.server.core.utils

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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
}
