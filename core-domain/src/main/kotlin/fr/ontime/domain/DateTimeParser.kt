package fr.ontime.domain

import java.time.DateTimeException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class DateTimeError { None, InvalidFormat, InvalidDate, InvalidOffset }

data class DateTimeResult(
    val instant: Instant? = null,
    val error: DateTimeError = DateTimeError.None,
) {
    val ok: Boolean get() = instant != null && error == DateTimeError.None
}

private val dateTimePrefix = Regex(
    "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?",
)
private val explicitOffset = Regex("^(?:Z|[+-]\\d{2}:\\d{2})$")

fun parseDateTime(value: String?): DateTimeResult {
    if (value == null) return DateTimeResult(error = DateTimeError.InvalidFormat)
    val prefix = dateTimePrefix.find(value)
        ?: return DateTimeResult(error = DateTimeError.InvalidFormat)
    val suffix = value.substring(prefix.range.last + 1)
    if (!explicitOffset.matches(suffix)) {
        val error = if (suffix.startsWith('.')) {
            DateTimeError.InvalidFormat
        } else {
            DateTimeError.InvalidOffset
        }
        return DateTimeResult(error = error)
    }
    if (suffix != "Z") {
        val offsetHour = suffix.substring(1, 3).toInt()
        val offsetMinute = suffix.substring(4, 6).toInt()
        if (offsetHour > 14 || offsetMinute > 59 ||
            (offsetHour == 14 && offsetMinute != 0)
        ) {
            return DateTimeResult(error = DateTimeError.InvalidOffset)
        }
    }

    return try {
        // The shared contract ignores sub-second precision. Removing the
        // already-validated fraction also accepts provider precision longer
        // than java.time's nanosecond limit, as the C++ parser does.
        val normalized = value.substring(0, 19) + suffix
        val parsed = OffsetDateTime.parse(normalized, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        if (parsed.year < 1) {
            DateTimeResult(error = DateTimeError.InvalidDate)
        } else {
            DateTimeResult(parsed.toInstant().truncatedTo(ChronoUnit.SECONDS))
        }
    } catch (_: DateTimeException) {
        DateTimeResult(error = DateTimeError.InvalidDate)
    }
}
