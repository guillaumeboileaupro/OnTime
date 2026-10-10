package fr.ontime.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RemindersTest {
    private val paris = ZoneId.of("Europe/Paris")
    private val weekdays = ReminderWindow(
        setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
        LocalTime.of(7, 30), LocalTime.of(9, 0),
    )
    private val trip = TravelProfile("t", "stop_area:A", "stop_area:B", 10, 2)

    private fun departure(id: String, at: String, cancelled: Boolean = false) = Departure(
        provider = "test", journeyId = id, stopId = "stop_area:A", lineId = "l", directionId = "r",
        destination = "B", mode = Mode.Train, departureAt = Instant.parse(at),
        fetchedAt = Instant.parse("2026-10-12T05:00:00Z"), quality = Quality.Realtime, cancelled = cancelled,
    )

    @Test
    fun `plans thirty minutes before the next open weekday window`() {
        // Saturday 10 Oct 2026, 10:00 Paris -> Monday 12 Oct 07:00 Paris (05:00 UTC).
        assertEquals(
            Instant.parse("2026-10-12T05:00:00Z"),
            weekdays.nextPlanningAt(Instant.parse("2026-10-10T08:00:00Z"), paris),
        )
        // Monday 08:00 Paris, inside the window: plan immediately.
        val inside = Instant.parse("2026-10-12T06:00:00Z")
        assertEquals(inside, weekdays.nextPlanningAt(inside, paris))
    }

    @Test
    fun `respects daylight saving time and invalid windows`() {
        // Sunday 25 Oct 2026 switches to winter time: Monday 07:30 Paris is 06:30 UTC, planning 06:00 UTC.
        assertEquals(
            Instant.parse("2026-10-26T06:00:00Z"),
            weekdays.nextPlanningAt(Instant.parse("2026-10-25T12:00:00Z"), paris),
        )
        assertNull(ReminderWindow(emptySet(), LocalTime.of(7, 0), LocalTime.of(8, 0)).nextPlanningAt(Instant.EPOCH, paris))
        assertNull(weekdays.copy(start = LocalTime.of(9, 0), end = LocalTime.of(8, 0)).nextPlanningAt(Instant.EPOCH, paris))
    }

    @Test
    fun `picks the first train whose leave time is inside the window`() {
        val tooEarly = departure("early", "2026-10-12T05:35:00Z") // leave 07:23 Paris
        val inside = departure("ok", "2026-10-12T05:50:00Z") // leave 07:38 Paris
        assertEquals(inside, weekdays.pickDeparture(listOf(inside, tooEarly), trip, paris))
        assertEquals(Instant.parse("2026-10-12T05:33:00Z"), trip.reminderAt(inside))
    }

    @Test
    fun `postpones for a late train and notifies when due`() {
        val planned = departure("j1", "2026-10-12T06:00:00Z")
        val late = departure("j1", "2026-10-12T06:10:00Z")
        val now = Instant.parse("2026-10-12T05:43:00Z")
        assertEquals(
            ReminderDecision.Postpone(Instant.parse("2026-10-12T05:53:00Z")),
            decideReminder(planned, trip, listOf(late), listOf(late), now),
        )
        assertEquals(
            ReminderDecision.Notify(planned, confirmed = true, replacement = false),
            decideReminder(planned, trip, listOf(planned), listOf(planned), now),
        )
    }

    @Test
    fun `replaces a cancelled train, flags missing data and never invents one`() {
        val planned = departure("j1", "2026-10-12T06:00:00Z")
        val next = departure("j2", "2026-10-12T06:20:00Z")
        val now = Instant.parse("2026-10-12T05:43:00Z")
        assertEquals(
            ReminderDecision.Notify(next, confirmed = true, replacement = true),
            decideReminder(planned, trip, listOf(departure("j1", "2026-10-12T06:00:00Z", cancelled = true), next), listOf(next), now),
        )
        assertEquals(
            ReminderDecision.Notify(planned, confirmed = false, replacement = false),
            decideReminder(planned, trip, null, emptyList(), now),
        )
        assertEquals(ReminderDecision.NoTrain, decideReminder(planned, trip, emptyList(), emptyList(), now))
    }
}
