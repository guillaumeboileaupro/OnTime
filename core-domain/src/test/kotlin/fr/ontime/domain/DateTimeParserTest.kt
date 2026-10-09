package fr.ontime.domain

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DateTimeParserTest {
    @Test
    fun `converts epoch and leap years exactly`() {
        assertEquals(Instant.EPOCH, parseDateTime("1970-01-01T00:00:00Z").instant)
        assertEquals(Instant.ofEpochSecond(-1), parseDateTime("1969-12-31T23:59:59Z").instant)
        assertEquals(1709210096, parseDateTime("2024-02-29T12:34:56Z").instant?.epochSecond)
        assertEquals(951827696, parseDateTime("2000-02-29T12:34:56Z").instant?.epochSecond)
    }

    @Test
    fun `normalizes midnight offsets and fractions`() {
        equivalent("2026-01-01T00:15:00+01:00", "2025-12-31T23:15:00Z")
        equivalent("2026-07-01T12:00:00-04:00", "2026-07-01T16:00:00Z")
        equivalent("2026-07-01T16:00:00.123Z", "2026-07-01T18:00:00.999+02:00")
    }

    @Test
    fun `uses explicit Paris offsets around daylight saving transitions`() {
        equivalent("2026-03-29T01:30:00+01:00", "2026-03-29T00:30:00Z")
        equivalent("2026-03-29T03:30:00+02:00", "2026-03-29T01:30:00Z")
        val summer = parseDateTime("2026-10-25T02:30:00+02:00").instant!!
        val winter = parseDateTime("2026-10-25T02:30:00+01:00").instant!!
        assertEquals(3600, winter.epochSecond - summer.epochSecond)
    }

    @Test
    fun `rejects invalid values without exposing an instant`() {
        val cases = listOf(
            null to DateTimeError.InvalidFormat,
            "" to DateTimeError.InvalidFormat,
            "2026-01-01 00:00:00Z" to DateTimeError.InvalidFormat,
            "2026-01-01T00:00:00" to DateTimeError.InvalidOffset,
            "2026-01-01T00:00:00z" to DateTimeError.InvalidOffset,
            "2026-01-01T00:00:00.Z" to DateTimeError.InvalidFormat,
            "2026-02-29T00:00:00Z" to DateTimeError.InvalidDate,
            "2024-02-30T00:00:00Z" to DateTimeError.InvalidDate,
            "1900-02-29T00:00:00Z" to DateTimeError.InvalidDate,
            "2100-02-29T00:00:00Z" to DateTimeError.InvalidDate,
            "2026-01-01T24:00:00Z" to DateTimeError.InvalidDate,
            "2026-01-01T00:00:60Z" to DateTimeError.InvalidDate,
            "2026-01-01T00:00:00+15:00" to DateTimeError.InvalidOffset,
            "2026-01-01T00:00:00+14:01" to DateTimeError.InvalidOffset,
        )
        cases.forEach { (value, expected) ->
            val result = parseDateTime(value)
            assertFalse(result.ok)
            assertEquals(null, result.instant)
            assertEquals(expected, result.error)
        }
    }

    private fun equivalent(first: String, second: String) {
        val firstResult = parseDateTime(first)
        val secondResult = parseDateTime(second)
        assertTrue(firstResult.ok)
        assertTrue(secondResult.ok)
        assertEquals(firstResult.instant, secondResult.instant)
    }
}
