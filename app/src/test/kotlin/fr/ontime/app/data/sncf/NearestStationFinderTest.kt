package fr.ontime.app.data.sncf

import fr.ontime.domain.RequestBudget
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NearestStationFinderTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-10T09:55:00Z"), ZoneOffset.UTC)
    private val nearby = resource("places_nearby.json")
    private val walking = resource("walking_journey.json")

    private fun resource(name: String) = requireNotNull(javaClass.getResource("/sncf/$name")).readText()

    private fun finder(vararg answers: SncfResponse, paths: MutableList<String> = mutableListOf()): NearestStationFinder {
        val queue = ArrayDeque(answers.toList())
        return NearestStationFinder({ paths += it; queue.removeFirst() }, RequestBudget(), clock)
    }

    @Test
    fun `returns the closest station with street walking time rounded up`() {
        val paths = mutableListOf<String>()
        val result = finder(SncfResponse.Body(nearby), SncfResponse.Body(walking), paths = paths).find(43.5, 7.25)

        assertEquals(
            NearestStationResult.Found(NearbyStation("stop_area:DEMO:SA:1", "Gare fictive"), 21, 1307),
            result,
        )
        assertTrue(paths[0].startsWith("/coord/7.25000;43.50000/places_nearby?"))
        assertTrue(paths[1].contains("to=stop_area%3ADEMO%3ASA%3A1"))
        assertTrue(paths[1].contains("direct_path=only"))
    }

    @Test
    fun `formats coordinates with a dot whatever the device locale`() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.FRANCE)
        try {
            val paths = mutableListOf<String>()
            finder(SncfResponse.Body(nearby), SncfResponse.Body(walking), paths = paths).find(43.5, 7.25)
            assertTrue(paths[0].startsWith("/coord/7.25000;43.50000/"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `distinguishes no station, too far, busy and errors`() {
        assertEquals(NearestStationResult.NoStation, finder(SncfResponse.Body("""{"places_nearby":[]}""")).find(43.5, 7.25))
        val far = walking.replace("\"duration\": 1224, \"tags\"", "\"duration\": 10861, \"tags\"")
        assertEquals(NearestStationResult.TooFar, finder(SncfResponse.Body(nearby), SncfResponse.Body(far)).find(43.5, 7.25))
        assertEquals(NearestStationResult.Error, finder(SncfResponse.Unauthorized).find(43.5, 7.25))
        assertEquals(NearestStationResult.Error, finder(SncfResponse.Body(nearby), SncfResponse.Body("""{"journeys":[]}""")).find(43.5, 7.25))
        assertEquals(NearestStationResult.Error, finder().find(91.0, 7.25))

        val budget = RequestBudget()
        budget.tryAcquire(clock.instant())
        val busy = NearestStationFinder({ error("no call") }, budget, clock)
        assertEquals(NearestStationResult.Busy, busy.find(43.5, 7.25))
    }

    @Test
    fun `ignores journeys that are not walking only`() {
        val mixed = """{"journeys":[{"duration":600,"distances":{"walking":100},
            "sections":[{"type":"public_transport","mode":"","duration":600}]}]}"""
        assertEquals(null, parseWalking(mixed))
    }
}
