package fr.ontime.domain

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TravelProfileTest {
    @Test
    fun `validates required fields and duration bounds`() {
        val errors = ProfileDraft(" ", "", -1, 61).validationErrors()
        assertEquals(
            setOf(
                ProfileError.EmptyStop,
                ProfileError.EmptyDestination,
                ProfileError.InvalidWalking,
                ProfileError.InvalidMargin,
            ),
            errors,
        )
        assertTrue(ProfileDraft("stop", "destination", 180, 60).validationErrors().isEmpty())
        assertEquals(
            setOf(ProfileError.SameStopAndDestination),
            ProfileDraft("stop", " stop ", 10, 2).validationErrors(),
        )
    }

    @Test
    fun `calculates home departure with injected clock`() {
        val now = Instant.parse("2026-10-09T06:30:00Z")
        val profile = TravelProfile("p1", "stop", "destination", 7, 2)
        val result = HomeDepartureCalculator(Clock.fixed(now, ZoneOffset.UTC))
            .calculate(Instant.parse("2026-10-09T06:42:00Z"), profile)

        assertEquals(Instant.parse("2026-10-09T06:33:00Z"), result.leaveAt)
        assertEquals(Duration.ofMinutes(3), result.timeUntilLeave)
    }
}
