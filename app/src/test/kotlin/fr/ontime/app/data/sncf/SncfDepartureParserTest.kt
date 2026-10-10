package fr.ontime.app.data.sncf

import fr.ontime.domain.Mode
import fr.ontime.domain.Quality
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SncfDepartureParserTest {
    private val fetchedAt = Instant.parse("2026-10-10T09:55:00Z")
    private val fixture = requireNotNull(javaClass.getResource("/sncf/departures.json")).readText()

    @Test
    fun `maps supported trains with local time zone and freshness`() {
        val departures = assertNotNull(parseSncfDepartures(fixture, fetchedAt))

        assertEquals(listOf("vehicle_journey:DEMO:800001", "vehicle_journey:DEMO:6001"), departures.map { it.journeyId })
        val ter = departures.first()
        assertEquals(SNCF_PROVIDER, ter.provider)
        assertEquals("stop_area:DEMO:SA:1", ter.stopId)
        assertEquals("line:DEMO:L1", ter.lineId)
        assertEquals("route:DEMO:R1", ter.directionId)
        assertEquals("Destination fictive A", ter.destination)
        assertEquals(Mode.Train, ter.mode)
        assertEquals(Instant.parse("2026-10-10T10:23:00Z"), ter.departureAt)
        assertEquals(Quality.Realtime, ter.quality)
        assertEquals(fetchedAt, ter.fetchedAt)
        assertEquals(Quality.Scheduled, departures.last().quality)
    }

    @Test
    fun `returns an empty list when no departure is published`() {
        val body = """{"context":{"timezone":"Europe/Paris"},"departures":[]}"""
        assertEquals(emptyList(), parseSncfDepartures(body, fetchedAt))
    }

    @Test
    fun `rejects malformed bodies instead of guessing`() {
        listOf(
            "not-json",
            """{"departures":[]}""",
            """{"context":{"timezone":"Europe/Paris"}}""",
            fixture.replace("\"vehicle_journey\", \"id\": \"vehicle_journey:DEMO:800001\"", "\"other\", \"id\": \"x\""),
            fixture.replace("20261010T122300", "2026-10-10 12:23"),
        ).forEach { assertNull(parseSncfDepartures(it, fetchedAt), it.take(60)) }
    }
}
