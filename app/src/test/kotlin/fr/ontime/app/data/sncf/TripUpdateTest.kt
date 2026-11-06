package fr.ontime.app.data.sncf

import fr.ontime.domain.Departure
import fr.ontime.domain.Mode
import fr.ontime.domain.Quality
import fr.ontime.domain.Status
import fr.ontime.domain.TravelProfile
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class TripUpdateTest {
    // Issue 26 example: 10 min to the station, train at 19:55 Paris (18:55 UTC in November).
    private val trip = TravelProfile("t", "stop:a", "stop:b", walkingMinutes = 10, marginMinutes = 0)
    private val train = Instant.parse("2026-11-06T18:55:00Z")
    private val later = train.plusSeconds(30 * 60)

    private fun departure(id: String, at: Instant, now: Instant) = Departure(
        provider = "sncf", journeyId = id, stopId = "stop:a", lineId = "line:1",
        directionId = "route:1", destination = "Terminus", mode = Mode.Train,
        departureAt = at, fetchedAt = now.minusSeconds(10), quality = Quality.Realtime, cancelled = false,
    )

    @Test
    fun `the 19h55 train stays listed at 19h45 and 19h50 with a 10 minute walk`() {
        listOf(10L, 5L, 0L).forEach { minutesBefore ->
            val now = train.minusSeconds(minutesBefore * 60)
            val update = buildTripUpdate(
                trip, Status.Available, listOf(departure("1955", train, now), departure("2025", later, now)), now,
            )
            val tight = if (minutesBefore == 10L) emptyList() else listOf("1955")
            val recommended = if (minutesBefore == 10L) "1955" else "2025"
            assertEquals(recommended, update.selection.departure?.journeyId, "at -$minutesBefore min")
            assertEquals(tight, update.tight.map { it.journeyId }, "at -$minutesBefore min")
        }
    }

    @Test
    fun `a tight train is shown even when nothing is reachable from home`() {
        val now = train.minusSeconds(4 * 60)
        val update = buildTripUpdate(trip, Status.Available, listOf(departure("1955", train, now)), now)
        assertEquals(Status.Empty, update.selection.status)
        assertEquals(listOf("1955"), update.tight.map { it.journeyId })
    }

    @Test
    fun `no tight train is offered from stale or failed data`() {
        val now = train.minusSeconds(4 * 60)
        val stale = departure("1955", train, now).copy(fetchedAt = now.minusSeconds(600))
        assertEquals(emptyList(), buildTripUpdate(trip, Status.Available, listOf(stale), now).tight)
        assertEquals(emptyList(), buildTripUpdate(trip, Status.Error, listOf(departure("1955", train, now)), now).tight)
    }
}
