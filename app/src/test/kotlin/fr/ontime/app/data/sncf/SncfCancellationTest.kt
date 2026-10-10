package fr.ontime.app.data.sncf

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SncfCancellationTest {
    private val fetchedAt = Instant.parse("2026-10-10T13:00:00Z")

    private fun journey(number: String, dep: String, base: String = dep, disruption: String? = null) = """
        {"sections":[{"type":"public_transport","data_freshness":"realtime",
          "departure_date_time":"$dep","base_departure_date_time":"$base","arrival_date_time":"20261010T170000",
          "display_informations":{"direction":"Terminus","headsign":"$number"},
          "links":[{"type":"vehicle_journey","id":"vj:$number"},{"type":"line","id":"line:1"},
                   {"type":"route","id":"route:1"},{"type":"physical_mode","id":"physical_mode:Train"}
                   ${disruption?.let { ",{\"type\":\"disruption\",\"id\":\"$it\"}" } ?: ""}]}]}"""

    private fun body(vararg journeys: String, disruptions: String = "[]") =
        """{"context":{"timezone":"Europe/Paris"},"journeys":[${journeys.joinToString(",")}],"disruptions":$disruptions}"""

    @Test
    fun `reads delay and disruption message`() {
        val disruptions = """[{"id":"d1","messages":[{"text":"Panne d'une installation"}]}]"""
        val train = assertNotNull(parseSncfTrains(body(journey("881", "20261010T155100", "20261010T154100", "d1"), disruptions = disruptions), "a", fetchedAt)).single()
        assertEquals("881", train.number)
        assertEquals(10L, train.departure.delayMinutes)
        assertEquals("Panne d'une installation", train.departure.disruption)
    }

    @Test
    fun `a timetabled train missing from real time is cancelled, only inside the covered period`() {
        val realtime = assertNotNull(parseSncfTrains(body(journey("1", "20261010T153100"), journey("3", "20261010T161500")), "a", fetchedAt))
        val timetable = assertNotNull(
            parseSncfTrains(body(journey("1", "20261010T153100"), journey("2", "20261010T160000"), journey("3", "20261010T161500"), journey("4", "20261010T170000")), "a", fetchedAt),
        )
        val merged = mergeCancellations(realtime, timetable)

        assertEquals(listOf("vj:1", "vj:2", "vj:3"), merged.map { it.journeyId })
        val removed = merged[1]
        assertTrue(removed.cancelled)
        assertEquals(Instant.parse("2026-10-10T14:00:00Z"), removed.departureAt)
        assertEquals(listOf(false, true, false), merged.map { it.cancelled })
    }
}
