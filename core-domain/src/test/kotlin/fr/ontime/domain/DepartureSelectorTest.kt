package fr.ontime.domain

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class DepartureSelectorTest {
    private val now = Instant.parse("2026-10-09T06:00:00Z")

    @Test
    fun `selects earliest reachable supplied departure`() {
        val late = departure("late", 600)
        val cancelled = departure("cancelled", 120, cancelled = true)
        val first = departure("first", 300, quality = Quality.Scheduled)
        val result = selectNextDeparture(
            listOf(late, cancelled, first), now,
            Duration.ofSeconds(60), Duration.ofSeconds(20), Duration.ofSeconds(120),
        )
        assertEquals(Status.Available, result.status)
        assertSame(first, result.departure)
    }

    @Test
    fun `never invents a service after missed departures`() {
        val result = selectNextDeparture(
            listOf(departure("missed-1", 10), departure("missed-2", 20)), now,
            Duration.ofSeconds(60), Duration.ZERO, Duration.ofSeconds(120),
        )
        assertEquals(Status.Empty, result.status)
        assertNull(result.departure)
    }

    @Test
    fun `distinguishes fresh empty stale and error`() {
        val stale = departure("stale", 600, fetchedSecondsAgo = 400)
        val freshMissed = departure("fresh-missed", -10)
        assertEquals(
            Status.Empty,
            selectNextDeparture(
                listOf(stale, freshMissed), now, Duration.ZERO, Duration.ZERO,
                Duration.ofSeconds(120),
            ).status,
        )
        assertEquals(
            Status.Stale,
            selectNextDeparture(
                listOf(stale), now, Duration.ZERO, Duration.ZERO, Duration.ofSeconds(120),
            ).status,
        )
        assertEquals(
            Status.Error,
            selectNextDeparture(
                listOf(departure("future", 600, fetchedSecondsAgo = -1)), now,
                Duration.ZERO, Duration.ZERO, Duration.ofSeconds(120),
            ).status,
        )
        assertEquals(Status.Empty, selectNextDeparture(emptyList(), now, Duration.ZERO, Duration.ZERO, Duration.ZERO).status)
    }

    @Test
    fun `handles cancellation estimated quality and exact threshold`() {
        val cancelled = departure("cancelled", 300, cancelled = true)
        val estimated = departure("estimated", 300, quality = Quality.Estimated)
        assertEquals(
            Status.Empty,
            selectNextDeparture(
                listOf(cancelled, estimated), now, Duration.ZERO, Duration.ZERO,
                Duration.ofSeconds(120),
            ).status,
        )
        val boundary = departure("boundary", 90)
        val available = selectNextDeparture(
            listOf(boundary), now, Duration.ofSeconds(60), Duration.ofSeconds(30),
            Duration.ofSeconds(120),
        )
        assertEquals(Status.Available, available.status)
        assertSame(boundary, available.departure)
    }

    @Test
    fun `propagates source states and invalid identities`() {
        assertEquals(
            Status.Error,
            selectNextDeparture(
                emptyList(), now, Duration.ZERO, Duration.ZERO, Duration.ZERO, Status.Error,
            ).status,
        )
        assertEquals(
            Status.Error,
            selectNextDeparture(
                listOf(departure("invalid", 300).copy(provider = "")), now,
                Duration.ZERO, Duration.ZERO, Duration.ofSeconds(120),
            ).status,
        )
        assertEquals(
            Status.Error,
            selectNextDeparture(
                listOf(departure("invalid-departure", 300).copy(departureAt = Instant.ofEpochSecond(-1))),
                now, Duration.ZERO, Duration.ZERO, Duration.ofSeconds(120),
            ).status,
        )
        assertEquals(
            Status.Error,
            selectNextDeparture(
                listOf(departure("invalid-fetch", 300).copy(fetchedAt = Instant.ofEpochSecond(-1))),
                now, Duration.ZERO, Duration.ZERO, Duration.ofSeconds(120),
            ).status,
        )
        assertEquals(
            Status.Error,
            selectNextDeparture(
                emptyList(), Instant.ofEpochSecond(-1), Duration.ZERO, Duration.ZERO, Duration.ZERO,
            ).status,
        )
    }

    private fun departure(
        id: String,
        secondsFromNow: Long,
        fetchedSecondsAgo: Long = 20,
        quality: Quality = Quality.Realtime,
        cancelled: Boolean = false,
    ) = Departure(
        provider = "demo",
        journeyId = id,
        stopId = "demo:stop",
        lineId = "demo:line",
        directionId = "demo:direction:outbound",
        destination = "Destination fictive",
        mode = Mode.Rer,
        departureAt = now.plusSeconds(secondsFromNow),
        fetchedAt = now.minusSeconds(fetchedSecondsAgo),
        quality = quality,
        cancelled = cancelled,
    )
}
