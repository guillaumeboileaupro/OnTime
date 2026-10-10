package fr.ontime.app.data.sncf

import fr.ontime.domain.Mode
import fr.ontime.domain.Quality
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SncfTripParsingTest {
    private val fetchedAt = Instant.parse("2026-10-10T10:00:00Z")

    @Test
    fun `keeps only direct supported trains with arrival time`() {
        val body = requireNotNull(javaClass.getResource("/sncf/journeys.json")).readText()
        val departures = assertNotNull(parseSncfJourneys(body, "stop_area:DEMO:A", fetchedAt))

        val train = departures.single()
        assertEquals("vehicle_journey:DEMO:1", train.journeyId)
        assertEquals("stop_area:DEMO:A", train.stopId)
        assertEquals("line:DEMO:1", train.lineId)
        assertEquals("route:DEMO:1", train.directionId)
        assertEquals(Mode.Train, train.mode)
        assertEquals(Quality.Realtime, train.quality)
        assertEquals(Instant.parse("2026-10-10T10:29:00Z"), train.departureAt)
        assertEquals(Instant.parse("2026-10-10T10:56:00Z"), train.arrivalAt)
    }

    @Test
    fun `no journey is an empty answer and malformed bodies are errors`() {
        assertEquals(emptyList(), parseSncfJourneys("""{"context":{"timezone":"Europe/Paris"}}""", "a", fetchedAt))
        assertNull(parseSncfJourneys("oops", "a", fetchedAt))
        assertNull(parseSncfJourneys("""{"journeys":[]}""", "a", fetchedAt))
    }

    @Test
    fun `lists every route terminus once, without the origin`() {
        val body = """{"routes":[
            {"id":"r1","direction":{"id":"stop_area:B","name":"Bêta (Commune)","embedded_type":"stop_area"}},
            {"id":"r2","direction":{"id":"stop_area:A","name":"Origine","embedded_type":"stop_area"}},
            {"id":"r3","direction":{"id":"stop_area:B","name":"Bêta (Commune)","embedded_type":"stop_area"}},
            {"id":"r4","direction":{"id":"stop_area:C","name":"Alpha","embedded_type":"stop_area"}}]}"""
        assertEquals(
            listOf(NearbyStation("stop_area:C", "Alpha"), NearbyStation("stop_area:B", "Bêta (Commune)")),
            parseRouteDirections(body, "stop_area:A"),
        )
        assertNull(parseRouteDirections("oops", "stop_area:A"))
    }
}
